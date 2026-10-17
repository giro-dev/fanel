package dev.agiro.fanel.wireguard.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import dev.agiro.fanel.shared.security.CurrentAccess;
import dev.agiro.fanel.shared.web.ForbiddenException;
import dev.agiro.fanel.wireguard.api.*;
import dev.agiro.fanel.wireguard.infra.WireGuardPairingTokenRepository;
import dev.agiro.fanel.wireguard.infra.WireGuardPeerRepository;
import dev.agiro.fanel.wireguard.infra.WireGuardProperties;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class WireGuardPeerService {
    private static final Logger audit = LoggerFactory.getLogger("fanel.audit.wireguard");
    private final WireGuardProperties properties;
    private final WireGuardAdapter adapter;
    private final WireGuardPeerRepository peers;
    private final WireGuardPairingTokenRepository tokens;
    private final CurrentAccess access;
    private final Clock clock;
    private final TransactionTemplate transactions;
    private final ActivePeerAddresses activeAddresses;
    private final SecureRandom random = new SecureRandom();
    private final ReentrantLock enrollmentLock = new ReentrantLock();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PskCipher cipher;

    public WireGuardPeerService(WireGuardProperties properties, WireGuardAdapter adapter,
                                WireGuardPeerRepository peers, WireGuardPairingTokenRepository tokens,
                                CurrentAccess access, Clock wireGuardClock,
                                PlatformTransactionManager transactionManager,
                                ActivePeerAddresses activeAddresses) {
        this.properties = properties;
        this.adapter = adapter;
        this.peers = peers;
        this.tokens = tokens;
        this.access = access;
        this.clock = wireGuardClock;
        this.transactions = new TransactionTemplate(transactionManager);
        this.activeAddresses = activeAddresses;
        this.cipher = properties.isEnabled() && properties.isUsePresharedKey()
                ? new PskCipher(properties.getPskEncryptionKey()) : null;
    }

    public PairingTokenResponse createPairingToken() {
        requireEnabled();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.getPairingTokenTtl());
        String hash = sha256(token);
        tokens.save(new WireGuardPairingToken(hash, expiresAt, access.memberId().orElse(null), now));
        String payload = pairingPayload(token);
        audit.info("pairing token created by {}", access.memberId().map(UUID::toString).orElse("global"));
        return new PairingTokenResponse(payload, qrCode(payload), expiresAt);
    }

    public EnrollResponse enroll(EnrollRequest request, String clientIp) {
        requireEnabled();
        String serverKey;
        try {
            serverKey = adapter.serverPublicKey();
        } catch (RuntimeException exception) {
            audit.warn("enroll rejected category=interface-unavailable client={}", clientIp);
            throw new WireGuardHttpException(HttpStatus.SERVICE_UNAVAILABLE, "WireGuard interface is unavailable");
        }
        if (serverKey == null || serverKey.isBlank()) {
            audit.warn("enroll rejected category=interface-unavailable client={}", clientIp);
            throw new WireGuardHttpException(HttpStatus.SERVICE_UNAVAILABLE, "WireGuard interface is unavailable");
        }
        enrollmentLock.lock();
        try {
            EnrollmentAttempt attempt = null;
            for (int retry = 0; retry < 2; retry++) {
                try {
                    attempt = transactions.execute(status -> enrollTransaction(request));
                    break;
                } catch (DataIntegrityViolationException exception) {
                    if (retry == 1) {
                        audit.warn("enroll rejected category=database-conflict client={}", clientIp);
                        throw new WireGuardHttpException(HttpStatus.CONFLICT, "Peer key or address is already in use");
                    }
                }
            }
            if (attempt == null || attempt.error() != null) {
                WireGuardHttpException error = attempt == null
                        ? new WireGuardHttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Enrollment failed")
                        : attempt.error();
                audit.warn("enroll rejected category={} client={}", rejectionCategory(error), clientIp);
                throw error;
            }
            WireGuardPeer peer = attempt.peer();
            String psk = attempt.plainPsk();
            try {
                adapter.addPeer(peer.getPublicKey(), peer.getAssignedIp() + "/32", psk);
            } catch (RuntimeException exception) {
                audit.warn("enroll rejected category=runtime-configuration client={}", clientIp);
                transactions.executeWithoutResult(status -> {
                    WireGuardPeer stored = peers.findById(peer.getId()).orElseThrow();
                    stored.revoke(clock.instant());
                    peers.save(stored);
                });
                refreshActiveAddresses();
                audit.error("peer enrollment failed after persistence id={}", peer.getId(), exception);
                throw new WireGuardHttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not configure WireGuard peer");
            }
            refreshActiveAddresses();
            audit.info("peer enrolled id={} owner={}", peer.getId(),
                    peer.getOwnerMemberId() == null ? "global" : peer.getOwnerMemberId());
            return new EnrollResponse(peer.getId().toString(), peer.getAssignedIp() + "/32",
                    serverKey, psk, properties.getEndpoint(), properties.getAllowedIpsForClient(),
                    properties.getPersistentKeepalive(), properties.getTunnelBaseUrl(), properties.getLanBaseUrl());
        } finally {
            enrollmentLock.unlock();
        }
    }

    private EnrollmentAttempt enrollTransaction(EnrollRequest request) {
        Instant now = clock.instant();
        String tokenValue = request == null ? null : request.token();
        String hash = tokenValue == null ? sha256("") : sha256(tokenValue);
        if (tokens.consume(hash, now) == 0) {
            return EnrollmentAttempt.error(new WireGuardHttpException(HttpStatus.UNAUTHORIZED, "Pairing token is invalid or expired"));
        }
        WireGuardPairingToken token = tokens.findByTokenHash(hash).orElse(null);
        try {
            if (request == null) throw new IllegalArgumentException("Enrollment request is required");
            WgKeys.validatePublicKey(request.publicKey());
            if (request.deviceName() == null || request.deviceName().trim().isEmpty()
                    || request.deviceName().trim().length() > 100) {
                throw new IllegalArgumentException("Device name must contain 1 to 100 characters");
            }
            if (peers.findByPublicKey(request.publicKey()).isPresent()) {
                return EnrollmentAttempt.error(new WireGuardHttpException(HttpStatus.CONFLICT, "Peer key is already enrolled"));
            }
            String ip = firstAvailableAddress();
            if (ip == null) {
                pruneUnusedRevokedPeers();
                ip = firstAvailableAddress();
            }
            if (ip == null) return EnrollmentAttempt.error(new WireGuardHttpException(HttpStatus.CONFLICT, "No WireGuard addresses remain"));
            String plainPsk = null;
            String encryptedPsk = null;
            if (properties.isUsePresharedKey()) {
                byte[] pskBytes = new byte[32];
                random.nextBytes(pskBytes);
                plainPsk = Base64.getEncoder().encodeToString(pskBytes);
                encryptedPsk = cipher.encrypt(plainPsk);
            }
            WireGuardPeer peer = peers.saveAndFlush(new WireGuardPeer(
                    request.deviceName().trim(), request.publicKey(), encryptedPsk, ip,
                    token == null ? null : token.getCreatedBy(), now));
            return EnrollmentAttempt.success(peer, plainPsk);
        } catch (IllegalArgumentException exception) {
            return EnrollmentAttempt.error(new WireGuardHttpException(HttpStatus.BAD_REQUEST, exception.getMessage()));
        }
    }

    public List<PeerDto> listPeers() {
        requireEnabled();
        return peers.findAllByOrderByCreatedAtDesc().stream().map(this::dto).toList();
    }

    public void revoke(UUID id) {
        requireEnabled();
        WireGuardPeer peer = peers.findById(id).orElseThrow(
                () -> new WireGuardHttpException(HttpStatus.NOT_FOUND, "WireGuard peer not found"));
        peer.revoke(clock.instant());
        peers.save(peer);
        try {
            adapter.removePeer(peer.getPublicKey());
        } catch (RuntimeException exception) {
            LoggerFactory.getLogger(WireGuardPeerService.class)
                    .error("Could not remove revoked WireGuard peer {}; reconciliation will retry", id, exception);
        }
        refreshActiveAddresses();
        audit.info("peer revoked id={} by={}", id, access.memberId().map(UUID::toString).orElse("global"));
    }

    public void revokeMe(String remoteAddress, String headerPublicKey) {
        requireEnabled();
        Optional<WireGuardPeer> peer;
        if (Ipv4Cidr.parse(properties.getNetwork()).contains(remoteAddress)) {
            peer = peers.findByAssignedIpAndStatus(remoteAddress, "ACTIVE");
        } else {
            peer = peers.findByPublicKey(headerPublicKey).filter(p -> "ACTIVE".equals(p.getStatus()));
        }
        WireGuardPeer found = peer.orElseThrow(
                () -> new WireGuardHttpException(HttpStatus.NOT_FOUND, "WireGuard peer not found"));
        revoke(found.getId());
    }

    public WireGuardStatus status() {
        if (!properties.isEnabled()) {
            return new WireGuardStatus(false, properties.getInterfaceName(), null, false,
                    properties.getEndpoint(), properties.getNetwork());
        }
        String serverKey = adapter.serverPublicKey();
        boolean reachable = false;
        try {
            adapter.listPeers();
            reachable = true;
        } catch (RuntimeException ignored) { }
        return new WireGuardStatus(true, properties.getInterfaceName(), serverKey, reachable,
                properties.getEndpoint(), properties.getNetwork());
    }

    public ServerIdentity identity() {
        requireEnabled();
        return new ServerIdentity(properties.getServerId());
    }

    public void refreshActiveAddresses() {
        if (!properties.isEnabled()) {
            activeAddresses.replace(Set.of());
            return;
        }
        activeAddresses.replace(peers.findAllByStatus("ACTIVE").stream()
                .map(WireGuardPeer::getAssignedIp).collect(Collectors.toSet()));
    }

    public void updateHandshake(String publicKey, Instant handshake) {
        peers.findByPublicKey(publicKey).ifPresent(peer -> {
            if (handshake != null && !handshake.equals(peer.getLastHandshakeAt())) {
                peer.handshake(handshake);
                peers.save(peer);
            }
        });
    }

    public List<WireGuardPeer> allPeers() { return peers.findAll(); }
    public WireGuardPeerRepository repository() { return peers; }
    public WireGuardProperties properties() { return properties; }
    public WireGuardAdapter adapter() { return adapter; }
    public PskCipher cipher() { return cipher; }
    public Instant now() { return clock.instant(); }

    public String decryptPsk(WireGuardPeer peer) {
        return peer.getPresharedKey() == null ? null : cipher.decrypt(peer.getPresharedKey());
    }

    private PeerDto dto(WireGuardPeer peer) {
        return new PeerDto(peer.getId(), peer.getDeviceName(), peer.getPublicKey(), peer.getAssignedIp(),
                peer.getStatus(), peer.getOwnerMemberId(), peer.getCreatedAt(), peer.getRevokedAt(),
                peer.getLastHandshakeAt());
    }

    private String firstAvailableAddress() {
        Ipv4Cidr cidr = Ipv4Cidr.parse(properties.getNetwork());
        Set<String> used = peers.findAll().stream().map(WireGuardPeer::getAssignedIp).collect(Collectors.toSet());
        return WireGuardAddressAllocator.firstAvailable(cidr, properties.getServerAddress(), used).orElse(null);
    }

    private void pruneUnusedRevokedPeers() {
        Set<String> runtimeKeys;
        try {
            runtimeKeys = adapter.listPeers().stream().map(PeerRuntime::publicKey).collect(Collectors.toSet());
        } catch (RuntimeException exception) {
            return;
        }
        peers.findAllByStatus("REVOKED").stream()
                .filter(peer -> !runtimeKeys.contains(peer.getPublicKey()))
                .forEach(peers::delete);
    }

    private String pairingPayload(String token) {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("v", 1);
        json.put("token", token);
        json.put("serverId", properties.getServerId().toString());
        json.put("lanBaseUrl", properties.getLanBaseUrl());
        json.put("tunnelBaseUrl", properties.getTunnelBaseUrl());
        json.put("spkiSha256", properties.getTlsSpkiSha256());
        try {
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    objectMapper.writeValueAsBytes(json));
            return "fanel://pair?d=" + encoded;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create pairing payload", exception);
        }
    }

    private String qrCode(String payload) {
        try {
            Map<EncodeHintType, Object> hints = Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            var matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 512, 512, hints);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create pairing QR code", exception);
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new WireGuardHttpException(HttpStatus.NOT_FOUND, "WireGuard is disabled");
        }
    }

    private static String rejectionCategory(WireGuardHttpException exception) {
        return switch (exception.status()) {
            case UNAUTHORIZED -> "invalid-token";
            case BAD_REQUEST -> "invalid-request";
            case CONFLICT -> exception.getMessage().contains("address") ? "address-exhausted" : "peer-conflict";
            case SERVICE_UNAVAILABLE -> "interface-unavailable";
            default -> "request-rejected";
        };
    }

    private record EnrollmentAttempt(WireGuardPeer peer, String plainPsk, WireGuardHttpException error) {
        static EnrollmentAttempt success(WireGuardPeer peer, String psk) { return new EnrollmentAttempt(peer, psk, null); }
        static EnrollmentAttempt error(WireGuardHttpException exception) { return new EnrollmentAttempt(null, null, exception); }
    }
}
