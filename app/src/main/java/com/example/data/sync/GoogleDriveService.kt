package com.example.data.sync

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class DriveFileInfo(
    val id: String,
    val name: String,
    val modifiedTime: String? = null,
    val size: Long = 0L
)

sealed class DriveSyncResult {
    data class Success(val message: String, val timestamp: Long = System.currentTimeMillis()) : DriveSyncResult()
    data class Restored(val newCount: Int, val updatedCount: Int, val dupeCount: Int, val timestamp: Long) : DriveSyncResult()
    data class Error(val error: String) : DriveSyncResult()
}

class GoogleDriveService(private val context: Context) {

    companion object {
        private const val TAG = "GoogleDriveService"
        const val BACKUP_FILENAME = "glass_notes_backup.json"
        private const val DRIVE_API_URL = "https://www.googleapis.com/drive/v3"
        private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(35, TimeUnit.SECONDS)
        .build()

    // Isolated per-account file ID cache to prevent cross-account file contamination
    private val accountFileIdMap = ConcurrentHashMap<String, String>()

    private fun getLocalDriveCacheFile(userEmail: String): File {
        val safe = userEmail.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(context.filesDir, "gdrive_cache_${safe}.json")
    }

    /**
     * Clears all cached file metadata and local sync cache for a specific user on sign out.
     */
    fun clearAccountCache(userEmail: String) {
        accountFileIdMap.remove(userEmail.lowercase())
        try {
            val file = getLocalDriveCacheFile(userEmail)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
    }

    /**
     * Finds the canonical backup file in Google Drive for the authenticated user.
     * STRICT: Requires a valid OAuth access token. Returns null if token is missing or invalid.
     */
    suspend fun findBackupFile(accessToken: String?, userEmail: String): DriveFileInfo? = withContext(Dispatchers.IO) {
        if (accessToken.isNullOrBlank() || userEmail.isBlank()) {
            Log.w(TAG, "findBackupFile rejected: missing access token or user email")
            return@withContext null
        }

        try {
            // Search user's Google Drive for the backup file
            val query = "name = '$BACKUP_FILENAME' and trashed = false"
            val url = "$DRIVE_API_URL/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id,name,modifiedTime,size)&pageSize=5"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Search files failed HTTP ${response.code}: ${response.message}")
                    return@withContext null
                }
                val bodyStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyStr)
                val files = json.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    val first = files.getJSONObject(0)
                    val fileId = first.getString("id")
                    accountFileIdMap[userEmail.lowercase()] = fileId
                    return@withContext DriveFileInfo(
                        id = fileId,
                        name = first.optString("name", BACKUP_FILENAME),
                        modifiedTime = if (first.has("modifiedTime")) first.getString("modifiedTime") else null,
                        size = first.optLong("size", 0L)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding backup file in Google Drive", e)
        }
        null
    }

    /**
     * Uploads notes backup JSON to the authenticated user's own Google Drive.
     * STRICT SECURITY MANDATE:
     * - Returns failure if accessToken is null, blank, or expired.
     * - NEVER creates dummy success.
     * - If file already exists in user's Drive, updates in-place (PATCH).
     */
    suspend fun uploadBackup(
        accessToken: String?,
        backupJson: String,
        userEmail: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (accessToken.isNullOrBlank()) {
            val errorMsg = "Authentication required: No valid Google OAuth access token for Google Drive upload."
            Log.e(TAG, errorMsg)
            return@withContext Result.failure(SecurityException(errorMsg))
        }

        if (userEmail.isBlank()) {
            val errorMsg = "Authentication required: Missing user email."
            Log.e(TAG, errorMsg)
            return@withContext Result.failure(SecurityException(errorMsg))
        }

        try {
            val existingFile = findBackupFile(accessToken, userEmail)
            val jsonMedia = "application/json; charset=UTF-8".toMediaType()

            if (existingFile != null) {
                // Update existing backup file in-place: PATCH to upload endpoint
                val url = "$DRIVE_UPLOAD_URL/files/${existingFile.id}?uploadType=media"
                val body = backupJson.toRequestBody(jsonMedia)
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .patch(body)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d(TAG, "Google Drive backup updated successfully for $userEmail: ${existingFile.id}")
                        // Save copy to user cache file
                        try { getLocalDriveCacheFile(userEmail).writeText(backupJson) } catch (_: Exception) {}
                        return@withContext Result.success(existingFile.id)
                    } else {
                        val err = response.body?.string() ?: response.message
                        Log.e(TAG, "Failed to update backup file HTTP ${response.code}: $err")
                        return@withContext Result.failure(Exception("Google Drive error (HTTP ${response.code}): $err"))
                    }
                }
            } else {
                // Create new backup file in user's Google Drive via multipart upload
                val metadataJson = JSONObject().apply {
                    put("name", BACKUP_FILENAME)
                    put("mimeType", "application/json")
                    put("description", "Glass Notes Cloud Backup for $userEmail")
                }.toString()

                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addPart(metadataJson.toRequestBody(jsonMedia))
                    .addPart(backupJson.toRequestBody(jsonMedia))
                    .build()

                val url = "$DRIVE_UPLOAD_URL/files?uploadType=multipart"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .post(multipartBody)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val respBody = response.body?.string() ?: ""
                        val fileId = JSONObject(respBody).optString("id", "")
                        if (fileId.isNotBlank()) {
                            accountFileIdMap[userEmail.lowercase()] = fileId
                        }
                        Log.d(TAG, "Google Drive backup created successfully for $userEmail: $fileId")
                        try { getLocalDriveCacheFile(userEmail).writeText(backupJson) } catch (_: Exception) {}
                        return@withContext Result.success(fileId)
                    } else {
                        val err = response.body?.string() ?: response.message
                        Log.e(TAG, "Failed to create backup file HTTP ${response.code}: $err")
                        return@withContext Result.failure(Exception("Google Drive error (HTTP ${response.code}): $err"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception uploading backup to Google Drive", e)
            Result.failure(e)
        }
    }

    /**
     * Downloads the notes backup JSON from the authenticated user's own Google Drive.
     * STRICT SECURITY MANDATE:
     * - Returns failure if accessToken is null, blank, or expired.
     * - NEVER silently restores unverified local cache when token is missing.
     */
    suspend fun downloadBackup(
        accessToken: String?,
        userEmail: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (accessToken.isNullOrBlank()) {
            val errorMsg = "Authentication required: No valid Google OAuth access token for Google Drive restore."
            Log.e(TAG, errorMsg)
            return@withContext Result.failure(SecurityException(errorMsg))
        }

        if (userEmail.isBlank()) {
            val errorMsg = "Authentication required: Missing user email."
            Log.e(TAG, errorMsg)
            return@withContext Result.failure(SecurityException(errorMsg))
        }

        val fileInfo = findBackupFile(accessToken, userEmail)
            ?: return@withContext Result.failure(Exception("No backup file '$BACKUP_FILENAME' found in Google Drive for $userEmail"))

        try {
            val url = "$DRIVE_API_URL/files/${fileInfo.id}?alt=media"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val content = response.body?.string() ?: ""
                    if (content.isBlank()) {
                        return@withContext Result.failure(Exception("Backup file in Google Drive is empty"))
                    }
                    try { getLocalDriveCacheFile(userEmail).writeText(content) } catch (_: Exception) {}
                    return@withContext Result.success(content)
                } else {
                    val err = response.body?.string() ?: response.message
                    return@withContext Result.failure(Exception("Failed to download from Google Drive (HTTP ${response.code}): $err"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception downloading backup from Google Drive", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes the backup file from Google Drive for the authenticated user.
     */
    suspend fun deleteBackupFile(
        accessToken: String?,
        userEmail: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (accessToken.isNullOrBlank() || userEmail.isBlank()) {
            return@withContext Result.failure(SecurityException("Authentication required: No valid Google OAuth access token."))
        }

        try {
            clearAccountCache(userEmail)
            val fileInfo = findBackupFile(accessToken, userEmail) ?: return@withContext Result.success(Unit)
            val url = "$DRIVE_API_URL/files/${fileInfo.id}"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .delete()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 404) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to delete cloud backup: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Removes a note from the authenticated user's Google Drive cloud backup.
     */
    suspend fun removeNoteFromBackup(
        accessToken: String?,
        noteId: String,
        userEmail: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (accessToken.isNullOrBlank() || userEmail.isBlank()) {
            return@withContext Result.success(Unit)
        }
        try {
            val downloadRes = downloadBackup(accessToken, userEmail)
            if (downloadRes.isSuccess) {
                val jsonStr = downloadRes.getOrNull() ?: return@withContext Result.success(Unit)
                val root = JSONObject(jsonStr)
                val notesArray = root.optJSONArray("notes")
                if (notesArray != null) {
                    val updatedArray = org.json.JSONArray()
                    for (i in 0 until notesArray.length()) {
                        val item = notesArray.optJSONObject(i) ?: continue
                        if (item.optString("id") != noteId) {
                            updatedArray.put(item)
                        }
                    }
                    root.put("notes", updatedArray)
                    root.put("updatedAt", System.currentTimeMillis())
                    uploadBackup(accessToken, root.toString(2), userEmail)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing note from cloud backup", e)
            Result.failure(e)
        }
    }
}
