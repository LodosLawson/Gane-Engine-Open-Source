package shadows;

import shaders.ShaderProgram;
import shaders.UniformMatrix;
import utils.MyFile;

public class ShadowShader extends ShaderProgram {

    private static final MyFile VERTEX_FILE = new MyFile("shadows", "shadowVertex.txt");
    private static final MyFile FRAGMENT_FILE = new MyFile("shadows", "shadowFragment.txt");

    protected UniformMatrix mvpMatrix = new UniformMatrix("mvpMatrix");
    protected shaders.UniformSampler modelTexture = new shaders.UniformSampler("modelTexture");

    protected ShadowShader() {
        super(VERTEX_FILE, FRAGMENT_FILE, "in_position", "in_textureCoords");
        super.storeAllUniformLocations(mvpMatrix, modelTexture);
    }
    
    public void connectTextureUnits() {
        super.start();
        modelTexture.loadTexUnit(0);
        super.stop();
    }
}
