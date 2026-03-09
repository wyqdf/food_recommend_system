import requests
import time
import json

# 先登录获取token
login_url = 'http://localhost:8081/api/admin/login'
login_data = {
    "username": "admin",
    "password": "123456"
}

print("1. 登录获取token...")
login_response = requests.post(login_url, json=login_data, timeout=10)
login_result = login_response.json()
token = login_result.get('data', {}).get('token')
print(f"获取到token")

# 先杀掉可能锁表的进程
import mysql.connector
conn = mysql.connector.connect(host='localhost', port=3306, user='root', password='123456', database='food_recommend')
cursor = conn.cursor()
print("\n2. 检查并杀掉锁表进程...")
cursor.execute("SHOW PROCESSLIST")
for row in cursor.fetchall():
    if row[7] and ('INSERT' in row[7] or 'DELETE' in row[7] or 'UPDATE' in row[7]):
        print(f"  杀掉进程 {row[0]}: {row[7][:50]}...")
        try:
            cursor.execute(f"KILL {row[0]}")
        except:
            pass
conn.close()

# 测试刷新
url = 'http://localhost:8081/api/admin/statistics/refresh'
headers = {'Authorization': f'Bearer {token}'}

print("\n3. 开始刷新统计数据...")
start = time.time()
response = requests.post(url, headers=headers, timeout=180)
elapsed = time.time() - start

print(f"\n响应状态码: {response.status_code}")
result = response.json()
print(f"响应: {result}")
print(f"\n总耗时: {elapsed:.2f}秒")
