package dev.agiro.fanel.android

import dev.agiro.fanel.android.data.CalendarRepositoryContract
import dev.agiro.fanel.android.data.offline.ChoresRepository
import dev.agiro.fanel.android.data.offline.MembersRepository
import dev.agiro.fanel.android.data.offline.MenuRepository
import dev.agiro.fanel.android.data.offline.RecipesRepository
import dev.agiro.fanel.android.data.offline.ShoppingRepository
import dev.agiro.fanel.android.data.remote.AssistantApi
import dev.agiro.fanel.android.data.remote.ChoresApi
import dev.agiro.fanel.android.data.remote.HouseholdApi
import dev.agiro.fanel.android.data.remote.MenuApi
import dev.agiro.fanel.android.data.remote.RecipesApi
import dev.agiro.fanel.android.data.remote.ShoppingApi
import dev.agiro.fanel.android.sync.HouseholdEvents
import dev.agiro.fanel.android.sync.HouseholdSyncContract

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
    val membersRepository: MembersRepository
    val recipesRepository: RecipesRepository
    val menuRepository: MenuRepository
    val shoppingRepository: ShoppingRepository
    val choresRepository: ChoresRepository
    val householdSync: HouseholdSyncContract
    fun refreshConnection()
}
