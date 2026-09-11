package com.example.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val idToken: String? = null
)

object GoogleAuthManager {
    private const val PREFS_NAME = "google_auth_prefs"
    private const val KEY_WEB_CLIENT_ID = "web_client_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_PHOTO = "user_photo"

    fun getWebClientId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_WEB_CLIENT_ID, null)
        if (!saved.isNullOrBlank()) return saved

        val buildConfigKey = com.example.BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (buildConfigKey.isNotBlank() && buildConfigKey != "YOUR_GOOGLE_WEB_CLIENT_ID") {
            return buildConfigKey
        }
        return ""
    }

    fun saveWebClientId(context: Context, clientId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_WEB_CLIENT_ID, clientId.trim())
            .apply()
    }

    fun getSavedUser(context: Context): UserProfile? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val email = prefs.getString(KEY_USER_EMAIL, null) ?: return null
        val name = prefs.getString(KEY_USER_NAME, "User") ?: "User"
        val photo = prefs.getString(KEY_USER_PHOTO, null)
        return UserProfile(displayName = name, email = email, photoUrl = photo)
    }

    fun saveUser(context: Context, user: UserProfile) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_NAME, user.displayName)
            .putString(KEY_USER_PHOTO, user.photoUrl)
            .apply()
    }

    fun clearUser(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_PHOTO)
            .apply()

        try {
            FirebaseAuth.getInstance().signOut()
        } catch (_: Exception) {}
    }

    suspend fun signInWithGoogle(
        context: Context,
        webClientId: String
    ): Result<UserProfile> {
        return try {
            val credentialManager = CredentialManager.create(context)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            handleSignIn(context, result)
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Login dengan Google dibatalkan."))
        } catch (e: Exception) {
            Log.e("GoogleAuthManager", "Error signing in", e)
            Result.failure(Exception("Gagal login dengan Google: ${e.localizedMessage ?: e.message}"))
        }
    }

    private suspend fun handleSignIn(context: Context, result: GetCredentialResponse): Result<UserProfile> {
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val displayName = googleIdTokenCredential.displayName ?: googleIdTokenCredential.givenName ?: "Pengguna Google"
                val email = googleIdTokenCredential.id
                val photoUrl = googleIdTokenCredential.profilePictureUri?.toString()

                val profile = UserProfile(
                    displayName = displayName,
                    email = email,
                    photoUrl = photoUrl,
                    idToken = idToken
                )

                // Optional: Sign in with Firebase if Firebase is configured
                try {
                    val auth = FirebaseAuth.getInstance()
                    val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(firebaseCred).await()
                } catch (fe: Exception) {
                    Log.w("GoogleAuthManager", "Firebase Auth optional sync note: ${fe.message}")
                }

                saveUser(context, profile)
                return Result.success(profile)
            } catch (e: GoogleIdTokenParsingException) {
                return Result.failure(Exception("Token Google tidak valid: ${e.message}"))
            }
        } else {
            return Result.failure(Exception("Tipe kredensial tidak dikenali: ${credential.type}"))
        }
    }
}
