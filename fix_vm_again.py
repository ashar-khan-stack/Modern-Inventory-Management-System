import sys

with open('app/src/main/java/com/example/ui/viewmodel/InventoryViewModel.kt', 'r') as f:
    content = f.read()

# I will find the second "package com.example.ui.viewmodel"
idx = content.find("package com.example.ui.viewmodel", 10)
if idx != -1:
    content = content[idx:]
    
with open('app/src/main/java/com/example/ui/viewmodel/InventoryViewModel.kt', 'w') as f:
    f.write(content)
print("Stripped prepended stuff!")
