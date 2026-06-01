package gane.objects;

import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector3f;

import extra.Camera;
import scene.GameObject;
import scene.Model;
import scene.Skin;

public class Player extends GameObject {

	private static final float RUN_SPEED = 20f;
	private static final float TURN_SPEED = 160f;
	private static final float JUMP_POWER = 30f;
	
	private float currentSpeed = 0;
	private float currentStrafeSpeed = 0;
	private float currentTurnSpeed = 0;
	private float upwardSpeed = 0;
	
	private Camera camera;
	private scene.Scene scene;

	public Player(Camera camera) {
		super((Model)null, (Skin)null);
		this.camera = camera;
		this.setBoundingBox(new physics.AABB(new org.lwjgl.util.vector.Vector3f(-1, 0, -1), new org.lwjgl.util.vector.Vector3f(1, 4, 1)));
	}

	/**
	 * GÃ¶rÃ¼nÃ¼r bir oyuncu nesnesi oluÅŸturur. 
	 * @param camera Oyuncuyu yÃ¶netecek kamera
	 * @param objFilePath Oyuncunun 3D model dosyasÄ± (.obj)
	 * @param colorFilePath Oyuncunun kaplama dosyasÄ± (.png vb.)
	 */
	public Player(Camera camera, String objFilePath, String colorFilePath) {
		super(objFilePath, colorFilePath); 
		this.camera = camera;
	}

	public void setScene(scene.Scene scene) {
		this.scene = scene;
	}

	@Override
	protected void onUpdate(float delta) {
		// --- FÄ°ZÄ°K VE SU ETKÄ°LEÅžÄ°MÄ° Ä°Ã‡Ä°N YÃœKSEKLÄ°KLERÄ° BUL ---
		float waterHeight = -2.0f; // VarsayÄ±lan su seviyesi
		float groundHeight = -1000.0f;
		
		if (scene != null) {
			if (!scene.getWater().isEmpty()) {
				water.tile.WaterTile tile = scene.getWater().get(0);
				waterHeight = tile.getWaterHeightAt(super.getPosition().x, super.getPosition().z);
			}
			for (terrain.ITerrain t : scene.getTerrains()) {
				if (t instanceof terrain.FlatTerrain) {
					groundHeight = ((terrain.FlatTerrain) t).getHeightAt(super.getPosition().x, super.getPosition().z);
					break;
				}
			}
		}

		boolean inWater = super.getPosition().y < waterHeight;
		
		checkInputs(inWater); // inWater bilgisini gÃ¶nder

		// DÃ¶nÃ¼ÅŸ (Yaw)
		super.getRotation().y += currentTurnSpeed * delta;
		
		float distance = currentSpeed * delta;
		float strafeDistance = currentStrafeSpeed * delta;
		
		float dx = 0;
		float dz = 0;
		float dy = 0; // YÃ¼zme iÃ§in Y ekseni hareketi

		if (camera != null && (camera.getMode() == Camera.CameraMode.FIRST_PERSON || camera.getMode() == Camera.CameraMode.RPG_THIRD_PERSON)) {
			float camYaw = camera.getYaw();
			float camPitch = camera.getPitch();
			
			// Ä°leri / Geri (W/S)
			dx += distance * Math.sin(Math.toRadians(camYaw));
			dz -= distance * Math.cos(Math.toRadians(camYaw));
			
			// YÃ¼zme esnasÄ±nda kameranÄ±n baktÄ±ÄŸÄ± yÃ¶ne doÄŸru (yukarÄ±/aÅŸaÄŸÄ±) yÃ¼zme
			if (inWater) {
				dy -= distance * Math.sin(Math.toRadians(camPitch));
			}
			
			// SaÄŸ / Sol Strafe (D/A)
			dx += strafeDistance * Math.cos(Math.toRadians(camYaw));
			dz += strafeDistance * Math.sin(Math.toRadians(camYaw));
			
			super.getRotation().y = 360 - camYaw;
		} else {
			// Normal mod
			dx += distance * Math.sin(Math.toRadians(super.getRotation().y));
			dz += distance * Math.cos(Math.toRadians(super.getRotation().y));
			
			dx += strafeDistance * Math.sin(Math.toRadians(super.getRotation().y + 90));
			dz += strafeDistance * Math.cos(Math.toRadians(super.getRotation().y + 90));
		}
		
		// X Axis
		super.getPosition().x += dx;
		if (checkCollision()) {
			super.getPosition().x -= dx;
		}

		// Z Axis
		super.getPosition().z += dz;
		if (checkCollision()) {
			super.getPosition().z -= dz;
		}

		// Y Axis
		super.getPosition().y += dy;
		if (checkCollision()) {
			super.getPosition().y -= dy;
		}
		
		if (inWater && super.getPosition().y > groundHeight) {
			// SUYUN Ä°Ã‡Ä°NDE YÃœZME (NÃ¶tr kaldÄ±rma kuvveti)
			upwardSpeed += 2.0f * delta; // Ã‡ok yavaÅŸÃ§a yukarÄ± doÄŸru Ã§Ä±kar
			
			// Suyun iÃ§indeyken yavaÅŸlama (SÃ¼rtÃ¼nme)
			upwardSpeed -= upwardSpeed * 4.0f * delta;
			currentSpeed -= currentSpeed * 2.5f * delta; 
		} else {
			// HAVADA VEYA KARADA (YerÃ§ekimi)
			upwardSpeed += -90f * delta; // YerÃ§ekimi
		}
		
		super.getPosition().y += upwardSpeed * delta;
		
		// Zemin Ã‡arpÄ±ÅŸmasÄ±
		if (super.getPosition().y < groundHeight) {
			upwardSpeed = 0;
			super.getPosition().y = groundHeight;
		}
		
		// Okyanus tabanÄ± kontrolÃ¼ (sonsuza dÃ¼ÅŸmesini engelle)
		if (super.getPosition().y < -1000.0f && groundHeight < -1000.0f) { 
			upwardSpeed = 0;
			super.getPosition().y = -1000.0f;
		}
	}
	
	private boolean checkCollision() {
		if (scene == null) return false;
		
		physics.AABB myAABB = this.getBoundingBox();
		for (scene.Entity e : scene.getAllEntities()) {
			if (e == this) continue; // Kendimizle carpismayalim
			
			if (e.getBoundingBox() != null) {
				if (myAABB.intersects(e.getBoundingBox(), super.getPosition(), e.getPosition())) {
					return true;
				}
			}
		}
		return false;
	}

	private void checkInputs(boolean inWater) {
		if (camera == null) return;
		
		Camera.CameraMode mode = camera.getMode();
		
		if (mode == Camera.CameraMode.FIRST_PERSON || mode == Camera.CameraMode.RPG_THIRD_PERSON) {
			
			float currentRunSpeed = inWater ? RUN_SPEED * 0.6f : RUN_SPEED; // Suda daha yavaÅŸ
			
			if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
				currentSpeed = currentRunSpeed;
			} else if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
				currentSpeed = -currentRunSpeed;
			} else {
				currentSpeed = 0;
			}

			if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
				currentStrafeSpeed = -currentRunSpeed;
				currentTurnSpeed = 0;
			} else if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
				currentStrafeSpeed = currentRunSpeed;
				currentTurnSpeed = 0;
			} else {
				currentTurnSpeed = 0;
				currentStrafeSpeed = 0;
			}

			if (inWater) {
				if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
					upwardSpeed = currentRunSpeed; // YÃ¼zerek yukarÄ± Ã§Ä±k
				} else if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
					upwardSpeed = -currentRunSpeed; // YÃ¼zerek aÅŸaÄŸÄ± in
				}
			} else {
				if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
					jump();
				}
			}
			
		} else if (mode == Camera.CameraMode.ISOMETRIC) {
			
			currentSpeed = 0;
			currentTurnSpeed = 0;
			
			float dx = 0, dz = 0;
			if (Keyboard.isKeyDown(Keyboard.KEY_W)) { dz = -1; }
			if (Keyboard.isKeyDown(Keyboard.KEY_S)) { dz = 1; }
			if (Keyboard.isKeyDown(Keyboard.KEY_A)) { dx = -1; }
			if (Keyboard.isKeyDown(Keyboard.KEY_D)) { dx = 1; }
			
			if (dx != 0 || dz != 0) {
				float cameraAngle = (float) Math.toRadians(camera.getYaw());
				float moveX = (float) (dx * Math.cos(cameraAngle) - dz * Math.sin(cameraAngle));
				float moveZ = (float) (dx * Math.sin(cameraAngle) + dz * Math.cos(cameraAngle));
				
				Vector3f dir = new Vector3f(moveX, 0, moveZ);
				if (dir.lengthSquared() != 0) {
					dir.normalise();
				}
				
				super.getPosition().x += dir.x * RUN_SPEED * 0.016f;
				super.getPosition().z += dir.z * RUN_SPEED * 0.016f;
				
				float targetRotation = (float) Math.toDegrees(Math.atan2(dir.x, dir.z));
				super.getRotation().y = targetRotation;
			}
			
			if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
				jump();
			}
		}
	}

	private void jump() {
		float waterHeight = -2.0f;
		float groundHeight = -1000.0f;
		if (scene != null) {
			if (!scene.getWater().isEmpty()) {
				waterHeight = scene.getWater().get(0).getWaterHeightAt(super.getPosition().x, super.getPosition().z);
			}
			for (terrain.ITerrain t : scene.getTerrains()) {
				if (t instanceof terrain.FlatTerrain) {
					groundHeight = ((terrain.FlatTerrain) t).getHeightAt(super.getPosition().x, super.getPosition().z);
					break;
				}
			}
		}
		
		// Su seviyesinde veya zeminde zÄ±plamaya izin ver
		if (super.getPosition().y <= waterHeight + 0.5f || super.getPosition().y <= groundHeight + 0.5f) { 
			upwardSpeed = JUMP_POWER;
		}
	}
}

