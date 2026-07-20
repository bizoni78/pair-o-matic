package com.pairomatic.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.pairomatic.ui.health.DeckHealthScreen
import com.pairomatic.ui.learn.LearnScreen
import com.pairomatic.ui.pairs.PairEditScreen
import com.pairomatic.ui.pairs.PairListScreen
import com.pairomatic.ui.settings.SettingsScreen
import com.pairomatic.ui.stats.StatsScreen

private sealed class TopDest(val route: String, val label: String, val icon: ImageVector) {
    data object Learn : TopDest("learn", "Nauka", Icons.Filled.School)
    data object Pairs : TopDest("pairs", "Pary", Icons.AutoMirrored.Filled.List)
    data object Stats : TopDest("stats", "Statystyki", Icons.Filled.BarChart)
    data object Settings : TopDest("settings", "Ustawienia", Icons.Filled.Settings)
}

private val topDestinations = listOf(TopDest.Learn, TopDest.Pairs, TopDest.Stats, TopDest.Settings)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(containerColor = Color.Transparent) {
                topDestinations.forEach { dest ->
                    val selected = currentRoute?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopDest.Learn.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(TopDest.Learn.route) { LearnScreen() }
            composable(TopDest.Pairs.route) {
                PairListScreen(
                    onAddPair = { navController.navigate("pairEdit/0") },
                    onEditPair = { id -> navController.navigate("pairEdit/$id") }
                )
            }
            composable(TopDest.Stats.route) { StatsScreen() }
            composable(TopDest.Settings.route) {
                SettingsScreen(onOpenDeckHealth = { navController.navigate("deckHealth") })
            }
            composable("deckHealth") {
                DeckHealthScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = "pairEdit/{pairId}",
                arguments = listOf(navArgument("pairId") { type = NavType.LongType })
            ) { entry ->
                val pairId = entry.arguments?.getLong("pairId") ?: 0L
                PairEditScreen(
                    pairId = pairId,
                    onDone = { navController.popBackStack() }
                )
            }
        }
    }
}
