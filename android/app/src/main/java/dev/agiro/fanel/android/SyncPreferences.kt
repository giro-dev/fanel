package dev.agiro.fanel.android

import android.content.Context
import android.util.AtomicFile
import dev.agiro.fanel.android.sync.HouseholdSyncKey
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

class SyncPreferences(context: Context) {
    private val baseDir = context.noBackupFilesDir

    @Synchronized
    fun getLastSuccessfulSync(householdId: String): Long {
        val atomicFile = atomicFile(householdId)
        return try {
            atomicFile.openRead().use { input ->
                input.readBytes().toString(StandardCharsets.UTF_8).toLongOrNull()
            } ?: 0L
        } catch (_: IOException) {
            0L
        }
    }

    @Synchronized
    fun setLastSuccessfulSync(householdId: String, timestampMillis: Long) {
        val atomicFile = atomicFile(householdId)
        val output = atomicFile.startWrite()
        try {
            output.write(timestampMillis.toString().toByteArray(StandardCharsets.UTF_8))
            atomicFile.finishWrite(output)
        } catch (exception: Exception) {
            atomicFile.failWrite(output)
            throw exception
        }
    }

    internal fun syncFile(householdId: String): File =
        File(baseDir, "last-successful-sync-${HouseholdSyncKey.opaqueKey(householdId)}.txt")

    private fun atomicFile(householdId: String): AtomicFile = AtomicFile(syncFile(householdId))
}
