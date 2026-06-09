package shadows;

import shaders.ShaderProgram;
import shaders.UniformMatrix;
import shaders.UniformFloat;
import shaders.UniformInt;
import shaders.UniformVec3;
import utils.MyFile;

public class ShadowTerrainShader extends ShaderProgram {

    private static final MyFile VERTEX_FILE = new MyFile("shadows", "shadowTerrainVertex.txt");
    private static final MyFile FRAGMENT_FILE = new MyFile("shadows", "shadowTerrainFragment.txt");

    public UniformMatrix projectionViewMatrix = new UniformMatrix("projectionViewMatrix");
    public UniformMatrix modelMatrix = new UniformMatrix("modelMatrix");
    
    // Terrain displacement uniforms
    public UniformFloat uMaxHeight = new UniformFloat("uMaxHeight");
    public UniformFloat uRoughness = new UniformFloat("uRoughness");
    public UniformInt uOctaves = new UniformInt("uOctaves");
    public UniformFloat uScale = new UniformFloat("uScale");
    public UniformFloat uOffsetX = new UniformFloat("uOffsetX");
    public UniformFloat uOffsetZ = new UniformFloat("uOffsetZ");
    public UniformFloat uBaseHeight = new UniformFloat("uBaseHeight");
    public UniformFloat uInfinite = new UniformFloat("uInfinite");

    public ShadowTerrainShader() {
        super(VERTEX_FILE, FRAGMENT_FILE, "inPosition");
        super.storeAllUniformLocations(projectionViewMatrix, modelMatrix, uMaxHeight, uRoughness, uOctaves, uScale, uOffsetX, uOffsetZ, uBaseHeight, uInfinite);
    }
}
