with open("app/src/main/java/com/example/ui/screens/SalesScreen.kt", "r") as f:
    content = f.read()
    
# Remove any excessive trailing braces before @Composable
import re
content = re.sub(r'\}\n        \}\n    \}\n        \}\n\}', '}\n    }\n}', content)
# Just to be safe, I'll count braces or find where it's broken.

with open("app/src/main/java/com/example/ui/screens/SalesScreen.kt", "w") as f:
    f.write(content)
