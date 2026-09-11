package dev.agiro.fanel.android

import okhttp3.Request
import java.util.Base64

class AuthStore {
    private var username: String? = null
    private var password: String? = null
    private var bearerToken: String? = null

    fun setBasicCredentials(username: String, password: String) {
        this.username = username
        this.password = password
        this.bearerToken = null
    }

    fun setBearerToken(token: String) {
        this.bearerToken = token
        this.username = null
        this.password = null
    }

    fun clear() {
        username = null
        password = null
        bearerToken = null
    }

    fun apply(builder: Request.Builder) {
        if (!bearerToken.isNullOrBlank()) {
            builder.header("Authorization", "Bearer " + bearerToken)
            return
        }
        if (!username.isNullOrBlank() && password != null) {
            val encoded = Base64.getEncoder().encodeToString("$username:$password".toByteArray())
            builder.header("Authorization", "Basic $encoded")
        }
    }
}
