import sys
import re

with open('app/src/main/java/com/example/ui/viewmodel/InventoryViewModel.kt', 'r') as f:
    content = f.read()

# We need to make sure the viewmodel parses. Let's run a check:
open_b = content.count('{')
close_b = content.count('}')
print(f"Open: {open_b}, Close: {close_b}")
