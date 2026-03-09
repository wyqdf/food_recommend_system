import requests

UNSPLASH_ACCESS_KEY = "mU3_JeR6DtALQDEfeFLJFilkeBQGP4JeYXXwI1tpZ6c"

url = "https://api.unsplash.com/search/photos"
params = {"query": "tomato scrambled eggs", "per_page": 1}
headers = {"Authorization": f"Client-ID {UNSPLASH_ACCESS_KEY}"}

response = requests.get(url, params=params, headers=headers, timeout=15)
print("Status:", response.status_code)

if response.status_code == 200:
    data = response.json()
    if data.get("results"):
        print("Image URL:", data["results"][0]["urls"]["regular"])
    else:
        print("No results found")
else:
    print("Error:", response.text[:200])
