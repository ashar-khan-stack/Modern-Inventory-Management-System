import re

with open("app/src/main/java/com/example/ui/screens/PeopleScreen.kt", "r") as f:
    original = f.read()

# Make CustomerInfoScreen
cust = original.replace("fun PeopleScreen(", "fun CustomerInfoScreen(")
cust = re.sub(r',\s*forceTab: Int = -1', '', cust)
cust = re.sub(r',\s*forceEmployeeSubTab: Int = -1', '', cust)
cust = re.sub(r'var mainTab by remember.*?\}', 'val mainTab = 0', cust)
cust = re.sub(r'var employeeSubTab by remember.*?\}', 'val employeeSubTab = 0', cust)
cust = cust.replace("TabRow(", "if(false) TabRow(")

with open("app/src/main/java/com/example/ui/screens/CustomerInfoScreen.kt", "w") as f:
    f.write(cust)

# Make EmployeeInfoScreen
emp = original.replace("fun PeopleScreen(", "fun EmployeeInfoScreen(")
emp = re.sub(r',\s*forceTab: Int = -1', '', emp)
emp = re.sub(r',\s*forceEmployeeSubTab: Int = -1', '', emp)
emp = re.sub(r'var mainTab by remember.*?\}', 'val mainTab = 1', emp)
emp = re.sub(r'var employeeSubTab by remember.*?\}', 'val employeeSubTab = 0', emp)
emp = emp.replace("TabRow(", "if(false) TabRow(")

with open("app/src/main/java/com/example/ui/screens/EmployeeInfoScreen.kt", "w") as f:
    f.write(emp)

# Make SalariesScreen
sal = original.replace("fun PeopleScreen(", "fun SalariesScreen(")
sal = re.sub(r',\s*forceTab: Int = -1', '', sal)
sal = re.sub(r',\s*forceEmployeeSubTab: Int = -1', '', sal)
sal = re.sub(r'var mainTab by remember.*?\}', 'val mainTab = 1', sal)
sal = re.sub(r'var employeeSubTab by remember.*?\}', 'val employeeSubTab = 1', sal)
sal = sal.replace("TabRow(", "if(false) TabRow(")

with open("app/src/main/java/com/example/ui/screens/SalariesScreen.kt", "w") as f:
    f.write(sal)

