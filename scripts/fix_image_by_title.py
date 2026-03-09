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
        
        print("Checking database IDs...")
        cursor.execute("SELECT MIN(id), MAX(id), COUNT(*) FROM recipes")
        db_result = cursor.fetchone()
        print(f"Database: ID range {db_result[0]} - {db_result[1]}, count: {db_result[2]}")
        
        cursor.execute("SELECT COUNT(*) FROM recipes WHERE image IS NOT NULL AND image != ''")
        current_images = cursor.fetchone()[0]
        print(f"Current recipes with image: {current_images}")
        
        print("\nLoading CSV data...")
        csv_data = []
        
        with open(csv_file_path, 'r', encoding='utf-8-sig') as file:
            reader = csv.reader(file)
            header = next(reader)
            
            for row in reader:
                if len(row) > 12:
                    csv_id = row[0]
                    title = row[1] if len(row) > 1 else ''
                    image_url = row[12]
                    if csv_id and image_url:
                        csv_data.append((csv_id, title, image_url))
        
        print(f"CSV records with image: {len(csv_data)}")
        
        print("\nMatching by title...")
        cursor.execute("SELECT id, title FROM recipes")
        db_recipes = {row[1]: row[0] for row in cursor.fetchall()}
        
        matched = 0
        updated = 0
        
        for csv_id, title, image_url in csv_data:
            if title in db_recipes:
                db_id = db_recipes[title]
                try:
                    cursor.execute(
                        "UPDATE recipes SET image = %s WHERE id = %s AND (image IS NULL OR image = '')",
                        (image_url, db_id)
                    )
                    if cursor.rowcount > 0:
                        updated += 1
                    matched += 1
                except Error:
                    pass
                
                if matched % 50000 == 0:
                    connection.commit()
                    print(f"  Processed {matched} matches, updated {updated}")
        
        connection.commit()
        
        cursor.execute("SELECT COUNT(*) FROM recipes WHERE image IS NOT NULL AND image != ''")
        final_count = cursor.fetchone()[0]
        
        print(f"\nResults:")
        print(f"  Matched by title: {matched}")
        print(f"  Updated: {updated}")
        print(f"  Total recipes with image: {final_count}")
        
        cursor.close()
        connection.close()
        
    except Error as e:
        print(f"Database error: {e}")

if __name__ == "__main__":
    main()
