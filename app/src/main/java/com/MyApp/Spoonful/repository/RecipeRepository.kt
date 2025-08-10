package com.MyApp.Spoonful.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.MyApp.Spoonful.model.Recipe

/**
 * Repository class responsible for all Firebase Realtime Database operations related to recipes.
 * 
 * Implements the Repository pattern to abstract data access logic from the ViewModels.
 * Handles CRUD operations for recipes, user favorites, and maintains data consistency
 * through Firebase transactions for counters and user statistics.
 */
object RecipeRepository {
    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("recipes")
    private val userRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("users")

    fun addRecipe(recipe: Recipe, onComplete: (Boolean) -> Unit) {
        val key = dbRef.push().key ?: return onComplete(false)
        val recipeWithId = recipe.copy(id = key)
        dbRef.child(key).setValue(recipeWithId)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Increment user's recipesCreated
                    userRef.child(recipe.authorId).child("recipesCreated").runTransaction(object : Transaction.Handler {
                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                            var count = currentData.getValue(Int::class.java) ?: 0
                            currentData.value = count + 1
                            return Transaction.success(currentData)
                        }
                        override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {}
                    })
                }
                onComplete(task.isSuccessful)
            }
    }

    fun getRecipes(onData: (List<Recipe>) -> Unit) {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(Recipe::class.java) }
                onData(list)
            }
            override fun onCancelled(error: DatabaseError) {
                onData(emptyList())
            }
        })
    }

    fun getUserFavorites(uid: String, onData: (List<String>) -> Unit) {
        userRef.child(uid).child("favorites").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(String::class.java) }
                onData(list)
            }
            override fun onCancelled(error: DatabaseError) {
                onData(emptyList())
            }
        })
    }

    /**
     * Manages user favorite state with atomic counter updates using Firebase transactions.
     * Updates both user favorites list and recipe favorite counter atomically.
     */
    fun setUserFavorite(uid: String, recipeId: String, isFavorite: Boolean, onComplete: (Boolean) -> Unit) {
        val favRef = userRef.child(uid).child("favorites").child(recipeId)
        val recipeRef = dbRef.child(recipeId)
        if (isFavorite) {
            favRef.setValue(recipeId).addOnCompleteListener { favTask ->
                if (favTask.isSuccessful) {
                    // Increment favoriteCounter
                    recipeRef.child("favoriteCounter").runTransaction(object : Transaction.Handler {
                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                            var count = currentData.getValue(Int::class.java) ?: 0
                            currentData.value = count + 1
                            return Transaction.success(currentData)
                        }
                        override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {}
                    })
                }
                onComplete(favTask.isSuccessful)
            }
        } else {
            favRef.removeValue().addOnCompleteListener { favTask ->
                if (favTask.isSuccessful) {
                    // Decrement favoriteCounter
                    recipeRef.child("favoriteCounter").runTransaction(object : Transaction.Handler {
                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                            var count = currentData.getValue(Int::class.java) ?: 0
                            if (count > 0) currentData.value = count - 1
                            return Transaction.success(currentData)
                        }
                        override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {}
                    })
                }
                onComplete(favTask.isSuccessful)
            }
        }
    }

    fun updateRecipe(recipe: Recipe, onComplete: (Boolean) -> Unit) {
        if (recipe.id.isBlank()) return onComplete(false)
        dbRef.child(recipe.id).setValue(recipe).addOnCompleteListener { task ->
            onComplete(task.isSuccessful)
        }
    }

    fun deleteRecipe(recipeId: String, onComplete: (Boolean) -> Unit) {
        if (recipeId.isBlank()) return onComplete(false)
        dbRef.child(recipeId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val recipe = snapshot.getValue(Recipe::class.java)
                dbRef.child(recipeId).removeValue().addOnCompleteListener { task ->
                    if (task.isSuccessful && recipe != null) {
                        // Decrement user's recipesCreated
                        userRef.child(recipe.authorId).child("recipesCreated").runTransaction(object : Transaction.Handler {
                            override fun doTransaction(currentData: MutableData): Transaction.Result {
                                var count = currentData.getValue(Int::class.java) ?: 0
                                if (count > 0) currentData.value = count - 1
                                return Transaction.success(currentData)
                            }
                            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {}
                        })
                    }
                    onComplete(task.isSuccessful)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                onComplete(false)
            }
        })
    }
} 