package postProcessing.cartoon;

import shaders.ShaderProgram;
import shaders.UniformFloat;
import utils.MyFile;

public class CartoonShader extends ShaderProgram {

	private static final MyFile VERTEX_FILE = new MyFile("postProcessing", "simpleVertex.txt");
	private static final MyFile FRAGMENT_FILE = new MyFile("postProcessing/cartoon", "cartoonFragment.txt");

	private UniformFloat screenWidth = new UniformFloat("screenWidth");
	private UniformFloat screenHeight = new UniformFloat("screenHeight");

	public CartoonShader() {
		super(VERTEX_FILE, FRAGMENT_FILE, "position");
		super.storeAllUniformLocations(screenWidth, screenHeight);
	}

	public void loadScreenSize(float width, float height) {
		screenWidth.loadFloat(width);
		screenHeight.loadFloat(height);
	}
}
