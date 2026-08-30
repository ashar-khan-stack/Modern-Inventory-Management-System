import re
import os

with open("app/src/main/java/com/example/ui/screens/PeopleScreen.kt", "r") as f:
    original = f.read()

# Make CustomerInfoScreen
cust = original.replace("fun PeopleScreen(", "fun CustomerInfoScreen(")
cust = re.sub(r',\s*employees: List<EmployeeEntity>', '', cust)
cust = re.sub(r',\s*salaryPayments: List<SalaryPaymentEntity>', '', cust)
cust = re.sub(r',\s*onSaveEmployee: \(EmployeeEntity\) -> Unit', '', cust)
cust = re.sub(r',\s*onDeleteEmployee: \(EmployeeEntity\) -> Unit', '', cust)
cust = re.sub(r',\s*onDisburseSalary: \(SalaryPaymentEntity\) -> Unit', '', cust)
cust = re.sub(r',\s*onUpdateSalary: \(SalaryPaymentEntity\) -> Unit = \{\}', '', cust)
cust = re.sub(r',\s*onDeleteSalary: \(SalaryPaymentEntity\) -> Unit = \{\}', '', cust)
cust = re.sub(r',\s*forceTab: Int = -1', '', cust)
cust = re.sub(r',\s*forceEmployeeSubTab: Int = -1', '', cust)
cust = re.sub(r'var mainTab by remember.*?\}', 'val mainTab = 0', cust)
cust = re.sub(r'var employeeSubTab by remember.*?\}', 'val employeeSubTab = 0', cust)

with open("app/src/main/java/com/example/ui/screens/CustomerInfoScreen.kt", "w") as f:
    f.write(cust)

# Make EmployeeInfoScreen
emp = original.replace("fun PeopleScreen(", "fun EmployeeInfoScreen(")
emp = re.sub(r',\s*customers: List<CustomerEntity>', '', emp)
emp = re.sub(r',\s*salaryPayments: List<SalaryPaymentEntity>', '', emp)
emp = re.sub(r',\s*onSaveCustomer: \(CustomerEntity\) -> Unit', '', emp)
emp = re.sub(r',\s*onDeleteCustomer: \(CustomerEntity\) -> Unit', '', emp)
emp = re.sub(r',\s*onSettlePayment: \(customerId: Long, amount: Double\) -> Unit', '', emp)
emp = re.sub(r',\s*onDisburseSalary: \(SalaryPaymentEntity\) -> Unit', '', emp)
emp = re.sub(r',\s*onUpdateSalary: \(SalaryPaymentEntity\) -> Unit = \{\}', '', emp)
emp = re.sub(r',\s*onDeleteSalary: \(SalaryPaymentEntity\) -> Unit = \{\}', '', emp)
emp = re.sub(r',\s*forceTab: Int = -1', '', emp)
emp = re.sub(r',\s*forceEmployeeSubTab: Int = -1', '', emp)
emp = re.sub(r'var mainTab by remember.*?\}', 'val mainTab = 1', emp)
emp = re.sub(r'var employeeSubTab by remember.*?\}', 'val employeeSubTab = 0', emp)

with open("app/src/main/java/com/example/ui/screens/EmployeeInfoScreen.kt", "w") as f:
    f.write(emp)

# Make SalariesScreen
sal = original.replace("fun PeopleScreen(", "fun SalariesScreen(")
sal = re.sub(r',\s*customers: List<CustomerEntity>', '', sal)
sal = re.sub(r',\s*onSaveCustomer: \(CustomerEntity\) -> Unit', '', sal)
sal = re.sub(r',\s*onDeleteCustomer: \(CustomerEntity\) -> Unit', '', sal)
sal = re.sub(r',\s*onSettlePayment: \(customerId: Long, amount: Double\) -> Unit', '', sal)
sal = re.sub(r',\s*onSaveEmployee: \(EmployeeEntity\) -> Unit', '', sal)
sal = re.sub(r',\s*onDeleteEmployee: \(EmployeeEntity\) -> Unit', '', sal)
sal = re.sub(r',\s*forceTab: Int = -1', '', sal)
sal = re.sub(r',\s*forceEmployeeSubTab: Int = -1', '', sal)
sal = re.sub(r'var mainTab by remember.*?\}', 'val mainTab = 1', sal)
sal = re.sub(r'var employeeSubTab by remember.*?\}', 'val employeeSubTab = 1', sal)

with open("app/src/main/java/com/example/ui/screens/SalariesScreen.kt", "w") as f:
    f.write(sal)

