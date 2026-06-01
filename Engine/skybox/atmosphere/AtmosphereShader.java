package skybox.atmosphere;

import shaders.ShaderProgram;
import shaders.UniformFloat;
import shaders.UniformMatrix;
import shaders.UniformVec3;
import utils.MyFile;

public class AtmosphereShader extends ShaderProgram {

	private static final MyFile VERTEX_SHADER = new MyFile("skybox/atmosphere", "atmosphereVertex.txt");
	private static final MyFile FRAGMENT_SHADER = new MyFile("skybox/atmosphere", "atmosphereFragment.txt");

	protected UniformMatrix projectionViewMatrix = new UniformMatrix("projectionViewMatrix");
	protected UniformMatrix modelMatrix = new UniformMatrix("modelMatrix");
	protected UniformVec3 sunPosition = new UniformVec3("sunPosition");
	protected UniformFloat atmosphereThickness = new UniformFloat("atmosphereThickness");
	protected UniformVec3 skyColorDay = new UniformVec3("skyColorDay");
	protected UniformVec3 skyColorSunset = new UniformVec3("skyColorSunset");
	protected UniformVec3 spaceColor = new UniformVec3("spaceColor");
	protected UniformVec3 cameraPosition = new UniformVec3("cameraPosition");
	protected UniformFloat isSun = new UniformFloat("isSun");
	protected UniformVec3 planetCenter = new UniformVec3("planetCenter");
	protected UniformFloat time = new UniformFloat("time");
	
	public AtmosphereShader() {
		super(VERTEX_SHADER, FRAGMENT_SHADER, "in_position", "modelMatrixCol0", "modelMatrixCol1", "modelMatrixCol2", "modelMatrixCol3", "cloudProperties");
		super.storeAllUniformLocations(projectionViewMatrix, modelMatrix, sunPosition, atmosphereThickness, skyColorDay, skyColorSunset, spaceColor, cameraPosition, isSun, planetCenter, time);
	}

}


