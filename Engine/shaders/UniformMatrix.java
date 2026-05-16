package shaders;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix4f;

/**
 * Shader kodundaki bir 4x4 Matris (Matrix4f) uniform değişkeni temsil eder.
 * Matrisleri shader'a yüklemek için java.nio.FloatBuffer kullanır.
 */
public class UniformMatrix extends Uniform{
	
	// Matris verilerini Java'dan C/C++(OpenGL) tarafına geçirmek için
	// global olarak paylaşılan tek bir Buffer kullanılır (Bellek optimizasyonu).
	private static FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

	/**
	 * Yeni bir Matrix4f uniform temsilcisi oluşturur.
	 * 
	 * @param name Shader içindeki matris uniform adı (Örn: "projectionMatrix")
	 */
	public UniformMatrix(String name) {
		super(name);
	}
	
	/**
	 * Verilen 4x4 matrisi FloatBuffer'a yazar ve ardından GPU'daki shader değişkenine yükler.
	 * Matrislerin boyutu büyük olduğu için her seferinde eşitlik kontrolü (caching) yapılmaz.
	 * 
	 * @param matrix Shader'a yüklenecek olan yeni matris nesnesi
	 */
	public void loadMatrix(Matrix4f matrix){
		matrix.store(matrixBuffer);
		matrixBuffer.flip();
		GL20.glUniformMatrix4(super.getLocation(), false, matrixBuffer);
	}
	
	

}
