package com.MyApp.Spoonful.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.google.firebase.database.FirebaseDatabase

/**
 * ViewModel responsible for managing Firebase Authentication state and user operations.
 * 
 * Provides reactive authentication state through LiveData, handles user registration,
 * login, and logout operations. Automatically manages Firebase AuthStateListener
 * lifecycle and stores user metadata in Firebase Realtime Database.
 */
class AuthViewModel : ViewModel() {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _user = MutableLiveData<FirebaseUser?>(firebaseAuth.currentUser)
    val user: LiveData<FirebaseUser?> = _user
    val isLoggedIn: LiveData<Boolean> = user.map { it != null }

    private val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        _user.value = auth.currentUser
    }

    init {
        firebaseAuth.addAuthStateListener(authListener)
    }

    fun register(email: String, password: String, username: String, onSuccess: () -> Unit) {
        _loading.value = true
        _error.value = null
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _loading.value = false
                if (task.isSuccessful) {
                    val uid = firebaseAuth.currentUser?.uid
                    if (uid != null) {
                        val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
                        userRef.child("username").setValue(username)
                        userRef.child("joined").setValue(System.currentTimeMillis())
                    }
                    onSuccess()
                } else {
                    _error.value = task.exception?.localizedMessage
                }
            }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        _loading.value = true
        _error.value = null
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _loading.value = false
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    _error.value = task.exception?.localizedMessage
                }
            }
    }

    fun logout() {
        firebaseAuth.signOut()
    }

    fun setError(msg: String?) { _error.value = msg }

    override fun onCleared() {
        super.onCleared()
        firebaseAuth.removeAuthStateListener(authListener)
    }
} 