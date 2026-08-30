import sys
import re

with open('app/src/main/java/com/example/ui/viewmodel/InventoryViewModel.kt', 'r') as f:
    content = f.read()

content = re.sub(r'val products\s*=.*?\n', '', content)
content = re.sub(r'fun saveProduct.*?\}', '', content, flags=re.DOTALL)
content = re.sub(r'fun deleteProduct.*?\}', '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/viewmodel/InventoryViewModel.kt', 'w') as f:
    f.write(content)
print("VM fixed")
