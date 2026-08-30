import sys

with open('app/src/main/java/com/example/ui/viewmodel/InventoryViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("import androidx.lifecycle.AndroidViewModel", "import androidx.lifecycle.ViewModel")
content = content.replace("class InventoryViewModel(application: Application) : AndroidViewModel(application) {", "class InventoryViewModel(private val db: AppDatabase) : ViewModel() {")
content = content.replace("    private val db = AppDatabase.getDatabase(application)\n", "")

comp = """    companion object {
        fun saveImageUriToAppStorage(context: android.content.Context, uri: android.net.Uri, oldPath: String? = null): String {
            return try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return uri.toString()
                val imagesDir = java.io.File(context.filesDir, "product_images")
                if (!imagesDir.exists()) {
                    imagesDir.mkdirs()
                }
                val fileName = "img_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
                val file = java.io.File(imagesDir, fileName)
                file.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
                file.absolutePath
            } catch (e: Exception) {
                uri.toString()
            }
        }
    }
}"""

content = content.replace("}\n\ndata class DashboardTotals", comp + "\n\ndata class DashboardTotals")

with open('app/src/main/java/com/example/ui/viewmodel/InventoryViewModel.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ui/util/AppStartupDiagnostics.kt', 'r') as f:
    diag = f.read()
diag = diag.replace("suspend fun runStartupDiagnostics(context: android.content.Context)", "suspend fun runStartupDiagnostics(db: AppDatabase)")
diag = diag.replace("val customerDao = AppDatabase.getDatabase(context).customerDao()", "val customerDao = db.customerDao()")
with open('app/src/main/java/com/example/ui/util/AppStartupDiagnostics.kt', 'w') as f:
    f.write(diag)
print("VM and Diag fixed")
