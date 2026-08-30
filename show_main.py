with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    lines = f.readlines()
for i, line in enumerate(lines):
    if i >= 410 and i <= 460:
        print(f"{i}: {line}", end='')
