package ide.utils;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONArray;
import org.json.JSONObject;

public class GameExporter {
    
    public static void exportGame(String ganeFilePath, String outputJavaFolder) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(ganeFilePath)));
            JSONObject root = new JSONObject(content);
            
            String pkgName = "gane";
            try {
                String fullPath = new File(outputJavaFolder).getCanonicalPath().replace('\\', '/');
                int srcIndex = fullPath.indexOf("/src/");
                if (srcIndex != -1) {
                    pkgName = fullPath.substring(srcIndex + 5).replace('/', '.');
                }
            } catch(Exception ignored) {}
            
            StringBuilder sb = new StringBuilder();
            if (pkgName != null && !pkgName.isEmpty()) {
                sb.append("package ").append(pkgName).append(";\n\n");
            }
            sb.append("import org.lwjgl.opengl.Display;\n");
            sb.append("import org.lwjgl.util.vector.Vector3f;\n");
            sb.append("import extra.Camera;\n");
            sb.append("import renderEngine.RenderEngine;\n");
            sb.append("import scene.Scene;\n");
            sb.append("import scene.GameObject;\n");
            sb.append("import gane.AppSettings;\n");
            sb.append("import terrain.flat.FlatTerrain;\n");
            sb.append("import water.tile.WaterTile;\n");
            sb.append("import environment.DayNightManager;\n");
            sb.append("import skybox.atmosphere.AtmosphereSky;\n\n");
            
            sb.append("public class MyGameLauncher {\n");
            sb.append("    public static void main(String[] args) {\n");
            sb.append("        gane.AppSettings.setup(1280, 720, false, \"Gane Engine - Exported Game\", null);\n");
            sb.append("\n");
            sb.append("        Camera camera = new Camera();\n");
            sb.append("        camera.getPosition().set(0, 10, 0);\n");
            sb.append("\n");
            sb.append("        Scene scene = new Scene(camera);\n");
            sb.append("        RenderEngine renderEngine = RenderEngine.init();\n");
            sb.append("\n");
            
            JSONObject env = root.optJSONObject("environment");
            if (env != null) {
                if (env.has("atmosphereSky") && env.getJSONObject("atmosphereSky").getBoolean("enabled")) {
                    sb.append("        scene.setSky(new AtmosphereSky());\n");
                }
                if (env.has("terrain") && env.getJSONObject("terrain").getBoolean("enabled")) {
                    JSONObject t = env.getJSONObject("terrain");
                    sb.append("        FlatTerrain ft = new FlatTerrain(2000, 2000);\n");
                    sb.append(String.format(java.util.Locale.US, "        ft.generateProceduralTerrainV2(%.2ff, %.2ff, %d, %.2ff, %dL);\n",
                        t.getFloat("maxHeight"), t.getFloat("roughness"), t.getInt("octaves"), t.getFloat("scale"), t.getLong("seed")));
                    sb.append("        scene.addTerrain(ft);\n");
                }
                if (env.has("water") && env.getJSONObject("water").getBoolean("enabled")) {
                    sb.append(String.format(java.util.Locale.US, "        WaterTile water = new WaterTile(0, 0, %.2ff, 400);\n", env.getJSONObject("water").getFloat("height")));
                    sb.append("        scene.addWater(water);\n");
                }
                if (env.has("dayNight") && env.getJSONObject("dayNight").getBoolean("enabled")) {
                    sb.append(String.format(java.util.Locale.US, "        DayNightManager dnm = new DayNightManager(scene, %.2ff, 10.0f);\n", env.getJSONObject("dayNight").getFloat("timeOfDay")));
                }
            }
            
            JSONArray entities = root.optJSONArray("entities");
            if (entities != null) {
                for (int i = 0; i < entities.length(); i++) {
                    JSONObject obj = entities.getJSONObject(i);
                    if (obj.getString("type").equals("GameObject")) {
                        sb.append("        try {\n");
                        sb.append("            GameObject go" + i + " = new GameObject(\"" + obj.getString("path") + "\");\n");
                        sb.append(String.format(java.util.Locale.US, "            go" + i + ".getPosition().set(%.2ff, %.2ff, %.2ff);\n", obj.getJSONObject("position").getFloat("x"), obj.getJSONObject("position").getFloat("y"), obj.getJSONObject("position").getFloat("z")));
                        sb.append(String.format(java.util.Locale.US, "            go" + i + ".getRotation().set(%.2ff, %.2ff, %.2ff);\n", obj.getJSONObject("rotation").getFloat("x"), obj.getJSONObject("rotation").getFloat("y"), obj.getJSONObject("rotation").getFloat("z")));
                        sb.append(String.format(java.util.Locale.US, "            go" + i + ".setScale(%.2ff);\n", obj.getFloat("scale")));
                        sb.append("            scene.addEntity(go" + i + ");\n");
                        sb.append("        } catch (Exception e) { e.printStackTrace(); }\n\n");
                    } else if (obj.getString("type").equals("CameraEntity")) {
                        sb.append(String.format(java.util.Locale.US, "        camera.getPosition().set(%.2ff, %.2ff, %.2ff);\n", obj.getJSONObject("position").getFloat("x"), obj.getJSONObject("position").getFloat("y"), obj.getJSONObject("position").getFloat("z")));
                    }
                }
            }
            
            sb.append("        \n");
            sb.append("        long lastTime = System.nanoTime();\n");
            sb.append("        while (!Display.isCloseRequested()) {\n");
            sb.append("            long currentTime = System.nanoTime();\n");
            sb.append("            float delta = (currentTime - lastTime) / 1000000000.0f;\n");
            sb.append("            lastTime = currentTime;\n");
            sb.append("            \n");
            sb.append("            camera.move();\n");
            sb.append("            \n");
            if (env != null && env.has("dayNight") && env.getJSONObject("dayNight").getBoolean("enabled")) {
                sb.append("            dnm.update(delta);\n");
            }
            sb.append("            for (scene.Entity e : scene.getAllEntities()) {\n");
            sb.append("                if (e instanceof scene.GameObject) ((scene.GameObject)e).update(delta);\n");
            sb.append("            }\n");
            if (env != null && env.has("water") && env.getJSONObject("water").getBoolean("enabled")) {
                sb.append("            water.update(delta);\n");
            }
            sb.append("            renderEngine.renderScene(scene, delta);\n");
            sb.append("            renderEngine.update();\n");
            sb.append("        }\n");
            sb.append("        \n");
            sb.append("        scene.delete();\n");
            sb.append("        renderEngine.close();\n");
            sb.append("    }\n");
            sb.append("}\n");
            
            File outFolder = new File(outputJavaFolder);
            if (!outFolder.exists()) outFolder.mkdirs();
            
            try (FileWriter fw = new FileWriter(new File(outFolder, "MyGameLauncher.java"))) {
                fw.write(sb.toString());
                System.out.println("Oyun kodu uretildi: " + new File(outFolder, "MyGameLauncher.java").getAbsolutePath());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
