package dev.agiro.fanel.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.agiro.fanel.android.ui.FanelTheme
import dev.agiro.fanel.android.ui.assistant.AssistantScreen
import dev.agiro.fanel.android.ui.calendar.CalendarScreen
import dev.agiro.fanel.android.ui.chores.ChoresScreen
import dev.agiro.fanel.android.ui.menu.MenuScreen
import dev.agiro.fanel.android.ui.recipes.RecipesScreen
import dev.agiro.fanel.android.ui.settings.SettingsDialog
import dev.agiro.fanel.android.ui.shopping.ShoppingScreen
import dev.agiro.fanel.android.ui.setup.SetupScreen

class MainActivity : ComponentActivity() {
    private val sessionViewModel by viewModels<SessionViewModel> {
        SessionViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FanelTheme {
                FanelApp(sessionViewModel)
            }
        }
    }

    @Composable
    private fun FanelApp(sessionViewModel: SessionViewModel) {
        val session by sessionViewModel.uiState.collectAsStateWithLifecycle()
        var showSettings by remember { mutableStateOf(false) }
        var section by remember { mutableStateOf(AppSection.CALENDAR) }

        if (!session.configured) {
            SetupScreen(sessionViewModel)
        } else {
            key(session.sessionKey) {
                val calendarViewModel: CalendarViewModel = viewModel(
                    key = "calendar|${session.sessionKey}",
                    factory = CalendarViewModel.Factory(application)
                )
                val recipesViewModel: RecipesViewModel = viewModel(
                    key = "recipes|${session.sessionKey}",
                    factory = RecipesViewModel.Factory(application)
                )
                val menuViewModel: MenuViewModel = viewModel(
                    key = "menu|${session.sessionKey}",
                    factory = MenuViewModel.Factory(application)
                )
                val shoppingViewModel: ShoppingViewModel = viewModel(
                    key = "shopping|${session.sessionKey}",
                    factory = ShoppingViewModel.Factory(application)
                )
                val choresViewModel: ChoresViewModel = viewModel(
                    key = "chores|${session.sessionKey}",
                    factory = ChoresViewModel.Factory(application)
                )
                val assistantViewModel: AssistantViewModel = viewModel(
                    key = "assistant|${session.sessionKey}",
                    factory = AssistantViewModel.Factory(application)
                )
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = section == AppSection.CALENDAR,
                                onClick = { section = AppSection.CALENDAR },
                                icon = {
                                    Icon(
                                        Icons.Filled.DateRange,
                                        contentDescription = stringResource(R.string.nav_calendar)
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_calendar)) }
                            )
                            NavigationBarItem(
                                selected = section == AppSection.MENU,
                                onClick = { section = AppSection.MENU },
                                icon = {
                                    Icon(
                                        Icons.Filled.Restaurant,
                                        contentDescription = stringResource(R.string.nav_menu)
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_menu)) }
                            )
                            NavigationBarItem(
                                selected = section == AppSection.RECIPES,
                                onClick = { section = AppSection.RECIPES },
                                icon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.List,
                                        contentDescription = stringResource(R.string.nav_recipes)
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_recipes)) }
                            )
                            NavigationBarItem(
                                selected = section == AppSection.SHOPPING,
                                onClick = { section = AppSection.SHOPPING },
                                icon = {
                                    Icon(
                                        Icons.Filled.ShoppingCart,
                                        contentDescription = stringResource(R.string.nav_shopping)
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_shopping)) }
                            )
                            NavigationBarItem(
                                selected = section == AppSection.CHORES,
                                onClick = { section = AppSection.CHORES },
                                icon = {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = stringResource(R.string.nav_chores)
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_chores)) }
                            )
                            NavigationBarItem(
                                selected = section == AppSection.ASSISTANT,
                                onClick = { section = AppSection.ASSISTANT },
                                icon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = stringResource(R.string.nav_assistant)
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_assistant)) }
                            )
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        when (section) {
                            AppSection.CALENDAR -> CalendarScreen(
                                viewModel = calendarViewModel,
                                onOpenSettings = { showSettings = true }
                            )
                            AppSection.MENU -> MenuScreen(
                                viewModel = menuViewModel,
                                onOpenSettings = { showSettings = true }
                            )
                            AppSection.RECIPES -> RecipesScreen(
                                viewModel = recipesViewModel,
                                onOpenSettings = { showSettings = true }
                            )
                            AppSection.SHOPPING -> ShoppingScreen(
                                viewModel = shoppingViewModel,
                                onOpenSettings = { showSettings = true }
                            )
                            AppSection.CHORES -> ChoresScreen(
                                viewModel = choresViewModel,
                                onOpenSettings = { showSettings = true }
                            )
                            AppSection.ASSISTANT -> AssistantScreen(
                                viewModel = assistantViewModel,
                                onOpenSettings = { showSettings = true }
                            )
                        }
                    }
                }
            }
        }

        if (showSettings) {
            SettingsDialog(sessionViewModel) { showSettings = false }
        }
    }
}

private enum class AppSection {
    CALENDAR, MENU, RECIPES, SHOPPING, CHORES, ASSISTANT
}
