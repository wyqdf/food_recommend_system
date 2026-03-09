import mysql.connector

conn = mysql.connector.connect(
    host='localhost',
    port=3306,
    user='root',
    password='123456',
    database='food_recommend'
)

cursor = conn.cursor()

# 查看趋势相关表结构
tables = ['user_trend_daily', 'recipe_trend_daily', 'comment_trend_daily', 'interaction_trend_summary']

for table in tables:
    print(f"\n=== {table} ===")
    cursor.execute(f"DESCRIBE {table}")
    for row in cursor.fetchall():
        print(f"  {row[0]}: {row[1]}")

# 查看现有数据
print("\n\n=== 现有 user_trend_daily 数据 ===")
cursor.execute("SELECT * FROM user_trend_daily ORDER BY stat_date DESC LIMIT 5")
for row in cursor.fetchall():
    print(row)

print("\n=== 现有 recipe_trend_daily 数据 ===")
cursor.execute("SELECT * FROM recipe_trend_daily ORDER BY stat_date DESC LIMIT 5")
for row in cursor.fetchall():
    print(row)

print("\n=== 现有 comment_trend_daily 数据 ===")
cursor.execute("SELECT * FROM comment_trend_daily ORDER BY stat_date DESC LIMIT 5")
for row in cursor.fetchall():
    print(row)

print("\n=== 现有 interaction_trend_summary 数据 ===")
cursor.execute("SELECT * FROM interaction_trend_summary ORDER BY stat_date DESC LIMIT 5")
for row in cursor.fetchall():
    print(row)

cursor.close()
conn.close()
