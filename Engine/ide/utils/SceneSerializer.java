package ide.utils;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONArray;
import org.json.JSONObject;

import scene.Entity;
import scene.GameObject;
import scene.Scene;
import ide.ui.EnvironmentPanel;
import ide.ViewportCanvas;
import environment.DayNightManager;
import skybox.atmosphere.AtmosphereSky;
import terrain.flat.FlatTerrain;
import water.tile.WaterTile;

/**
 * Sahneyi (Levels) disari aktarmak ve iceri yuklemek icin JSON serilestiricisi.
 */
public class SceneSerializer {

	public static void saveScene(Scene scene, String filepath, ViewportCanvas viewport) {
		JSONObject root = new JSONObject();
		JSONArray entitiesArray = new JSONArray();
		
		for (Entity e : scene.getAllEntities()) {
			// Yalnizca GameObject'leri (ve originalFilePath'i olanlari) kaydediyoruz.
			if (e instanceof GameObject) {
				GameObject go = (GameObject) e;
				if (go.getOriginalFilePath() != null) {
					JSONObject objJson = new JSONObject();
					objJson.put("type", "GameObject");
					objJson.put("path", go.getOriginalFilePath());
					
					JSONObject pos = new JSONObject();
					pos.put("x", go.getPosition().x);
					pos.put("y", go.getPosition().y);
					pos.put("z", go.getPosition().z);
					objJson.put("position", pos);
					
					JSONObject rot = new JSONObject();
					rot.put("x", go.getRotation().x);
					rot.put("y", go.getRotation().y);
					rot.put("z", go.getRotation().z);
					objJson.put("rotation", rot);
					
					objJson.put("scale", go.getScale());
					
					entitiesArray.put(objJson);
				}
			} else if (e instanceof scene.CameraEntity) {
				scene.CameraEntity ce = (scene.CameraEntity) e;
				JSONObject objJson = new JSONObject();
				objJson.put("type", "CameraEntity");
				objJson.put("mode", ce.getMode().name());
				JSONObject pos = new JSONObject();
				pos.put("x", ce.getPosition().x);
				pos.put("y", ce.getPosition().y);
				pos.put("z", ce.getPosition().z);
				objJson.put("position", pos);
				entitiesArray.put(objJson);
			}
		}
		
		root.put("entities", entitiesArray);
		
		JSONObject envJson = new JSONObject();
		
		if (scene.getTerrains().size() > 0 && scene.getTerrains().get(0) instanceof FlatTerrain) {
			FlatTerrain ft = (FlatTerrain) scene.getTerrains().get(0);
			JSONObject terrainJson = new JSONObject();
			terrainJson.put("enabled", true);
			terrainJson.put("maxHeight", ft.getPMaxHeight());
			terrainJson.put("roughness", ft.getPRoughness());
			terrainJson.put("octaves", ft.getPOctaves());
			terrainJson.put("scale", ft.getPScale());
			terrainJson.put("seed", ft.getPSeed());
			envJson.put("terrain", terrainJson);
		}
		
		if (scene.getWater().size() > 0) {
			WaterTile wt = scene.getWater().get(0);
			JSONObject waterJson = new JSONObject();
			waterJson.put("enabled", true);
			waterJson.put("height", wt.getHeight());
			envJson.put("water", waterJson);
		}
		
		if (scene.getSky() != null && scene.getSky() instanceof AtmosphereSky) {
			JSONObject skyJson = new JSONObject();
			skyJson.put("enabled", true);
			envJson.put("atmosphere", skyJson);
		}
		
		// IDE uzerinden kaydedildigi icin ViewportCanvas erisimi yok (saveScene'de ViewportCanvas gondermiyoruz), 
		// bu nedenle DayNight'i sahneden veya parametre gondererek alabiliriz.
		// ViewportCanvas statik olarak bagli degil. Method'a ekleyelim.

		
		if (viewport.getDayNightManager() != null && viewport.getDayNightManager().getTimeOfDay() > -1) {
			JSONObject dayNightJson = new JSONObject();
			// Aslinda enabled bilgisini slider durumundan (veya viewport'tan) okumaliyiz ama basite indirgeyelim
			// Biz UI durumunu degil manager durumunu kaydediyoruz
			dayNightJson.put("enabled", true); 
			dayNightJson.put("timeOfDay", viewport.getDayNightManager().getTimeOfDay());
			envJson.put("dayNight", dayNightJson);
		}
		
		root.put("environment", envJson);
		
		try (FileWriter writer = new FileWriter(new File(filepath))) {
			writer.write(root.toString(4)); // 4 bosluklu guzel formatlama
			System.out.println("Sahne kaydedildi: " + filepath);
		} catch (Exception ex) {
			System.err.println("Sahne kaydedilirken hata: " + ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	public static void loadScene(String filepath, ViewportCanvas viewport, EnvironmentPanel envPanel) {
		try {
			String content = new String(Files.readAllBytes(Paths.get(filepath)));
			JSONObject root = new JSONObject(content);
			JSONArray entitiesArray = root.getJSONArray("entities");
			JSONObject envJson = root.optJSONObject("environment");
			
			// Objelerin VBO'larinin yaratilmasi icin OpenGL Thread'ine atiyoruz
			viewport.enqueue(() -> {
				
				// Mevcut objeleri temizleyebiliriz ama simdilik ustune ekliyoruz
				// (Yeni bir sahne yuklendiginde scene.delete() veya cleanUp gerekebilir)
				
				for (int i = 0; i < entitiesArray.length(); i++) {
					JSONObject objJson = entitiesArray.getJSONObject(i);
					String type = objJson.getString("type");
					
					if (type.equals("GameObject")) {
						String path = objJson.getString("path");
						try {
							GameObject go = new GameObject(path);
							
							JSONObject pos = objJson.getJSONObject("position");
							go.getPosition().set(pos.getFloat("x"), pos.getFloat("y"), pos.getFloat("z"));
							
							JSONObject rot = objJson.getJSONObject("rotation");
							go.getRotation().set(rot.getFloat("x"), rot.getFloat("y"), rot.getFloat("z"));
							
							go.setScale(objJson.getFloat("scale"));
							
							viewport.getScene().addEntity(go);
						} catch (Exception objEx) {
							System.err.println("Obje yuklenemedi (" + path + "): " + objEx.getMessage());
						}
					} else if (type.equals("CameraEntity")) {
						scene.CameraEntity cam = new scene.CameraEntity();
						JSONObject pos = objJson.getJSONObject("position");
						cam.getPosition().set(pos.getFloat("x"), pos.getFloat("y"), pos.getFloat("z"));
						try {
							String modeStr = objJson.getString("mode");
							cam.setMode(extra.Camera.CameraMode.valueOf(modeStr));
						} catch (Exception e) {}
						
						viewport.getScene().addEntity(cam);
						
						javax.swing.SwingUtilities.invokeLater(() -> {
							// Gorsel secim simdilik pasif
						});
					}
				}
				
				if (envJson != null) {
					if (envJson.has("terrain")) {
						JSONObject terrainJson = envJson.getJSONObject("terrain");
						if (terrainJson.getBoolean("enabled")) {
							viewport.setTerrainEnabled(true);
							// Procedural parametreler varsa onlari da al
							// Not: Viewport'taki terrain nesnesi olusunca onu guncelleyebiliriz.
							// Bunun icin ayri bir method gerekir, ama simdilik sadece "Enable" ediyoruz
						}
					}
					
					if (envJson.has("water")) {
						JSONObject waterJson = envJson.getJSONObject("water");
						if (waterJson.getBoolean("enabled")) {
							viewport.setWaterEnabled(true);
						}
					}
					
					if (envJson.has("atmosphere")) {
						JSONObject skyJson = envJson.getJSONObject("atmosphere");
						if (skyJson.getBoolean("enabled")) {
							viewport.setAtmosphereEnabled(true);
						}
					}
					
					if (envJson.has("dayNight")) {
						JSONObject dnJson = envJson.getJSONObject("dayNight");
						if (dnJson.getBoolean("enabled")) {
							float time = dnJson.getFloat("timeOfDay");
							javax.swing.SwingUtilities.invokeLater(() -> {
								envPanel.setTimeSlider(time);
							});
						}
					}
				}
				
				javax.swing.SwingUtilities.invokeLater(() -> {
					envPanel.updateUIFromState();
				});
				
				System.out.println("Sahne yuklendi: " + filepath);
			});
			
		} catch (Exception ex) {
			System.err.println("Sahne yuklenirken hata: " + ex.getMessage());
			ex.printStackTrace();
		}
	}
}
