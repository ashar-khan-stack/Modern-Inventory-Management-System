import sys
import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# I will find the exact drawer section and fix it
# Looking at the previous output:
#                    DrawerNavMenuItem(
#                        screen = AppScreen.REPORTS,
#                        currentScreen = currentScreen,
#                        onClick = {
#                            currentScreen = AppScreen.REPORTS
#                            scope.launch { drawerState.close() }
#                        }
#                    )
#                                                }
#                        )
#                    }
#                    DrawerNavMenuItem(

print("Finding messy section...")
