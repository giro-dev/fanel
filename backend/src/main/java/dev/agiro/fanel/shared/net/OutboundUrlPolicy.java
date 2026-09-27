package dev.agiro.fanel.shared.net;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * Guard for user-supplied outbound URLs: only http(s) URLs are allowed, and by default the
 * host must resolve exclusively to public addresses, blocking SSRF against loopback,
 * link-local and private ranges. Resolution happens on every call so the DNS answer is
 * checked for each connection attempt. Set {@code FANEL_OUTBOUND_ALLOW_PRIVATE_HOSTS=true}
 * on deployments that legitimately fetch feeds from the local network.
 */
@Component
public class OutboundUrlPolicy {

    private final boolean allowPrivateHosts;

    public OutboundUrlPolicy(
            @Value("${fanel.outbound.allow-private-hosts:false}") boolean allowPrivateHosts) {
        this.allowPrivateHosts = allowPrivateHosts;
    }

    /** Parses the URL, requires http(s) and rejects disallowed hosts after DNS resolution. */
    public URI requireHttp(String url) {
        URI uri = parseHttp(url);
        assertAllowedHost(uri);
        return uri;
    }

    /** Parses the URL and requires an http(s) scheme with a host. */
    public static URI parseHttp(String url) {
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid URL");
        }
        if (uri.getScheme() == null
                || !(uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"))
                || uri.getHost() == null) {
            throw new IllegalArgumentException("Only http(s) URLs are supported");
        }
        return uri;
    }

    /** Resolves the host and rejects the URL unless every resolved address is allowed. */
    public void assertAllowedHost(URI uri) {
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(uri.getHost());
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Host cannot be resolved: " + uri.getHost());
        }
        for (InetAddress address : addresses) {
            if (!isAllowed(address)) {
                throw new IllegalArgumentException("URL points to a non-public address");
            }
        }
    }

    private boolean isAllowed(InetAddress address) {
        return allowPrivateHosts || isPublic(address);
    }

    static boolean isPublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int b0 = bytes[0] & 0xff;
            int b1 = bytes[1] & 0xff;
            return b0 != 0                                  // 0.0.0.0/8
                    && !(b0 == 100 && (b1 & 0xc0) == 64)    // CGNAT 100.64.0.0/10
                    && !(b0 == 198 && (b1 & 0xfe) == 18)    // benchmarking 198.18.0.0/15
                    && b0 < 224;                            // multicast + reserved 240.0.0.0/4
        }
        return (bytes[0] & 0xfe) != 0xfc;                   // IPv6 unique local fc00::/7
    }

    /** Keeps scheme, host and port; drops user-info, path and query where feed tokens live. */
    public static String redact(String url) {
        try {
            URI uri = URI.create(url);
            StringBuilder sb = new StringBuilder();
            if (uri.getScheme() != null) {
                sb.append(uri.getScheme()).append("://");
            }
            sb.append(uri.getHost() != null ? uri.getHost() : "***");
            if (uri.getPort() > 0) {
                sb.append(':').append(uri.getPort());
            }
            return sb.toString();
        } catch (Exception e) {
            return "***";
        }
    }
}
