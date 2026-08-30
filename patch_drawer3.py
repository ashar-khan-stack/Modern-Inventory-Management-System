import sys

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if "AppScreen.values().forEach { screen ->" in line:
        new_lines.append(line)
        new_lines.append("                        if (!com.example.BuildConfig.DEBUG && screen == AppScreen.ACCEPTANCE_REPORT) return@forEach\n")
        new_lines.append("                        if (screen == AppScreen.INVOICE_VIEW) return@forEach\n")
    elif "if (screen == AppScreen.INVOICE_VIEW) return@forEach" in line:
        pass # handled above
    else:
        new_lines.append(line)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.writelines(new_lines)
print("Drawer filtered!")
