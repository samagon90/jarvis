package com.jarvis.master.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.jarvis.master.ui.screens.DashboardScreen
import com.jarvis.master.ui.screens.RepairsScreen
import com.jarvis.master.ui.screens.PartsScreen
import com.jarvis.master.ui.screens.MoneyScreen
import com.jarvis.master.ui.screens.ClientsScreen
import com.jarvis.master.ui.screens.RepairEditScreen
import com.jarvis.master.ui.screens.RepairDetailScreen
import com.jarvis.master.ui.screens.PartEditScreen
import com.jarvis.master.ui.screens.ClientDetailScreen
import com.jarvis.master.ui.screens.SettingsScreen
import com.jarvis.master.ui.theme.JarvisTheme

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomItems = listOf(
    BottomItem("dashboard", "Главная", Icons.Filled.Dashboard),
    BottomItem("repairs", "Ремонты", Icons.Filled.Build),
    BottomItem("parts", "Запчасти", Icons.Filled.ShoppingCart),
    BottomItem("money", "Финансы", Icons.Filled.Money),
    BottomItem("clients", "Клиенты", Icons.Filled.AccountCircle)
)

private val topLevelRoutes = bottomItems.map { it.route }.toSet()

@Composable
fun JarvisRoot(themeMode: Int = 0) {
    JarvisTheme(themeMode = themeMode) {
        val navController = rememberNavController()
        val backStack by navController.currentBackStackEntryAsState()
        val currentDest = backStack?.destination
        val showBottomBar = currentDest?.route in topLevelRoutes

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        bottomItems.forEach { item ->
                            val selected = currentDest?.hierarchy?.any {
                                it.route == item.route
                            } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("dashboard") { DashboardScreen(navController) }
                composable("repairs") { RepairsScreen(navController) }

                composable("repair_edit?repairId={repairId}",
                    arguments = listOf(navArgument("repairId") { type = NavType.LongType; defaultValue = 0L })
                ) { RepairEditScreen(navController) }

                composable("repair_detail?repairId={repairId}",
                    arguments = listOf(navArgument("repairId") { type = NavType.LongType })
                ) { RepairDetailScreen(navController) }

                composable("parts") { PartsScreen(navController) }

                composable("part_edit?partId={partId}",
                    arguments = listOf(navArgument("partId") { type = NavType.LongType; defaultValue = 0L })
                ) { PartEditScreen(navController) }

                composable("money") { MoneyScreen(navController) }
                composable("clients") { ClientsScreen(navController) }

                composable("client_detail?clientId={clientId}",
                    arguments = listOf(navArgument("clientId") { type = NavType.LongType })
                ) { ClientDetailScreen(navController) }

                composable("settings") { SettingsScreen(navController) }
            }
        }
    }
}
