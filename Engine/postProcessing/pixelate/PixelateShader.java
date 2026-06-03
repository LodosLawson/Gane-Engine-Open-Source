package postProcessing.pixelate;

import shaders.ShaderProgram;
import shaders.UniformFloat;
import utils.MyFile;

public class PixelateShader extends ShaderProgram {

	private static final MyFile VERTEX_FILE = new MyFile("postProcessing", "simpleVertex.txt");
	private static final MyFile FRAGMENT_FILE = new MyFile("postProcessing/pixelate", "pixelateFragment.txt");

	private UniformFloat screenWidth = new UniformFloat("screenWidth");
	private UniformFloat screenHeight = new UniformFloat("screenHeight");
	private UniformFloat pixelSize = new UniformFloat("pixelSize");

	public PixelateShader() {
		super(VERTEX_FILE, FRAGMENT_FILE, "position");
		super.storeAllUniformLocations(screenWidth, screenHeight, pixelSize);
	}

	public void loadScreenSize(float width, float height) {
		screenWidth.loadFloat(width);
		screenHeight.loadFloat(height);
	}

	public void loadPixelSize(float size) {
		pixelSize.loadFloat(size);
	}

}
