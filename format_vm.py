import sys

with open('app/src/main/java/com/example/ui/viewmodel/InventoryViewModel.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
skip = False
for line in lines:
    if "fun saveProduct" in line:
        skip = True
    elif "fun deleteProduct" in line:
        skip = True
    
    if skip and line.strip() == "}":
        # we can't just skip to } because there are nested { }
        pass

