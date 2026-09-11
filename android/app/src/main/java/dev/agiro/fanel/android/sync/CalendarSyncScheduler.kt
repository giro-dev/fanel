package dev.agiro.fanel.android.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object CalendarSyncScheduler {
    fun enqueuePeriodic(context: Context, householdId: String) {
        if (householdId.isBlank()) return
        val inputData = defaultInputData(householdId)
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(defaultConstraints())
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            uniqueWorkName(householdId),
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun enqueueImmediate(context: Context, householdId: String) {
        if (householdId.isBlank()) return
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(defaultConstraints())
            .setInputData(defaultInputData(householdId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueImmediateWorkName(householdId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun defaultConstraints(): Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private fun defaultInputData(householdId: String): Data {
        val today = LocalDate.now()
        return Data.Builder()
            .putString(SyncWorker.KEY_HOUSEHOLD_ID, householdId)
            .putString(SyncWorker.KEY_FROM, today.minusMonths(1).toString())
            .putString(SyncWorker.KEY_TO, today.plusMonths(3).toString())
            .build()
    }

    private fun uniqueWorkName(householdId: String): String = "calendar_sync_${householdKey(householdId)}"

    private fun uniqueImmediateWorkName(householdId: String): String = "calendar_sync_now_${householdKey(householdId)}"

    private fun householdKey(householdId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(householdId.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(16)
    }
}
