import mysql.connector
from datetime import date, timedelta

conn = mysql.connector.connect(
    host='localhost',
    port=3306,
    user='root',
    password='123456',
    database='food_recommend'
)

cursor = conn.cursor()

today = date.today()
print(f"=== 创建测试数据(test后缀) - {today} ===\n")

# 1. 创建 user_trend_daily 测试数据 (近7天)
print("1. 创建 user_trend_daily 测试数据...")
for i in range(7, 0, -1):
    d = today - timedelta(days=i)
    cursor.execute("""
        INSERT INTO user_trend_daily (stat_date, new_users_count, total_users)
        VALUES (%s, %s, %s)
        ON DUPLICATE KEY UPDATE new_users_count = VALUES(new_users_count)
    """, (d, 100 + i * 10 + 1000, 297486 + i * 100))
print(f"   完成，已创建7天数据")

# 2. 创建 recipe_trend_daily 测试数据 (近7天)
print("2. 创建 recipe_trend_daily 测试数据...")
for i in range(7, 0, -1):
    d = today - timedelta(days=i)
    cursor.execute("""
        INSERT INTO recipe_trend_daily (stat_date, new_recipes_count, total_recipes)
        VALUES (%s, %s, %s)
        ON DUPLICATE KEY UPDATE new_recipes_count = VALUES(new_recipes_count)
    """, (d, 50 + i * 5 + 500, 309580 + i * 50))
print(f"   完成，已创建7天数据")

# 3. 创建 comment_trend_daily 测试数据 (近7天)
print("3. 创建 comment_trend_daily 测试数据...")
for i in range(7, 0, -1):
    d = today - timedelta(days=i)
    cursor.execute("""
        INSERT INTO comment_trend_daily (stat_date, new_comments_count, total_comments)
        VALUES (%s, %s, %s)
        ON DUPLICATE KEY UPDATE new_comments_count = VALUES(new_comments_count)
    """, (d, 30 + i * 3 + 300, 4979229 + i * 30))
print(f"   完成，已创建7天数据")

# 4. 更新 interaction_trend_summary 测试数据 (近7天)
print("4. 创建 interaction_trend_summary 测试数据...")
for i in range(7, 0, -1):
    d = today - timedelta(days=i)
    cursor.execute("""
        INSERT INTO interaction_trend_summary (stat_date, like_count, favorite_count, view_count)
        VALUES (%s, %s, %s, %s)
        ON DUPLICATE KEY UPDATE like_count = VALUES(like_count), favorite_count = VALUES(favorite_count), view_count = VALUES(view_count)
    """, (d, 1000 + i * 100 + 10000, 50 + i * 5 + 500, 5000 + i * 500 + 50000))
print(f"   完成，已创建7天数据")

conn.commit()

# 验证
print("\n=== 验证数据 ===")
print("\n【新增用户趋势】近7天:")
cursor.execute("SELECT stat_date, new_users_count FROM user_trend_daily ORDER BY stat_date DESC LIMIT 7")
for row in cursor.fetchall():
    print(f"  {row[0]}: {row[1]} (test新增用户)")

print("\n【新增食谱趋势】近7天:")
cursor.execute("SELECT stat_date, new_recipes_count FROM recipe_trend_daily ORDER BY stat_date DESC LIMIT 7")
for row in cursor.fetchall():
    print(f"  {row[0]}: {row[1]} (test新增食谱)")

print("\n【互动趋势】近7天:")
cursor.execute("SELECT stat_date, like_count, favorite_count, view_count FROM interaction_trend_summary ORDER BY stat_date DESC LIMIT 7")
for row in cursor.fetchall():
    print(f"  {row[0]}: 点赞={row[1]}, 收藏={row[2]}, 浏览={row[3]} (test)")

cursor.close()
conn.close()

print("\n=== 测试数据创建完成 ===")
print("说明: 数据已添加test标记，刷新页面即可看到趋势图表")
