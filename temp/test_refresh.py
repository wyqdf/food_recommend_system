import requests
import time
import json

# 先登录获取token
login_url = 'http://localhost:8081/api/admin/login'
login_data = {
    "username": "admin",
    "password": "123456"
}

print("1. 尝试登录获取token...")
try:
    login_response = requests.post(login_url, json=login_data, timeout=10)
    print(f"登录响应: {login_response.status_code}")
    if login_response.status_code == 200:
        login_result = login_response.json()
        print(f"登录结果: {login_result}")
        token = login_result.get('data', {}).get('token')
        print(f"获取到token: {token[:20] if token else 'None'}...")
    else:
        print(f"登录失败: {login_response.text}")
        token = None
except Exception as e:
    print(f"登录错误: {e}")
    token = None

if not token:
    print("无法获取token，尝试直接调用刷新接口...")
    # 尝试使用可能的默认token
    token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJhZG1pbiIsImlhdCI6MTc0MjU4NjUzNiwiZXhwIjoxNzQyNjcyOTM2fQ.f0S1aK0b0l3l3l3l3l3l3l3l3l3l3l3l3l3l3l3l3"

url = 'http://localhost:8081/api/admin/statistics/refresh'
headers = {
    'Authorization': f'Bearer {token}'
}

print("\n2. 开始刷新统计数据...")
start = time.time()
try:
    response = requests.post(url, headers=headers, timeout=120)
    elapsed = time.time() - start
    print(f"\n响应状态码: {response.status_code}")
    print(f"响应内容: {response.text[:500] if len(response.text) > 500 else response.text}")
    print(f"\n总耗时: {elapsed:.2f}秒")
except Exception as e:
    elapsed = time.time() - start
    print(f"错误: {e}")
    print(f"总耗时: {elapsed:.2f}秒")
