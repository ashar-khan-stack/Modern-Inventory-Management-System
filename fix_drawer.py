import sys
import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# Fix DrawerNavMenuItem for ACCEPTANCE_REPORT
# The drawer is manually built with DrawerNavMenuItem, not AppScreen.values().forEach

# We need to find DrawerNavMenuItem that corresponds to AppScreen.ACCEPTANCE_REPORT and wrap it correctly.
# First, let's remove all the messed up if statements around it

content = re.sub(r'(?s)if \(com\.example\.BuildConfig\.DEBUG\) \{\s*DrawerNavMenuItem\(\s*if \(com\.example\.BuildConfig\.DEBUG\) \{\s*screen = AppScreen\.ACCEPTANCE_REPORT,.*?\)\s*\}', '', content)
# It's probably easier to just replace the whole messed up block

# Also remove the AppScreen.values().forEach changes from patch_drawer3.py
content = content.replace("                        if (!com.example.BuildConfig.DEBUG && screen == AppScreen.ACCEPTANCE_REPORT) return@forEach\n", "")
content = content.replace("                        if (screen == AppScreen.INVOICE_VIEW) return@forEach\n", "")

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)

