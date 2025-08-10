package com.MyApp.Spoonful.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.MyApp.Spoonful.model.Recipe
import androidx.compose.runtime.Composable

/**
 * Reusable recipe card component for displaying recipe previews.
 * 
 * Displays recipe image, title, and categories in a horizontal card layout.
 * Uses Material Design 3 styling with rounded corners and elevation.
 * Handles click events for navigation to recipe details.
 * 
 * @param recipe Recipe object containing display data
 * @param onClick Callback function for card click events
 */
@Composable
fun RecipeCard(recipe: Recipe, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = recipe.imageUrl.ifEmpty { "https://via.placeholder.com/80" }
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(recipe.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                val cats = if (recipe.categories.isEmpty()) "" else recipe.categories.joinToString(", ")
                Text(cats, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
} 