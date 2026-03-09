import mysql.connector

conn = mysql.connector.connect(
    host='localhost',
    port=3306,
    user='root',
    password='123456',
    database='food_recommend'
)

cursor = conn.cursor()

print("=== 食材使用 Top 10 ===")
cursor.execute("SELECT ingredient_name, recipe_count FROM ingredient_usage_summary ORDER BY recipe_count DESC LIMIT 10")
for row in cursor.fetchall():
    print(f"  {row[0]}: {row[1]}")

print("\n=== 口味分布 Top 5 ===")
cursor.execute("SELECT taste_name, recipe_count FROM taste_distribution_summary ORDER BY recipe_count DESC LIMIT 5")
for row in cursor.fetchall():
    print(f"  {row[0]}: {row[1]}")

print("\n=== 质量分析 ===")
cursor.execute("SELECT * FROM quality_analysis_summary")
row = cursor.fetchone()
print(f"  高质量食谱: {row[1]}")
print(f"  平均点赞: {row[2]}")
print(f"  平均评论: {row[3]}")
print(f"  零互动食谱: {row[4]}")
print(f"  总食谱数: {row[5]}")

print("\n=== 活跃用户 Top 5 ===")
cursor.execute("SELECT username, comment_count FROM active_users_summary ORDER BY user_rank LIMIT 5")
for row in cursor.fetchall():
    print(f"  {row[0]}: {row[1]} 条评论")

cursor.close()
conn.close()
