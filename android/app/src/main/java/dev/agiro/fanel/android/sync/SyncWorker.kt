package dev.agiro.fanel.android.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.agiro.fanel.android.FanelApplication

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val householdId = inputData.getString(KEY_HOUSEHOLD_ID).orEmpty()
        val from = inputData.getString(KEY_FROM).orEmpty()
        val to = inputData.getString(KEY_TO).orEmpty()
        if (householdId.isBlank() || from.isBlank() || to.isBlank()) return Result.success()

        return try {
            val repository = (applicationContext as FanelApplication).appContainer.calendarRepository
            repository.fullSync(householdId, from, to)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_HOUSEHOLD_ID = "householdId"
        const val KEY_FROM = "from"
        const val KEY_TO = "to"
    }
}
