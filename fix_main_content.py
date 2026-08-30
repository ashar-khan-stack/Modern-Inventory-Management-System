import sys

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

open_b = content.count('{')
close_b = content.count('}')
print(f"Open: {open_b}, Close: {close_b}")
