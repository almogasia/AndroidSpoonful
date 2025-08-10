package com.MyApp.Spoonful.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.MyApp.Spoonful.viewmodel.AuthViewModel
import com.MyApp.Spoonful.viewmodel.RecipeViewModel
import com.MyApp.Spoonful.model.Recipe
import androidx.compose.material.icons.filled.Add
import com.MyApp.Spoonful.ui.components.BottomNavigationBar
import androidx.compose.ui.Alignment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.MyApp.Spoonful.repository.UnsplashApi
import com.MyApp.Spoonful.repository.UnsplashPhoto
import com.MyApp.Spoonful.BuildConfig
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.input.ImeAction
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ListItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import org.burnoutcrew.reorderable.*
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.foundation.ExperimentalFoundationApi

/**
 * Data class representing nutritional information for ingredients.
 * 
 * Contains calorie values for different measurement units to support
 * accurate calorie calculation during recipe creation.
 */
data class IngredientInfo(
    val name: String,
    val caloriesPerG: Double,
    val caloriesPerPiece: Double,
    val caloriesPerTbsp: Double,
    val caloriesPerTsp: Double,
    val caloriesPerMl: Double,
    val caloriesPerCup: Double
)

/**
 * Parses ingredient data from the ingredients.txt file format.
 * 
 * Expects pipe-separated values with 7 columns: name and calorie values
 * for different measurement units. Returns null for invalid lines.
 * 
 * @param line Raw line from ingredients.txt file
 * @return IngredientInfo object or null if parsing fails
 */
fun parseIngredientLine(line: String): IngredientInfo? {
    val parts = line.split("|")
    if (parts.size != 7) return null
    return IngredientInfo(
        name = parts[0].trim(),
        caloriesPerG = parts[1].toDoubleOrNull() ?: -1.0,
        caloriesPerPiece = parts[2].toDoubleOrNull() ?: -1.0,
        caloriesPerTbsp = parts[3].toDoubleOrNull() ?: -1.0,
        caloriesPerTsp = parts[4].toDoubleOrNull() ?: -1.0,
        caloriesPerMl = parts[5].toDoubleOrNull() ?: -1.0,
        caloriesPerCup = parts[6].toDoubleOrNull() ?: -1.0
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
/**
 * Recipe creation screen composable for uploading new recipes.
 * 
 * Provides comprehensive recipe creation interface with title, description,
 * ingredient selection with calorie calculation, category assignment,
 * difficulty rating, cooking time, and step-by-step directions.
 * Integrates with ingredient database and category system for structured input.
 * 
 * @param navController Navigation controller for screen transitions
 * @param authViewModel ViewModel for user authentication
 * @param recipeViewModel ViewModel for recipe operations
 */
@Composable
fun UploadRecipeScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    recipeViewModel: RecipeViewModel = viewModel()
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategories by remember { mutableStateOf(listOf<String>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    var ingredientDialogOpen by remember { mutableStateOf(false) }
    var ingredientSearch by remember { mutableStateOf("") }
    var selectedIngredient by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var ingredientsList by remember { mutableStateOf(listOf<Triple<String, String, Double>>()) }
    var editIndex by remember { mutableStateOf(-1) }
    var editAmount by remember { mutableStateOf("") }
    var editUnit by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf(1) }
    var time by remember { mutableStateOf("") }

    var directions by remember { mutableStateOf(listOf<String>()) }
    var editingDirectionIndex by remember { mutableStateOf(-1) }
    var newDirectionText by remember { mutableStateOf("") }

    // Load ingredients from file (assets or raw resource)
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
    val filteredIngredients = allIngredientInfos.filter { it.name.contains(ingredientSearch, ignoreCase = true) }

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

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController, selected = "upload")
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
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text("Upload Recipe", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Card with image and form
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
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Basic information section
                    Text(
                        "Basic information",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black,
                        modifier = Modifier.align(Alignment.Start).padding(start = 16.dp, top = 16.dp)
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Time (minutes)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp)
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    // Calories box
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val totalCalories = ingredientsList.sumOf { it.third }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                            Icon(Icons.Filled.LocalFireDepartment, contentDescription = "Calories", tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${totalCalories.toInt()} kcal", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Ingredients
                    Text("Ingredients", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.Black, modifier = Modifier.align(Alignment.Start).padding(start = 16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Button(onClick = { ingredientDialogOpen = true }, shape = RoundedCornerShape(16.dp)) {
                            Text("Add Ingredient")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Display ingredient chips with edit and remove functionality
                        ingredientsList.forEachIndexed { idx, (name, amt, cal) ->
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
                                    IconButton(onClick = { editIndex = idx; editAmount = amt.split(" ")[0]; editUnit = amt.split(" ").getOrElse(1) { "" } }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(onClick = {
                                        ingredientsList = ingredientsList.toMutableList().also { it.removeAt(idx) }
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove")
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    // Categories
                    Text("Categories (up to 5)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.Black, modifier = Modifier.align(Alignment.Start).padding(start = 16.dp))
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        if (selectedCategories.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                selectedCategories.forEach { cat ->
                                    FilterChip(
                                        selected = true,
                                        onClick = { selectedCategories = selectedCategories - cat },
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
                            if (selectedCategories.size < 5) {
                                Spacer(Modifier.height(8.dp))
                                AssistChip(
                                    onClick = { categoryPickerOpen = true },
                                    label = { Text("Add Category") },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        } else {
                            Button(onClick = { categoryPickerOpen = true }, shape = RoundedCornerShape(12.dp)) {
                                Text("Pick Categories")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    // Difficulty
                    Text("Difficulty", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.Black, modifier = Modifier.align(Alignment.Start).padding(start = 16.dp))
                    Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                        (1..5).forEach { i ->
                            IconButton(onClick = { difficulty = i }) {
                                if (i <= difficulty) {
                                    Icon(Icons.Filled.Star, contentDescription = "Star $i", tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    Icon(Icons.Outlined.StarBorder, contentDescription = "Star $i", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    // Directions
                    Text("Directions", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    
                    // Drag and drop reorderable directions
                    val reorderableState = rememberReorderableLazyListState(
                        onMove = { from, to ->
                            directions = directions.toMutableList().apply {
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
                        itemsIndexed(directions, key = { index, item -> "$index-$item" }) { idx, dir ->
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
                                                    directions = directions.toMutableList().apply { set(idx, newDirectionText) }
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
                                            directions = directions.toMutableList().apply { removeAt(idx) }
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
                                    "Step ${directions.size + 1}",
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
                                        directions = directions + newDirectionText
                                        newDirectionText = ""
                                        editingDirectionIndex = -1
                                    }
                                }) { 
                                    Icon(Icons.Default.Check, contentDescription = "Add") 
                                }
                            }
                        }
                    }
                    
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
                    Spacer(modifier = Modifier.height(24.dp))
                    if (error != null) {
                        Text(error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (success) {
                        Text("Recipe uploaded successfully!", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Button(
                        onClick = {
                            if (title.isBlank() || description.isBlank() || selectedCategories.isEmpty()) {
                                error = "All fields are required."
                                return@Button
                            }
                            loading = true
                            error = null
                            coroutineScope.launch {
                                val unsplashApi = UnsplashApi.create()
                                val accessKey = BuildConfig.UNSPLASH_ACCESS_KEY
                                var imageUrl = "https://via.placeholder.com/600x400"
                                try {
                                    val response = unsplashApi.getRandomPhoto(
                                        authorization = "Client-ID $accessKey",
                                        query = title
                                    )
                                    if (response.isSuccessful) {
                                        val photo = response.body()
                                        if (photo != null) {
                                            imageUrl = photo.urls.regular
                                        }
                                    }
                                } catch (e: Exception) {
                                    // fallback to placeholder
                                }
                                val totalCalories = ingredientsList.sumOf { it.third }
                                // Validate ingredient format: name must be non-blank and amount must have exactly 2 parts (value + unit)
                                val validIngredients = ingredientsList.filter { (name, amt, _) ->
                                    val parts = amt.trim().split(" ")
                                    name.isNotBlank() && parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()
                                }
                                val recipe = Recipe(
                                    title = title,
                                    description = description,
                                    ingredients = validIngredients.map { it.first + " (" + it.second + ")" },
                                    categories = selectedCategories,
                                    imageUrl = imageUrl,
                                    authorId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                                    calories = totalCalories.toInt(),
                                    difficulty = difficulty,
                                    time = time,
                                    directions = directions // Include directions
                                )
                                recipeViewModel.addRecipe(recipe) { ok ->
                                    loading = false
                                    if (ok) {
                                        success = true
                                        navController.navigate("home") {
                                            popUpTo("upload") { inclusive = true }
                                        }
                                    } else {
                                        error = "Failed to upload recipe."
                                    }
                                }
                            }
                        },
                        enabled = !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        else Text("Upload Recipe", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
            // Ingredient picker dialog
            if (ingredientDialogOpen) {
                Dialog(onDismissRequest = { ingredientDialogOpen = false }) {
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
                            if (selectedIngredient.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Selected: $selectedIngredient", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val fieldHeight = 60.dp
                                    OutlinedTextField(
                                        value = amount,
                                        onValueChange = {
                                            // Validate numeric input with optional decimal point
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
                                            // Build list of available measurement units for selected ingredient
                                            val availableUnits = allIngredientInfos.find { it.name == selectedIngredient }?.let { info ->
                                                buildList {
                                                    if (info != null) {
                                                        if (info.caloriesPerG != -1.0) add("g")
                                                        if (info.caloriesPerPiece != -1.0) add("piece")
                                                        if (info.caloriesPerTbsp != -1.0) add("tbsp")
                                                        if (info.caloriesPerTsp != -1.0) add("tsp")
                                                        if (info.caloriesPerMl != -1.0) add("ml")
                                                        if (info.caloriesPerCup != -1.0) add("cup")
                                                    }
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
                                            if (selectedIngredient.isNotBlank() && amount.isNotBlank() && unit.isNotBlank() && addAmountError == null) {
                                                // Only add if amount and unit are both present and valid
                                                if (amount.toDoubleOrNull() != null && unit.isNotBlank()) {
                                                    ingredientsList = ingredientsList + Triple(selectedIngredient, "$amount $unit", calories)
                                                    selectedIngredient = ""
                                                    amount = ""
                                                    unit = ""
                                                    ingredientSearch = ""
                                                    ingredientDialogOpen = false
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
            }
            // Ingredient edit dialog
            if (editIndex != -1) {
                val editing = ingredientsList.getOrNull(editIndex)
                val info = allIngredientInfos.find { it.name == editing?.first }
                // Build list of available measurement units for the selected ingredient
                val availableUnits = buildList {
                    if (info != null) {
                        if (info.caloriesPerG != -1.0) add("g")
                        if (info.caloriesPerPiece != -1.0) add("piece")
                        if (info.caloriesPerTbsp != -1.0) add("tbsp")
                        if (info.caloriesPerTsp != -1.0) add("tsp")
                        if (info.caloriesPerMl != -1.0) add("ml")
                        if (info.caloriesPerCup != -1.0) add("cup")
                    }
                }
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
                                        // Calculate calories based on ingredient unit and amount
                                        val newCalories = when (editUnit) {
                                            "g" -> (info?.caloriesPerG ?: 0.0) * (editAmount.toDoubleOrNull() ?: 0.0)
                                            "piece" -> (info?.caloriesPerPiece ?: 0.0) * (editAmount.toDoubleOrNull() ?: 0.0)
                                            "tbsp" -> (info?.caloriesPerTbsp ?: 0.0) * (editAmount.toDoubleOrNull() ?: 0.0)
                                            "tsp" -> (info?.caloriesPerTsp ?: 0.0) * (editAmount.toDoubleOrNull() ?: 0.0)
                                            "ml" -> (info?.caloriesPerMl ?: 0.0) * (editAmount.toDoubleOrNull() ?: 0.0)
                                            "cup" -> (info?.caloriesPerCup ?: 0.0) * (editAmount.toDoubleOrNull() ?: 0.0)
                                            else -> 0.0
                                        }
                                        ingredientsList = ingredientsList.toMutableList().also {
                                            if (editIndex in it.indices) it[editIndex] = Triple(editing!!.first, "$editAmount $editUnit", newCalories)
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
                                    val isSelected = selectedCategories.contains(cat)
                                    ListItem(
                                        headlineContent = { Text(cat) },
                                        trailingContent = { if (isSelected) Icon(Icons.Default.Check, contentDescription = null) },
                                        modifier = Modifier
                                            .clickable {
                                                selectedCategories = if (isSelected) {
                                                    selectedCategories - cat
                                                } else {
                                                    if (selectedCategories.size < 5) selectedCategories + cat else selectedCategories
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
        }
    }
} 