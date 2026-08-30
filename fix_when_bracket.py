import sys

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

bad_snippet = """                    AppScreen.REPORTS -> ReportsScreen(
                        sales = sales,
                        expenses = expenses,
                        customers = customers,
                        salaryPayments = salaryPayments
                    )
                    }
                    AppScreen.SETTINGS -> SettingsScreen("""

good_snippet = """                    AppScreen.REPORTS -> ReportsScreen(
                        sales = sales,
                        expenses = expenses,
                        customers = customers,
                        salaryPayments = salaryPayments
                    )
                    AppScreen.ACCEPTANCE_REPORT -> {
                        if (com.example.BuildConfig.DEBUG) {
                            com.example.ui.screens.AcceptanceReportScreen(
                                onNavigateToDashboard = { currentScreen = AppScreen.DASHBOARD }
                            )
                        } else {
                            // Empty in production
                        }
                    }
                    AppScreen.SETTINGS -> SettingsScreen("""

content = content.replace(bad_snippet, good_snippet)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
