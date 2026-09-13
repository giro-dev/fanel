package dev.agiro.fanel.android.sync

import kotlinx.coroutines.flow.Flow

interface HouseholdEvents {
    fun observe(householdId: String): Flow<String>
}
