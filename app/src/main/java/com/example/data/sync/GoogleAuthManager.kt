package com.example.data.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class GoogleUser(
    val email: String,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val id: String? = null
)

sealed interface AuthState {
    data object SignedOut : AuthState
    data object Loading : AuthState
    data class SignedIn(val user: GoogleUser) : AuthState
    data class Error(val message: String) : AuthState
}

class GoogleAuthManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("glass_notes_prefs", Context.MODE_PRIVATE)
    private val _authState = MutableStateFlow<AuthState>(loadInitialAuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private fun loadInitialAuthState(): AuthState {
        val savedEmail = prefs.getString("pref_google_account_email", null)
        return if (!savedEmail.isNullOrBlank()) {
            val name = prefs.getString("pref_google_account_name", null)
            val photo = prefs.getString("pref_google_account_photo", null)
            AuthState.SignedIn(GoogleUser(email = savedEmail, displayName = name, photoUrl = photo))
        } else {
            AuthState.SignedOut
        }
    }

    suspend fun getAccessToken(context: Context, email: String): String? = withContext(Dispatchers.IO) {
        try {
            // First check if a cached token exists in prefs
            val cachedToken = prefs.getString("pref_google_access_token", null)
            if (!cachedToken.isNullOrBlank()) {
                return@withContext cachedToken
            }

            // Attempt to retrieve auth token from Android AccountManager if available
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.getAccountsByType("com.google")
            val targetAccount = accounts.find { it.name.equals(email, ignoreCase = true) }
            if (targetAccount != null) {
                val tokenFuture = accountManager.getAuthToken(
                    targetAccount,
                    "oauth2:https://www.googleapis.com/auth/drive.appdata https://www.googleapis.com/auth/drive.file",
                    null,
                    false,
                    null,
                    null
                )
                val bundle = tokenFuture.result
                val token = bundle.getString(AccountManager.KEY_AUTHTOKEN)
                if (!token.isNullOrBlank()) {
                    prefs.edit().putString("pref_google_access_token", token).apply()
                    return@withContext token
                }
            }
        } catch (e: Exception) {
            Log.w("GoogleAuthManager", "Could not acquire system auth token: ${e.message}")
        }
        // Return null or cached session
        prefs.getString("pref_google_access_token", null)
    }

    fun setSignedInUser(user: GoogleUser, accessToken: String? = null) {
        prefs.edit()
            .putString("pref_google_account_email", user.email)
            .putString("pref_google_account_name", user.displayName)
            .putString("pref_google_account_photo", user.photoUrl)
            .apply()
        if (!accessToken.isNullOrBlank()) {
            prefs.edit().putString("pref_google_access_token", accessToken).apply()
        }
        _authState.value = AuthState.SignedIn(user)
    }

    fun signOut() {
        prefs.edit()
            .remove("pref_google_account_email")
            .remove("pref_google_account_name")
            .remove("pref_google_account_photo")
            .remove("pref_google_access_token")
            .apply()
        _authState.value = AuthState.SignedOut
    }

    fun setError(message: String) {
        _authState.value = AuthState.Error(message)
    }
}
