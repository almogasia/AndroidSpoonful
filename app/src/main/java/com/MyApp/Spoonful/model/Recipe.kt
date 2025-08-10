package com.MyApp.Spoonful.model

/**
 * Data model representing a recipe in the Spoonful application.
 * 
 * Contains all essential recipe information including ingredients, directions,
 * metadata, and user engagement metrics. Supports multiple categories per recipe
 * and integrates with Firebase for real-time data synchronization.
 */
data class Recipe(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val ingredients: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val imageUrl: String = "",
    val authorId: String = "",
    val calories: Int = 0,
    val difficulty: Int = 1,
    val time: String = "",
    val favoriteCounter: Int = 0,
    val directions: List<String> = emptyList()
) 