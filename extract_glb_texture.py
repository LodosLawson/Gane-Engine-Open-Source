import json
import struct
import sys
import os

def extract_textures(glb_path, out_dir):
    with open(glb_path, 'rb') as f:
        magic, ver, length = struct.unpack('<III', f.read(12))
        chunk0_len, chunk0_type = struct.unpack('<II', f.read(8))
        json_data = f.read(chunk0_len)
        gltf = json.loads(json_data)
        
        chunk1_len, chunk1_type = struct.unpack('<II', f.read(8))
        bin_data = f.read(chunk1_len)
        
    if 'images' not in gltf:
        print("No images found in GLB")
        return
        
    if not os.path.exists(out_dir):
        os.makedirs(out_dir)
        
    for i, img in enumerate(gltf['images']):
        if 'bufferView' in img:
            bv = gltf['bufferViews'][img['bufferView']]
            offset = bv.get('byteOffset', 0)
            length = bv['byteLength']
            img_bytes = bin_data[offset:offset+length]
            
            mime = img.get('mimeType', 'image/png')
            ext = '.png' if 'png' in mime else '.jpg'
            
            out_path = os.path.join(out_dir, f'texture_{i}{ext}')
            with open(out_path, 'wb') as out_f:
                out_f.write(img_bytes)
            print(f"Extracted {out_path}")

if __name__ == '__main__':
    extract_textures('src/res/DEFAULT_BIRD/DEF_BIRD.glb', 'src/res/DEFAULT_BIRD')
