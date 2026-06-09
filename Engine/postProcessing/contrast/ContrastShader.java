package postProcessing.contrast;

import shaders.ShaderProgram;
import utils.MyFile;

public class ContrastShader extends ShaderProgram {

	private static final MyFile VERTEX_FILE = new MyFile("postProcessing/contrast", "contrastVertex.txt");
	private static final MyFile FRAGMENT_FILE = new MyFile("postProcessing/contrast", "contrastFragment.txt");

	public ContrastShader() {
		super(VERTEX_FILE, FRAGMENT_FILE, "position");
	}

}


