import zlib
import re

path = r'C:\Users\Majagatha\AppData\Local\Google\AndroidStudio2025.2.3\LocalHistory\changes.storageData'
with open(path, 'rb') as f:
    data = f.read()

def find_zlib_streams(data):
    chunks = []
    # zlib headers usually start with 78 01, 78 9C, 78 DA
    for i in range(len(data) - 2):
        if data[i] == 0x78 and data[i+1] in (0x01, 0x9c, 0xda):
            try:
                dec = zlib.decompress(data[i:])
                chunks.append(dec)
            except:
                pass
    return chunks

chunks = find_zlib_streams(data)
print(f"Found {len(chunks)} compressed chunks.")

for i, dec in enumerate(chunks):
    try:
        text = dec.decode('utf-8', errors='ignore')
        if 'class MainActivity' in text and 'composable(AppRoute.Customers.route)' in text:
            print(f"Found match in chunk {i}, len {len(text)}")
            with open(f"recovered_main_{i}.kt", "w", encoding="utf-8") as out:
                out.write(text)
    except:
        pass
