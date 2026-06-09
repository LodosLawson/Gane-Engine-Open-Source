package textures;

import java.nio.ByteBuffer;

/**
 * Bir resim dosyasından okunan ham (raw) piksel verilerini ve
 * genişlik/yükseklik boyutlarını tutan basit bir veri konteyneri.
 */
public class TextureData {
	
	// Dokunun genişliği
	private int width;
	// Dokunun yüksekliği
	private int height;
	// Piksellerin renk (RGBA vb.) değerlerini barındıran bellek alanı
	private ByteBuffer buffer;
	
	/**
	 * Yeni bir doku verisi nesnesi oluşturur.
	 * 
	 * @param buffer Ham piksel verisi (ByteBuffer formatında)
	 * @param width Genişlik
	 * @param height Yükseklik
	 */
	public TextureData(ByteBuffer buffer, int width, int height){
		this.buffer = buffer;
		this.width = width;
		this.height = height;
	}
	
	/** @return Dokunun genişliği */
	public int getWidth(){
		return width;
	}
	
	/** @return Dokunun yüksekliği */
	public int getHeight(){
		return height;
	}
	
	/** @return Ham piksel verisi */
	public ByteBuffer getBuffer(){
		return buffer;
	}

}
