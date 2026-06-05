package gane;

import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Vector3f;
import extra.Camera;
import renderEngine.RenderEngine;
import scene.Scene;
import scene.GameObject;
import gane.AppSettings;
import terrain.flat.FlatTerrain;
import water.tile.WaterTile;
import environment.DayNightManager;
import skybox.atmosphere.AtmosphereSky;

public class MyGameLauncher {
    public static void main(String[] args) {
        gane.AppSettings.setup(1280, 720, false, "Gane Engine - Exported Game", null);

        Camera camera = new Camera();
        camera.getPosition().set(0, 10, 0);

        Scene scene = new Scene(camera);
        RenderEngine renderEngine = RenderEngine.init();

        scene.setSky(new AtmosphereSky());
        FlatTerrain ft = new FlatTerrain(2000, 2000);
        ft.generateProceduralTerrainV2(60.00f, 0.40f, 4, 300.00f, 12345L);
        scene.addTerrain(ft);
        WaterTile water = new WaterTile(0, 0, -2.00f, 400);
        scene.addWater(water);
        DayNightManager dnm = new DayNightManager(scene, 17.74f, 10.0f);
        camera.getPosition().set(0.00f, 10.00f, 0.00f);
        try {
            GameObject go1 = new GameObject("src/res/DEFAULT_TREE_ALL_PACK/low_poly_forest_tree_pack.glb");
            go1.getPosition().set(0.00f, 0.00f, 0.00f);
            go1.getRotation().set(0.00f, 0.00f, 0.00f);
            go1.setScale(1.00f);
            scene.addEntity(go1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        long lastTime = System.nanoTime();
        while (!Display.isCloseRequested()) {
            long currentTime = System.nanoTime();
            float delta = (currentTime - lastTime) / 1000000000.0f;
            lastTime = currentTime;

            camera.move();

            dnm.update(delta);
            for (scene.Entity e : scene.getAllEntities()) {
                if (e instanceof scene.GameObject)
                    ((scene.GameObject) e).update(delta);
            }
            water.update(delta);
            renderEngine.renderScene(scene, delta);
            renderEngine.update();
        }

        scene.delete();
        renderEngine.close();
    }
}
