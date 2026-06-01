package utils;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import terrain.ITerrain;


public class CinematicCamera implements ICamera {

	private Vector3f position = new Vector3f(0, 30, 0);
	private float pitch = 5f;
	private float yaw = 0f;
	private float roll = 0f;

	private float speed = 35f;
	private float elapsedTime = 0f;

	private Matrix4f projectionMatrix;
	private ITerrain terrain;
	private Vector3f lightDir;

	private Matrix4f viewMatrix = new Matrix4f();

	public CinematicCamera(float fov, float near, float far, ITerrain terrain, Vector3f lightDir) {
		this.projectionMatrix = createProjectionMatrix(fov, near, far);
		this.terrain = terrain;
		this.lightDir = lightDir;
	}

	public void move() {
		// Do nothing
	}
	
	@Override
	public float getPitch() {
		return pitch;
	}
	
	@Override
	public float getYaw() {
		return yaw;
	}

	public void update(float delta) {
		elapsedTime += delta;
		float targetHeight = position.y;
		float targetPitch = pitch;
		float currentSpeed = speed;
		
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
			targetPitch = -15f; // Yukari bak
			currentSpeed = speed * (1.0f + progress * 3f); // Hizlan
		} 
		else if (elapsedTime < 60.0f) {
			// AŞAMA 4: Uzaya Çıkış ve Güneşe Uçuş (40-60 Saniye)
			float progress = Math.min((elapsedTime - 40.0f) / 15.0f, 1.0f);
			targetHeight = 1600f + (progress) * 8000f; // Uzaya firlayis
			
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
			targetHeight = 9600f; // Uzayda sabit yukseklik
			currentSpeed = speed * 15f;
			// Gunesin etrafinda yay ciz (kamera yaw acisini yavasca dondurerek orbit yap)
			yaw += 18.0f * delta; 
			targetPitch = 0f;
		}
		else if (elapsedTime < 95.0f) {
			// AŞAMA 6: Dünyaya Eve Dönüş (80-95 Saniye)
			targetHeight = groundHeight + 50f; // Yeryuzune dalis
			targetPitch = 85f; // Dimdik asagi (Meteor gibi)
			currentSpeed = speed * 40f; // Cok hizli dusus
		}
		else {
			// AŞAMA 7: Kararma ve Final Ekranı (95+ Saniye)
			targetHeight = groundHeight + 50f;
			targetPitch = 0f;
			currentSpeed = 0f;
		}

		// İleriye dogru hareket (X ve Z ekseninde)
		float dx = (float) (currentSpeed * delta * Math.sin(Math.toRadians(yaw)));
		float dz = (float) -(currentSpeed * delta * Math.cos(Math.toRadians(yaw)));

		position.x += dx;
		position.z += dz;
		
		// Eğer serbest uçuş dışındaysa yavaşça etrafa bakın
		if (elapsedTime < 40.0f) {
			yaw += 2.0f * delta; 
		}

		// Yumusak yukseklik ve pitch gecisi (interpolation)
		position.y += (targetHeight - position.y) * 2.5f * delta;
		pitch += (targetPitch - pitch) * 1.5f * delta;
		
		// Yerin altına girmeyi KESINLIKLE engelle (Hard Clamp)
		if (position.y < groundHeight + 2.0f) {
			position.y = groundHeight + 2.0f;
		}

		updateViewMatrix();
	}
	
	public float getElapsedTime() {
		return elapsedTime;
	}

	private void updateViewMatrix() {
		viewMatrix.setIdentity();
		Matrix4f.rotate((float) Math.toRadians(pitch), new Vector3f(1, 0, 0), viewMatrix, viewMatrix);
		Matrix4f.rotate((float) Math.toRadians(yaw), new Vector3f(0, 1, 0), viewMatrix, viewMatrix);
		Vector3f negativeCameraPos = new Vector3f(-position.x, -position.y, -position.z);
		Matrix4f.translate(negativeCameraPos, viewMatrix, viewMatrix);
	}

	private Matrix4f createProjectionMatrix(float fov, float near, float far) {
		Matrix4f matrix = new Matrix4f();
		float aspectRatio = (float) org.lwjgl.opengl.Display.getWidth() / (float) org.lwjgl.opengl.Display.getHeight();
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
