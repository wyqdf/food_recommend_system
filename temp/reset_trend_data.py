import mysql.connector
from datetime import date, timedelta
import random

conn = mysql.connector.connect(
    host='localhost',
    port=3306,
    user='root',
    password='123456',
    database='food_recommend'
)

cursor = conn.cursor()

today = date.today()
print(f"=== 清理旧数据并创建随机测试数据 - {today} ===\n")

# 清理旧数据
print("清理旧数据...")
cursor.execute("DELETE FROM user_trend_daily")
cursor.execute("DELETE FROM recipe_trend_daily")
cursor.execute("DELETE FROM comment_trend_daily")
cursor.execute("DELETE FROM interaction_trend_summary")
print("旧数据已清除\n")

# 创建随机数据
random.seed(42)  # 固定种子，保证可重复但看起来随机

# 1. 新增用户趋势 - 有波动的数据
print("1. 创建【新增用户趋势】数据...")
user_base = 500
user_data = []
for i in range(7, 0, -1):
    d = today - timedelta(days=i)
    # 随机波动 +/- 30%
    variation = int(user_base * random.uniform(-0.3, 0.4))
    count = user_base + variation + i * 20  # 略微上升趋势
    user_data.append((d, count, 297486 + (7-i) * 100))
    cursor.execute("""
        INSERT INTO user_trend_daily (stat_date, new_users_count, total_users)
        VALUES (%s, %s, %s)
    """, (d, count, 297486 + (7-i) * 100))
print(f"   完成: {[x[1] for x in user_data]}")

# 2. 新增食谱趋势 - 波动较大
print("2. 创建【新增食谱趋势】数据...")
recipe_base = 300
recipe_data = []
for i in range(7, 0, -1):
    d = today - timedelta(days=i)
    variation = int(recipe_base * random.uniform(-0.5, 0.6))
    count = recipe_base + variation + i * 10
    recipe_data.append((d, count))
    cursor.execute("""
        INSERT INTO recipe_trend_daily (stat_date, new_recipes_count, total_recipes)
        VALUES (%s, %s, %s)
    """, (d, count, 309580 + (7-i) * 50))
print(f"   完成: {[x[1] for x in recipe_data]}")

# 3. 新增评论趋势 - 波动较大
print("3. 创建【新增评论趋势】数据...")
comment_base = 200
for i in range(7, 0, -1):
    d = today - timedelta(days=i)
    variation = int(comment_base * random.uniform(-0.4, 0.5))
    count = comment_base + variation + i * 5
    cursor.execute("""
        INSERT INTO comment_trend_daily (stat_date, new_comments_count, total_comments)
        VALUES (%s, %s, %s)
    """, (d, count, 4979229 + (7-i) * 30))

# 4. 互动趋势 - 多种数据组合
print("4. 创建【互动趋势】数据...")
for i in range(7, 0, -1):
    d = today - timedelta(days=i)
    # 点赞数 - 波动较大
    like_base = 5000
    like_variation = int(like_base * random.uniform(-0.4, 0.5))
    likes = like_base + like_variation + i * 100

    # 收藏数 - 相对稳定
    fav_base = 500
    fav_variation = int(fav_base * random.uniform(-0.3, 0.3))
    favorites = fav_base + fav_variation + i * 20

    # 浏览数 - 波动大
    view_base = 20000
    view_variation = int(view_base * random.uniform(-0.5, 0.6))
    views = view_base + view_variation + i * 500

    cursor.execute("""
        INSERT INTO interaction_trend_summary (stat_date, like_count, favorite_count, view_count)
        VALUES (%s, %s, %s, %s)
    """, (d, likes, favorites, views))

conn.commit()

# 验证
print("\n=== 验证数据 ===")

print("\n【新增用户趋势】:")
cursor.execute("SELECT stat_date, new_users_count FROM user_trend_daily ORDER BY stat_date")
for row in cursor.fetchall():
    bar = "█" * (row[1] // 100)
    print(f"  {row[0]}: {row[1]:4d} {bar}")

print("\n【新增食谱趋势】:")
cursor.execute("SELECT stat_date, new_recipes_count FROM recipe_trend_daily ORDER BY stat_date")
for row in cursor.fetchall():
    bar = "█" * (row[1] // 30)
    print(f"  {row[0]}: {row[1]:4d} {bar}")

print("\n【互动趋势 - 点赞】:")
cursor.execute("SELECT stat_date, like_count FROM interaction_trend_summary ORDER BY stat_date")
for row in cursor.fetchall():
    bar = "█" * (row[1] // 200)
    print(f"  {row[0]}: {row[1]:5d} {bar}")

print("\n【互动趋势 - 收藏】:")
cursor.execute("SELECT stat_date, favorite_count FROM interaction_trend_summary ORDER BY stat_date")
for row in cursor.fetchall():
    bar = "█" * (row[1] // 20)
    print(f"  {row[0]}: {row[1]:4d} {bar}")

print("\n【互动趋势 - 浏览】:")
cursor.execute("SELECT stat_date, view_count FROM interaction_trend_summary ORDER BY stat_date")
for row in cursor.fetchall():
    bar = "█" * (row[1] // 800)
    print(f"  {row[0]}: {row[1]:5d} {bar}")

cursor.close()
conn.close()

print("\n=== 完成！随机波动的测试数据已创建 ===")
