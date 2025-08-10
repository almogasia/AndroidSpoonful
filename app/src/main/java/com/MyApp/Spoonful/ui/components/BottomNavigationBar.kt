package com.MyApp.Spoonful.ui.components

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.compose.material3.Icon

/**
 * Bottom navigation bar component for main app navigation.
 * 
 * Provides navigation between the four main app sections: Home, Favorites,
 * Upload Recipe, and Profile. Uses Material Design 3 navigation bar with
 * appropriate icons and labels for each section.
 * 
 * @param navController Navigation controller for screen transitions
 * @param selected Currently selected navigation item identifier
 */
@Composable
fun BottomNavigationBar(navController: NavController, selected: String) {
    NavigationBar {
        NavigationBarItem(
            selected = selected == "home",
            onClick = { navController.navigate("home") },
            label = { Text("Home") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") }
        )
        NavigationBarItem(
            selected = selected == "favorites",
            onClick = { navController.navigate("favorites") },
            label = { Text("Favorites") },
            icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") }
        )
        NavigationBarItem(
            selected = selected == "upload",
            onClick = { navController.navigate("upload") },
            label = { Text("Upload") },
            icon = { Icon(Icons.Default.Add, contentDescription = "Upload") }
        )
        NavigationBarItem(
            selected = selected == "profile",
            onClick = { navController.navigate("profile") },
            label = { Text("Profile") },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") }
        )
    }
} 