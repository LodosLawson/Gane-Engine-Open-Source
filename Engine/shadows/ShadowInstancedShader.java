package shadows;

import shaders.ShaderProgram;
import shaders.UniformMatrix;
import utils.MyFile;

public class ShadowInstancedShader extends ShaderProgram {

    private static final MyFile VERTEX_FILE = new MyFile("shadows", "shadowVertexInstanced.txt");
    private static final MyFile FRAGMENT_FILE = new MyFile("shadows", "shadowFragment.txt");

    protected UniformMatrix projectionViewMatrix = new UniformMatrix("projectionViewMatrix");
    protected shaders.UniformSampler modelTexture = new shaders.UniformSampler("modelTexture");
    protected shaders.UniformFloat numberOfRows = new shaders.UniformFloat("numberOfRows");

    protected ShadowInstancedShader() {
        super(VERTEX_FILE, FRAGMENT_FILE, "in_position", "in_textureCoords", "transformationMatrix", "textureOffsetIndex");
        super.storeAllUniformLocations(projectionViewMatrix, modelTexture, numberOfRows);
    }
    
    public void connectTextureUnits() {
        super.start();
        modelTexture.loadTexUnit(0);
        super.stop();
    }
}
