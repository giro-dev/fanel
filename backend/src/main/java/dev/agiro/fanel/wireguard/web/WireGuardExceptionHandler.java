package dev.agiro.fanel.wireguard.web;

import dev.agiro.fanel.wireguard.domain.WireGuardHttpException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import java.net.URI;

@RestControllerAdvice(basePackages = "dev.agiro.fanel.wireguard.web")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WireGuardExceptionHandler {
    private final MessageSource messages;

    public WireGuardExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(WireGuardHttpException.class)
    ProblemDetail handle(WireGuardHttpException exception) {
        String key = switch (exception.status()) {
            case BAD_REQUEST -> "wireguard.request.invalid";
            case UNAUTHORIZED -> "wireguard.token.invalid";
            case FORBIDDEN -> "wireguard.forbidden";
            case NOT_FOUND -> exception.getMessage().contains("disabled")
                    ? "wireguard.disabled" : "wireguard.peer.not-found";
            case CONFLICT -> "wireguard.conflict";
            case SERVICE_UNAVAILABLE -> "wireguard.unavailable";
            default -> "wireguard.operation.failed";
        };
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(exception.status(), message(key));
        detail.setTitle(exception.status().getReasonPhrase());
        detail.setType(URI.create("about:blank"));
        return detail;
    }

    @ExceptionHandler(WireGuardController.RateLimitedException.class)
    ResponseEntity<ProblemDetail> rateLimited(WireGuardController.RateLimitedException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                message("wireguard.rate-limit"));
        detail.setTitle("Too Many Requests");
        return ResponseEntity.status(429).header("Retry-After", Long.toString(exception.retryAfter())).body(detail);
    }

    private String message(String key) {
        return messages.getMessage(key, null, LocaleContextHolder.getLocale());
    }
}
