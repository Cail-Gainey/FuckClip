package app.fuckclip.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.fuckclip.R
import app.fuckclip.data.SettingsRepository
import app.fuckclip.ui.home.HomeScreen
import app.fuckclip.ui.log.LogScreen
import app.fuckclip.ui.settings.SettingsScreen
import app.fuckclip.ui.settings.UpdateDialogHost
import app.fuckclip.ui.theme.ThemeScreen
import app.fuckclip.ui.whitelist.WhitelistScreen
import org.koin.compose.koinInject

/**
 * Enumeration of navigation destinations available in the bottom navigation bar.
 *
 * 底部导航栏中可用的导航目标枚举。
 */
sealed class Screen(
    /**
     * Unique navigation route path string.
     *
     * 唯一的导航路由路径字符串。
     */
    val route: String,

    /**
     * Icon displayed for this navigation destination.
     *
     * 该导航目标所展示的图标。
     */
    val icon: ImageVector,

    /**
     * String resource ID for the destination title.
     *
     * 目标页面标题的字符串资源 ID。
     */
    @param:StringRes val titleRes: Int
) {
    /**
     * Home dashboard destination.
     *
     * 首页仪表盘导航目标。
     */
    object Home : Screen("home", Icons.Default.Home, R.string.title_home)

    /**
     * Application whitelist and rule management destination.
     *
     * 应用白名单与规则管理导航目标。
     */
    object Whitelist : Screen("whitelist", Icons.Default.Checklist, R.string.title_app)

    /**
     * Interception and access logs history destination.
     *
     * 拦截与放行历史记录导航目标。
     */
    object Log : Screen("log", Icons.Default.History, R.string.title_history)

    /**
     * Settings, theme, and about destination.
     *
     * 设置、主题与关于导航目标。
     */
    object Settings : Screen("settings", Icons.Default.Settings, R.string.title_settings)
}

private val navItems = listOf(
    Screen.Home,
    Screen.Whitelist,
    Screen.Log,
    Screen.Settings
)

/**
 * Main application navigation graph hosting the bottom navigation bar and page destinations.
 *
 * 承载底部导航栏与各页面导航目标的主导航图组件。
 */
@Composable
fun MainNavHost(
    modifier: Modifier = Modifier,
    settingsRepository: SettingsRepository = koinInject()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isFloatingNav by settingsRepository.isFloatingNavEnabled.collectAsStateWithLifecycle()

    val showBottomBar = navItems.any { it.route == currentRoute }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                if (isFloatingNav) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(28.dp),
                        tonalElevation = 4.dp,
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            navItems.forEach { screen ->
                                val selected = currentRoute == screen.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        if (!selected) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = { Icon(screen.icon, contentDescription = stringResource(screen.titleRes)) },
                                    alwaysShowLabel = false
                                )
                            }
                        }
                    }
                } else {
                    NavigationBar {
                        navItems.forEach { screen ->
                            val selected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { Icon(screen.icon, contentDescription = null) },
                                label = { Text(stringResource(screen.titleRes)) }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToTheme = { navController.navigate("theme") },
                    onNavigateToWhitelist = {
                        navController.navigate(Screen.Whitelist.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToLog = {
                        navController.navigate(Screen.Log.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Whitelist.route) {
                WhitelistScreen()
            }
            composable(Screen.Log.route) {
                LogScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onNavigateToTheme = { navController.navigate("theme") })
            }
            composable("theme") {
                ThemeScreen(onBack = { navController.popBackStack() })
            }
        }

        UpdateDialogHost()
    }
}
