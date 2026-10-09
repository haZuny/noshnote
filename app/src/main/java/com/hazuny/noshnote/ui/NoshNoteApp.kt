package com.hazuny.noshnote.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hazuny.noshnote.MealViewModel
import com.hazuny.noshnote.R
import java.time.LocalDate

private data class MainDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val mainDestinations = listOf(
    MainDestination("home", "홈", Icons.Default.Home),
    MainDestination("calendar", "달력", Icons.Default.DateRange),
    MainDestination("settings", "설정", Icons.Default.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoshNoteApp(viewModel: MealViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "home"
    var selectedDateKey by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val foodTemplates by viewModel.foodTemplates.collectAsStateWithLifecycle()
    val goal by viewModel.goal.collectAsStateWithLifecycle()
    val swipeThresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { 72.dp.toPx() }
    val title = when (currentRoute) {
        "calendar" -> "달력"
        "settings" -> "설정"
        else -> LocalDate.parse(selectedDateKey).let { date ->
            if (date == LocalDate.now()) "오늘 기록" else "${date.monthValue}월 ${date.dayOfMonth}일 기록"
        }
    }
    fun navigateToDestination(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    Row(
                        modifier = Modifier.padding(start = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.noshnote_symbol),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                        Text(
                            text = "NoshNote",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                    }
                },
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                mainDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = { navigateToDestination(destination.route) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        label = { Text(destination.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier
                .padding(innerPadding)
                .pointerInput(currentRoute, swipeThresholdPx) {
                    var dragDistance = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, amount -> dragDistance += amount },
                        onDragEnd = {
                            val currentIndex = mainDestinations.indexOfFirst { it.route == currentRoute }
                                .coerceAtLeast(0)
                            val nextIndex = when {
                                dragDistance < -swipeThresholdPx -> currentIndex + 1
                                dragDistance > swipeThresholdPx -> currentIndex - 1
                                else -> currentIndex
                            }
                            mainDestinations.getOrNull(nextIndex)?.let { navigateToDestination(it.route) }
                        },
                        onDragCancel = { dragDistance = 0f },
                    )
                },
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
