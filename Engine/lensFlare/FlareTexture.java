package lensFlare;

import org.lwjgl.util.vector.Vector2f;

import textures.Texture;

/**
 * Mercek parlaması efektindeki tek bir dokuyu (flare) temsil eder.
 * Dokunun kendisi, ölçeği (büyüklüğü) ve ekrandaki konumu bilgilerini tutar.
 */
public class FlareTexture {
	
	// Parlama için kullanılacak doku (resim)
	private final Texture texture;
	// Parlamanın ekrandaki büyüklüğü/ölçeği
	private final float scale;
	
	// Parlamanın ekran uzayındaki merkezi konumu (merkez (0,0) olacak şekilde)
	private Vector2f screenPos = new Vector2f();

	/**
	 * Yeni bir FlareTexture oluşturur.
	 * 
	 * @param texture Parlama için kullanılacak kaplama (texture)
	 * @param scale Parlamanın ekrandaki boyutu
	 */
	public FlareTexture(Texture texture, float scale){
		this.texture = texture;
		this.scale = scale;
	}
	
	/**
	 * Parlamanın ekrandaki konumunu günceller.
	 * 
	 * @param newPos Ekran uzayındaki yeni konum
	 */
	public void setScreenPos(Vector2f newPos){
		this.screenPos.set(newPos);
	}
	
	/**
	 * @return Parlama dokusunu döndürür
	 */
	public Texture getTexture() {
		return texture;
	}

	/**
	 * @return Parlamanın ölçeğini döndürür
	 */
	public float getScale() {
		return scale;
	}

	/**
	 * @return Parlamanın ekrandaki konumunu döndürür
	 */
	public Vector2f getScreenPos() {
		return screenPos;
	}
	
}
