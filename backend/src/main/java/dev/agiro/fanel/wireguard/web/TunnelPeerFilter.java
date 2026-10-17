package dev.agiro.fanel.wireguard.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.agiro.fanel.wireguard.domain.ActivePeerAddresses;
import dev.agiro.fanel.wireguard.domain.Ipv4Cidr;
import dev.agiro.fanel.wireguard.infra.WireGuardProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import java.io.IOException;

@Component
public class TunnelPeerFilter extends OncePerRequestFilter {
    private final WireGuardProperties properties;
    private final ActivePeerAddresses activeAddresses;
    private final MessageSource messages;
    private final ObjectMapper mapper = new ObjectMapper();

    public TunnelPeerFilter(WireGuardProperties properties, ActivePeerAddresses activeAddresses,
                            MessageSource messages) {
        this.properties = properties;
        this.activeAddresses = activeAddresses;
        this.messages = messages;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!properties.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }
        String remote = request.getRemoteAddr();
        if (!Ipv4Cidr.parse(properties.getNetwork()).contains(remote)
                || remote.equals(properties.getServerAddress())) {
            chain.doFilter(request, response);
            return;
        }
        if (request.getRequestURI().startsWith("/api/v1/admin/")
                && !properties.isAllowAdminFromTunnel()) {
            reject(response, HttpStatus.FORBIDDEN, message("wireguard.tunnel.admin.forbidden"));
            return;
        }
        if (properties.isEnforceTunnelPeer() && !activeAddresses.contains(remote)) {
            reject(response, HttpStatus.FORBIDDEN, message("wireguard.tunnel.peer.inactive"));
            return;
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        mapper.writeValue(response.getOutputStream(), problem);
    }

    private String message(String key) {
        return messages.getMessage(key, null, LocaleContextHolder.getLocale());
    }
}
