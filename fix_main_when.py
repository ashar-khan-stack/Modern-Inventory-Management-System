import sys

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

bad = """                    AppScreen.SALARIES -> {
                        com.example.ui.screens.SalariesScreen(
                            employees = employees,
                            salaryPayments = salaryPayments,
                            onDisburseSalary = { viewModel.disburseSalary(it) },
                            onUpdateSalary = { viewModel.updateSalary(it) },
                            onDeleteSalary = { viewModel.deleteSalary(it) }
                        )
                    }
                }
            }"""

good = """                    AppScreen.SALARIES -> {
                        com.example.ui.screens.SalariesScreen(
                            employees = employees,
                            salaryPayments = salaryPayments,
                            onDisburseSalary = { viewModel.disburseSalary(it) },
                            onUpdateSalary = { viewModel.updateSalary(it) },
                            onDeleteSalary = { viewModel.deleteSalary(it) }
                        )
                    }
                    else -> {
                        // Fallback or debug screens
                    }
                }
            }"""

content = content.replace(bad, good)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
