package shaders;

import java.io.BufferedReader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL43;
import utils.MyFile;

public class ComputeShader {

	private int programID;

	public ComputeShader(MyFile computeFile) {
		int computeShaderID = loadShader(computeFile, GL43.GL_COMPUTE_SHADER);
		programID = GL20.glCreateProgram();
		GL20.glAttachShader(programID, computeShaderID);
		GL20.glLinkProgram(programID);
		GL20.glDetachShader(programID, computeShaderID);
		GL20.glDeleteShader(computeShaderID);
	}
	
	protected void storeAllUniformLocations(Uniform... uniforms){
		for(Uniform uniform : uniforms){
			uniform.storeUniformLocation(programID);
		}
		GL20.glValidateProgram(programID);
	}

	public void start() {
		GL20.glUseProgram(programID);
	}

	public void stop() {
		GL20.glUseProgram(0);
	}

	public int getProgramID() {
		return programID;
	}

	public void cleanUp() {
		stop();
		GL20.glDeleteProgram(programID);
	}
	
	public void bindImageTexture(int unit, int textureID, int level, boolean layered, int layer, int access, int format) {
		org.lwjgl.opengl.GL42.glBindImageTexture(unit, textureID, level, layered, layer, access, format);
	}
	
	public void dispatch(int numGroupsX, int numGroupsY, int numGroupsZ) {
		GL43.glDispatchCompute(numGroupsX, numGroupsY, numGroupsZ);
	}
	
	private int loadShader(MyFile file, int type) {
		StringBuilder shaderSource = new StringBuilder();
		try {
			BufferedReader reader = file.getReader();
			String line;
			while ((line = reader.readLine()) != null) {
				shaderSource.append(line).append("\n");
			}
			reader.close();
		} catch (Exception e) {
			System.err.println("Could not read file " + file.getPath());
			e.printStackTrace();
			System.exit(-1);
		}
		
		// Add hardcoded defines for Ocean FFT since we aren't dynamically injecting them yet
		String source = shaderSource.toString();
		if (source.contains("DISP_MAP_SIZE")) {
			String defines = "#version 430\n" +
			                 "#define DISP_MAP_SIZE 256\n" +
			                 "#define LOG2_DISP_MAP_SIZE 8\n" +
			                 "#define TILE_SIZE_X2 0.15625\n" +
			                 "#define INV_TILE_SIZE 12.8\n";
			source = source.replaceFirst("#version 430", defines);
		}
		
		int shaderID = GL20.glCreateShader(type);
		GL20.glShaderSource(shaderID, source);
		GL20.glCompileShader(shaderID);
		if (GL20.glGetShaderi(shaderID, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
			System.out.println(GL20.glGetShaderInfoLog(shaderID, 500));
			System.err.println("Could not compile shader "+ file.getPath());
			System.exit(-1);
		}
		return shaderID;
	}
}
