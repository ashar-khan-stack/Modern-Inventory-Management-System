import re

files = [
    "app/src/main/java/com/example/ui/screens/CustomerInfoScreen.kt",
    "app/src/main/java/com/example/ui/screens/EmployeeInfoScreen.kt",
    "app/src/main/java/com/example/ui/screens/SalariesScreen.kt"
]

for file in files:
    with open(file, "r") as f:
        content = f.read()

    # Remove the first TabRow (Main Top Level Tabs: Customers vs Employees)
    # The syntax is something like:
    # TabRow(
    #     selectedTabIndex = mainTab,
    #     containerColor = MaterialTheme.colorScheme.surface,
    #     contentColor = BrandBluePrimary
    # ) { ... }
    # We can use a regex to match it and remove it.
    
    # Actually, simpler way is to replace "TabRow(" with "if(false) TabRow(" so it's never rendered!
    content = content.replace("TabRow(", "if(false) TabRow(")
    
    # We should also replace the secondary TabRow (Staff Directory vs Salary Payroll)
    content = content.replace("TabRow(", "if(false) TabRow(")
    
    # And there's a TabRow in `CustomerInfoScreen` ? Wait, there is no other TabRow.
    
    with open(file, "w") as f:
        f.write(content)

