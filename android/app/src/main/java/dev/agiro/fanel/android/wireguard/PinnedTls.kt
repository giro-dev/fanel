package dev.agiro.fanel.android.wireguard

import okhttp3.OkHttpClient
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.Base64
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.net.ssl.HostnameVerifier

class PinnedTrustManager(pin: ByteArray) : X509TrustManager {
    private val expected = pin.copyOf()

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        val leaf = chain?.firstOrNull() ?: throw CertificateException("Empty certificate chain")
        val actual = MessageDigest.getInstance("SHA-256").digest(leaf.publicKey.encoded)
        if (!MessageDigest.isEqual(expected, actual)) {
            throw CertificateException("Server certificate pin mismatch")
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}

class PinnedHostnameVerifier(pin: ByteArray) : HostnameVerifier {
    private val expected = pin.copyOf()

    override fun verify(hostname: String?, session: SSLSession?): Boolean {
        return try {
            val leaf = session?.peerCertificates?.firstOrNull() as? X509Certificate ?: return false
            MessageDigest.isEqual(
                expected,
                MessageDigest.getInstance("SHA-256").digest(leaf.publicKey.encoded),
            )
        } catch (_: Exception) {
            false
        }
    }
}

fun OkHttpClient.Builder.pinned(pinBase64: String): OkHttpClient.Builder {
    val pin = Base64.getDecoder().decode(pinBase64)
    require(pin.size == 32)
    val trustManager = PinnedTrustManager(pin)
    val context = SSLContext.getInstance("TLS")
    context.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
    return sslSocketFactory(context.socketFactory, trustManager)
        .hostnameVerifier(PinnedHostnameVerifier(pin))
}
