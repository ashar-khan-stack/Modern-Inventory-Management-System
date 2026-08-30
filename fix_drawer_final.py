import sys
import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# Replace the messy block with a clean conditional DrawerNavMenuItem for ACCEPTANCE_REPORT
mess = """                    if (com.example.BuildConfig.DEBUG) {
                        DrawerNavMenuItem(
                        if (com.example.BuildConfig.DEBUG) {
                            screen = AppScreen.ACCEPTANCE_REPORT,
                            currentScreen = currentScreen,
                            onClick = {
                                currentScreen = AppScreen.ACCEPTANCE_REPORT
                        }
                                scope.launch { drawerState.close() }
                            }
                        )
                    }"""

clean = """                    if (com.example.BuildConfig.DEBUG) {
                        DrawerNavMenuItem(
                            screen = AppScreen.ACCEPTANCE_REPORT,
                            currentScreen = currentScreen,
                            onClick = {
                                currentScreen = AppScreen.ACCEPTANCE_REPORT
                                scope.launch { drawerState.close() }
                            }
                        )
                    }"""

content = content.replace(mess, clean)
with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
print("Drawer final fix!")
