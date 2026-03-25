#!/usr/bin/env python3
"""
将食谱图片URL更新为阿里云OSS格式
OSS路径: https://recipe-images.oss-cn-beijing.aliyuncs.com/images/{old_recipe_id}.jpg
"""

import mysql.connector

db_config = {
    'host': 'localhost',
    'database': 'food_recommend',
    'user': 'root',
    'password': '123456',
    'charset': 'utf8mb4'
}

OSS_BASE_URL = "https://recipe-images.oss-cn-beijing.aliyuncs.com/images/"

def main():
    print("=" * 60)
    print("Update Recipe Image URLs to OSS Format")
    print("=" * 60)

    try:
        connection = mysql.connector.connect(**db_config)
        cursor = connection.cursor()

        print("\nBuilding ID mapping...")
        cursor.execute("SELECT old_recipe_id, new_recipe_id FROM recipe_id_mapping")
        id_mapping = {row[1]: row[0] for row in cursor.fetchall()}
        print(f"  Loaded {len(id_mapping)} ID mappings")

        print("\nFetching all recipes...")
        cursor.execute("SELECT id, title, image FROM recipes ORDER BY id")
        recipes = cursor.fetchall()
        print(f"  Found {len(recipes)} recipes")

        print("\nUpdating image URLs...")
        updated = 0
        batch_size = 1000
        old_id_not_found = 0
        oss_url_pattern = "recipe-images.oss-cn-beijing.aliyuncs.com/images/"

        for i, (recipe_id, title, current_image) in enumerate(recipes):
            if recipe_id in id_mapping:
                old_recipe_id = id_mapping[recipe_id]
                new_oss_url = f"{OSS_BASE_URL}{old_recipe_id}.jpg"

                try:
                    cursor.execute(
                        "UPDATE recipes SET image = %s WHERE id = %s",
                        (new_oss_url, recipe_id)
                    )
                    if cursor.rowcount > 0:
                        updated += 1
                except Exception as e:
                    pass
            else:
                old_id_not_found += 1

            if (i + 1) % batch_size == 0:
                connection.commit()
                print(f"  Progress: {i + 1}/{len(recipes)}, updated: {updated}")

        connection.commit()

        cursor.execute("SELECT COUNT(*) FROM recipes WHERE image LIKE '%recipe-images.oss-cn-beijing.aliyunc.com%'")
        oss_count = cursor.fetchone()[0]

        print("\n" + "=" * 60)
        print("Results:")
        print(f"  Total recipes: {len(recipes)}")
        print(f"  Updated: {updated}")
        print(f"  Old ID not found: {old_id_not_found}")
        print(f"  Recipes with OSS URL: {oss_count}")
        print("=" * 60)

        cursor.close()
        connection.close()

    except mysql.connector.Error as e:
        print(f"Database error: {e}")
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    main()
