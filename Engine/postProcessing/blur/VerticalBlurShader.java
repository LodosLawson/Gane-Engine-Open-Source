package postProcessing.blur;

import shaders.ShaderProgram;
import utils.MyFile;

public class VerticalBlurShader extends ShaderProgram {

	private static final MyFile VERTEX_FILE = new MyFile("postProcessing/blur", "verticalBlurVertex.txt");
	private static final MyFile FRAGMENT_FILE = new MyFile("postProcessing/blur", "blurFragment.txt");

	private shaders.UniformFloat targetHeight = new shaders.UniformFloat("targetHeight");

	public VerticalBlurShader() {
		super(VERTEX_FILE, FRAGMENT_FILE, "position");
		super.storeAllUniformLocations(targetHeight);
	}

	public void loadTargetHeight(float height) {
		targetHeight.loadFloat(height);
	}

}


