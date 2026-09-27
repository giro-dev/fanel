package dev.agiro.fanel.calendar.infra;

import dev.agiro.fanel.shared.net.OutboundUrlPolicy;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class HttpIcsFetcher implements IcsFetcher {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final int MAX_REDIRECTS = 5;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final OutboundUrlPolicy outbound;

    public HttpIcsFetcher(OutboundUrlPolicy outbound) {
        this.outbound = outbound;
    }

    /**
     * Downloads the ICS document, following redirects manually so every hop is re-validated
     * against the public-address policy, and aborting the stream as soon as it exceeds
     * {@code MAX_BYTES} instead of buffering the whole body.
     */
    @Override
    public String fetch(String url) {
        URI current = URI.create(toHttpUrl(url));
        for (int hops = 0; ; hops++) {
            outbound.requireHttp(current.toString());
            HttpResponse<InputStream> response = send(current);
            int status = response.statusCode();
            String location = response.headers().firstValue("Location").orElse(null);
            if (status >= 300 && status < 400 && location != null) {
                closeQuietly(response.body());
                if (hops >= MAX_REDIRECTS) {
                    throw new IllegalArgumentException("Too many redirects fetching calendar");
                }
                current = current.resolve(location);
                continue;
            }
            if (status / 100 != 2) {
                closeQuietly(response.body());
                throw new IllegalArgumentException("HTTP " + status + " fetching calendar");
            }
            return readBounded(response.body());
        }
    }

    private HttpResponse<InputStream> send(URI uri) {
        try {
            return client.send(
                    HttpRequest.newBuilder(uri).timeout(TIMEOUT).GET().build(),
                    HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not fetch the calendar: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("Interrupted while fetching the calendar", e);
        }
    }

    private String readBounded(InputStream in) {
        try (in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            long total = 0;
            while ((read = in.read(chunk)) != -1) {
                total += read;
                if (total > MAX_BYTES) {
                    throw new IllegalArgumentException("ICS document exceeds 5 MB");
                }
                out.write(chunk, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the calendar: " + e.getMessage(), e);
        }
    }

    private void closeQuietly(InputStream in) {
        try {
            in.close();
        } catch (IOException ignored) {
        }
    }

    /** Normalizes and validates a subscription URL: webcal:// is rewritten to https://. */
    public static String toHttpUrl(String url) {
        if (url == null) throw new IllegalArgumentException("URL is required");
        String normalized = url.startsWith("webcal://") ? "https://" + url.substring("webcal://".length()) : url;
        String scheme;
        try {
            scheme = URI.create(normalized).getScheme();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid calendar URL: " + url);
        }
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Only http(s) calendar URLs are supported");
        }
        return normalized;
    }
}
