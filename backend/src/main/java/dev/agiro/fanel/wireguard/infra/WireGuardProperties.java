package dev.agiro.fanel.wireguard.infra;

import dev.agiro.fanel.wireguard.domain.Ipv4Cidr;
import dev.agiro.fanel.wireguard.domain.IpAddress;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Validated
@ConfigurationProperties(prefix = "fanel.wireguard")
public class WireGuardProperties {
    private boolean enabled;
    private String interfaceName = "wg0";
    private String network = "10.8.0.0/24";
    private String serverAddress = "10.8.0.1";
    private String endpoint;
    private String lanBaseUrl;
    private String tunnelBaseUrl = "https://10.8.0.1:8443";
    private String allowedIpsForClient = "10.8.0.0/24";
    private int persistentKeepalive = 25;
    private boolean usePresharedKey = true;
    private String pskEncryptionKey;
    private String wgCommand = "/usr/bin/wg";
    private java.time.Duration commandTimeout = java.time.Duration.ofSeconds(5);
    private java.time.Duration pairingTokenTtl = java.time.Duration.ofMinutes(10);
    private String tlsSpkiSha256;
    private UUID serverId;
    private java.time.Duration reconcileInterval = java.time.Duration.ofMinutes(5);
    private boolean removeUnknownPeers;
    private boolean enrollPrivateOnly;
    private List<String> trustedProxies = new ArrayList<>();
    private int enrollRateLimitPerMinute = 5;
    private boolean enforceTunnelPeer = true;
    private boolean allowAdminFromTunnel;

    public void validateEnabledConfiguration() {
        if (!enabled) return;
        List<String> errors = new ArrayList<>();
        required(endpoint, "endpoint", errors);
        required(lanBaseUrl, "lan-base-url", errors);
        required(tunnelBaseUrl, "tunnel-base-url", errors);
        validateEndpoint(errors);
        validateBaseUrl(lanBaseUrl, "lan-base-url", errors);
        validateBaseUrl(tunnelBaseUrl, "tunnel-base-url", errors);
        if (serverId == null) errors.add("server-id must be a UUID");
        if (!is32ByteBase64(tlsSpkiSha256)) errors.add("tls-spki-sha256 must be base64 encoding of 32 bytes");
        if (usePresharedKey && !is32ByteBase64(pskEncryptionKey)) {
            errors.add("psk-encryption-key must be base64 encoding of 32 bytes when use-preshared-key is true");
        }
        if (enrollRateLimitPerMinute <= 0) errors.add("enroll-rate-limit-per-minute must be positive");
        try {
            Ipv4Cidr cidr = Ipv4Cidr.parse(network);
            if (cidr.prefix() > 30) errors.add("network prefix must be at most 30");
            if (serverAddress == null || !cidr.contains(serverAddress)) {
                errors.add("server-address must be inside network");
            }
        } catch (IllegalArgumentException exception) {
            errors.add("network must be an IPv4 CIDR with prefix at most 30");
        }
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid fanel.wireguard configuration: " + String.join("; ", errors));
        }
    }

    private static boolean is32ByteBase64(String value) {
        try {
            return value != null && java.util.Base64.getDecoder().decode(value).length == 32;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static void required(String value, String name, List<String> errors) {
        if (value == null || value.isBlank()) errors.add(name + " is required");
    }

    private void validateEndpoint(List<String> errors) {
        if (endpoint == null || endpoint.isBlank()) return;
        if (!endpoint.matches("^(\\[[0-9a-fA-F:]+]|[A-Za-z0-9.-]+):[0-9]{1,5}$")) {
            errors.add("endpoint must be a hostname or IP followed by a UDP port");
            return;
        }
        int port = Integer.parseInt(endpoint.substring(endpoint.lastIndexOf(':') + 1));
        if (port < 1 || port > 65535) errors.add("endpoint port must be between 1 and 65535");
    }

    private static void validateBaseUrl(String value, String name, List<String> errors) {
        if (value == null || value.isBlank()) return;
        try {
            java.net.URI uri = java.net.URI.create(value);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getPort() > 65535 || uri.getPort() == 0
                    || uri.getUserInfo() != null) {
                errors.add(name + " must be an absolute HTTP(S) URL with a valid host and port");
            }
        } catch (IllegalArgumentException exception) {
            errors.add(name + " must be an absolute HTTP(S) URL with a valid host and port");
        }
    }

    public boolean isPrivateOnlyAddress(String address) {
        try {
            byte[] bytes = IpAddress.parseNumeric(address);
            java.net.InetAddress ip = java.net.InetAddress.getByAddress(bytes);
            if (ip.isLoopbackAddress() || ip.isLinkLocalAddress()) return true;
            if (bytes.length == 4) {
                int a = Byte.toUnsignedInt(bytes[0]);
                int b = Byte.toUnsignedInt(bytes[1]);
                return a == 10 || (a == 172 && b >= 16 && b <= 31)
                        || (a == 192 && b == 168);
            }
            return (Byte.toUnsignedInt(bytes[0]) & 0xfe) == 0xfc;
        } catch (Exception exception) {
            return false;
        }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getInterfaceName() { return interfaceName; }
    public void setInterfaceName(String interfaceName) { this.interfaceName = interfaceName; }
    public String getNetwork() { return network; }
    public void setNetwork(String network) { this.network = network; }
    public String getServerAddress() { return serverAddress; }
    public void setServerAddress(String serverAddress) { this.serverAddress = serverAddress; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getLanBaseUrl() { return lanBaseUrl; }
    public void setLanBaseUrl(String lanBaseUrl) { this.lanBaseUrl = lanBaseUrl; }
    public String getTunnelBaseUrl() { return tunnelBaseUrl; }
    public void setTunnelBaseUrl(String tunnelBaseUrl) { this.tunnelBaseUrl = tunnelBaseUrl; }
    public String getAllowedIpsForClient() { return allowedIpsForClient; }
    public void setAllowedIpsForClient(String allowedIpsForClient) { this.allowedIpsForClient = allowedIpsForClient; }
    public int getPersistentKeepalive() { return persistentKeepalive; }
    public void setPersistentKeepalive(int persistentKeepalive) { this.persistentKeepalive = persistentKeepalive; }
    public boolean isUsePresharedKey() { return usePresharedKey; }
    public void setUsePresharedKey(boolean usePresharedKey) { this.usePresharedKey = usePresharedKey; }
    public String getPskEncryptionKey() { return pskEncryptionKey; }
    public void setPskEncryptionKey(String pskEncryptionKey) { this.pskEncryptionKey = pskEncryptionKey; }
    public String getWgCommand() { return wgCommand; }
    public void setWgCommand(String wgCommand) { this.wgCommand = wgCommand; }
    public java.time.Duration getCommandTimeout() { return commandTimeout; }
    public void setCommandTimeout(java.time.Duration commandTimeout) { this.commandTimeout = commandTimeout; }
    public java.time.Duration getPairingTokenTtl() { return pairingTokenTtl; }
    public void setPairingTokenTtl(java.time.Duration pairingTokenTtl) { this.pairingTokenTtl = pairingTokenTtl; }
    public String getTlsSpkiSha256() { return tlsSpkiSha256; }
    public void setTlsSpkiSha256(String tlsSpkiSha256) { this.tlsSpkiSha256 = tlsSpkiSha256; }
    public UUID getServerId() { return serverId; }
    public void setServerId(UUID serverId) { this.serverId = serverId; }
    public java.time.Duration getReconcileInterval() { return reconcileInterval; }
    public void setReconcileInterval(java.time.Duration reconcileInterval) { this.reconcileInterval = reconcileInterval; }
    public boolean isRemoveUnknownPeers() { return removeUnknownPeers; }
    public void setRemoveUnknownPeers(boolean removeUnknownPeers) { this.removeUnknownPeers = removeUnknownPeers; }
    public boolean isEnrollPrivateOnly() { return enrollPrivateOnly; }
    public void setEnrollPrivateOnly(boolean enrollPrivateOnly) { this.enrollPrivateOnly = enrollPrivateOnly; }
    public List<String> getTrustedProxies() { return trustedProxies; }
    public void setTrustedProxies(List<String> trustedProxies) { this.trustedProxies = trustedProxies == null ? new ArrayList<>() : trustedProxies; }
    public int getEnrollRateLimitPerMinute() { return enrollRateLimitPerMinute; }
    public void setEnrollRateLimitPerMinute(int enrollRateLimitPerMinute) { this.enrollRateLimitPerMinute = enrollRateLimitPerMinute; }
    public boolean isEnforceTunnelPeer() { return enforceTunnelPeer; }
    public void setEnforceTunnelPeer(boolean enforceTunnelPeer) { this.enforceTunnelPeer = enforceTunnelPeer; }
    public boolean isAllowAdminFromTunnel() { return allowAdminFromTunnel; }
    public void setAllowAdminFromTunnel(boolean allowAdminFromTunnel) { this.allowAdminFromTunnel = allowAdminFromTunnel; }
}
