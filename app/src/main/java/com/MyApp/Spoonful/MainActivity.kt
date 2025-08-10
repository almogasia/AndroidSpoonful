package com.MyApp.Spoonful

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.MyApp.Spoonful.ui.theme.SpoonfulTheme
import com.MyApp.Spoonful.viewmodel.AuthViewModel
import com.MyApp.Spoonful.ui.screens.*

/**
 * MainActivity serves as the entry point for the Spoonful recipe sharing application.
 * 
 * This activity implements a single-activity architecture using Jetpack Compose Navigation.
 * The app uses Firebase Authentication to determine the initial navigation destination:
 * - If user is logged in: Navigate to Home screen
 * - If user is not logged in: Navigate to Login screen
 * 
 * The navigation structure supports the following screens:
 * - Authentication: Login, Register
 * - Main app: Home, Favorites, Upload Recipe, Profile, Recipe Detail
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpoonfulTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()
                val isLoggedIn by authViewModel.isLoggedIn.observeAsState(false)
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(navController, isLoggedIn)
                }
            }
        }
    }
}

/**
 * AppNavHost defines the navigation structure for the entire application.
 * 
 * Uses Jetpack Compose Navigation to manage screen transitions and navigation state.
 * The start destination is dynamically determined based on authentication status.
 * 
 * @param navController The navigation controller managing screen transitions
 * @param isLoggedIn Current authentication status from AuthViewModel
 */
@Composable
fun AppNavHost(navController: NavHostController, isLoggedIn: Boolean) {
    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "home" else "login"
    ) {
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("home") { HomeScreen(navController) }
        composable("favorites") { FavoritesScreen(navController) }
        composable("recipe/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            RecipeDetailScreen(navController, id)
        }
        composable("upload") { UploadRecipeScreen(navController) }
        composable("profile") { ProfileScreen(navController) }
    }
}