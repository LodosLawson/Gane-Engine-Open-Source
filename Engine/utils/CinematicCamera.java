package utils;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import terrain.ITerrain;

/**
 * Oyun içi sinematik geçişler, cutscene (ara sahne) animasyonları veya sabit rotalı 
 * kamera hareketleri oluşturmak için kullanılan kamera sınıfı.
 * ICamera arayüzünü uygular ve zamana bağlı olarak (elapsedTime) belirli senaryo aşamalarından geçer.
 * 
 * <p><b>Kullanım Örneği:</b></p>
 * <pre>
 * {@code
 * // 70 FOV açısıyla yeni bir sinematik kamera oluştur
 * CinematicCamera cineCam = new CinematicCamera(70f, 0.1f, 10000f, terrain, sun.getDirection());
 * 
 * // Her karede zamanı (delta) vererek güncelleyin
 * cineCam.update(DisplayManager.getFrameTime());
 * 
 * // Render motoruna bu kamerayı verin
 * renderer.render(entities, cineCam, light);
 * }
 * </pre>
 */
public class CinematicCamera implements ICamera {

	/** Kameranın dünyadaki mevcut konumu. Varsayılan olarak yüksekte başlar. */
	private Vector3f position = new Vector3f(0, 30, 0);
	/** Dikey bakış açısı (Aşağı/Yukarı) */
	private float pitch = 5f;
	/** Yatay bakış açısı (Sağa/Sola) */
	private float yaw = 0f;
	/** Yuvarlanma açısı (Şu an kullanılmıyor) */
	private float roll = 0f;

	/** İleri doğru hareket etme hızı */
	private float speed = 35f;
	/** Kameranın var olduğu andan itibaren geçen toplam süre (saniye) */
	private float elapsedTime = 0f;

	/** Dünyayı 2D ekrana yansıtma matrisi */
	private Matrix4f projectionMatrix;
	/** Yerin altına girmemek için yükseklik referansı alınan arazi */
	private ITerrain terrain;
	/** Kameranın Güneş'e dönmesi (veya takip etmesi) için ışık yönü */
	private Vector3f lightDir;

	/** Kameranın View (Görünüm) matrisi. Her karede yeniden hesaplanır. */
	private Matrix4f viewMatrix = new Matrix4f();

	/**
	 * Yeni bir sinematik kamera oluşturur ve başlangıç projeksiyon matrisini hesaplar.
	 * 
	 * @param fov     Görüş açısı (Field of View). Örn: 70
	 * @param near    Kameranın görebileceği en yakın mesafe (Near Plane). Örn: 0.1f
	 * @param far     Kameranın görebileceği en uzak mesafe (Far Plane). Örn: 10000f
	 * @param terrain Yerin altına inilmesini önlemek için arazi objesi
	 * @param lightDir Güneşe uçuş animasyonu için güneşin yön vektörü
	 */
	public CinematicCamera(float fov, float near, float far, ITerrain terrain, Vector3f lightDir) {
		this.projectionMatrix = createProjectionMatrix(fov, near, far);
		this.terrain = terrain;
		this.lightDir = lightDir;
	}

	/** 
	 * Sabit rota animasyonlu bir kamera olduğu için standart kullanıcı girdisi 
	 * ile manuel hareket iptal edilmiştir.
	 */
	public void move() {
		// Do nothing (Manuel kontrol yok, zaman bazlı çalışıyor)
	}
	
	@Override
	public float getPitch() {
		return pitch;
	}
	
	@Override
	public float getYaw() {
		return yaw;
	}

	/**
	 * Zaman sayacını arttırır ve senaryo aşamalarına (Fazlarına) göre 
	 * kameranın hedeflenen konumu ve açısını hesaplar.
	 * 
	 * @param delta Önceki kare ile şu anki kare arasındaki süre farkı (Saniye)
	 */
	public void update(float delta) {
		elapsedTime += delta;
		float targetHeight = position.y;
		float targetPitch = pitch;
		float currentSpeed = speed;
		
		// Arazi yüksekliğini al. Arazi yoksa 0 varsay.
		float groundHeight = (terrain != null) ? terrain.getHeightAt(position.x, position.z) : 0f;

		if (elapsedTime < 10.0f) {
			// AŞAMA 1: Su Altı (0-10 Saniye)
			targetHeight = Math.max(groundHeight + 3.0f, -8.0f);
			targetPitch = 0f; 
		} 
		else if (elapsedTime < 25.0f) {
			// AŞAMA 2: Arazi Üstü (10-25 Saniye)
			targetHeight = groundHeight + 60f;
			targetPitch = 8f + (float)Math.sin(elapsedTime * 1.5f) * 4f; 
		} 
		else if (elapsedTime < 40.0f) {
			// AŞAMA 3: Bulutlara Tırmanış (25-40 Saniye)
			float progress = (elapsedTime - 25.0f) / 15.0f; // 0.0 to 1.0
			targetHeight = groundHeight + 60f + (progress * 1500f);
			targetPitch = -15f; // Yukarı bak
			currentSpeed = speed * (1.0f + progress * 3f); // Hızlan
		} 
		else if (elapsedTime < 60.0f) {
			// AŞAMA 4: Uzaya Çıkış ve Güneşe Uçuş (40-60 Saniye)
			float progress = Math.min((elapsedTime - 40.0f) / 15.0f, 1.0f);
			targetHeight = 1600f + (progress) * 8000f; // Uzaya fırlayış
			
			if (lightDir != null) {
				Vector3f sunDir = new Vector3f(-lightDir.x, -lightDir.y, -lightDir.z);
				sunDir.normalise();
				float desiredYaw = (float) Math.toDegrees(Math.atan2(sunDir.x, sunDir.z));
				float desiredPitch = (float) Math.toDegrees(Math.asin(-sunDir.y)); 
				
				float yawDiff = desiredYaw - yaw;
				while (yawDiff < -180) yawDiff += 360;
				while (yawDiff > 180) yawDiff -= 360;
				yaw += yawDiff * 1.5f * delta;
				targetPitch = desiredPitch;
			}
			currentSpeed = speed * 12f; 
		}
		else if (elapsedTime < 80.0f) {
			// AŞAMA 5: Yörünge ve 3D Yazı Gecişi (60-80 Saniye)
			targetHeight = 9600f; // Uzayda sabit yükseklik
			currentSpeed = speed * 15f;
			// Güneşin etrafında yay çiz (kamera yaw açısını yavaşça döndürerek orbit yap)
			yaw += 18.0f * delta; 
			targetPitch = 0f;
		}
		else if (elapsedTime < 95.0f) {
			// AŞAMA 6: Dünyaya Eve Dönüş (80-95 Saniye)
			targetHeight = groundHeight + 50f; // Yeryüzüne dalış
			targetPitch = 85f; // Dimdik aşağı (Meteor gibi)
			currentSpeed = speed * 40f; // Çok hızlı düşüş
		}
		else {
			// AŞAMA 7: Kararma ve Final Ekranı (95+ Saniye)
			targetHeight = groundHeight + 50f;
			targetPitch = 0f;
			currentSpeed = 0f;
		}

		// İleriye doğru hareket (X ve Z ekseninde yön tayini)
		float dx = (float) (currentSpeed * delta * Math.sin(Math.toRadians(yaw)));
		float dz = (float) -(currentSpeed * delta * Math.cos(Math.toRadians(yaw)));

		position.x += dx;
		position.z += dz;
		
		// Eğer serbest uçuş dışındaysa yavaşça etrafa bakın
		if (elapsedTime < 40.0f) {
			yaw += 2.0f * delta; 
		}

		// Yumuşak yükseklik ve pitch geçişi (Interpolasyon - Smooth transition)
		position.y += (targetHeight - position.y) * 2.5f * delta;
		pitch += (targetPitch - pitch) * 1.5f * delta;
		
		// Yerin altına girmeyi KESİNLİKLE engelle (Hard Clamp - Hata önleme)
		if (position.y < groundHeight + 2.0f) {
			position.y = groundHeight + 2.0f;
		}

		updateViewMatrix();
	}
	
	/**
	 * @return Sinematik videonun başlangıcından itibaren geçen toplam süre
	 */
	public float getElapsedTime() {
		return elapsedTime;
	}

	/**
	 * Kameranın 3D konum ve dönme açılarına göre görünüm matrisini (View Matrix) yeniden hesaplar.
	 */
	private void updateViewMatrix() {
		viewMatrix.setIdentity();
		Matrix4f.rotate((float) Math.toRadians(pitch), new Vector3f(1, 0, 0), viewMatrix, viewMatrix);
		Matrix4f.rotate((float) Math.toRadians(yaw), new Vector3f(0, 1, 0), viewMatrix, viewMatrix);
		Vector3f negativeCameraPos = new Vector3f(-position.x, -position.y, -position.z);
		Matrix4f.translate(negativeCameraPos, viewMatrix, viewMatrix);
	}

	/**
	 * Kameranın bakış açısı parametrelerine göre projeksiyon matrisi oluşturur.
	 */
	private Matrix4f createProjectionMatrix(float fov, float near, float far) {
		Matrix4f matrix = new Matrix4f();
		float width = (float) org.lwjgl.opengl.Display.getWidth();
		float height = (float) org.lwjgl.opengl.Display.getHeight();
		
		// Sıfıra bölme hatasını (Divide by Zero) engellemek için kontrol
		if(height <= 0) {
			height = 1f; 
		}
		
		float aspectRatio = width / height;
		float y_scale = (float) ((1f / Math.tan(Math.toRadians(fov / 2f))));
		float x_scale = y_scale / aspectRatio;
		float frustum_length = far - near;

		matrix.m00 = x_scale;
		matrix.m11 = y_scale;
		matrix.m22 = -((far + near) / frustum_length);
		matrix.m23 = -1f;
		matrix.m32 = -((2 * near * far) / frustum_length);
		matrix.m33 = 0f;
		return matrix;
	}

	@Override
	public Vector3f getPosition() {
		return position;
	}

	@Override
	public Matrix4f getViewMatrix() {
		return viewMatrix;
	}

	@Override
	public void reflect(float height) {
		// Yüksekliğe göre dikey eksende aynala (Özellikle su altı renderlarında)
		position.y -= 2 * (position.y - height);
		pitch = -pitch;
	}

	@Override
	public Matrix4f getProjectionMatrix() {
		return projectionMatrix;
	}

	@Override
	public Matrix4f getProjectionViewMatrix() {
		return Matrix4f.mul(projectionMatrix, getViewMatrix(), null);
	}
}
