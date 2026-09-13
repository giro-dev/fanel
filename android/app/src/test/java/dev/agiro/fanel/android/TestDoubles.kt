package dev.agiro.fanel.android

import dev.agiro.fanel.android.sync.HouseholdEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

object EmptyHouseholdEvents : HouseholdEvents {
    override fun observe(householdId: String): Flow<String> = emptyFlow()
}
