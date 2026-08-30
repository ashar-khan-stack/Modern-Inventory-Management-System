import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    lines = f.readlines()

out = []
skip = False
for i, line in enumerate(lines):
    if "DrawerNavMenuItem(" in line and "AppScreen.REPORTS" in lines[i+1]:
        out.append(line)
        out.append(lines[i+1])
        out.append(lines[i+2])
        out.append(lines[i+3])
        out.append(lines[i+4])
        out.append(lines[i+5])
        out.append(lines[i+6])
        out.append(lines[i+7])
        
        # Now append ACCEPTANCE_REPORT if debug
        out.append("                    if (com.example.BuildConfig.DEBUG) {\n")
        out.append("                        DrawerNavMenuItem(\n")
        out.append("                            screen = AppScreen.ACCEPTANCE_REPORT,\n")
        out.append("                            currentScreen = currentScreen,\n")
        out.append("                            onClick = {\n")
        out.append("                                currentScreen = AppScreen.ACCEPTANCE_REPORT\n")
        out.append("                                scope.launch { drawerState.close() }\n")
        out.append("                            }\n")
        out.append("                        )\n")
        out.append("                    }\n")
        skip = True
    elif skip and "DrawerNavMenuItem(" in line and "AppScreen.SETTINGS" in lines[i+1]:
        skip = False
        out.append(line)
    elif not skip:
        out.append(line)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.writelines(out)

