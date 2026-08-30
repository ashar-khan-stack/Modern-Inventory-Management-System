import sys

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
in_drawer = False
for line in lines:
    if "AppScreen.ACCEPTANCE_REPORT -> {" in line:
        new_lines.append("                    AppScreen.ACCEPTANCE_REPORT -> {\n")
        new_lines.append("                        if (com.example.BuildConfig.DEBUG) {\n")
        new_lines.append("                            com.example.ui.screens.AcceptanceReportScreen()\n")
        new_lines.append("                        }\n")
        new_lines.append("                    }\n")
        continue
    if "com.example.ui.screens.AcceptanceReportScreen()" in line and "if (com.example.BuildConfig.DEBUG)" not in "".join(new_lines[-2:]):
        pass # Already handled above
    elif "AppScreen.ACCEPTANCE_REPORT," in line and "screen = " in line:
        new_lines.append("                        if (com.example.BuildConfig.DEBUG) {\n")
        new_lines.append(line)
    elif "currentScreen = AppScreen.ACCEPTANCE_REPORT" in line and "if (com.example.BuildConfig.DEBUG)" not in "".join(new_lines[-2:]):
        new_lines.append(line)
        new_lines.append("                        }\n")
    else:
        new_lines.append(line)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.writelines(new_lines)
print("Drawer patched!")
