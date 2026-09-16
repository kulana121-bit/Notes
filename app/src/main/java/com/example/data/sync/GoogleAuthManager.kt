package com.example.data.sync

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class GoogleUserProfile(
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val id: String? = null
)

/**
 * Authoritative authentication state machine.
 * Email string is NEVER authentication. Only verified Google credentials dictate auth state.
 */
sealed class AuthState {
    object Loading : AuthState()
    object SignedOut : AuthState()
    data class SignedIn(
        val user: GoogleUserProfile,
        val verifiedAt: Long = System.currentTimeMillis()
    ) : AuthState()
    data class ReauthRequired(
        val email: String,
        val message: String
    ) : AuthState()
    data class Error(val message: String) : AuthState()
}

class GoogleAuthManager(private val context: Context) {

    companion object {
        private const val TAG = "GoogleAuthManager"
        private const val PREFS_NAME = "google_auth_secure_prefs"
        private const val KEY_VERIFIED_EMAIL = "verified_auth_email"
        private const val KEY_VERIFIED_NAME = "verified_auth_name"
        private const val KEY_VERIFIED_PHOTO = "verified_auth_photo"
        private const val KEY_VERIFIED_ID = "verified_auth_id"

        // Least-privilege scopes: Dedicated AppData folder, files created by app, and basic userinfo
        const val SCOPE_DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
        const val SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
        const val SCOPE_EMAIL = "https://www.googleapis.com/auth/userinfo.email"
        const val SCOPE_PROFILE = "https://www.googleapis.com/auth/userinfo.profile"

        private const val OAUTH_SCOPE_STRING = "oauth2:$SCOPE_DRIVE_APPDATA $SCOPE_DRIVE_FILE $SCOPE_EMAIL $SCOPE_PROFILE"
    }

    private val authScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val securePrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestProfile()
        .requestScopes(
            Scope(SCOPE_DRIVE_APPDATA),
            Scope(SCOPE_DRIVE_FILE),
            Scope(SCOPE_EMAIL),
            Scope(SCOPE_PROFILE)
        )
        .build()

    private val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Derived states
    val currentUser: StateFlow<GoogleUserProfile?> = _authState
        .map { state -> (state as? AuthState.SignedIn)?.user }
        .stateIn(authScope, SharingStarted.Eagerly, null)

    val isDriveConnected: StateFlow<Boolean> = _authState
        .map { state -> state is AuthState.SignedIn }
        .stateIn(authScope, SharingStarted.Eagerly, false)

    init {
        // Automatically restore previous authenticated session on app start
        authScope.launch {
            restoreSession()
        }
    }

    /**
     * Session Restoration (Fixes automatic logout on app restart).
     * Authoritatively revalidates with Google Play Services and system account manager.
     */
    suspend fun restoreSession(): AuthState = withContext(Dispatchers.IO) {
        try {
            // 1. Check GoogleSignIn last signed in account
            val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
            if (lastAccount != null && !lastAccount.email.isNullOrBlank()) {
                val profile = GoogleUserProfile(
                    email = lastAccount.email!!,
                    displayName = lastAccount.displayName ?: lastAccount.email!!,
                    photoUrl = lastAccount.photoUrl?.toString(),
                    id = lastAccount.id
                )
                persistVerifiedAccount(profile)
                val newState = AuthState.SignedIn(profile)
                _authState.value = newState
                Log.d(TAG, "Restored session via GoogleSignIn: ${profile.email}")
                return@withContext newState
            }

            // 2. Check if a previously verified account exists on device
            val savedEmail = securePrefs.getString(KEY_VERIFIED_EMAIL, null)
            if (!savedEmail.isNullOrBlank()) {
                val am = android.accounts.AccountManager.get(context)
                val accounts = am.getAccountsByType("com.google")
                val matching = accounts.find { it.name.equals(savedEmail, ignoreCase = true) }

                if (matching != null) {
                    try {
                        // Test if token can be obtained to prove active authorization
                        val token = GoogleAuthUtil.getToken(context, matching, OAUTH_SCOPE_STRING)
                        if (!token.isNullOrBlank()) {
                            val savedName = securePrefs.getString(KEY_VERIFIED_NAME, null) ?: matching.name.substringBefore("@")
                            val savedPhoto = securePrefs.getString(KEY_VERIFIED_PHOTO, null)
                            val savedId = securePrefs.getString(KEY_VERIFIED_ID, null)
                            val profile = GoogleUserProfile(
                                email = matching.name,
                                displayName = savedName,
                                photoUrl = savedPhoto,
                                id = savedId
                            )
                            val newState = AuthState.SignedIn(profile)
                            _authState.value = newState
                            Log.d(TAG, "Restored session via AccountManager: ${profile.email}")
                            return@withContext newState
                        }
                    } catch (e: UserRecoverableAuthException) {
                        Log.w(TAG, "User recoverable auth required for $savedEmail: ${e.message}")
                        val newState = AuthState.ReauthRequired(savedEmail, e.localizedMessage ?: "Reauthorization required")
                        _authState.value = newState
                        return@withContext newState
                    } catch (e: Exception) {
                        Log.w(TAG, "Token validation failed during restore: ${e.message}")
                    }
                }
            }

            // If no valid session was restored
            clearStoredAccount()
            val newState = AuthState.SignedOut
            _authState.value = newState
            newState
        } catch (e: Exception) {
            Log.e(TAG, "Exception during session restore", e)
            val newState = AuthState.SignedOut
            _authState.value = newState
            newState
        }
    }

    fun getSignInIntent(): Intent {
        try {
            googleSignInClient.signOut()
        } catch (_: Exception) {}
        return googleSignInClient.signInIntent
    }

    /**
     * Native Android OS System Account Picker intent.
     * Launches the system dialog showing authentic Google accounts registered on this device.
     */
    fun getSystemAccountPickerIntent(currentEmail: String? = null): Intent {
        val selectedAccount = if (!currentEmail.isNullOrBlank()) {
            android.accounts.Account(currentEmail, "com.google")
        } else null

        return android.accounts.AccountManager.newChooseAccountIntent(
            selectedAccount,
            null,
            arrayOf("com.google"),
            null,
            null,
            null,
            null
        )
    }

    suspend fun handleSignInResult(data: Intent?): Result<GoogleUserProfile> = withContext(Dispatchers.IO) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            if (account != null && !account.email.isNullOrBlank()) {
                val profile = GoogleUserProfile(
                    email = account.email!!,
                    displayName = account.displayName ?: account.email!!,
                    photoUrl = account.photoUrl?.toString(),
                    id = account.id
                )
                persistVerifiedAccount(profile)
                _authState.value = AuthState.SignedIn(profile)
                Log.d(TAG, "Google Sign-In successful for: ${profile.email}")
                Result.success(profile)
            } else {
                val error = "Google Sign-In returned null account"
                _authState.value = AuthState.Error(error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            val msg = e.localizedMessage ?: "Google Sign-In failed"
            _authState.value = AuthState.Error(msg)
            Result.failure(e)
        }
    }

    suspend fun handleAccountPickerResult(data: Intent?): Result<GoogleUserProfile> = withContext(Dispatchers.IO) {
        val accountName = data?.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME)
        if (!accountName.isNullOrBlank()) {
            // Verify that this account actually exists in Android AccountManager
            val am = android.accounts.AccountManager.get(context)
            val accounts = am.getAccountsByType("com.google")
            val matching = accounts.find { it.name.equals(accountName, ignoreCase = true) }

            if (matching == null) {
                val err = "Account '$accountName' is not registered on this device"
                return@withContext Result.failure(SecurityException(err))
            }

            try {
                // Verify authorization by fetching OAuth token
                val token = GoogleAuthUtil.getToken(context, matching, OAUTH_SCOPE_STRING)
                if (token.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("Failed to obtain authorization token for $accountName"))
                }

                val displayName = accountName.substringBefore("@")
                    .split(".", "_", "-")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
                    .ifBlank { accountName }

                val profile = GoogleUserProfile(
                    email = accountName,
                    displayName = displayName,
                    id = accountName
                )
                persistVerifiedAccount(profile)
                _authState.value = AuthState.SignedIn(profile)
                Log.d(TAG, "Device Google Account authorized: ${profile.email}")
                Result.success(profile)
            } catch (e: UserRecoverableAuthException) {
                _authState.value = AuthState.ReauthRequired(accountName, e.localizedMessage ?: "Authorization required")
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("No Google account selected from device"))
        }
    }

    /**
     * Explicit Sign Out.
     * Revokes Google session, clears stored verified account preferences, and resets auth state.
     */
    suspend fun signOut(activity: Activity? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Clear any cached token
            val currentEmail = (_authState.value as? AuthState.SignedIn)?.user?.email
            if (!currentEmail.isNullOrBlank()) {
                try {
                    val account = android.accounts.Account(currentEmail, "com.google")
                    val token = GoogleAuthUtil.getToken(context, account, OAUTH_SCOPE_STRING)
                    if (!token.isNullOrBlank()) {
                        GoogleAuthUtil.clearToken(context, token)
                    }
                } catch (_: Exception) {}
            }

            try {
                googleSignInClient.signOut().await()
            } catch (_: Exception) {}

            clearStoredAccount()
            _authState.value = AuthState.SignedOut
            Log.d(TAG, "Google Sign-Out completed")
            Result.success(Unit)
        } catch (e: Exception) {
            clearStoredAccount()
            _authState.value = AuthState.SignedOut
            Result.success(Unit)
        }
    }

    /**
     * Authoritative access token retriever.
     * ONLY returns a token if the user is genuinely authenticated and matches the requested account.
     * Never returns a fake token, another user's token, or a developer token.
     */
    suspend fun getAccessToken(context: Context, forEmail: String? = null): String? = withContext(Dispatchers.IO) {
        val currentSignedIn = (_authState.value as? AuthState.SignedIn)?.user
        if (currentSignedIn == null) {
            Log.w(TAG, "getAccessToken called but user is NOT signed in")
            return@withContext null
        }

        if (forEmail != null && !forEmail.equals(currentSignedIn.email, ignoreCase = true)) {
            Log.e(TAG, "Security check failed: getAccessToken for '$forEmail' does not match authenticated user '${currentSignedIn.email}'")
            return@withContext null
        }

        val targetEmail = currentSignedIn.email
        try {
            val androidAccount = android.accounts.Account(targetEmail, "com.google")
            val token = GoogleAuthUtil.getToken(context, androidAccount, OAUTH_SCOPE_STRING)
            if (!token.isNullOrBlank()) {
                return@withContext token
            }
        } catch (e: UserRecoverableAuthException) {
            Log.w(TAG, "User recoverable auth required for $targetEmail: ${e.message}")
            _authState.value = AuthState.ReauthRequired(targetEmail, e.localizedMessage ?: "Reauthorization required")
            return@withContext null
        } catch (e: Exception) {
            Log.w(TAG, "GoogleAuthUtil token attempt for $targetEmail failed: ${e.message}")
        }

        // Fallback to GoogleSignInAccount if available
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount?.account != null && lastAccount.email.equals(targetEmail, ignoreCase = true)) {
            try {
                return@withContext GoogleAuthUtil.getToken(context, lastAccount.account!!, OAUTH_SCOPE_STRING)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to get token with GoogleSignInAccount", e2)
            }
        }
        null
    }

    private fun persistVerifiedAccount(profile: GoogleUserProfile) {
        securePrefs.edit()
            .putString(KEY_VERIFIED_EMAIL, profile.email)
            .putString(KEY_VERIFIED_NAME, profile.displayName)
            .putString(KEY_VERIFIED_PHOTO, profile.photoUrl)
            .putString(KEY_VERIFIED_ID, profile.id)
            .apply()
    }

    private fun clearStoredAccount() {
        securePrefs.edit()
            .remove(KEY_VERIFIED_EMAIL)
            .remove(KEY_VERIFIED_NAME)
            .remove(KEY_VERIFIED_PHOTO)
            .remove(KEY_VERIFIED_ID)
            .apply()
    }
}
