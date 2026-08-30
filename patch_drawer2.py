import sys

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# I will just insert the case back inside the when(currentScreen) block
if "AppScreen.ACCEPTANCE_REPORT -> {" not in content:
    idx = content.find("AppScreen.SETTINGS -> {")
    if idx != -1:
        content = content[:idx] + """                    AppScreen.ACCEPTANCE_REPORT -> {
                        if (com.example.BuildConfig.DEBUG) {
                            com.example.ui.screens.AcceptanceReportScreen()
                        }
                    }
""" + content[idx:]

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
print("Drawer restored!")
