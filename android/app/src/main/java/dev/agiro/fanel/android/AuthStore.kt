package dev.agiro.fanel.android

import android.content.Context
import okhttp3.Request
import java.util.Base64

class AuthStore(context: Context) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    fun setBasicCredentials(username: String, password: String) {
        prefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, password)
            .remove(KEY_BEARER)
            .apply()
    }

    fun setBearerToken(token: String) {
        prefs.edit()
            .putString(KEY_BEARER, token)
            .remove(KEY_USERNAME)
            .remove(KEY_PASSWORD)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun apply(builder: Request.Builder) {
        val bearer = prefs.getString(KEY_BEARER, null)
        if (!bearer.isNullOrBlank()) {
            builder.header("Authorization", "Bearer " + bearer)
            return
        }
        val username = prefs.getString(KEY_USERNAME, null)
        val password = prefs.getString(KEY_PASSWORD, null)
        if (!username.isNullOrBlank() && password != null) {
            val encoded = Base64.getEncoder().encodeToString("$username:$password".toByteArray())
            builder.header("Authorization", "Basic $encoded")
        }
    }

    private companion object {
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
        const val KEY_BEARER = "bearer"
    }
}
