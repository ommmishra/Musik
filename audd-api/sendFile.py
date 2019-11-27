import requests
data = {
    'return': 'timecode,apple_music,deezer,spotify',
    'api_token': 'test'
}
result = requests.post('https://api.audd.io/', data=data, files={'file': open('/home/omm/Downloads/Telegram Desktop/27 Nov, 21.04.mp3', 'rb')})
print(result.text)