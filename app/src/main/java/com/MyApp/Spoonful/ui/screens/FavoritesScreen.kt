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
import com.MyApp.Spoonful.ui.components.BottomNavigationBar
import androidx.compose.ui.Alignment
import com.MyApp.Spoonful.viewmodel.RecipeViewModel
import com.MyApp.Spoonful.model.Recipe
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as itemsRow
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Favorites screen composable that displays user's saved recipes.
 * 
 * Shows all recipes marked as favorites by the current user with search
 * and filtering capabilities. Supports filtering by difficulty, cooking time,
 * categories, and text search across recipe content. Provides navigation
 * to recipe details and favorite management.
 * 
 * @param navController Navigation controller for screen transitions
 * @param authViewModel ViewModel for user authentication
 * @param recipeViewModel ViewModel for recipe data and favorites
 */
@Composable
fun FavoritesScreen(navController: NavController, authViewModel: AuthViewModel = viewModel(), recipeViewModel: RecipeViewModel = viewModel()) {
    val recipes by recipeViewModel.recipes.observeAsState(emptyList())
    val favorites by recipeViewModel.favorites.observeAsState(emptyList())
    // Filters state (mirror Home)
    var search by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var filterByDifficulty by remember { mutableStateOf(false) }
    var filterByTime by remember { mutableStateOf(false) }
    var selectedDifficulty by remember { mutableStateOf(1f) }
    var selectedMaxTime by remember { mutableStateOf(60f) }
    var filterByCategories by remember { mutableStateOf(false) }
    var selectedFilterCategories by remember { mutableStateOf(setOf<String>()) }
    var categorySearchText by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val categories = remember {
        try {
            val input = context.assets.open("categories.txt")
            val reader = java.io.BufferedReader(java.io.InputStreamReader(input))
            reader.readLines().map { it.trim() }.filter { it.isNotEmpty() }
        } catch (e: Exception) { emptyList<String>() }
    }
    val favoriteRecipesRaw = recipes.filter { favorites.contains(it.id) }
    val favoriteRecipes = remember(
        recipes, favorites, search, filterByDifficulty, filterByTime, selectedDifficulty, selectedMaxTime, filterByCategories, selectedFilterCategories
    ) {
        var filtered = favoriteRecipesRaw
        if (search.isNotBlank()) {
            filtered = filtered.filter { r ->
                r.title.contains(search, true) ||
                r.description.contains(search, true) ||
                r.ingredients.any { it.contains(search, true) }
            }
        }
        if (filterByDifficulty) {
            filtered = filtered.filter { it.difficulty <= selectedDifficulty.toInt() }
        }
        if (filterByTime) {
            val maxTime = selectedMaxTime.toInt()
            filtered = filtered.filter { (it.time.toIntOrNull() ?: 0) <= maxTime }
        }
        if (filterByCategories && selectedFilterCategories.isNotEmpty()) {
            // Filter recipes that match any of the selected categories (case-insensitive)
            filtered = filtered.filter { recipe ->
                recipe.categories.any { rc -> selectedFilterCategories.any { it.equals(rc, ignoreCase = true) } }
            }
        }
        filtered
    }
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController, selected = "favorites")
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F8F8)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Favorites",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.Black,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Search + filter row (mirror Home) — moved below title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search favorites...", color = Color(0xFFBBBBBB)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFFFC107)) },
                    trailingIcon = {
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color(0xFFFFC107))
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )
            }
            if (favoriteRecipes.isEmpty()) {
                Text("No favorite recipes yet.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                ) {
                    items(favoriteRecipes) { recipe ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .clickable { navController.navigate("recipe/${recipe.id}") },
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = recipe.imageUrl.ifEmpty { "https://via.placeholder.com/150" }),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        recipe.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        recipe.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF888888),
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFFFF9C4)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Outlined.FavoriteBorder,
                                                contentDescription = "Favorite",
                                                tint = Color(0xFFFFC107),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            (recipe.time.ifBlank { "-" } + " mins."),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFBBBBBB)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // Filter dialog (mirror Home)
        if (showFilterDialog) {
            ModalBottomSheet(
                onDismissRequest = { showFilterDialog = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        "Filter Favorites",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    // Difficulty
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Filter by Difficulty", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Switch(
                            checked = filterByDifficulty,
                            onCheckedChange = { filterByDifficulty = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFFC107),
                                checkedTrackColor = Color(0xFFFFC107).copy(alpha = 0.5f)
                            )
                        )
                    }
                    if (filterByDifficulty) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Max Difficulty: ${selectedDifficulty.toInt()}")
                        Slider(
                            value = selectedDifficulty,
                            onValueChange = { selectedDifficulty = it },
                            valueRange = 1f..5f,
                            steps = 3,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFFC107),
                                activeTrackColor = Color(0xFFFFC107)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Time
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Filter by Time", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Switch(
                            checked = filterByTime,
                            onCheckedChange = { filterByTime = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFFC107),
                                checkedTrackColor = Color(0xFFFFC107).copy(alpha = 0.5f)
                            )
                        )
                    }
                    if (filterByTime) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Max Time: ${selectedMaxTime.toInt()} minutes")
                        Slider(
                            value = selectedMaxTime,
                            onValueChange = { selectedMaxTime = it },
                            valueRange = 5f..180f,
                            steps = 34,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFFC107),
                                activeTrackColor = Color(0xFFFFC107)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Categories
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Filter by Categories", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Switch(
                            checked = filterByCategories,
                            onCheckedChange = { filterByCategories = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFFC107),
                                checkedTrackColor = Color(0xFFFFC107).copy(alpha = 0.5f)
                            )
                        )
                    }
                    if (filterByCategories) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = categorySearchText,
                            onValueChange = { categorySearchText = it },
                            placeholder = { Text("Search categories...", color = Color(0xFFBBBBBB)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.textFieldColors(
                                containerColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val filterableCats = categories.filter { it.contains(categorySearchText, ignoreCase = true) }
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsRow(filterableCats) { cat ->
                                val isSelected = selectedFilterCategories.any { it.equals(cat, ignoreCase = true) }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedFilterCategories = if (isSelected) {
                                            selectedFilterCategories.filterNot { it.equals(cat, ignoreCase = true) }.toSet()
                                        } else {
                                            (selectedFilterCategories + cat).toSet()
                                        }
                                    },
                                    label = { Text(cat) },
                                    shape = RoundedCornerShape(24.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFFC107),
                                        containerColor = Color.White
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                // Clear all filters
                                filterByDifficulty = false
                                filterByTime = false
                                filterByCategories = false
                                selectedDifficulty = 1f
                                selectedMaxTime = 60f
                                selectedFilterCategories = emptySet()
                                categorySearchText = ""
                                search = ""
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Clear all filters") }
                        Button(
                            onClick = { showFilterDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Apply Filters", color = Color.White) }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
} 