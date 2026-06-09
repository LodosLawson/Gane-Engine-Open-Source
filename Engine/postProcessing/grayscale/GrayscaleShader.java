package postProcessing.grayscale;

import shaders.ShaderProgram;
import utils.MyFile;

public class GrayscaleShader extends ShaderProgram {

	private static final MyFile VERTEX_FILE = new MyFile("postProcessing", "simpleVertex.txt");
	private static final MyFile FRAGMENT_FILE = new MyFile("postProcessing/grayscale", "grayscaleFragment.txt");

	public GrayscaleShader() {
		super(VERTEX_FILE, FRAGMENT_FILE, "position");
	}
}
