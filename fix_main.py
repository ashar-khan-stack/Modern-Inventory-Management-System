import sys

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

bad = """    LaunchedEffect(Unit) {
        if (com.example.BuildConfig.DEBUG) {
        if (com.example.BuildConfig.DEBUG) {
            val result = com.example.ui.util.AppStartupDiagnostics.runStartupDiagnostics(context)
            com.example.ui.util.DebugNavigationLogger.logScreenState("Startup", "Diagnostics completed. Consistency issues: ${result.issues.size}")
        }
        }
    }"""

good = """    LaunchedEffect(Unit) {
        if (com.example.BuildConfig.DEBUG) {
            com.example.ui.util.AppStartupDiagnostics.runStartupDiagnostics(context)
            com.example.ui.util.DebugNavigationLogger.logScreenState("Startup", "Diagnostics completed.")
        }
    }"""

content = content.replace(bad, good)

# Also let's check what's around 420-470 to fix the missing brackets

# Fix bracket issue in drawer section
messy_drawer = """                    )
                                                }
                        )
                    }
                    DrawerNavMenuItem("""

clean_drawer = """                    )
                    
                    if (com.example.BuildConfig.DEBUG) {
                        DrawerNavMenuItem(
                            screen = AppScreen.ACCEPTANCE_REPORT,
                            currentScreen = currentScreen,
                            onClick = {
                                currentScreen = AppScreen.ACCEPTANCE_REPORT
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                    
                    DrawerNavMenuItem("""

content = content.replace(messy_drawer, clean_drawer)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
print("MainActivity syntax fixed")
