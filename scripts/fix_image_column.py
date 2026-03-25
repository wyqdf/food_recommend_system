import csv
import mysql.connector
from mysql.connector import Error

db_config = {
    'host': 'localhost',
    'database': 'food_recommend',
    'user': 'root',
    'password': '123456',
    'charset': 'utf8mb4'
}

csv_file_path = r'F:\Desktop\food-recommendation-system\dataset\菜谱RAW.csv'

def main():
    try:
        connection = mysql.connector.connect(**db_config)
        cursor = connection.cursor()
        
        cursor.execute("SHOW COLUMNS FROM recipes LIKE 'image'")
        result = cursor.fetchone()
        print(f"Current image column: {result}")
        
        print("Modifying image column to TEXT...")
        cursor.execute("ALTER TABLE recipes MODIFY COLUMN image TEXT COMMENT '封面图URL'")
        connection.commit()
        print("Column modified successfully")
        
        print("\nLoading image URLs from CSV...")
        image_map = {}
        
        with open(csv_file_path, 'r', encoding='utf-8-sig') as file:
            reader = csv.reader(file)
            header = next(reader)
            
            id_idx = 0
            image_idx = 12
            
            for row in reader:
                if len(row) > image_idx:
                    recipe_id = row[id_idx]
                    image_url = row[image_idx]
                    if recipe_id and image_url:
                        image_map[recipe_id] = image_url
        
        print(f"Loaded {len(image_map)} image URLs from CSV")
        
        print("\nUpdating database...")
        updated = 0
        failed = 0
        
        for recipe_id, image_url in image_map.items():
            try:
                cursor.execute(
                    "UPDATE recipes SET image = %s WHERE id = %s",
                    (image_url, recipe_id)
                )
                if cursor.rowcount > 0:
                    updated += 1
                else:
                    failed += 1
            except Error as e:
                failed += 1
        
        connection.commit()
        
        cursor.execute("SELECT COUNT(*) FROM recipes WHERE image IS NOT NULL AND image != ''")
        count = cursor.fetchone()[0]
        
        print(f"\nResults:")
        print(f"  Updated: {updated}")
        print(f"  Failed (no matching ID): {failed}")
        print(f"  Total recipes with image: {count}")
        
        cursor.close()
        connection.close()
        print("\nDone!")
        
    except Error as e:
        print(f"Database error: {e}")

if __name__ == "__main__":
    main()
