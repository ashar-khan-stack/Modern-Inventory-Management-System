import sys

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace("summaryTotals = dashboardSummaryTotals,", "summaryTotals = com.example.ui.screens.DashboardSummaryTotals(dashboardSummaryTotals.totalSales, dashboardSummaryTotals.totalExpenses, dashboardSummaryTotals.totalOutstanding),")
content = content.replace("dashboardTotals = dashboardSummaryTotals,", "dashboardTotals = com.example.ui.screens.DashboardSummaryTotals(dashboardSummaryTotals.totalSales, dashboardSummaryTotals.totalExpenses, dashboardSummaryTotals.totalOutstanding),")

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
