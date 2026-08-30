import re

def process_customer():
    with open("app/src/main/java/com/example/ui/screens/CustomerInfoScreen.kt", "r") as f:
        content = f.read()

    # Rename
    content = content.replace("fun PeopleScreen(", "fun CustomerInfoScreen(")
    
    # Remove unused arguments (employees, salaryPayments, sales are used for metrics maybe? Sales is used for ledger)
    # employee operations: onSaveEmployee, onDeleteEmployee, onDisburseSalary, onUpdateSalary, onDeleteSalary
    content = re.sub(r',\s*employees:\s*List<EmployeeEntity>', '', content)
    content = re.sub(r',\s*salaryPayments:\s*List<SalaryPaymentEntity>', '', content)
    content = re.sub(r',\s*onSaveEmployee:\s*\(EmployeeEntity\) -> Unit', '', content)
    content = re.sub(r',\s*onDeleteEmployee:\s*\(EmployeeEntity\) -> Unit', '', content)
    content = re.sub(r',\s*onDisburseSalary:\s*\(SalaryPaymentEntity\) -> Unit', '', content)
    content = re.sub(r',\s*onUpdateSalary:\s*\(SalaryPaymentEntity\) -> Unit = \{\}', '', content)
    content = re.sub(r',\s*onDeleteSalary:\s*\(SalaryPaymentEntity\) -> Unit = \{\}', '', content)
    content = re.sub(r',\s*forceTab:\s*Int = -1', '', content)
    content = re.sub(r',\s*forceEmployeeSubTab:\s*Int = -1', '', content)

    # We can just keep the whole file but set `mainTab = 0` and remove the TabRow and the `else` branch for mainTab
    # Actually, simpler: just add a quick sed-like replacement
    content = content.replace("var mainTab by remember(forceTab) { mutableIntStateOf(if (forceTab >= 0) forceTab else 0) }", "val mainTab = 0")
    
    # Let's remove the TabRows using regex or just keep them hidden?
    # "Do NOT simply swap content inside one giant composable"
    # It's better if we just hide the TabRow in the AST, or we can just leave it for now but remove the TabRow block.
    # To remove the TabRow block safely:
    tab_row_regex = r'TabRow\([\s\S]*?\}\n            \}'
    # Actually it's risky to regex match braces.
    
    with open("app/src/main/java/com/example/ui/screens/CustomerInfoScreen.kt", "w") as f:
        f.write(content)

process_customer()
