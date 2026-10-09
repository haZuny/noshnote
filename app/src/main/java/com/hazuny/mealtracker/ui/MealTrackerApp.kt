package com.hazuny.mealtracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hazuny.mealtracker.MealViewModel
import java.time.LocalDate

private data class MainDestination(val route: String, val label: String, val symbol: String)

private val mainDestinations = listOf(
    MainDestination("home", "홈", "⌂"),
    MainDestination("calendar", "달력", "▦"),
    MainDestination("settings", "설정", "⚙"),
)

@Composable
fun MealTrackerApp(viewModel: MealViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "home"
    var selectedDateKey by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val foodTemplates by viewModel.foodTemplates.collectAsStateWithLifecycle()
    val goal by viewModel.goal.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavigationBar {
                mainDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(destination.symbol) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    dateKey = selectedDateKey,
                    entries = entries,
                    foodTemplates = foodTemplates,
                    goalCalories = goal?.caloriesKcal,
                    goalProtein = goal?.proteinG,
                    onDateChange = { selectedDateKey = it },
                    onOpenSettings = { navController.navigate("settings") },
                )
            }
            composable("calendar") {
                CalendarScreen(
                    entries = entries,
                    goalCalories = goal?.caloriesKcal,
                    goalProtein = goal?.proteinG,
                    onSelectDate = { dateKey ->
                        selectedDateKey = dateKey
                        navController.navigate("home") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    goalCalories = goal?.caloriesKcal,
                    goalProtein = goal?.proteinG,
                    foodTemplates = foodTemplates,
                )
            }
        }
    }
}
