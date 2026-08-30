import sys

with open('app/src/main/java/com/example/data/repository/DatabaseExporter.kt', 'r') as f:
    content = f.read()

content = content.replace('suspend fun exportToJsonObject(', 'suspend fun exportToJsonString(')
content = content.replace(': JSONObject {', ': String {')
content = content.replace('return root\n    }', 'return root.toString(4)\n    }')

import_json_str = """
    fun validateBackupJson(jsonString: String): BackupValidationResult {
        return try {
            val root = JSONObject(jsonString)
            val v = root.optInt("backupVersion", 0)
            if (v == 0) return BackupValidationResult(isValid = false, errorMessage = "Missing or invalid backupVersion")
            
            BackupValidationResult(
                isValid = true,
                backupVersion = v,
                appVersion = root.optString("appVersion", ""),
                backupDate = root.optString("backupDate", ""),
                customersCount = root.optJSONArray("customers")?.length() ?: 0,
                salesCount = root.optJSONArray("sales")?.length() ?: 0,
                saleItemsCount = root.optJSONArray("saleItems")?.length() ?: 0,
                expensesCount = root.optJSONArray("expenses")?.length() ?: 0,
                employeesCount = root.optJSONArray("employees")?.length() ?: 0,
                salariesCount = root.optJSONArray("salaryPayments")?.length() ?: 0,
                usersCount = root.optJSONArray("users")?.length() ?: 0,
                hasBusinessProfile = root.has("businessProfile")
            )
        } catch (e: Exception) {
            BackupValidationResult(isValid = false, errorMessage = "Malformed JSON: ${e.message}")
        }
    }

    suspend fun importFromJsonString(
        jsonString: String,
        db: AppDatabase,
        profileManager: BusinessProfileManager,
        themeManager: ThemePreferenceManager,
        clearExistingBeforeRestore: Boolean = true
    ): Result<BackupValidationResult> {
        val root = try {
            JSONObject(jsonString)
        } catch (e: Exception) {
            return Result.failure(Exception("Invalid JSON format."))
        }
"""
content = content.replace('    suspend fun importFromJsonObject(\n        db: AppDatabase,\n        profileManager: BusinessProfileManager,\n        themeManager: ThemePreferenceManager,\n        root: JSONObject\n    ): Result<BackupValidationResult> {\n', import_json_str)

with open('app/src/main/java/com/example/data/repository/DatabaseExporter.kt', 'w') as f:
    f.write(content)
print("Fixed!")

