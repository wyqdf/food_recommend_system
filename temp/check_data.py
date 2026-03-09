import mysql.connector

conn = mysql.connector.connect(
    host='localhost',
    port=3306,
    user='root',
    password='123456',
    database='food_recommend'
)

cursor = conn.cursor()

print("=== 检查各统计表数据 ===\n")

# 检查 ingredient_usage_summary
cursor.execute("SELECT COUNT(*) FROM ingredient_usage_summary")
count = cursor.fetchone()[0]
print(f"ingredient_usage_summary 记录数: {count}")

# 检查 taste_distribution_summary
cursor.execute("SELECT COUNT(*) FROM taste_distribution_summary")
count = cursor.fetchone()[0]
print(f"taste_distribution_summary 记录数: {count}")

# 检查 technique_distribution_summary
cursor.execute("SELECT COUNT(*) FROM technique_distribution_summary")
count = cursor.fetchone()[0]
print(f"technique_distribution_summary 记录数: {count}")

# 检查 timecost_distribution_summary
cursor.execute("SELECT COUNT(*) FROM timecost_distribution_summary")
count = cursor.fetchone()[0]
print(f"timecost_distribution_summary 记录数: {count}")

# 检查 active_users_summary
cursor.execute("SELECT COUNT(*) FROM active_users_summary")
count = cursor.fetchone()[0]
print(f"active_users_summary 记录数: {count}")

# 检查 quality_analysis_summary
cursor.execute("SELECT COUNT(*) FROM quality_analysis_summary")
count = cursor.fetchone()[0]
print(f"quality_analysis_summary 记录数: {count}")

# 检查 interaction_trend_summary
cursor.execute("SELECT COUNT(*) FROM interaction_trend_summary")
count = cursor.fetchone()[0]
print(f"interaction_trend_summary 记录数: {count}")

# 检查原材料表是否有数据
print("\n=== 检查基础数据 ===")
cursor.execute("SELECT COUNT(*) FROM ingredients")
count = cursor.fetchone()[0]
print(f"ingredients 记录数: {count}")

cursor.execute("SELECT COUNT(*) FROM tastes")
count = cursor.fetchone()[0]
print(f"tastes 记录数: {count}")

cursor.execute("SELECT COUNT(*) FROM techniques")
count = cursor.fetchone()[0]
print(f"techniques 记录数: {count}")

cursor.execute("SELECT COUNT(*) FROM time_costs")
count = cursor.fetchone()[0]
print(f"time_costs 记录数: {count}")

cursor.execute("SELECT COUNT(*) FROM recipes")
count = cursor.fetchone()[0]
print(f"recipes 记录数: {count}")

cursor.execute("SELECT COUNT(*) FROM recipe_ingredients")
count = cursor.fetchone()[0]
print(f"recipe_ingredients 记录数: {count}")

cursor.close()
conn.close()
