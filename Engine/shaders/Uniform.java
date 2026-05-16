package shaders;

import org.lwjgl.opengl.GL20;

/**
 * Shader içindeki 'uniform' değişkenleri temsil eden soyut (abstract) temel sınıf.
 * Bütün uniform türleri (float, vector, matrix vb.) bu sınıftan türer.
 */
public abstract class Uniform {
	
	// Uniform değişken shader içinde bulunamazsa atanacak varsayılan hata değeri
	private static final int NOT_FOUND = -1;
	
	// Shader kodundaki değişkenin adı (Örn: "transformationMatrix")
	private String name;
	// OpenGL GPU belleğinde bu değişkenin bulunduğu adres/konum kimliği
	private int location;
	
	/**
	 * Yeni bir uniform temsilci nesnesi oluşturur.
	 * 
	 * @param name Shader kodundaki uniform değişkenin ismi
	 */
	protected Uniform(String name){
		this.name = name;
	}
	
	/**
	 * Shader programı derlendikten sonra bu uniform değişkenin
	 * ekran kartındaki adresini bulur ve hafızaya alır.
	 * 
	 * @param programID Değişkenin aranacağı Shader Programının kimliği
	 */
	protected void storeUniformLocation(int programID){
		location = GL20.glGetUniformLocation(programID, name);
		if(location == NOT_FOUND){
			System.err.println("No uniform variable called " + name + " found!");
		}
	}
	
	/** @return GPU belleğindeki uniform adresi (location) */
	protected int getLocation(){
		return location;
	}

}
