package dev.agiro.fanel.android.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDate
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
            "calendar_sync",
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
        WorkManager.getInstance(context).enqueue(request)
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
}
