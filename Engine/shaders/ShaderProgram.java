package shaders;

import java.io.BufferedReader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import utils.MyFile;

/**
 * OpenGL Shader Programlarının (Gölgelendirici Programları) genel işleyişini yöneten temel sınıf.
 * Bir Vertex Shader ve bir Fragment Shader'ı derleyip bağlayarak (link) bir program oluşturur.
 * Alt sınıflar bu sınıfı genişleterek kendilerine özgü shader'ları kullanabilirler.
 */
public class ShaderProgram {

	// OpenGL tarafında oluşturulan Shader Programının kimliği (ID)
	private int programID;

	/**
	 * Yeni bir Shader Programı yükler, derler ve oluşturur.
	 * 
	 * @param vertexFile Vertex Shader kodunu içeren dosya
	 * @param fragmentFile Fragment Shader kodunu içeren dosya
	 * @param inVariables Vertex Shader'a aktarılacak olan 'in' (attribute) değişkenlerinin adları
	 */
	public ShaderProgram(MyFile vertexFile, MyFile fragmentFile, String... inVariables) {
		int vertexShaderID = loadShader(vertexFile, GL20.GL_VERTEX_SHADER);
		int fragmentShaderID = loadShader(fragmentFile, GL20.GL_FRAGMENT_SHADER);
		programID = GL20.glCreateProgram();
		GL20.glAttachShader(programID, vertexShaderID);
		GL20.glAttachShader(programID, fragmentShaderID);
		bindAttributes(inVariables);
		GL20.glLinkProgram(programID);
		GL20.glDetachShader(programID, vertexShaderID);
		GL20.glDetachShader(programID, fragmentShaderID);
		GL20.glDeleteShader(vertexShaderID);
		GL20.glDeleteShader(fragmentShaderID);
	}
	
	/**
	 * Programa ait olan tüm 'uniform' değişkenlerin GPU belleğindeki konumlarını bulur ve kaydeder.
	 * Bu işlem shader programı bağlandıktan sonra yapılmalıdır.
	 * 
	 * @param uniforms Konumları bulunacak uniform değişken nesneleri
	 */
	protected void storeAllUniformLocations(Uniform... uniforms){
		for(Uniform uniform : uniforms){
			uniform.storeUniformLocation(programID);
		}
		GL20.glValidateProgram(programID);
	}

	/** Bu shader programını aktif hale getirir (Çizim yapmadan önce çağrılmalıdır). */
	public void start() {
		GL20.glUseProgram(programID);
	}

	/** Bu shader programının kullanımını durdurur. */
	public void stop() {
		GL20.glUseProgram(0);
	}

	/** Shader programını OpenGL belleğinden tamamen siler. */
	public void cleanUp() {
		stop();
		GL20.glDeleteProgram(programID);
	}

	/**
	 * Vertex Shader içindeki 'in' değişkenleri (örneğin position, textureCoords, normal)
	 * ile VAO içindeki attribute dizinlerini (0, 1, 2) birbirine bağlar.
	 * 
	 * @param inVariables Değişken adları dizisi
	 */
	private void bindAttributes(String[] inVariables){
		for(int i=0;i<inVariables.length;i++){
			GL20.glBindAttribLocation(programID, i, inVariables[i]);
		}
	}
	
	/**
	 * Verilen bir dosyadan shader kodunu okur ve derler.
	 * 
	 * @param file Okunacak shader dosyası
	 * @param type Shader'ın türü (Örn: GL_VERTEX_SHADER, GL_FRAGMENT_SHADER)
	 * @return Derlenmiş shader'ın OpenGL kimliği (ID)
	 */
	private int loadShader(MyFile file, int type) {
		StringBuilder shaderSource = new StringBuilder();
		try {
			BufferedReader reader = file.getReader();
			String line;
			while ((line = reader.readLine()) != null) {
				shaderSource.append(line).append("//\n");
			}
			reader.close();
		} catch (Exception e) {
			System.err.println("Could not read file.");
			e.printStackTrace();
			System.exit(-1);
		}
		int shaderID = GL20.glCreateShader(type);
		GL20.glShaderSource(shaderID, shaderSource);
		GL20.glCompileShader(shaderID);
		if (GL20.glGetShaderi(shaderID, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
			System.out.println(GL20.glGetShaderInfoLog(shaderID, 500));
			System.err.println("Could not compile shader "+ file);
			System.exit(-1);
		}
		return shaderID;
	}


}
