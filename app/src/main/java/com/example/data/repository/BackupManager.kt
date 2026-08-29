package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.ui.theme.ThemePreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class BackupManager(private val context: Context, private val db: AppDatabase) {

    private val businessProfileManager = BusinessProfileManager.getInstance(context)
    private val themePreferenceManager = ThemePreferenceManager.getInstance(context)

    /**
     * Generates a complete JSON backup string of all tables using DatabaseExporter.
     */
    suspend fun generateBackupJson(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val json = DatabaseExporter.exportToJsonString(
                db = db,
                profileManager = businessProfileManager,
                themeManager = themePreferenceManager
            )
            Result.success(json)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Writes the full database backup to a specified SAF Uri.
     */
    suspend fun writeBackupToUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jsonResult = generateBackupJson()
            if (jsonResult.isFailure) {
                return@withContext Result.failure(jsonResult.exceptionOrNull() ?: Exception("Failed to generate backup JSON"))
            }

            val jsonString = jsonResult.getOrThrow()
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(jsonString)
                    writer.flush()
                }
            } ?: return@withContext Result.failure(Exception("Could not open file for writing"))

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Validates a backup JSON string.
     */
    fun validateBackupJsonString(jsonContent: String): BackupValidationResult {
        return DatabaseExporter.validateBackupJson(jsonContent)
    }

    /**
     * Validates a backup file from a Uri.
     */
    suspend fun validateBackupFile(uri: Uri): BackupValidationResult = withContext(Dispatchers.IO) {
        try {
            val content = readUriToString(uri)
            validateBackupJsonString(content)
        } catch (e: Exception) {
            BackupValidationResult(isValid = false, errorMessage = "Failed to read backup file: ${e.localizedMessage}")
        }
    }

    /**
     * Restores all entities from a Uri within a single atomic Room database transaction.
     */
    suspend fun restoreBackupFromUri(uri: Uri, clearExisting: Boolean = false): Result<BackupValidationResult> = withContext(Dispatchers.IO) {
        try {
            val content = readUriToString(uri)
            restoreBackupFromJsonString(content, clearExisting)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Restores all entities from a JSON string within a single atomic Room database transaction.
     */
    suspend fun restoreBackupFromJsonString(content: String, clearExisting: Boolean = false): Result<BackupValidationResult> = withContext(Dispatchers.IO) {
        DatabaseExporter.importFromJsonString(
            jsonString = content,
            db = db,
            profileManager = businessProfileManager,
            themeManager = themePreferenceManager,
            clearExistingBeforeRestore = clearExisting
        )
    }

    private fun readUriToString(uri: Uri): String {
        val stringBuilder = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line).append("\n")
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString()
    }
}
