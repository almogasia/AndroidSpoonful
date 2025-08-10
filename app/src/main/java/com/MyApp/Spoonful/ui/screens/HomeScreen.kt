@file:OptIn(ExperimentalMaterial3Api::class)
package com.MyApp.Spoonful.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.rememberAsyncImagePainter
import com.MyApp.Spoonful.viewmodel.AuthViewModel
import com.MyApp.Spoonful.viewmodel.RecipeViewModel
import com.MyApp.Spoonful.model.Recipe
import androidx.compose.runtime.livedata.observeAsState
import com.MyApp.Spoonful.ui.components.BottomNavigationBar
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.platform.LocalContext
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults

/**
 * Home screen composable that serves as the main recipe discovery interface.
 * 
 * Features comprehensive recipe browsing with search, filtering, and categorization.
 * Supports multiple filter types: text search, category selection, difficulty level,
 * cooking time, and user-specific recipes. Displays popular recipes based on
 * favorite counts and provides navigation to recipe details.
 * 
 * @param navController Navigation controller for screen transitions
 * @param authViewModel ViewModel for user authentication state
 * @param recipeViewModel ViewModel for recipe data and favorites
 */
@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    recipeViewModel: RecipeViewModel = viewModel()
) {
    val context = LocalContext.current
    val recipes by recipeViewModel.recipes.observeAsState(emptyList())
    val favorites by recipeViewModel.favorites.observeAsState(emptyList())
    val currentUser by authViewModel.user.observeAsState()
    var search by remember { mutableStateOf(TextFieldValue("")) }
    var selectedCategory by remember { mutableStateOf("Popular") }
    var filterMyRecipes by remember { mutableStateOf(false) }
    var showAllPopular by remember { mutableStateOf(false) }
    
    // Filter states
    var showFilterDialog by remember { mutableStateOf(false) }
    var filterByDifficulty by remember { mutableStateOf(false) }
    var filterByTime by remember { mutableStateOf(false) }
    var selectedDifficulty by remember { mutableStateOf(1f) }
    var selectedMaxTime by remember { mutableStateOf(60f) }
    var timeRange by remember { mutableStateOf(0f..60f) }
    // Category filter state
    var filterByCategories by remember { mutableStateOf(false) }
    var selectedFilterCategories by remember { mutableStateOf(setOf<String>()) }
    var categorySearchText by remember { mutableStateOf("") }

    // Load categories from file
    val categories = remember {
        try {
            val input = context.assets.open("categories.txt")
            val reader = BufferedReader(InputStreamReader(input))
            reader.readLines().map { it.trim() }.filter { it.isNotEmpty() }
        } catch (e: Exception) {
            emptyList<String>()
        }
    }

    // Special categories (show first 6 for demo)
    val specialCategories = categories.take(6)

    // Category chips (Popular + categories)
    val chips = listOf("Popular") + categories.take(6)

    // Popular recipes: sorted by favoriteCounter desc
    val popularRecipes = recipes.sortedByDescending { it.favoriteCounter }.take(10)

    // Multi-step recipe filtering with search, category, difficulty, time, and user filters
    val filteredRecipes = remember(
        recipes, 
        search.text, 
        selectedCategory, 
        filterByDifficulty, 
        filterByTime, 
        selectedDifficulty, 
        selectedMaxTime, 
        filterByCategories, 
        selectedFilterCategories,
        filterMyRecipes,
        showAllPopular,
        currentUser
    ) {
        // Start with full list or just user's recipes if toggled
        val uid = currentUser?.uid
        var base: List<Recipe> = if (filterMyRecipes && uid != null) {
            recipes.filter { it.authorId == uid }
        } else {
            recipes
        }
        
        // Start with all recipes for filtering
        var filtered = base
        
        // Apply category filter first
        if (selectedCategory != "Popular") {
            filtered = filtered.filter { recipe ->
                recipe.categories.any { it.equals(selectedCategory, ignoreCase = true) }
            }
        }
        
        // Apply search filter
        if (search.text.isNotBlank()) {
            filtered = filtered.filter { recipe ->
                recipe.title.contains(search.text, ignoreCase = true) ||
                recipe.description.contains(search.text, ignoreCase = true) ||
                recipe.ingredients.any { it.contains(search.text, ignoreCase = true) }
            }
        }
        
        // Apply difficulty filter
        if (filterByDifficulty) {
            filtered = filtered.filter { it.difficulty <= selectedDifficulty.toInt() }
        }
        
        // Apply time filter
        if (filterByTime) {
            val maxTimeMinutes = selectedMaxTime.toInt()
            filtered = filtered.filter { recipe ->
                val recipeTime = recipe.time.toIntOrNull() ?: 0
                recipeTime <= maxTimeMinutes
            }
        }
        
        // Apply multi-category filter
        if (filterByCategories && selectedFilterCategories.isNotEmpty()) {
            // Filter recipes that match any of the selected categories (case-insensitive)
            filtered = filtered.filter { recipe ->
                recipe.categories.any { rc -> selectedFilterCategories.any { it.equals(rc, ignoreCase = true) } }
            }
        }
        
        // For Popular category with no other filters, limit to top 10
        if (selectedCategory == "Popular" && search.text.isBlank() && !filterByDifficulty && !filterByTime && !filterByCategories) {
            if (showAllPopular) {
                filtered = filtered.sortedByDescending { it.favoriteCounter }
            } else {
                filtered = filtered.sortedByDescending { it.favoriteCounter }.take(10)
            }
        } else if (selectedCategory == "Popular") {
            // For Popular category with filters, sort by popularity but don't limit
            filtered = filtered.sortedByDescending { it.favoriteCounter }
        }
        
        filtered
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController, selected = "home")
        },
        containerColor = Color(0xFFF8F8F8)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F8F8)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar - Centered title
            Text(
                "Home", 
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), 
                color = Color.Black,
                modifier = Modifier.padding(top = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search recipes...", color = Color(0xFFBBBBBB)) },
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
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Special Categories
            Text(
                "Special Categories",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.Black,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 16.dp, bottom = 8.dp)
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(specialCategories) { cat ->
                    Card(
                        modifier = Modifier
                            .size(width = 140.dp, height = 100.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Placeholder image
                            Image(
                                painter = rememberAsyncImagePainter("https://source.unsplash.com/400x300/?${cat}"),
                                contentDescription = cat,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f))
                            )
                            Text(
                                cat,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Category Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chips) { chip ->
                    FilterChip(
                        selected = selectedCategory == chip,
                        onClick = { 
                            selectedCategory = chip 
                            showAllPopular = false
                        },
                        label = {
                            Text(
                                chip,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (selectedCategory == chip) Color.White else Color.Black
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFC107),
                            containerColor = Color.White
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Filtered Recipes
            if (filteredRecipes.isEmpty()) {
                Text("No recipes found.", color = Color.Gray, modifier = Modifier.padding(top = 32.dp))
            } else {
                // Determine whether to show the View more button (only for Popular when there are more than 10 filtered items)
                val shouldShowViewMore = selectedCategory == "Popular" && !showAllPopular && filteredRecipes.size > 9
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredRecipes) { recipe ->
                        Card(
                            modifier = Modifier
                                .width(180.dp)
                                .height(240.dp)
                                .clickable { navController.navigate("recipe/${recipe.id}") },
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    painter = rememberAsyncImagePainter(recipe.imageUrl.ifEmpty { "https://via.placeholder.com/300" }),
                                    contentDescription = recipe.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(24.dp))
                                )
                                // Heart and rating overlay
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                        .align(Alignment.TopStart),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = "Favorite",
                                        tint = Color(0xFFFFC107),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "${recipe.favoriteCounter}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                // Title overlay at the bottom
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                                ) {
                                    Text(
                                        recipe.title,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (shouldShowViewMore) {
                        item {
                            OutlinedButton(
                                onClick = { showAllPopular = true },
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(240.dp)
                            ) {
                                Text("View more")
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Filter Dialog
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
                        "Filter Recipes",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    // My recipes toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Only My recipes",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = filterMyRecipes,
                            onCheckedChange = { filterMyRecipes = it },
                            enabled = currentUser != null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFFC107),
                                checkedTrackColor = Color(0xFFFFC107).copy(alpha = 0.5f)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Difficulty Filter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Filter by Difficulty",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
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
                    // Time Filter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Filter by Time",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
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
                    // Category filter (moved after Difficulty and Time to match Favorites)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Filter by Categories",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
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
                            items(filterableCats) { cat ->
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
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                // Clear all filter states
                                filterByDifficulty = false
                                filterByTime = false
                                filterByCategories = false
                                selectedDifficulty = 1f
                                selectedMaxTime = 60f
                                selectedFilterCategories = emptySet()
                                categorySearchText = ""
                                filterMyRecipes = false
                                showAllPopular = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Clear all filters")
                        }
                        Button(
                            onClick = { showFilterDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Apply Filters", color = Color.White)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

 