import mysql.connector

conn = mysql.connector.connect(
    host='localhost',
    port=3306,
    user='root',
    password='123456',
    database='food_recommend'
)

cursor = conn.cursor()

tables_to_check = [
    'users', 'recipes', 'comments', 'ingredients', 'tastes', 'techniques', 'time_costs',
    'recipe_ingredients', 'recipe_categories', 'difficulties',
    'ingredient_usage_summary', 'taste_distribution_summary', 'technique_distribution_summary',
    'timecost_distribution_summary', 'active_users_summary', 'quality_analysis_summary',
    'interaction_trend_summary'
]

for table in tables_to_check:
    print(f"\n{'='*50}")
    print(f"=== {table} ===")
    print('='*50)
    cursor.execute(f"DESCRIBE {table}")
    for row in cursor.fetchall():
        print(f"{row[0]:30} {row[1]:20} {row[3]}")

cursor.close()
conn.close()
