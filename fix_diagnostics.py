import sys

with open('app/src/main/java/com/example/ui/util/AppStartupDiagnostics.kt', 'r') as f:
    content = f.read()

content = content.replace("suspend fun runStartupDiagnostics(db: AppDatabase)", "suspend fun runStartupDiagnostics(context: android.content.Context)")
content = content.replace("val customerDao = db.customerDao()", "val customerDao = AppDatabase.getDatabase(context).customerDao()")

with open('app/src/main/java/com/example/ui/util/AppStartupDiagnostics.kt', 'w') as f:
    f.write(content)
print("Diagnostics fixed")
