package dev.agiro.fanel.android

import dev.agiro.fanel.android.data.CalendarRepositoryContract
import dev.agiro.fanel.android.data.remote.AssistantApi
import dev.agiro.fanel.android.data.remote.ChoresApi
import dev.agiro.fanel.android.data.remote.HouseholdApi
import dev.agiro.fanel.android.data.remote.MenuApi
import dev.agiro.fanel.android.data.remote.RecipesApi
import dev.agiro.fanel.android.data.remote.ShoppingApi
import dev.agiro.fanel.android.sync.HouseholdEvents

interface AppContainerContract {
    val authStore: AuthStore
    val sessionStore: SessionStore
    val householdApi: HouseholdApi
    val recipesApi: RecipesApi
    val menuApi: MenuApi
    val shoppingApi: ShoppingApi
    val choresApi: ChoresApi
    val assistantApi: AssistantApi
    val householdEvents: HouseholdEvents
    val calendarRepository: CalendarRepositoryContract
    fun refreshConnection()
}
