import mysql.connector

conn = mysql.connector.connect(
    host='localhost',
    port=3306,
    user='root',
    password='123456',
    database='food_recommend'
)

cursor = conn.cursor()

# 检查 users 表结构
cursor.execute("DESCRIBE users")
print("=== users 表结构 ===")
for row in cursor.fetchall():
    print(row)

# 检查 recipes 表是否有 user_id
cursor.execute("DESCRIBE recipes")
print("\n=== recipes 表结构 ===")
for row in cursor.fetchall():
    print(row)

cursor.close()
conn.close()
