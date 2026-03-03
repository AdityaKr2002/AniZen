import urllib.request
import json
url = 'https://api.github.com/search/code?q=idm.internet.download.manager+intent'
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
try:
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode())
        for item in data.get('items', [])[:5]:
            print(item['html_url'])
except Exception as e:
    print(e)
