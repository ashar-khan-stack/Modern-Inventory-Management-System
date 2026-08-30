import re

with open("app/src/main/java/com/example/ui/screens/SalesScreen.kt", "r") as f:
    content = f.read()

# Rename SalesScreen to SalesHistoryScreen
content = content.replace("fun SalesScreen(", "fun SalesHistoryScreen(")

# Remove startInCreateMode parameter
content = re.sub(r',\s*startInCreateMode: Boolean = false', '', content)

# We want to remove the logic that shows the dialog inside SalesHistoryScreen
# `var showNewSaleDialog by remember(startInCreateMode) { mutableStateOf(startInCreateMode) }`
content = re.sub(r'var showNewSaleDialog by remember.*?\{.*?\}', 'var showNewSaleDialog by remember { mutableStateOf(false) }', content)

# But wait, we want to call MainActivity's navigation instead of just showing dialog?
# The FAB currently does: `onAction = { showNewSaleDialog = true }`
# We need to add an `onNavigateToNewSale: () -> Unit` parameter and call it.
content = content.replace("fun SalesHistoryScreen(", "fun SalesHistoryScreen(\n    onNavigateToNewSale: () -> Unit,")
content = content.replace("showNewSaleDialog = true", "onNavigateToNewSale()")

# We should also remove the dialog invocation inside SalesHistoryScreen
# We can just remove the whole block:
# if (showNewSaleDialog) {
#    CreateSaleDialog(...) 
# }
# Since it's renamed to NewSaleScreen, it might look like:
# if (showNewSaleDialog) {
#    NewSaleScreen(...) 
# }
content = re.sub(r'if \(showNewSaleDialog\) \{[\s\S]*?NewSaleScreen\([\s\S]*?\}\n        \)', '', content)

with open("app/src/main/java/com/example/ui/screens/SalesScreen.kt", "w") as f:
    f.write(content)

