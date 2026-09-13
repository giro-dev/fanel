package dev.agiro.fanel.android.sync

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object HouseholdSyncKey {
    fun opaqueKey(householdId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(householdId.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(16)
    }
}
