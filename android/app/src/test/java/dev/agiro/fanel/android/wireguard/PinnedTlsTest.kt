package dev.agiro.fanel.android.wireguard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.Base64
import javax.net.ssl.SSLSession

class PinnedTlsTest {
    @Test
    fun trustManagerAcceptsOnlyPinnedLeafCertificate() {
        val good = certificate("wg-good-test-cert.pem")
        val other = certificate("wg-other-test-cert.pem")
        val manager = PinnedTrustManager(Base64.getDecoder().decode(pin(good)))

        manager.checkServerTrusted(arrayOf(good), "RSA")
        org.junit.Assert.assertThrows(CertificateException::class.java) {
            manager.checkServerTrusted(arrayOf(other), "RSA")
        }
        org.junit.Assert.assertThrows(CertificateException::class.java) {
            manager.checkServerTrusted(emptyArray<X509Certificate>(), "RSA")
        }
    }

    @Test
    fun hostnameVerifierMatchesThePinnedCertificateRatherThanTheHostName() {
        val good = certificate("wg-good-test-cert.pem")
        val other = certificate("wg-other-test-cert.pem")
        val verifier = PinnedHostnameVerifier(Base64.getDecoder().decode(pin(good)))

        assertTrue(verifier.verify("unrelated.example", session(good)))
        assertFalse(verifier.verify("unrelated.example", session(other)))
    }

    private fun certificate(name: String): X509Certificate =
        requireNotNull(javaClass.getResourceAsStream("/$name")).use { stream ->
            CertificateFactory.getInstance("X.509").generateCertificate(stream) as X509Certificate
        }

    private fun pin(certificate: X509Certificate): String =
        Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(certificate.publicKey.encoded),
        )

    private fun session(certificate: X509Certificate): SSLSession =
        Proxy.newProxyInstance(
            SSLSession::class.java.classLoader,
            arrayOf(SSLSession::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getPeerCertificates" -> arrayOf(certificate)
                else -> null
            }
        } as SSLSession
}
