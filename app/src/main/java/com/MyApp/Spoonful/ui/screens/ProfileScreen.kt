package com.MyApp.Spoonful.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.MyApp.Spoonful.viewmodel.AuthViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import com.MyApp.Spoonful.ui.components.BottomNavigationBar
import androidx.compose.ui.Alignment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.Email
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import com.MyApp.Spoonful.viewmodel.RecipeViewModel
import com.MyApp.Spoonful.model.Recipe
import androidx.compose.runtime.livedata.observeAsState
import coil.compose.rememberAsyncImagePainter
import androidx.compose.material.icons.filled.Favorite

/**
 * Profile screen composable that displays user information and statistics.
 * 
 * Shows user profile data including username, email, join date, recipes created,
 * favorites count, and most popular recipe. Fetches user statistics from Firebase
 * and displays them in a card-based layout. Provides logout functionality.
 * 
 * @param navController Navigation controller for screen transitions
 * @param authViewModel ViewModel for user authentication
 * @param recipeViewModel ViewModel for recipe data
 */
@Composable
fun ProfileScreen(navController: NavController, authViewModel: AuthViewModel = viewModel(), recipeViewModel: RecipeViewModel = viewModel()) {
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController, selected = "profile")
        },
        containerColor = Color(0xFFF8F8F8)
    ) { innerPadding ->
        val user = FirebaseAuth.getInstance().currentUser
        var username by remember { mutableStateOf("") }
        val email = user?.email ?: ""
        var recipesCreated by remember { mutableStateOf(0) }
        var favoritesCount by remember { mutableStateOf(0) }
        var joinedDate by remember { mutableStateOf(0L) }
        var joinedDateString by remember { mutableStateOf("") }
        val allRecipes by recipeViewModel.recipes.observeAsState(emptyList())
        val myRecipes = allRecipes.filter { it.authorId == user?.uid }
        val mostLikedRecipe = myRecipes.maxByOrNull { it.favoriteCounter }
        LaunchedEffect(user) {
            val uid = user?.uid
            if (uid != null) {
                val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
                userRef.child("username").get().addOnSuccessListener { snapshot ->
                    username = snapshot.getValue(String::class.java) ?: ""
                }
                userRef.child("recipesCreated").get().addOnSuccessListener { snapshot ->
                    recipesCreated = snapshot.getValue(Int::class.java) ?: 0
                }
                userRef.child("favorites").get().addOnSuccessListener { snapshot ->
                    favoritesCount = snapshot.childrenCount.toInt()
                }
                userRef.child("joined").get().addOnSuccessListener { snapshot ->
                    joinedDate = snapshot.getValue(Long::class.java) ?: 0L
                    if (joinedDate > 0L) {
                        val sdf = java.text.SimpleDateFormat("MMM yyyy")
                        joinedDateString = sdf.format(java.util.Date(joinedDate))
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F8F8)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar - Centered title
            Text(
                "Profile", 
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), 
                color = Color.Black,
                modifier = Modifier.padding(top = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile image
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFE0E0E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color(0xFFBBBBBB), modifier = Modifier.size(60.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (username.isNotBlank()) username else "User",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    if (email.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Email, contentDescription = "Email", tint = Color(0xFFBBBBBB), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(email, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF888888))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    // Profile stats (placeholders, you can connect to real data)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$recipesCreated", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                            Text("Recipes", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$favoritesCount", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                            Text("Favorites", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(joinedDateString, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                            Text("Joined", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    if (mostLikedRecipe != null) {
                        Text("Most Liked Recipe", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(mostLikedRecipe.imageUrl.ifEmpty { "https://via.placeholder.com/150" }),
                                    contentDescription = mostLikedRecipe.title,
                                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mostLikedRecipe.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Favorite, contentDescription = "Likes", tint = Color(0xFFFFC107), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${mostLikedRecipe.favoriteCounter} likes", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Button(onClick = { navController.navigate("recipe/${mostLikedRecipe.id}") }, shape = RoundedCornerShape(8.dp)) {
                                    Text("View")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Button(
                        onClick = {
                            authViewModel.logout()
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Logout", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
} 