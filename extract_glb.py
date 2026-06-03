import sys
import struct
import json
import os

def extract_glb_textures(glb_path):
    with open(glb_path, 'rb') as f:
        magic = f.read(4)
        if magic != b'glTF':
            print("Not a valid GLB file")
            return

        version, length = struct.unpack('<II', f.read(8))
        print(f"GLB Version: {version}, Total Length: {length}")

        json_chunk_length, = struct.unpack('<I', f.read(4))
        json_chunk_type = f.read(4)
        if json_chunk_type != b'JSON':
            print("First chunk is not JSON")
            return
            
        json_data = f.read(json_chunk_length).decode('utf-8')
        gltf = json.loads(json_data)
        
        # Check if BIN chunk exists
        bin_header = f.read(8)
        if len(bin_header) < 8:
            print("No BIN chunk found")
            return
            
        bin_chunk_length, = struct.unpack('<I', bin_header[:4])
        bin_chunk_type = bin_header[4:]
        if bin_chunk_type != b'BIN\x00':
            print("Second chunk is not BIN")
            return
            
        bin_data = f.read(bin_chunk_length)
        
        if 'images' not in gltf:
            print("No images found in the GLB.")
            return
            
        buffer_views = gltf.get('bufferViews', [])
        
        dir_name = os.path.dirname(glb_path)
        base_name = os.path.splitext(os.path.basename(glb_path))[0]
        
        extracted_files = []
        
        for i, img in enumerate(gltf['images']):
            if 'bufferView' in img:
                bv_idx = img['bufferView']
                bv = buffer_views[bv_idx]
                offset = bv.get('byteOffset', 0)
                length = bv.get('byteLength', 0)
                
                img_data = bin_data[offset:offset+length]
                mime = img.get('mimeType', '')
                
                ext = '.png'
                if mime == 'image/jpeg':
                    ext = '.jpg'
                    
                out_path = os.path.join(dir_name, f"{base_name}_tex_{i}{ext}")
                with open(out_path, 'wb') as img_f:
                    img_f.write(img_data)
                extracted_files.append(out_path)
                print(f"Extracted: {out_path}")
            else:
                print(f"Image {i} does not use bufferView (might be external URI).")

if __name__ == "__main__":
    if len(sys.argv) > 1:
        extract_glb_textures(sys.argv[1])
    else:
        print("Usage: python extract_glb.py <path_to_glb>")
