package com.soumit.mine.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentUser: FirebaseUser? = null
)

class AuthViewModel(
    private val auth: FirebaseAuth = Firebase.auth
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(currentUser = auth.currentUser)
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _uiState.update { it.copy(currentUser = firebaseAuth.currentUser) }
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }

    fun signInWithEmail(email: String, pass: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || pass.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter both email and password") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val result = auth.signInWithEmailAndPassword(trimmedEmail, pass).await()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentUser = result.user,
                        errorMessage = null,
                        successMessage = "Signed in successfully!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = formatAuthError(e)
                    )
                }
            }
        }
    }

    fun signUpWithEmail(name: String, email: String, pass: String, confirmPass: String) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()

        if (trimmedEmail.isEmpty() || pass.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please fill in all required fields") }
            return
        }
        if (pass != confirmPass) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match") }
            return
        }
        if (pass.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val result = auth.createUserWithEmailAndPassword(trimmedEmail, pass).await()
                val user = result.user

                if (trimmedName.isNotEmpty() && user != null) {
                    val profileUpdates = userProfileChangeRequest {
                        displayName = trimmedName
                    }
                    user.updateProfile(profileUpdates).await()
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentUser = auth.currentUser ?: user,
                        errorMessage = null,
                        successMessage = "Account created successfully!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = formatAuthError(e)
                    )
                }
            }
        }
    }

    fun sendPasswordReset(email: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email address") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                auth.sendPasswordResetEmail(trimmedEmail).await()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        successMessage = "Password reset email sent to $trimmedEmail"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = formatAuthError(e)
                    )
                }
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _uiState.update {
            AuthUiState(
                currentUser = null,
                successMessage = "Signed out"
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private fun formatAuthError(e: Exception): String {
        val msg = e.localizedMessage ?: "Authentication failed"
        return when {
            msg.contains("The email address is already in use", ignoreCase = true) ->
                "This email is already registered. Please sign in instead."
            msg.contains("The email address is badly formatted", ignoreCase = true) ->
                "Invalid email format. Please check your email."
            msg.contains("invalid credential", ignoreCase = true) ||
            msg.contains("password is invalid", ignoreCase = true) ||
            msg.contains("user-not-found", ignoreCase = true) ->
                "Incorrect email or password."
            msg.contains("network error", ignoreCase = true) ->
                "Network error. Please check your internet connection."
            else -> msg
        }
    }
}
