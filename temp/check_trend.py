import mysql.connector

conn = mysql.connector.connect(
    host='localhost',
    port=3306,
    user='root',
    password='123456',
    database='food_recommend'
)

cursor = conn.cursor()

# 检查 recipe_trend_daily 表结构
cursor.execute("DESCRIBE recipe_trend_daily")
print("=== recipe_trend_daily 表结构 ===")
for row in cursor.fetchall():
    print(row)

# 检查数据
cursor.execute("SELECT * FROM recipe_trend_daily LIMIT 3")
print("\n=== 示例数据 ===")
for row in cursor.fetchall():
    print(row)

cursor.close()
conn.close()
