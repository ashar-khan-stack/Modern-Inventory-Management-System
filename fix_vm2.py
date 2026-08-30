import sys

with open('app/src/main/java/com/example/ui/viewmodel/InventoryViewModel.kt', 'r') as f:
    content = f.read()

# I will count { and } and see if it matches.
open_b = content.count('{')
close_b = content.count('}')
print(f"Open: {open_b}, Close: {close_b}")

