package postProcessing.blur;

import shaders.ShaderProgram;
import utils.MyFile;

public class HorizontalBlurShader extends ShaderProgram {

	private static final MyFile VERTEX_FILE = new MyFile("postProcessing/blur", "horizontalBlurVertex.txt");
	private static final MyFile FRAGMENT_FILE = new MyFile("postProcessing/blur", "blurFragment.txt");

	private shaders.UniformFloat targetWidth = new shaders.UniformFloat("targetWidth");

	public HorizontalBlurShader() {
		super(VERTEX_FILE, FRAGMENT_FILE, "position");
		super.storeAllUniformLocations(targetWidth);
	}

	public void loadTargetWidth(float width) {
		targetWidth.loadFloat(width);
	}

}


