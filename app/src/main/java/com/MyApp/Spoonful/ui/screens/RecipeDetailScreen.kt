package com.MyApp.Spoonful.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.MyApp.Spoonful.model.Recipe
import com.MyApp.Spoonful.ui.components.BottomNavigationBar
import com.MyApp.Spoonful.viewmodel.RecipeViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember

import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import com.google.accompanist.flowlayout.FlowRow

import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.AssistChip

import androidx.compose.runtime.LaunchedEffect
import java.io.BufferedReader
import java.io.InputStreamReader
import com.MyApp.Spoonful.ui.screens.IngredientInfo
import com.MyApp.Spoonful.ui.screens.parseIngredientLine

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.ListItem
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import com.google.firebase.database.FirebaseDatabase
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Divider
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import org.burnoutcrew.reorderable.*
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.ExperimentalFoundationApi


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
/**
 * Recipe detail screen composable that displays comprehensive recipe information.
 * 
 * Shows recipe image, title, author, difficulty rating, ingredients, and cooking directions.
 * Provides favorite functionality and edit/delete capabilities for recipe authors.
 * Supports ingredient editing with calorie calculation and category management.
 * 
 * @param navController Navigation controller for screen transitions
 * @param id Recipe ID to display
 * @param recipeViewModel ViewModel for recipe data and favorites
 */
@Composable
fun RecipeDetailScreen(
    navController: NavController,
    id: String,
    recipeViewModel: RecipeViewModel = viewModel()
) {
    val recipes by recipeViewModel.recipes.observeAsState(emptyList())
    val favorites by recipeViewModel.favorites.observeAsState(emptyList())
    val recipe = recipes.find { it.id == id }
    val isFavorite = favorites.contains(recipe?.id)
    val user = FirebaseAuth.getInstance().currentUser
    val isAuthor = user?.uid == recipe?.authorId
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf(recipe?.title ?: "") }
    var editDescription by remember { mutableStateOf(recipe?.description ?: "") }
    var editIngredients by remember { mutableStateOf(recipe?.ingredients?.joinToString(", ") ?: "") }
    var editCategories by remember(recipe?.id) { mutableStateOf<List<String>>(recipe?.categories?.toList() ?: emptyList()) }
    var editIngredientsDialogOpen by remember { mutableStateOf(false) }
    var editIngredientsList by remember { mutableStateOf(listOf<Triple<String, String, Double>>()) }
    var ingredientSearch by remember { mutableStateOf("") }
    var selectedIngredient by remember { mutableStateOf<String?>(null) }
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var editIndex by remember { mutableStateOf(-1) }
    var editAmount by remember { mutableStateOf("") }
    var editUnit by remember { mutableStateOf("") }
    var editDifficulty by remember { mutableStateOf(recipe?.difficulty ?: 1) }
    var editTime by remember { mutableStateOf(recipe?.time ?: "") }
    var chefUsername by remember { mutableStateOf("") }

    var editDirections by remember { mutableStateOf(recipe?.directions?.toList() ?: emptyList()) }
    var editingDirectionIndex by remember { mutableStateOf(-1) }
    var newDirectionText by remember { mutableStateOf("") }

    LaunchedEffect(recipe?.authorId) {
        val authorId = recipe?.authorId
        if (!authorId.isNullOrBlank()) {
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(authorId).child("username")
            userRef.get().addOnSuccessListener { snapshot ->
                chefUsername = snapshot.getValue(String::class.java) ?: "Unknown"
            }.addOnFailureListener {
                chefUsername = "Unknown"
            }
        } else {
            chefUsername = "Unknown"
        }
    }

    val context = LocalContext.current
    val allIngredientInfos = remember {
        try {
            val input = context.assets.open("ingredients.txt")
            val reader = BufferedReader(InputStreamReader(input))
            reader.readLines().mapNotNull { parseIngredientLine(it) }.sortedBy { it.name }
        } catch (e: Exception) {
            emptyList<IngredientInfo>()
        }
    }

    // Load categories from file
    val allCategories = remember {
        try {
            val input = context.assets.open("categories.txt")
            val reader = BufferedReader(InputStreamReader(input))
            reader.readLines().map { it.trim() }.filter { it.isNotEmpty() }.sortedBy { it.lowercase() }
        } catch (e: Exception) {
            emptyList<String>()
        }
    }
    var categoryPickerOpen by remember { mutableStateOf(false) }
    var categorySearch by remember { mutableStateOf("") }

    LaunchedEffect(ingredientSearch) {
        // Ingredient search functionality handled in the UI
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController, selected = "")
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .background(MaterialTheme.colorScheme.background)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = recipe?.imageUrl?.ifEmpty { "https://via.placeholder.com/600x400" }),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    )
                    // Back button overlay
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopStart)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    // Favorite overlay (top right)
                    IconButton(
                        onClick = { recipe?.let { recipeViewModel.toggleFavorite(it.id, !isFavorite) } },
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopEnd)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(50))
                    ) {
                        if (isFavorite) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Unfavorite", tint = MaterialTheme.colorScheme.error)
                        } else {
                            Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Favorite", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    // Removed title overlay at the bottom
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            recipe?.title ?: "",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        // Removed bottom favorite button
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Person, contentDescription = "Author", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Chef $chefUsername", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Row {
                            (1..5).forEach { i ->
                                if (i <= (recipe?.difficulty ?: 1)) {
                                    Icon(Icons.Filled.Star, contentDescription = "Star $i", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                } else {
                                    Icon(Icons.Outlined.StarBorder, contentDescription = "Star $i", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        // Add like count
                        if (recipe != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Favorite, contentDescription = "Likes", tint = Color(0xFFFFC107), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${recipe.favoriteCounter}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFFFC107))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Time: Soft Yellow, black icon/text
                        Box(
                            modifier = Modifier.weight(1f).padding(end = 4.dp).background(Color(0xFFFFF9C4), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(12.dp)) {
                                Icon(Icons.Filled.AccessTime, contentDescription = "Time", tint = Color.Black)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(recipe?.time?.ifBlank { "-" } ?: "-", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                            }
                        }
                        // Difficulty: Soft Purple, black icon/text
                        Box(
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp).background(Color(0xFFF3E5F5), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(12.dp)) {
                                Icon(Icons.Filled.Star, contentDescription = "Difficulty", tint = Color.Black)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    // Map numeric difficulty to human-readable levels
                                    when (recipe?.difficulty) {
                                        1, 2 -> "Easy"
                                        3 -> "Medium"
                                        4, 5 -> "Hard"
                                        else -> "-"
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.Black
                                )
                            }
                        }
                        // Calories: Soft Green, black icon/text
                        Box(
                            modifier = Modifier.weight(1f).padding(start = 4.dp).background(Color(0xFFE8F5E9), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(12.dp)) {
                                Icon(Icons.Filled.LocalFireDepartment, contentDescription = "Calories", tint = Color.Black)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${recipe?.calories ?: 0} kcal", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Ingredients", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column {
                        recipe?.ingredients?.forEach { ing ->
                            // Parse ingredient string format: "name (amount unit)" using regex
                            val match = Regex("^(.*) \\((.+) (.+)\\)").find(ing)
                            val (name, amount, unit) = if (match != null) {
                                Triple(match.groupValues[1], match.groupValues[2], match.groupValues[3])
                            } else {
                                Triple(ing, "", "")
                            }
                            if (amount.isNotBlank() && unit.isNotBlank()) {
                                Text("• $name ($amount $unit)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            } else {
                                Text("• $name", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    if (recipe?.directions != null && recipe.directions.isNotEmpty()) {
                        Text("Directions", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Column {
                            recipe.directions.forEachIndexed { idx, dir ->
                                Text("Step ${idx + 1}: $dir", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                    if (isAuthor) {
                        Spacer(modifier = Modifier.height(148.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = {
                                    // Reset fields to current values every time dialog opens
                                    editTitle = recipe?.title ?: ""
                                    editDescription = recipe?.description ?: ""
                                    editIngredients = recipe?.ingredients?.joinToString(", ") ?: ""
                                    editCategories = recipe?.categories?.toList() ?: emptyList()
                                    editDifficulty = recipe?.difficulty ?: 1
                                    editTime = recipe?.time ?: ""
                                    // When building editIngredientsList from recipe?.ingredients, use this logic:
                                    editIngredientsList = recipe?.ingredients?.map { ing ->
                                        // Parse ingredient string and calculate calories for editing
                                        val match = Regex("^(.*) \\((.+) (.+)\\)").find(ing)
                                        val (name, amount, unit) = if (match != null) {
                                            Triple(match.groupValues[1], match.groupValues[2], match.groupValues[3])
                                        } else {
                                            // fallback: treat all as name
                                            Triple(ing, "", "")
                                        }
                                        val info = allIngredientInfos.find { it.name == name }
                                        val calories = when (unit) {
                                            "g" -> (info?.caloriesPerG ?: 0.0) * (amount.toDoubleOrNull() ?: 0.0)
                                            "piece" -> (info?.caloriesPerPiece ?: 0.0) * (amount.toDoubleOrNull() ?: 0.0)
                                            "tbsp" -> (info?.caloriesPerTbsp ?: 0.0) * (amount.toDoubleOrNull() ?: 0.0)
                                            "tsp" -> (info?.caloriesPerTsp ?: 0.0) * (amount.toDoubleOrNull() ?: 0.0)
                                            "ml" -> (info?.caloriesPerMl ?: 0.0) * (amount.toDoubleOrNull() ?: 0.0)
                                            "cup" -> (info?.caloriesPerCup ?: 0.0) * (amount.toDoubleOrNull() ?: 0.0)
                                            else -> 0.0
                                        }
                                        Triple(name, "$amount $unit", calories)
                                    } ?: emptyList()
                                    editDirections = recipe?.directions?.toMutableList() ?: mutableListOf()
                                    showEditDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Text("Edit", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { showDeleteDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog && recipe != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Recipe") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Basic information", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Title") })
                    OutlinedTextField(value = editDescription, onValueChange = { editDescription = it }, label = { Text("Description") })
                    OutlinedTextField(value = editTime, onValueChange = { editTime = it }, label = { Text("Time (minutes)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    // --- Categories multi-select ---
                    Text("Categories (up to 5)", style = MaterialTheme.typography.titleMedium)
                    Column {
                        if (editCategories.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                editCategories.forEach { cat ->
                                    FilterChip(
                                        selected = true,
                                        onClick = { editCategories = editCategories.toMutableList().apply { remove(cat) } },
                                        label = { Text(cat) },
                                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                }
                            }
                            if (editCategories.size < 5) {
                                Spacer(modifier = Modifier.height(8.dp))
                                AssistChip(
                                    onClick = { categoryPickerOpen = true },
                                    label = { Text("Add Category") },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        } else {
                            Button(onClick = { categoryPickerOpen = true }) { Text("Pick Categories") }
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Difficulty", style = MaterialTheme.typography.titleMedium)
                    Row {
                        (1..5).forEach { i ->
                            IconButton(onClick = { editDifficulty = i }) {
                                if (i <= editDifficulty) {
                                    Icon(Icons.Filled.Star, contentDescription = "Star $i")
                                } else {
                                    Icon(Icons.Outlined.StarBorder, contentDescription = "Star $i")
                                }
                            }
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Ingredients", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Display ingredient chips with edit and remove functionality in edit dialog
                        editIngredientsList.forEachIndexed { idx, (name, amt, cal) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "$name ($amt)",
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(onClick = {
                                        editIndex = idx
                                        editAmount = amt.split(" ")[0]
                                        editUnit = amt.split(" ").getOrElse(1) { "" }
                                        selectedIngredient = name
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(onClick = {
                                        editIngredientsList = editIngredientsList.toMutableList().also { it.removeAt(idx) }
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove")
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = {
                            ingredientSearch = ""
                            amount = ""
                            unit = ""
                            selectedIngredient = ""
                            editIngredientsDialogOpen = true
                        }) {
                            Text("Add Ingredient")
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Directions", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    
                    // Drag and drop reorderable directions
                    val reorderableState = rememberReorderableLazyListState(
                        onMove = { from, to ->
                            editDirections = editDirections.toMutableList().apply {
                                add(to.index, removeAt(from.index))
                            }
                        }
                    )
                    
                    LazyColumn(
                        state = reorderableState.listState,
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                            .reorderable(reorderableState)
                    ) {
                        itemsIndexed(editDirections, key = { index, item -> "$index-$item" }) { idx, dir ->
                            ReorderableItem(reorderableState, key = "$idx-$dir") { isDragging ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .animateItemPlacement(),
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = if (isDragging) 8.dp else 2.dp
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDragging) 
                                            MaterialTheme.colorScheme.primaryContainer 
                                        else 
                                            MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp)
                                    ) {
                                        // Drag handle
                                        Icon(
                                            Icons.Default.DragHandle,
                                            contentDescription = "Drag to reorder",
                                            modifier = Modifier
                                                .size(24.dp)
                                                .padding(end = 8.dp)
                                                .detectReorder(reorderableState),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        
                                        // Step number
                                        Text(
                                            "Step ${idx + 1}",
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(64.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        
                                        // Direction content
                                        if (editingDirectionIndex == idx) {
                                            OutlinedTextField(
                                                value = newDirectionText,
                                                onValueChange = { newDirectionText = it },
                                                modifier = Modifier.weight(1f),
                                                singleLine = false,
                                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                            IconButton(onClick = {
                                                if (newDirectionText.isNotBlank()) {
                                                    editDirections = editDirections.toMutableList().apply { set(idx, newDirectionText) }
                                                    editingDirectionIndex = -1
                                                    newDirectionText = ""
                                                }
                                            }) { 
                                                Icon(Icons.Default.Check, contentDescription = "Save") 
                                            }
                                        } else {
                                            Text(
                                                dir,
                                                modifier = Modifier.weight(1f),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            IconButton(onClick = {
                                                editingDirectionIndex = idx
                                                newDirectionText = dir
                                            }) { 
                                                Icon(Icons.Default.Edit, contentDescription = "Edit") 
                                            }
                                        }
                                        
                                        // Delete button
                                        IconButton(onClick = {
                                            editDirections = editDirections.toMutableList().apply { removeAt(idx) }
                                            if (editingDirectionIndex == idx) editingDirectionIndex = -1
                                        }) { 
                                            Icon(Icons.Default.Close, contentDescription = "Delete") 
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Add new direction section
                    if (editingDirectionIndex == -2) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Text(
                                    "Step ${editDirections.size + 1}",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(64.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                OutlinedTextField(
                                    value = newDirectionText,
                                    onValueChange = { newDirectionText = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = false,
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                IconButton(onClick = {
                                    if (newDirectionText.isNotBlank()) {
                                        editDirections = editDirections + newDirectionText
                                        newDirectionText = ""
                                        editingDirectionIndex = -1
                                    }
                                }) { 
                                    Icon(Icons.Default.Check, contentDescription = "Add") 
                                }
                            }
                        }
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Button(
                            onClick = {
                                editingDirectionIndex = -2
                                newDirectionText = ""
                            },
                            modifier = Modifier.padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) { 
                            Text("Add Instruction") 
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val updatedRecipe = recipe.copy(
                        title = editTitle,
                        description = editDescription,
                        ingredients = editIngredientsList.map { it.first + " (" + it.second + ")" },
                        categories = editCategories,
                        difficulty = editDifficulty,
                        time = editTime,
                        calories = editIngredientsList.sumOf { it.third }.toInt(), // Always recalculate
                        directions = editDirections.toList() // Include directions
                    )
                    recipeViewModel.updateRecipe(updatedRecipe)
                    showEditDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }
    if (showDeleteDialog && recipe != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Recipe") },
            text = { Text("Are you sure you want to delete this recipe?") },
            confirmButton = {
                TextButton(onClick = {
                    recipeViewModel.deleteRecipe(recipe.id)
                    showDeleteDialog = false
                    navController.popBackStack()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (editIngredientsDialogOpen) {
        Dialog(onDismissRequest = { editIngredientsDialogOpen = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                var addAmountError by remember { mutableStateOf<String?>(null) }
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("Add Ingredient", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    TextField(
                        value = ingredientSearch,
                        onValueChange = { ingredientSearch = it },
                        label = { Text("Search ingredients") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val filteredIngredients = allIngredientInfos.filter { it.name.contains(ingredientSearch, ignoreCase = true) }
                    if (filteredIngredients.isEmpty()) {
                        Text("No results found", modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                        ) {
                            items(filteredIngredients) { ing ->
                                ListItem(
                                    headlineContent = { Text(ing.name) },
                                    modifier = Modifier
                                        .clickable { selectedIngredient = ing.name }
                                        .background(if (selectedIngredient == ing.name) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                )
                            }
                        }
                    }
                    if (!selectedIngredient.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Selected: $selectedIngredient", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val fieldHeight = 60.dp
                            OutlinedTextField(
                                value = amount,
                                onValueChange = {
                                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                        amount = it
                                        addAmountError = if (it.isEmpty() || it.toDoubleOrNull() == null || it.toDoubleOrNull()!! <= 0.0) "Enter a valid amount" else null
                                    }
                                },
                                label = { Text("Amount") },
                                isError = addAmountError != null,
                                supportingText = { if (addAmountError != null) Text(addAmountError!!, color = MaterialTheme.colorScheme.error) },
                                modifier = Modifier.weight(1f).height(fieldHeight),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                                OutlinedTextField(
                                    value = unit,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Unit") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier.menuAnchor().width(120.dp).height(fieldHeight),
                                    textStyle = MaterialTheme.typography.bodyLarge
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    // Build list of available measurement units for the selected ingredient
                                    val availableUnits = allIngredientInfos.find { it.name == selectedIngredient }?.let { info ->
                                        buildList {
                                            if (info.caloriesPerG != -1.0) add("g")
                                            if (info.caloriesPerPiece != -1.0) add("piece")
                                            if (info.caloriesPerTbsp != -1.0) add("tbsp")
                                            if (info.caloriesPerTsp != -1.0) add("tsp")
                                            if (info.caloriesPerMl != -1.0) add("ml")
                                            if (info.caloriesPerCup != -1.0) add("cup")
                                        }
                                    } ?: emptyList()
                                    availableUnits.forEach { u ->
                                        DropdownMenuItem(text = { Text(u) }, onClick = {
                                            unit = u
                                            expanded = false
                                        })
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Button(
                            onClick = {
                                val info = allIngredientInfos.find { it.name == selectedIngredient }
                                val calories = when (unit) {
                                    "g" -> (info?.caloriesPerG ?: 0.0) * (amount.toDoubleOrNull() ?: 0.0)
                                    "piece" -> (info?.caloriesPerPiece ?: 0.0) * (amount.toDoubleOrNull() ?: 0.0)
                                    "tbsp" -> (info?.caloriesPerTbsp ?: 0.0) * (amount.toDoubleOrNull() ?: 0.0)
                                    "tsp" -> (info?.caloriesPerTsp ?: 0.0) * (amount.toDoubleOrNull() ?: 0.0)
                                    "ml" -> (info?.caloriesPerMl ?: 0.0) * (amount.toDoubleOrNull() ?: 0.0)
                                    "cup" -> (info?.caloriesPerCup ?: 0.0) * (amount.toDoubleOrNull() ?: 0.0)
                                    else -> 0.0
                                }
                                if (!selectedIngredient.isNullOrBlank() && amount.isNotBlank() && unit.isNotBlank() && addAmountError == null) {
                                    if (amount.toDoubleOrNull() != null && unit.isNotBlank()) {
                                        editIngredientsList = editIngredientsList + Triple(selectedIngredient!!, "${amount.trim()} ${unit.trim()}", calories)
                                        selectedIngredient = ""
                                        amount = ""
                                        unit = ""
                                        ingredientSearch = ""
                                        editIngredientsDialogOpen = false
                                    } else {
                                        addAmountError = "Please enter a valid amount and unit."
                                    }
                                }
                            },
                            enabled = amount.isNotBlank() && unit.isNotBlank() && addAmountError == null,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }

    // Category picker dialog
    if (categoryPickerOpen) {
        Dialog(onDismissRequest = { categoryPickerOpen = false }) {
            Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 8.dp) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth().heightIn(min = 200.dp, max = 400.dp)) {
                    TextField(
                        value = categorySearch,
                        onValueChange = { categorySearch = it },
                        label = { Text("Search categories") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val filteredCategories = allCategories.filter { it.contains(categorySearch, ignoreCase = true) }
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                        items(filteredCategories) { cat ->
                            val isSelected = editCategories.contains(cat)
                            ListItem(
                                headlineContent = { Text(cat) },
                                trailingContent = { if (isSelected) Icon(Icons.Default.Check, contentDescription = null) },
                                modifier = Modifier
                                    .clickable {
                                        editCategories = if (isSelected) {
                                            editCategories.toMutableList().apply { remove(cat) }
                                        } else {
                                            if (editCategories.size < 5) editCategories.toMutableList().apply { add(cat) } else editCategories
                                        }
                                    }
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { categoryPickerOpen = false }) { Text("Done") }
                    }
                }
            }
        }
    }

    // Ingredient edit dialog for editing an existing ingredient
    if (editIndex != -1) {
        val editing = editIngredientsList.getOrNull(editIndex)
        val info = allIngredientInfos.find { it.name == editing?.first }
        // Build list of available measurement units for the selected ingredient
        val availableUnits = info?.let {
            buildList {
                if (it.caloriesPerG != -1.0) add("g")
                if (it.caloriesPerPiece != -1.0) add("piece")
                if (it.caloriesPerTbsp != -1.0) add("tbsp")
                if (it.caloriesPerTsp != -1.0) add("tsp")
                if (it.caloriesPerMl != -1.0) add("ml")
                if (it.caloriesPerCup != -1.0) add("cup")
            }
        } ?: emptyList()
        // Parse amount/unit robustly
        LaunchedEffect(editIndex) {
            val amtUnit = editing?.second?.trim()?.split(" ") ?: listOf("")
            editAmount = amtUnit.getOrNull(0)?.takeIf { it.matches(Regex("^\\d*\\.?\\d*$")) } ?: ""
            editUnit = amtUnit.getOrNull(1) ?: ""
        }
        var amountError by remember { mutableStateOf<String?>(null) }
        Dialog(onDismissRequest = { editIndex = -1 }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("Edit Ingredient", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    // Always show editing?.first (the full name) above the amount/unit fields.
                    Text(editing?.first ?: "", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val fieldHeight = 60.dp
                        OutlinedTextField(
                            value = editAmount,
                            onValueChange = {
                                if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    editAmount = it
                                    amountError = if (it.isEmpty() || it.toDoubleOrNull() == null || it.toDoubleOrNull()!! <= 0.0) "Enter a valid amount" else null
                                }
                            },
                            label = { Text("Amount") },
                            isError = amountError != null,
                            supportingText = { if (amountError != null) Text(amountError!!, color = MaterialTheme.colorScheme.error) },
                            modifier = Modifier.weight(1f).height(fieldHeight),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                            OutlinedTextField(
                                value = editUnit,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Unit") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().width(120.dp).height(fieldHeight),
                                textStyle = MaterialTheme.typography.bodyLarge
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                availableUnits.forEach { u ->
                                    DropdownMenuItem(text = { Text(u) }, onClick = {
                                        editUnit = u
                                        expanded = false
                                    })
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        TextButton(onClick = { editIndex = -1 }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = {
                                val newCalories = when (editUnit) {
                                    "g" -> (info?.caloriesPerG ?: 0.0) * (editAmount.toDoubleOrNull() ?: 0.0)
                                    "piece" -> (info?.caloriesPerPiece ?: 0.0) * (editAmount.toDoubleOrNull() ?: 0.0)
                                    "tbsp" -> (info?.caloriesPerTbsp ?: 0.0) * (editAmount.toDoubleOrNull() ?: 0.0)
                                    "tsp" -> (info?.caloriesPerTsp ?: 0.0) * (editAmount.toDoubleOrNull() ?: 0.0)
                                    "ml" -> (info?.caloriesPerMl ?: 0.0) * (editAmount.toDoubleOrNull() ?: 0.0)
                                    "cup" -> (info?.caloriesPerCup ?: 0.0) * (editAmount.toDoubleOrNull() ?: 0.0)
                                    else -> 0.0
                                }
                                editIngredientsList = editIngredientsList.toMutableList().also {
                                    if (editIndex in it.indices) it[editIndex] = Triple(editing!!.first, "${editAmount.trim()} ${editUnit.trim()}", newCalories)
                                }
                                editIndex = -1
                            },
                            enabled = amountError == null && editAmount.isNotBlank() && editUnit.isNotBlank(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
} 
 