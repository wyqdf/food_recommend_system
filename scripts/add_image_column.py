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

def add_image_column(cursor):
    try:
        cursor.execute("SHOW COLUMNS FROM recipes LIKE 'image'")
        result = cursor.fetchone()
        if result:
            print("image column already exists")
        else:
            cursor.execute("ALTER TABLE recipes ADD COLUMN image VARCHAR(255) COMMENT '封面图URL' AFTER cookware")
            print("Added image column to recipes table")
    except Error as e:
        print(f"Error adding column: {e}")

def update_image_urls(cursor, connection):
    image_map = {}
    
    with open(csv_file_path, 'r', encoding='utf-8-sig') as file:
        reader = csv.reader(file)
        header = next(reader)
        print(f"CSV Header: {header}")
        
        id_idx = None
        image_idx = None
        
        for i, col in enumerate(header):
            col_clean = col.strip().lower().replace('\ufeff', '')
            if col_clean == 'id':
                id_idx = i
            elif col_clean == 'image_url':
                image_idx = i
        
        print(f"ID column index: {id_idx}, Image column index: {image_idx}")
        
        if id_idx is None or image_idx is None:
            print("Could not find id or image_url column")
            return
        
        count = 0
        for row in reader:
            if len(row) > max(id_idx, image_idx):
                recipe_id = row[id_idx]
                image_url = row[image_idx]
                if recipe_id and image_url:
                    image_map[recipe_id] = image_url
                    count += 1
        
        print(f"Read {count} image URLs from CSV")
    
    updated = 0
    for recipe_id, image_url in image_map.items():
        try:
            cursor.execute(
                "UPDATE recipes SET image = %s WHERE id = %s",
                (image_url, recipe_id)
            )
            if cursor.rowcount > 0:
                updated += 1
        except Error as e:
            pass
    
    connection.commit()
    print(f"Updated {updated} recipes with image URLs")

def main():
    try:
        connection = mysql.connector.connect(**db_config)
        cursor = connection.cursor()
        
        print("Adding image column...")
        add_image_column(cursor)
        connection.commit()
        
        print("\nUpdating image URLs...")
        update_image_urls(cursor, connection)
        
        cursor.execute("SELECT COUNT(*) FROM recipes WHERE image IS NOT NULL AND image != ''")
        count = cursor.fetchone()[0]
        print(f"\nTotal recipes with image: {count}")
        
        cursor.close()
        connection.close()
        print("\nDone!")
        
    except Error as e:
        print(f"Database error: {e}")

if __name__ == "__main__":
    main()
