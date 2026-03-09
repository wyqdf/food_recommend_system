import mysql.connector
from datetime import date
import time

conn = mysql.connector.connect(
    host='localhost',
    port=3306,
    user='root',
    password='123456',
    database='food_recommend'
)

today = date.today()
start_time = time.time()
print(f"=== 手动刷新统计数据 - {today} ===\n")

cursor = conn.cursor()

# ============ 1. 食材使用统计 ============
print("1. 刷新食材使用统计...")
t0 = time.time()
cursor.execute("""
    SELECT ri.ingredient_id, i.name, COUNT(*) as cnt
    FROM recipe_ingredients ri
    INNER JOIN ingredients i ON ri.ingredient_id = i.id
    GROUP BY ri.ingredient_id
    ORDER BY cnt DESC
    LIMIT 20
""")
data = cursor.fetchall()

cursor.execute("DELETE FROM ingredient_usage_summary WHERE stat_date = %s", (today,))
for row in data:
    cursor.execute("""
        INSERT INTO ingredient_usage_summary (stat_date, ingredient_id, ingredient_name, recipe_count)
        VALUES (%s, %s, %s, %s)
    """, (today, row[0], row[1], row[2]))
conn.commit()
print(f"   完成，耗时 {time.time()-t0:.2f}秒，插入 {len(data)} 条")

# ============ 2. 口味分布统计 ============
print("2. 刷新口味分布统计...")
t0 = time.time()
cursor.execute("SELECT id, name FROM tastes")
tastes = {t[0]: t[1] for t in cursor.fetchall()}

cursor.execute("SELECT taste_id, COUNT(*) FROM recipes WHERE taste_id IS NOT NULL GROUP BY taste_id")
taste_counts = dict(cursor.fetchall())

data = [(tid, tastes.get(tid, ''), cnt) for tid, cnt in taste_counts.items() if cnt > 0]
data.sort(key=lambda x: x[2], reverse=True)

cursor.execute("DELETE FROM taste_distribution_summary WHERE stat_date = %s", (today,))
for row in data:
    cursor.execute("""
        INSERT INTO taste_distribution_summary (stat_date, taste_id, taste_name, recipe_count)
        VALUES (%s, %s, %s, %s)
    """, (today, row[0], row[1], row[2]))
conn.commit()
print(f"   完成，耗时 {time.time()-t0:.2f}秒，插入 {len(data)} 条")

# ============ 3. 技法分布统计 ============
print("3. 刷新烹饪技法分布统计...")
t0 = time.time()
cursor.execute("SELECT id, name FROM techniques")
techniques = {t[0]: t[1] for t in cursor.fetchall()}

cursor.execute("SELECT technique_id, COUNT(*) FROM recipes WHERE technique_id IS NOT NULL GROUP BY technique_id")
tech_counts = dict(cursor.fetchall())

data = [(tid, techniques.get(tid, ''), cnt) for tid, cnt in tech_counts.items() if cnt > 0]
data.sort(key=lambda x: x[2], reverse=True)

cursor.execute("DELETE FROM technique_distribution_summary WHERE stat_date = %s", (today,))
for row in data:
    cursor.execute("""
        INSERT INTO technique_distribution_summary (stat_date, technique_id, technique_name, recipe_count)
        VALUES (%s, %s, %s, %s)
    """, (today, row[0], row[1], row[2]))
conn.commit()
print(f"   完成，耗时 {time.time()-t0:.2f}秒，插入 {len(data)} 条")

# ============ 4. 耗时分布统计 ============
print("4. 刷新耗时分布统计...")
t0 = time.time()
cursor.execute("SELECT id, name FROM time_costs")
timecosts = {t[0]: t[1] for t in cursor.fetchall()}

cursor.execute("SELECT time_cost_id, COUNT(*) FROM recipes WHERE time_cost_id IS NOT NULL GROUP BY time_cost_id")
tc_counts = dict(cursor.fetchall())

data = [(tid, timecosts.get(tid, ''), cnt) for tid, cnt in tc_counts.items() if cnt > 0]

cursor.execute("DELETE FROM timecost_distribution_summary WHERE stat_date = %s", (today,))
for row in data:
    cursor.execute("""
        INSERT INTO timecost_distribution_summary (stat_date, timecost_id, timecost_name, recipe_count)
        VALUES (%s, %s, %s, %s)
    """, (today, row[0], row[1], row[2]))
conn.commit()
print(f"   完成，耗时 {time.time()-t0:.2f}秒，插入 {len(data)} 条")

# ============ 5. 质量分析统计 ============
print("5. 刷新质量分析统计...")
t0 = time.time()

cursor.execute("""
    SELECT
        (SELECT COUNT(*) FROM recipes) as total,
        (SELECT COUNT(*) FROM recipes WHERE like_count > 100) as high_quality,
        (SELECT AVG(like_count) FROM recipes) as avg_like,
        (SELECT AVG(reply_count) FROM recipes) as avg_reply,
        (SELECT COUNT(*) FROM recipes WHERE like_count = 0 AND reply_count = 0) as zero_interaction
""")
row = cursor.fetchone()
total, high_quality, avg_like, avg_reply, zero_interaction = row

cursor.execute("DELETE FROM quality_analysis_summary WHERE stat_date = %s", (today,))
cursor.execute("""
    INSERT INTO quality_analysis_summary
    (stat_date, high_quality_recipes, average_like_count, average_comment_count, zero_interaction_recipes, total_recipes)
    VALUES (%s, %s, %s, %s, %s, %s)
""", (today, high_quality, avg_like or 0, avg_reply or 0, zero_interaction, total))
conn.commit()
print(f"   完成，耗时 {time.time()-t0:.2f}秒")

# ============ 6. 互动趋势统计 ============
print("6. 刷新互动趋势统计...")
t0 = time.time()

cursor.execute("DELETE FROM interaction_trend_summary WHERE stat_date = %s", (today,))
cursor.execute("""
    INSERT INTO interaction_trend_summary (stat_date, like_count, favorite_count, view_count)
    VALUES (%s,
        (SELECT SUM(like_count) FROM recipes),
        (SELECT SUM(favorite_count) FROM recipes),
        0)
""", (today,))
conn.commit()
print(f"   完成，耗时 {time.time()-t0:.2f}秒")

# ============ 7. 活跃用户 Top 10 (从comments表获取) ============
print("7. 刷新活跃用户统计...")
t0 = time.time()

# 使用 comments 表的 user_id 来统计活跃用户
cursor.execute("""
    SELECT c.user_id, u.username, COUNT(*) as comment_cnt
    FROM comments c
    LEFT JOIN users u ON c.user_id = u.id
    WHERE c.user_id IS NOT NULL
    GROUP BY c.user_id
    ORDER BY comment_cnt DESC
    LIMIT 10
""")
data = cursor.fetchall()

cursor.execute("DELETE FROM active_users_summary WHERE stat_date = %s", (today,))
for rank, row in enumerate(data, 1):
    cursor.execute("""
        INSERT INTO active_users_summary
        (stat_date, user_id, username, recipe_count, comment_count, total_score, user_rank)
        VALUES (%s, %s, %s, 0, %s, 0, %s)
    """, (today, row[0], row[1], row[2], rank))
conn.commit()
print(f"   完成，耗时 {time.time()-t0:.2f}秒，插入 {len(data)} 条")

# 验证
print("\n=== 验证统计数据 ===")
tables = [
    "ingredient_usage_summary",
    "taste_distribution_summary",
    "technique_distribution_summary",
    "timecost_distribution_summary",
    "quality_analysis_summary",
    "interaction_trend_summary",
    "active_users_summary"
]

for table in tables:
    cursor.execute(f"SELECT COUNT(*) FROM {table}")
    count = cursor.fetchone()[0]
    print(f"{table}: {count} 条")

cursor.close()
conn.close()

print(f"\n=== 统计数据刷新完成，总耗时 {time.time()-start_time:.2f}秒 ===")
