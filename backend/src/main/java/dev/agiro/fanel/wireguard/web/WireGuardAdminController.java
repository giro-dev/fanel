package dev.agiro.fanel.wireguard.web;

import dev.agiro.fanel.shared.security.CurrentAccess;
import dev.agiro.fanel.shared.web.ForbiddenException;
import dev.agiro.fanel.wireguard.api.PairingTokenResponse;
import dev.agiro.fanel.wireguard.api.PeerDto;
import dev.agiro.fanel.wireguard.api.WireGuardStatus;
import dev.agiro.fanel.wireguard.domain.WireGuardPeerService;
import org.springframework.http.ResponseEntity;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/wg")
public class WireGuardAdminController {
    private final WireGuardPeerService service;
    private final CurrentAccess access;
    private final MessageSource messages;
    public WireGuardAdminController(WireGuardPeerService service, CurrentAccess access, MessageSource messages) {
        this.service = service;
        this.access = access;
        this.messages = messages;
    }
    @PostMapping("/pairing-tokens")
    public ResponseEntity<PairingTokenResponse> createToken() {
        requireGlobalAdmin();
        return ResponseEntity.status(201).body(service.createPairingToken());
    }
    @GetMapping("/peers")
    public List<PeerDto> peers() {
        requireGlobalAdmin();
        return service.listPeers();
    }
    @DeleteMapping("/peers/{id}")
    public ResponseEntity<Void> revoke(@PathVariable UUID id) {
        requireGlobalAdmin();
        service.revoke(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/status")
    public WireGuardStatus status() {
        requireGlobalAdmin();
        return service.status();
    }
    private void requireGlobalAdmin() {
        if (!access.hasGlobalAccess()) {
            throw new ForbiddenException(messages.getMessage("wireguard.forbidden", null, LocaleContextHolder.getLocale()));
        }
    }
}
