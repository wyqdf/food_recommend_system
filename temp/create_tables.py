import mysql.connector

conn = mysql.connector.connect(
    host='localhost',
    port=3306,
    user='root',
    password='123456',
    database='food_recommend'
)

cursor = conn.cursor()

# 检查并创建 ingredient_usage_summary 表
tables_to_create = [
    """
    CREATE TABLE IF NOT EXISTS ingredient_usage_summary (
        stat_date DATE NOT NULL COMMENT 'Statistics date',
        ingredient_id INT NOT NULL COMMENT 'Ingredient ID',
        ingredient_name VARCHAR(100) NOT NULL COMMENT 'Ingredient name',
        recipe_count INT DEFAULT 0 COMMENT 'Recipe count',
        last_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        PRIMARY KEY (stat_date, ingredient_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Ingredient usage summary'
    """,
    """
    CREATE TABLE IF NOT EXISTS taste_distribution_summary (
        stat_date DATE NOT NULL COMMENT 'Statistics date',
        taste_id INT NOT NULL COMMENT 'Taste ID',
        taste_name VARCHAR(50) NOT NULL COMMENT 'Taste name',
        recipe_count INT DEFAULT 0 COMMENT 'Recipe count',
        last_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        PRIMARY KEY (stat_date, taste_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Taste distribution summary'
    """,
    """
    CREATE TABLE IF NOT EXISTS technique_distribution_summary (
        stat_date DATE NOT NULL COMMENT 'Statistics date',
        technique_id INT NOT NULL COMMENT 'Technique ID',
        technique_name VARCHAR(50) NOT NULL COMMENT 'Technique name',
        recipe_count INT DEFAULT 0 COMMENT 'Recipe count',
        last_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        PRIMARY KEY (stat_date, technique_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Technique distribution summary'
    """,
    """
    CREATE TABLE IF NOT EXISTS timecost_distribution_summary (
        stat_date DATE NOT NULL COMMENT 'Statistics date',
        timecost_id INT NOT NULL COMMENT 'Time cost ID',
        timecost_name VARCHAR(50) NOT NULL COMMENT 'Time cost name',
        recipe_count INT DEFAULT 0 COMMENT 'Recipe count',
        last_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        PRIMARY KEY (stat_date, timecost_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Time cost distribution summary'
    """,
    """
    CREATE TABLE IF NOT EXISTS active_users_summary (
        stat_date DATE NOT NULL COMMENT 'Statistics date',
        user_id INT NOT NULL COMMENT 'User ID',
        username VARCHAR(100) COMMENT 'Username',
        recipe_count INT DEFAULT 0 COMMENT 'Recipe count',
        comment_count INT DEFAULT 0 COMMENT 'Comment count',
        total_score INT DEFAULT 0 COMMENT 'Total score',
        user_rank INT NOT NULL COMMENT 'Rank',
        last_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        PRIMARY KEY (stat_date, user_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Active users summary'
    """,
    """
    CREATE TABLE IF NOT EXISTS quality_analysis_summary (
        stat_date DATE PRIMARY KEY COMMENT 'Statistics date',
        high_quality_recipes INT DEFAULT 0 COMMENT 'High quality recipes (>100 likes)',
        average_like_count DECIMAL(10,2) DEFAULT 0 COMMENT 'Average like count',
        average_comment_count DECIMAL(10,2) DEFAULT 0 COMMENT 'Average comment count',
        zero_interaction_recipes INT DEFAULT 0 COMMENT 'Zero interaction recipes',
        total_recipes INT DEFAULT 0 COMMENT 'Total recipes',
        last_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Quality analysis summary'
    """,
    """
    CREATE TABLE IF NOT EXISTS interaction_trend_summary (
        stat_date DATE PRIMARY KEY COMMENT 'Statistics date',
        like_count INT DEFAULT 0 COMMENT 'Like count',
        favorite_count INT DEFAULT 0 COMMENT 'Favorite count',
        view_count INT DEFAULT 0 COMMENT 'View count',
        last_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Interaction trend summary'
    """
]

for sql in tables_to_create:
    cursor.execute(sql)

conn.commit()

# 检查现有表
cursor.execute("SHOW TABLES")
tables = cursor.fetchall()
print("=== 现有表 ===")
for table in tables:
    print(table[0])

# 检查 ingredient_usage_summary 表是否存在
cursor.execute("SHOW TABLES LIKE 'ingredient_usage_summary'")
result = cursor.fetchone()
print(f"\ningredient_usage_summary 表存在: {result is not None}")

cursor.close()
conn.close()

print("\n=== 所有缺失的统计表已创建完成 ===")
