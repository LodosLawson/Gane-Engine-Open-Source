import sys, json
def analyze_glb(filepath):
    with open(filepath, 'rb') as f:
        magic = f.read(4)
        version = int.from_bytes(f.read(4), 'little')
        length = int.from_bytes(f.read(4), 'little')
        chunk_len = int.from_bytes(f.read(4), 'little')
        chunk_type = f.read(4)
        json_data = f.read(chunk_len)
        gltf = json.loads(json_data.decode('utf-8'))
        
        nodes = gltf.get('nodes', [])
        for i, n in enumerate(nodes):
            children = n.get('children', [])
            mesh = n.get('mesh')
            name = n.get('name', 'unnamed')
            print(f"Node {i} ({name}): mesh={mesh}, children={children}")
analyze_glb('src/res/DEFAULTTREES/mighty_oak_trees.glb')
