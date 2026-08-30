import sys

with open('app/src/main/java/com/example/data/local/Daos.kt', 'r') as f:
    content = f.read()

content = content.replace("@Dao\n@Dao", "@Dao")

with open('app/src/main/java/com/example/data/local/Daos.kt', 'w') as f:
    f.write(content)
