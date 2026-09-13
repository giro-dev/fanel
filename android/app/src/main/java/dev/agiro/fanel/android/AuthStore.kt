package dev.agiro.fanel.android

import android.content.Context
import android.util.Base64
import okhttp3.Request
import java.nio.charset.StandardCharsets

class AuthStore(context: Context? = null) {
    private val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var authHeader: String? = loadPersisted()

    val isConfigured: Boolean
        get() = authHeader != null

    fun setBasicCredentials(username: String, password: String) {
        val encoded = Base64.encodeToString(
            "$username:$password".toByteArray(StandardCharsets.UTF_8),
            Base64.NO_WRAP
        )
        authHeader = "Basic $encoded"
        persist()
    }

    fun setBearerToken(token: String) {
        authHeader = "Bearer $token"
        persist()
    }

    fun clear() {
        authHeader = null
        prefs?.edit()?.remove(KEY_HEADER)?.apply()
    }

    fun apply(builder: Request.Builder) {
        authHeader?.let { builder.header("Authorization", it) }
    }

    private fun persist() {
        val sharedPrefs = prefs ?: return
        val header = authHeader ?: return
        try {
            sharedPrefs.edit().putString(KEY_HEADER, CredentialCipher.encrypt(header)).apply()
        } catch (_: Exception) {
            // Keystore unavailable (e.g. emulators without lockscreen setup); keep in-memory only.
        }
    }

    private fun loadPersisted(): String? {
        val sharedPrefs = prefs ?: return null
        val encoded = sharedPrefs.getString(KEY_HEADER, null) ?: return null
        return try {
            CredentialCipher.decrypt(encoded)
        } catch (_: Exception) {
            sharedPrefs.edit().remove(KEY_HEADER).apply()
            null
        }
    }

    private companion object {
        const val PREFS_NAME = "fanel_auth"
        const val KEY_HEADER = "auth_header"
    }
}
