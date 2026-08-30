import sys

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if "DebugNavigationLogger.logScreenState" in line:
        line = line.replace('com.example.ui.util.DebugNavigationLogger.logScreenState', 'if (BuildConfig.DEBUG) com.example.ui.util.DebugNavigationLogger.logScreenState')
    if "DebugNavigationLogger.logNavigation" in line:
        line = line.replace('com.example.ui.util.DebugNavigationLogger.logNavigation', 'if (BuildConfig.DEBUG) com.example.ui.util.DebugNavigationLogger.logNavigation')
    if "AppStartupDiagnostics.runDiagnostics" in line:
        line = line.replace('AppStartupDiagnostics.runDiagnostics', 'if (BuildConfig.DEBUG) AppStartupDiagnostics.runDiagnostics')
    
    new_lines.append(line)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.writelines(new_lines)
print("Patched debug logs!")
