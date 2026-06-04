package ide;

import java.awt.Canvas;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import gane.AppSettings;
import renderEngine.RenderEngine;
import scene.Scene;
import utils.DisplayManager;
import utils.MousePicker;
import extra.Camera;
import environment.DayNightManager;
import skybox.atmosphere.AtmosphereSky;
import terrain.flat.FlatTerrain;
import water.tile.WaterTile;
import java.util.function.Consumer;

/**
 * Gane Engine IDE'sinin 3D penceresi.
 * LWJGL2'nin Display'ini standart bir Java AWT Canvas icine gomer.
 */
public class ViewportCanvas extends Canvas {
	
	private static final long serialVersionUID = 1L;
	private boolean running = false;
	private Thread engineThread;
	
	private RenderEngine renderEngine;
	private Scene scene;
	private Camera camera;
	private MousePicker picker;
	private Consumer<scene.Entity> onEntitySelected;
	
	public void setOnEntitySelected(Consumer<scene.Entity> callback) {
		this.onEntitySelected = callback;
	}
	
	// Environment references
	private DayNightManager dayNightManager;
	private FlatTerrain terrain;
	private WaterTile water;
	private AtmosphereSky atmosphereSky;
	
	private java.util.Queue<Runnable> glTasks = new java.util.concurrent.ConcurrentLinkedQueue<>();
	
	public void enqueue(Runnable r) {
		glTasks.add(r);
	}
	
	public ViewportCanvas() {
		setFocusable(true);
		
		// Pencere boyutu degistiginde (Resize) OpenGL Viewport'unu da guncelle
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				if (Display.isCreated()) {
					GL11.glViewport(0, 0, getWidth(), getHeight());
				}
			}
		});
	}

	public void startEngine() {
		if (running) return;
		running = true;
		
		engineThread = new Thread(() -> {
			try {
				// LWJGL DisplayManager'a bu canvas'i parent olarak ata
				DisplayManager.setParent(this);
				
				// Sadece Editor icin gecici bir ayar
				AppSettings.setup(getWidth(), getHeight(), false, "Gane IDE", null);
				
				renderEngine = RenderEngine.init();
				
				camera = new Camera();
				camera.setMode(Camera.CameraMode.EDITOR); 
				camera.getPosition().set(0, 5, 20);
				
				scene = new Scene(camera);
				scene.setAmbientLight(0.3f);
				scene.setLightBrightness(1.2f);
				
				// DayNightManager varsayilan olarak baslatilabilir, ama devre disi olabilir
				dayNightManager = new DayNightManager(scene, 12.0f, 10.0f);
				
				picker = new MousePicker(camera, camera.getProjectionMatrix());
				
				long lastTime = System.nanoTime();
				boolean wasLeftMouseDown = false;
				
				// Editor Loop
				while (running && !Display.isCloseRequested()) {
					long currentTime = System.nanoTime();
					float delta = (currentTime - lastTime) / 1000000000.0f;
					lastTime = currentTime;
					
					// Ana thread'den (Swing) gelen OpenGL gorevlerini isle
					while(!glTasks.isEmpty()) {
						glTasks.poll().run();
					}
					
					// Eger farenin odagi (focus) bu Canvas uzerindeyse, kamera hareket edebilir
					if (hasFocus()) {
						camera.move();
						
						picker.update();
						
						boolean isLeftDown = org.lwjgl.input.Mouse.isButtonDown(0);
						if (isLeftDown && !wasLeftMouseDown) {
							// Ekrana tiklandi, Raycast yap
							scene.Entity closestHit = null;
							float closestDist = Float.MAX_VALUE;
							
							for (scene.Entity e : scene.getAllEntities()) {
								if (e instanceof scene.GameObject) {
									// Cok basit bir Bounding Sphere carpisma testi
									float radius = e.getScale() * 5.0f; // Varsayilan bir yariçap
									if (MousePicker.intersects(camera.getPosition(), picker.getCurrentRay(), e.getPosition(), radius)) {
										float dist = org.lwjgl.util.vector.Vector3f.sub(camera.getPosition(), e.getPosition(), null).length();
										if (dist < closestDist) {
											closestDist = dist;
											closestHit = e;
										}
									}
								}
							}
							
							if (closestHit != null && onEntitySelected != null) {
								final scene.Entity hit = closestHit;
								javax.swing.SwingUtilities.invokeLater(() -> onEntitySelected.accept(hit));
							}
						}
						wasLeftMouseDown = isLeftDown;
					}
					
					// Animasyonlu objeleri vs guncelle
					for (scene.Entity e : scene.getAllEntities()) {
						if (e instanceof scene.GameObject) {
							((scene.GameObject)e).update(delta);
						}
					}
					
					// Eger su aciksa animasyonunu calistir
					if (water != null) {
						water.update(delta);
					}
					
					// Day Night guncelle
					if (dayNightManager != null && isDayNightEnabled) {
						dayNightManager.update(delta);
					}
					
					renderEngine.renderScene(scene, delta);
					renderEngine.update();
				}
				
				scene.delete();
				renderEngine.close();
				System.exit(0);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, "GaneEditor-EngineThread");
		
		engineThread.start();
	}
	
	public void stopEngine() {
		running = false;
	}
	
	public Scene getScene() {
		return scene;
	}
	
	public Camera getCamera() {
		return camera;
	}
	
	private boolean isDayNightEnabled = false;
	
	public void setDayNightEnabled(boolean enabled) {
		this.isDayNightEnabled = enabled;
		if (!enabled && scene != null) {
			// Sifirla
			scene.setLightBrightness(1.0f);
			scene.setAmbientLight(0.4f);
		}
	}
	
	public DayNightManager getDayNightManager() {
		return dayNightManager;
	}
	
	public void setTerrainEnabled(boolean enabled) {
		enqueue(() -> {
			if (enabled) {
				if (terrain == null) {
					terrain = new FlatTerrain(2000, 2000);
					terrain.generateProceduralTerrainV2(60f, 0.4f, 4, 300f, 12345L);
				}
				if (!scene.getTerrains().contains(terrain)) {
					scene.addTerrain(terrain);
				}
			} else {
				if (terrain != null) {
					scene.getTerrains().remove(terrain);
				}
			}
		});
	}
	
	public FlatTerrain getTerrain() { return terrain; }
	public void setTerrain(FlatTerrain t) { this.terrain = t; }
	
	public void setWaterEnabled(boolean enabled) {
		enqueue(() -> {
			if (enabled) {
				if (water == null) {
					water = new WaterTile(0, 0, -2.0f, 2000f);
				}
				if (!scene.getWater().contains(water)) {
					scene.addWater(water);
				}
			} else {
				if (water != null) {
					scene.getWater().remove(water);
				}
			}
		});
	}
	
	public WaterTile getWater() { return water; }
	public void setWater(WaterTile w) { this.water = w; }
	
	public void setAtmosphereEnabled(boolean enabled) {
		enqueue(() -> {
			if (enabled) {
				if (atmosphereSky == null) {
					atmosphereSky = new AtmosphereSky();
				}
				scene.setSky(atmosphereSky);
			} else {
				scene.setSky(null);
			}
		});
	}
	
	public AtmosphereSky getAtmosphereSky() { return atmosphereSky; }
	public void setAtmosphereSky(AtmosphereSky sky) { this.atmosphereSky = sky; }
}
