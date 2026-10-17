package dev.agiro.fanel.android.wireguard

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException

const val LOGICAL_BASE_URL = "https://fanel.invalid/"

class BaseUrlInterceptor(
    private val holder: ConnectionStateHolder,
    private val config: () -> WgConfig?,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.scheme != "https" || request.url.host != "fanel.invalid") {
            throw IOException("Paired requests must use the logical Fanel URL")
        }
        val current = config() ?: throw IOException("No paired WireGuard configuration")
        val base = when (holder.state.value.mode) {
            ConnectionMode.LAN -> current.lanBaseUrl
            ConnectionMode.REMOTE -> current.tunnelBaseUrl
            else -> throw IOException("Fanel server is not reachable")
        }.toHttpUrlOrNull() ?: throw IOException("Invalid paired server URL")
        val url = request.url.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .build()
        return chain.proceed(request.newBuilder().url(url).build())
    }
}

class RetryOnModeChangeInterceptor(
    private val holder: ConnectionStateHolder,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val initialMode = holder.state.value.mode
        return try {
            chain.proceed(original)
        } catch (exception: IOException) {
            if (holder.state.value.mode == initialMode) throw exception
            chain.proceed(original)
        }
    }
}
