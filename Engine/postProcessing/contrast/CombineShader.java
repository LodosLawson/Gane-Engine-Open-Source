package postProcessing.contrast;

import shaders.ShaderProgram;
import utils.MyFile;

public class CombineShader extends ShaderProgram {

	private static final MyFile VERTEX_FILE = new MyFile("postProcessing/blur", "simpleVertex.txt");
	private static final MyFile FRAGMENT_FILE = new MyFile("postProcessing/contrast", "combineFragment.txt");

	private shaders.UniformSampler colourTexture = new shaders.UniformSampler("colourTexture");
	private shaders.UniformSampler highlightTexture = new shaders.UniformSampler("highlightTexture");

	public CombineShader() {
		super(VERTEX_FILE, FRAGMENT_FILE, "position");
		super.storeAllUniformLocations(colourTexture, highlightTexture);
	}

	public void connectTextureUnits() {
		super.start();
		colourTexture.loadTexUnit(0);
		highlightTexture.loadTexUnit(1);
		super.stop();
	}

}


