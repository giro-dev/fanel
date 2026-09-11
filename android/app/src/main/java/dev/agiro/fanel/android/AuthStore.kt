package dev.agiro.fanel.android

import okhttp3.Request
import java.util.Base64

class AuthStore {
    @Volatile
    private var authHeader: String? = null

    fun setBasicCredentials(username: String, password: String) {
        val encoded = Base64.getEncoder().encodeToString("$username:$password".toByteArray())
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
