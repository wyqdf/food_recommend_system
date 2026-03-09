import mysql.connector

conn = mysql.connector.connect(
    host='localhost',
    port=3306,
    user='root',
    password='123456',
    database='food_recommend'
)

cursor = conn.cursor()

# 检查 recipe_categories 表结构
cursor.execute("DESCRIBE recipe_categories")
print("=== recipe_categories 表结构 ===")
for row in cursor.fetchall():
    print(row)

# 检查表中有哪些数据
cursor.execute("SELECT * FROM recipe_categories LIMIT 5")
print("\n=== recipe_categories 示例数据 ===")
for row in cursor.fetchall():
    print(row)

# 检查 recipes 表结构
cursor.execute("DESCRIBE recipes")
print("\n=== recipes 表结构 ===")
for row in cursor.fetchall():
    print(row)

cursor.close()
conn.close()
