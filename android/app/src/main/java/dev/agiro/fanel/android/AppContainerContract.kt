package dev.agiro.fanel.android

import dev.agiro.fanel.android.data.CalendarRepositoryContract

interface AppContainerContract {
    val authStore: AuthStore
    val calendarRepository: CalendarRepositoryContract
}
