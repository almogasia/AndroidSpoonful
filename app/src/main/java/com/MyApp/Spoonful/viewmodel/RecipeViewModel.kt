package com.MyApp.Spoonful.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.MyApp.Spoonful.model.Recipe
import com.MyApp.Spoonful.repository.RecipeRepository
import com.google.firebase.auth.FirebaseAuth

/**
 * ViewModel responsible for managing recipe data and user favorites.
 * 
 * Provides reactive access to recipes and user favorites through LiveData.
 * Handles CRUD operations for recipes and manages favorite state synchronization
 * with Firebase Realtime Database via RecipeRepository.
 */
class RecipeViewModel : ViewModel() {
    private val _recipes = MutableLiveData<List<Recipe>>(emptyList())
    val recipes: LiveData<List<Recipe>> = _recipes

    private val _favorites = MutableLiveData<List<String>>(emptyList())
    val favorites: LiveData<List<String>> = _favorites

    private val _editSuccess = MutableLiveData<Boolean?>(null)
    val editSuccess: LiveData<Boolean?> = _editSuccess
    private val _deleteSuccess = MutableLiveData<Boolean?>(null)
    val deleteSuccess: LiveData<Boolean?> = _deleteSuccess

    private val auth = FirebaseAuth.getInstance()

    init {
        fetchRecipes()
        loadFavorites()
    }

    fun fetchRecipes() {
        RecipeRepository.getRecipes { list ->
            _recipes.postValue(list)
        }
    }

    fun addRecipe(recipe: Recipe, onComplete: (Boolean) -> Unit) {
        RecipeRepository.addRecipe(recipe, onComplete)
    }

    fun loadFavorites() {
        val uid = auth.currentUser?.uid ?: return
        RecipeRepository.getUserFavorites(uid) { favs ->
            _favorites.postValue(favs)
        }
    }

    fun toggleFavorite(recipeId: String, isFavorite: Boolean, onComplete: (Boolean) -> Unit = {}) {
        val uid = auth.currentUser?.uid ?: return
        RecipeRepository.setUserFavorite(uid, recipeId, isFavorite) { ok ->
            if (ok) loadFavorites()
            onComplete(ok)
        }
    }

    fun isFavorite(recipeId: String): Boolean {
        return favorites.value?.contains(recipeId) == true
    }

    fun updateRecipe(recipe: Recipe) {
        RecipeRepository.updateRecipe(recipe) { ok ->
            _editSuccess.postValue(ok)
            fetchRecipes()
        }
    }

    fun deleteRecipe(recipeId: String) {
        RecipeRepository.deleteRecipe(recipeId) { ok ->
            _deleteSuccess.postValue(ok)
            fetchRecipes()
        }
    }
} 