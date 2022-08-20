package com.github.watabee.cameraxbasiccompose

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.watabee.cameraxbasiccompose.ui.AppTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, findViewById(android.R.id.content)).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            AppTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = Screen.Camera.route) {
                        composable(route = Screen.Camera.route) {
                            PermissionScreen(
                                openGalleryScreen = { rootDirectory ->
                                    navController.navigate(Screen.Gallery.createNavigationRoute(rootDirectory))
                                }
                            )
                        }
                        composable(
                            route = Screen.Gallery.route,
                            arguments = listOf(navArgument(Screen.Gallery.rootDirectoryArg) { type = NavType.StringType })
                        ) { backStackEntry ->
                            val rootDirectory = Screen.Gallery.fromNavArg(backStackEntry)
                            GalleryScreen(
                                rootDirectory = rootDirectory,
                                navigateUp = navController::navigateUp
                            )
                        }
                    }
                }
            }
        }
    }
}

sealed interface Screen {
    val route: String

    object Camera : Screen {
        override val route: String = "camera"
    }

    object Gallery : Screen {
        const val rootDirectoryArg = "root_directory"
        override val route: String = "gallery/{$rootDirectoryArg}"

        fun createNavigationRoute(rootDirectory: String): String {
            return "gallery/${Uri.encode(rootDirectory)}"
        }

        fun fromNavArg(entry: NavBackStackEntry): File {
            val rootDirectory = entry.arguments?.getString(rootDirectoryArg)!!
            return File(Uri.decode(rootDirectory))
        }
    }
}