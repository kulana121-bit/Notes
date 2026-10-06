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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

sealed interface DriveSyncResult {
    data class Success(val message: String, val timestamp: Long) : DriveSyncResult
    data class Restored(val newCount: Int, val updatedCount: Int, val dupeCount: Int, val timestamp: Long) : DriveSyncResult
    data class Error(val message: String) : DriveSyncResult
}

class GoogleDriveService(private val context: Context) {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val backupFileName = "glass_notes_backup.json"

    suspend fun uploadBackup(token: String, jsonData: String, email: String? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Check if backup file already exists in Google Drive AppData / files
            val existingFileId = findBackupFileId(token)

            val jsonMediaType = "application/json; charset=UTF-8".toMediaType()
            val metadataObj = JSONObject().apply {
                put("name", backupFileName)
                if (existingFileId == null) {
                    put("parents", JSONArray().put("appDataFolder"))
                }
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "metadata",
                    null,
                    metadataObj.toString().toRequestBody("application/json; charset=UTF-8".toMediaType())
                )
                .addFormDataPart(
                    "file",
                    backupFileName,
                    jsonData.toRequestBody(jsonMediaType)
                )
                .build()

            val url = if (existingFileId != null) {
                "https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=multipart"
            } else {
                "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .method(if (existingFileId != null) "PATCH" else "POST", requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val fileId = try {
                    JSONObject(responseBody).optString("id", existingFileId ?: "backup_id")
                } catch (_: Exception) {
                    existingFileId ?: "backup_id"
                }
                saveLocalBackupMirror(jsonData)
                Result.success(fileId)
            } else {
                val errorBody = response.body?.string() ?: "HTTP ${response.code}"
                Log.e("GoogleDriveService", "Drive upload failed ($response.code): $errorBody")
                // Save locally so user data is never lost
                saveLocalBackupMirror(jsonData)
                Result.failure(Exception("Drive upload failed (${response.code}): ${response.message}"))
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveService", "Drive upload exception: ${e.message}", e)
            saveLocalBackupMirror(jsonData)
            Result.failure(e)
        }
    }

    suspend fun downloadBackup(token: String, email: String? = null): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val fileId = findBackupFileId(token)
            if (fileId == null) {
                val localMirror = loadLocalBackupMirror()
                if (localMirror != null) {
                    return@withContext Result.success(localMirror)
                }
                return@withContext Result.failure(Exception("No backup found on Google Drive"))
            }

            val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val content = response.body?.string()
                if (!content.isNullOrBlank()) {
                    saveLocalBackupMirror(content)
                    Result.success(content)
                } else {
                    Result.failure(Exception("Backup file on Google Drive is empty"))
                }
            } else {
                val localMirror = loadLocalBackupMirror()
                if (localMirror != null) {
                    Result.success(localMirror)
                } else {
                    Result.failure(Exception("Failed to download from Drive (${response.code})"))
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveService", "Drive download exception: ${e.message}", e)
            val localMirror = loadLocalBackupMirror()
            if (localMirror != null) {
                Result.success(localMirror)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun removeNoteFromBackup(token: String?, noteId: String, email: String? = null): Result<Boolean> = withContext(Dispatchers.IO) {
        if (token.isNullOrBlank()) {
            return@withContext Result.success(false)
        }
        try {
            val downloadRes = downloadBackup(token, email)
            if (downloadRes.isSuccess) {
                val rawJson = downloadRes.getOrNull()
                if (!rawJson.isNullOrBlank()) {
                    val root = JSONObject(rawJson)
                    val notesArray = root.optJSONArray("notes")
                    if (notesArray != null) {
                        val updatedArray = JSONArray()
                        var removed = false
                        for (i in 0 until notesArray.length()) {
                            val noteObj = notesArray.optJSONObject(i) ?: continue
                            if (noteObj.optString("id") == noteId) {
                                removed = true
                            } else {
                                updatedArray.put(noteObj)
                            }
                        }
                        if (removed) {
                            root.put("notes", updatedArray)
                            root.put("updatedAt", System.currentTimeMillis())
                            uploadBackup(token, root.toString(2), email)
                            return@withContext Result.success(true)
                        }
                    }
                }
            }
            Result.success(false)
        } catch (e: Exception) {
            Log.w("GoogleDriveService", "Error removing note $noteId from Drive backup: ${e.message}")
            Result.failure(e)
        }
    }

    private fun findBackupFileId(token: String): String? {
        try {
            val query = "name = '$backupFileName' and trashed = false"
            val url = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder,drive&q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id,name,modifiedTime)"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val files = json.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    return files.getJSONObject(0).optString("id")
                }
            }
        } catch (e: Exception) {
            Log.w("GoogleDriveService", "findBackupFileId error: ${e.message}")
        }
        return null
    }

    private fun saveLocalBackupMirror(json: String) {
        try {
            val file = File(context.filesDir, "google_drive_mirror_backup.json")
            file.writeText(json)
        } catch (_: Exception) {}
    }

    private fun loadLocalBackupMirror(): String? {
        return try {
            val file = File(context.filesDir, "google_drive_mirror_backup.json")
            if (file.exists() && file.length() > 0) file.readText() else null
        } catch (_: Exception) {
            null
        }
    }
}
