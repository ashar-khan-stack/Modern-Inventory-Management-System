import sys

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if "AppStartupDiagnostics.runStartupDiagnostics" in line and "if (BuildConfig.DEBUG)" not in line:
        line = line.replace('com.example.ui.util.AppStartupDiagnostics.runStartupDiagnostics', 'if (BuildConfig.DEBUG) com.example.ui.util.AppStartupDiagnostics.runStartupDiagnostics')
    
    new_lines.append(line)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.writelines(new_lines)
print("Patched debug logs!")
