package dev.agiro.fanel.android

import android.util.Base64
import okhttp3.Request
import java.nio.charset.StandardCharsets

class AuthStore {
    @Volatile
    private var authHeader: String? = null

    fun setBasicCredentials(username: String, password: String) {
        val encoded = Base64.encodeToString(
            "$username:$password".toByteArray(StandardCharsets.UTF_8),
            Base64.NO_WRAP
        )
        authHeader = "Basic $encoded"
    }

    fun setBearerToken(token: String) {
        authHeader = "Bearer " + token
    }

    fun clear() {
        authHeader = null
    }

    fun apply(builder: Request.Builder) {
        authHeader?.let { builder.header("Authorization", it) }
    }
}
