package com.andrecoura.homemonitor.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.andrecoura.homemonitor.R
import com.andrecoura.homemonitor.ui.alerts.AlertsRoute
import com.andrecoura.homemonitor.ui.devices.CameraViewerRoute
import com.andrecoura.homemonitor.ui.devices.DeviceFormRoute
import com.andrecoura.homemonitor.ui.devices.DeviceFormViewModel
import com.andrecoura.homemonitor.ui.devices.DeviceListRoute
import com.andrecoura.homemonitor.ui.gates.GatesRoute
import com.andrecoura.homemonitor.ui.home.HomeRoute
import com.andrecoura.homemonitor.ui.intercom.IntercomRoute
import com.andrecoura.homemonitor.ui.theme.AppIcons

private enum class TopLevel(
    val route: String,
    @StringRes val title: Int,
    val icon: ImageVector,
) {
    Home("home", R.string.tab_home, Icons.Filled.Home),
    Devices("devices", R.string.tab_devices, AppIcons.Videocam),
    Gates("gates", R.string.tab_gates, Icons.Filled.Lock),
    Intercom("intercom", R.string.tab_intercom, Icons.Filled.Phone),
    Alerts("alerts", R.string.tab_alerts, Icons.Filled.Notifications),
}

private const val FORM_ROUTE = "device-form"
private const val CAMERA_ROUTE = "camera"
private const val ARG = DeviceFormViewModel.DEVICE_ID_ARG

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val topLevel = TopLevel.entries.firstOrNull { it.route == route }
    val title =
        when {
            topLevel != null -> stringResource(topLevel.title)
            route?.startsWith(CAMERA_ROUTE) == true -> stringResource(R.string.camera_title)
            backStack?.arguments?.getLong(ARG, 0L)?.let { it > 0 } == true -> stringResource(R.string.form_title_edit)
            else -> stringResource(R.string.form_title_new)
        }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (topLevel == null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (topLevel != null) {
                NavigationBar {
                    TopLevel.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == topLevel,
                            onClick = { navController.navigateTopLevel(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.title)) },
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        NavHost(navController, startDestination = TopLevel.Home.route, modifier = Modifier.padding(padding)) {
            composable(TopLevel.Home.route) {
                HomeRoute(
                    onOpenAlerts = { navController.navigateTopLevel(TopLevel.Alerts.route) },
                    onOpenGates = { navController.navigateTopLevel(TopLevel.Gates.route) },
                    onOpenDevices = { navController.navigateTopLevel(TopLevel.Devices.route) },
                    onOpenIntercom = { navController.navigateTopLevel(TopLevel.Intercom.route) },
                )
            }
            composable(TopLevel.Devices.route) {
                DeviceListRoute(
                    onAdd = { navController.navigate("$FORM_ROUTE/0") },
                    onEdit = { navController.navigate("$FORM_ROUTE/${it.id}") },
                    onWatch = { navController.navigate("$CAMERA_ROUTE/${it.id}") },
                )
            }
            composable(TopLevel.Gates.route) { GatesRoute(snackbar) }
            composable(TopLevel.Intercom.route) { IntercomRoute(snackbar) }
            composable(TopLevel.Alerts.route) { AlertsRoute() }
            composable("$FORM_ROUTE/{$ARG}", arguments = listOf(navArgument(ARG) { type = NavType.LongType })) {
                DeviceFormRoute(onDone = { navController.popBackStack() })
            }
            composable("$CAMERA_ROUTE/{$ARG}", arguments = listOf(navArgument(ARG) { type = NavType.LongType })) {
                CameraViewerRoute()
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) =
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
