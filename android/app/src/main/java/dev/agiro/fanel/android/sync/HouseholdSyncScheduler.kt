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
import java.time.LocalDate
import java.util.concurrent.TimeUnit

object HouseholdSyncScheduler : SyncSchedulerContract {
    override fun enqueuePeriodic(context: Context, householdId: String) {
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

    override fun enqueueImmediate(context: Context, householdId: String) {
        if (householdId.isBlank()) return
        enqueueImmediate(context, householdId, defaultInputData(householdId))
    }

    override fun enqueueImmediate(context: Context, householdId: String, from: String, to: String) {
        if (householdId.isBlank() || from.isBlank() || to.isBlank()) return
        val inputData = Data.Builder()
            .putString(SyncWorker.KEY_HOUSEHOLD_ID, householdId)
            .putString(SyncWorker.KEY_FROM, from)
            .putString(SyncWorker.KEY_TO, to)
            .build()
        enqueueImmediate(context, householdId, inputData)
    }

    private fun enqueueImmediate(context: Context, householdId: String, inputData: Data) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(defaultConstraints())
            .setInputData(inputData)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueImmediateWorkName(householdId),
            ExistingWorkPolicy.APPEND_OR_REPLACE,
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
            .putString(SyncWorker.KEY_FROM, today.minusMonths(3).toString())
            .putString(SyncWorker.KEY_TO, today.plusMonths(12).toString())
            .build()
    }

    private fun uniqueWorkName(householdId: String): String = "household_sync_${HouseholdSyncKey.opaqueKey(householdId)}"

    private fun uniqueImmediateWorkName(householdId: String): String = "household_sync_now_${HouseholdSyncKey.opaqueKey(householdId)}"
}
