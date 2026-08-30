with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    lines = f.readlines()
for i, line in enumerate(lines):
    if i > 470 and i < 590:
        print(line, end='')
