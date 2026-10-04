package com.liangleionline.poetry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Settings
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.liangleionline.poetry.ui.AppViewModel
import com.liangleionline.poetry.ui.screens.BookshelfScreen
import com.liangleionline.poetry.ui.screens.CollectionScreen
import com.liangleionline.poetry.ui.screens.FavoritesScreen
import com.liangleionline.poetry.ui.screens.ReaderScreen
import com.liangleionline.poetry.ui.screens.SettingsScreen
import com.liangleionline.poetry.ui.theme.PoetryTheme

object Routes {
    const val BOOKSHELF = "bookshelf"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val COLLECTION = "collection/{collectionId}/{name}"
    const val READER = "reader/{poemId}"
    fun collection(id: Long, name: String) = "collection/$id/${java.net.URLEncoder.encode(name, "UTF-8")}"
    fun reader(id: Long) = "reader/$id"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PoetryTheme {
                MainScreen()
            }
        }
    }
}

@Composable
private fun MainScreen(vm: AppViewModel = viewModel()) {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStack?.destination

    val showBottomBar = currentDestination?.hierarchy?.any { dest ->
        dest.route == Routes.BOOKSHELF || dest.route == Routes.FAVORITES || dest.route == Routes.SETTINGS
    } ?: true

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                BottomBar(currentDestination?.route ?: Routes.BOOKSHELF) { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.BOOKSHELF,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            composable(Routes.BOOKSHELF) {
                BookshelfScreen(
                    vm,
                    onOpenCollection = { id, name ->
                        navController.navigate(Routes.collection(id, name))
                    },
                    onOpenPoem = { id -> navController.navigate(Routes.reader(id)) }
                )
            }
            composable(
                Routes.COLLECTION,
                arguments = listOf(
                    navArgument("collectionId") { type = androidx.navigation.NavType.LongType },
                    navArgument("name") { type = androidx.navigation.NavType.StringType }
                )
            ) { entry ->
                val cid = entry.arguments?.getLong("collectionId") ?: 0L
                val name = java.net.URLDecoder.decode(entry.arguments?.getString("name") ?: "", "UTF-8")
                CollectionScreen(vm, cid, name, onBack = { navController.popBackStack() },
                    onOpenPoem = { navController.navigate(Routes.reader(it)) })
            }
            composable(Routes.READER, arguments = listOf(navArgument("poemId") { type = androidx.navigation.NavType.LongType })) { entry ->
                val pid = entry.arguments?.getLong("poemId") ?: 0L
                ReaderScreen(vm, pid, onBack = { navController.popBackStack() })
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(vm, onOpenPoem = { navController.navigate(Routes.reader(it)) })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(vm)
            }
        }
    }
}

private data class TabItem(val route: String, val labelRes: Int, val selected: ImageVector, val unselected: ImageVector)

private val tabs = listOf(
    TabItem(Routes.BOOKSHELF, R.string.bookshelf, Icons.Filled.Book, Icons.Outlined.Book),
    TabItem(Routes.FAVORITES, R.string.favorites, Icons.Filled.Favorite, Icons.Outlined.Favorite),
    TabItem(Routes.SETTINGS, R.string.settings, Icons.Outlined.Settings, Icons.Outlined.Settings)
)

@Composable
private fun BottomBar(currentRoute: String, onNavigate: (String) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        val backStack by androidx.compose.runtime.rememberUpdatedState(currentRoute)
        tabs.forEach { tab ->
            val selected = backStack == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(tab.route) },
                icon = { androidx.compose.material3.Icon(
                    if (selected) tab.selected else tab.unselected, contentDescription = null) },
                label = { Text(stringResource(tab.labelRes)) }
            )
        }
    }
}
