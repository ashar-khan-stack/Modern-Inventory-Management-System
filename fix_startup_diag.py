import sys

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if "val result = if (BuildConfig.DEBUG)" in line or "val result = com.example.ui.util.AppStartupDiagnostics" in line:
        new_lines.append("        if (com.example.BuildConfig.DEBUG) {\n")
        new_lines.append(line.replace("if (BuildConfig.DEBUG) ", "").replace("val result = ", "val result = "))
        continue
    if "Consistency issues: ${result.issues.size}" in line:
        new_lines.append(line)
        new_lines.append("        }\n")
        continue
        
    new_lines.append(line)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.writelines(new_lines)
