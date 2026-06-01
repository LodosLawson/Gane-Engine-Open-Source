import os
import subprocess
from PIL import Image

Image.MAX_IMAGE_PIXELS = None  # Allow opening 8K/16K images

files_to_resize = [
    "Engine/res/TerrainTexture/DEFAULT_GRASS/Ground067_8K-PNG_NormalDX.png",
    "Engine/res/TerrainTexture/DEFAULT_GRASS/Ground067_8K-PNG_NormalGL.png",
    "Engine/res/TerrainTexture/DEFAULT_GRASS/Ground067_8K-PNG_Color.png"
]

exr_file = "src/res/SKY_DEFAULT_MATERIAL/DaySkyHDRI063B_16K_HDR.exr"

# 1. Resize PNGs to 2048x2048 using PIL
for file_path in files_to_resize:
    if os.path.exists(file_path):
        print(f"Downscaling {file_path}...")
        img = Image.open(file_path)
        img = img.resize((2048, 2048), Image.Resampling.LANCZOS)
        img.save(file_path, optimize=True)
        print(f"Done downscaling {file_path}")

# 2. Resize EXR to 4096x2048 using FFmpeg
if os.path.exists(exr_file):
    print(f"Downscaling {exr_file} using FFmpeg...")
    temp_file = exr_file + ".temp.exr"
    subprocess.run([
        "ffmpeg", "-y", "-i", exr_file,
        "-vf", "scale=4096:2048",
        temp_file
    ])
    if os.path.exists(temp_file):
        os.remove(exr_file)
        os.rename(temp_file, exr_file)
        print(f"Done downscaling {exr_file}")

print("Downscaling complete.")
