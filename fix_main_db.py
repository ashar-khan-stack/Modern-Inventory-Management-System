import sys

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace("runStartupDiagnostics(context)", "runStartupDiagnostics(com.example.data.local.AppDatabase.getInstance(context))")

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
