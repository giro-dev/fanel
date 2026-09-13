package dev.agiro.fanel.android.sync

import android.content.Context

interface SyncSchedulerContract {
    fun enqueuePeriodic(context: Context, householdId: String)
    fun enqueueImmediate(context: Context, householdId: String, from: String, to: String)
}
