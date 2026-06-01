package terrain;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Vector2f;
import shaders.ShaderProgram;
import shaders.UniformFloat;
import shaders.UniformMatrix;
import shaders.UniformSampler;
import shaders.UniformVec3;
import shaders.UniformVec4;
import shaders.UniformVec2;
import utils.MyFile;

public class GrassShader extends ShaderProgram {

    private static final MyFile VERT = new MyFile("terrain/grass.vert");
    private static final MyFile FRAG = new MyFile("terrain/grass.frag");

    private UniformMatrix uModel      = new UniformMatrix("uModel");
    private UniformMatrix uView       = new UniformMatrix("uView");
    private UniformMatrix uProjection = new UniformMatrix("uProjection");
    private UniformVec4   uClipPlane  = new UniformVec4("uClipPlane");
    private UniformSampler uGrassTex  = new UniformSampler("uGrassTex");
    private UniformFloat  uTime       = new UniformFloat("uTime");
    private UniformVec2   uWindDir    = new UniformVec2("uWindDir");
    private UniformVec3   uLightDir   = new UniformVec3("uLightDir");
    private UniformVec3   uLightColor = new UniformVec3("uLightColor");
    private UniformFloat  uAmbient    = new UniformFloat("uAmbient");
    private UniformVec3   uFogColor   = new UniformVec3("uFogColor");
    private UniformFloat  uFogDensity = new UniformFloat("uFogDensity");
    private UniformFloat  uFogStart   = new UniformFloat("uFogStart");
    private UniformVec3   uCameraPos  = new UniformVec3("uCameraPos");

    // Cloud Shadows
    private UniformFloat uCloudShadowEnabled = new UniformFloat("uCloudShadowEnabled");
    private UniformVec4[] uCloudShadowPos = new UniformVec4[32];
    private UniformFloat[] uCloudShadowAlpha = new UniformFloat[32];
    private shaders.UniformInt uNumCloudShadows = new shaders.UniformInt("uNumCloudShadows");

    private UniformMatrix toShadowMapSpace = new UniformMatrix("toShadowMapSpace");
    private UniformSampler shadowMap = new UniformSampler("shadowMap");

    public GrassShader() {
        super(VERT, FRAG, "inPosition", "inNormal", "inUV");
        
        for (int i = 0; i < 32; i++) {
            uCloudShadowPos[i] = new UniformVec4("uCloudShadowPos[" + i + "]");
            uCloudShadowAlpha[i] = new UniformFloat("uCloudShadowAlpha[" + i + "]");
        }

        java.util.List<shaders.Uniform> list = new java.util.ArrayList<>();
        list.add(uModel);
        list.add(uView);
        list.add(uProjection);
        list.add(uClipPlane);
        list.add(uGrassTex);
        list.add(uTime);
        list.add(uWindDir);
        list.add(uLightDir);
        list.add(uLightColor);
        list.add(uAmbient);
        list.add(uFogColor);
        list.add(uFogDensity);
        list.add(uFogStart);
        list.add(uCameraPos);
        list.add(uCloudShadowEnabled);
        for (int i = 0; i < 32; i++) {
            list.add(uCloudShadowPos[i]);
            list.add(uCloudShadowAlpha[i]);
        }
        list.add(uNumCloudShadows);
        list.add(toShadowMapSpace);
        list.add(shadowMap);

        storeAllUniformLocations(list.toArray(new shaders.Uniform[0]));

        start();
        uGrassTex.loadTexUnit(0);
        shadowMap.loadTexUnit(6);
        stop();
    }

    public void loadMatrices(Matrix4f model, Matrix4f view, Matrix4f proj) {
        uModel.loadMatrix(model);
        uView.loadMatrix(view);
        uProjection.loadMatrix(proj);
    }

    public void loadClipPlane(Vector4f plane) {
        uClipPlane.loadVec4(plane);
    }

    public void loadTime(float time) {
        uTime.loadFloat(time);
    }

    public void loadWindDir(Vector2f windDir) {
        uWindDir.loadVec2(windDir);
    }

    public void loadLight(Vector3f dir, Vector3f color, float ambient) {
        uLightDir.loadVec3(dir);
        uLightColor.loadVec3(color);
        uAmbient.loadFloat(ambient);
    }

    public void loadFogParams(Vector3f color, float density, float start) {
        uFogColor.loadVec3(color);
        uFogDensity.loadFloat(density);
        uFogStart.loadFloat(start);
    }

    public void loadCameraPos(Vector3f pos) {
        uCameraPos.loadVec3(pos);
    }

    public void loadCloudShadowData(float time, org.lwjgl.util.vector.Vector2f windDir, boolean enabled, java.util.List<skybox.atmosphere.CloudCluster> clusters) {
        uTime.loadFloat(time);
        uWindDir.loadVec2(windDir);
        uCloudShadowEnabled.loadFloat(enabled ? 1.0f : 0.0f);

        int count = 0;
        if (clusters != null) {
            count = Math.min(clusters.size(), 32);
        }
        uNumCloudShadows.loadInt(count);
        for (int i = 0; i < count; i++) {
            skybox.atmosphere.CloudCluster c = clusters.get(i);
            float r = c.currentScale * 650.0f;

            uCloudShadowPos[i].loadVec4(new org.lwjgl.util.vector.Vector4f(c.position.x, c.position.y, c.position.z, r));
            uCloudShadowAlpha[i].loadFloat(c.alpha);
        }
        for (int i = count; i < 32; i++) {
            uCloudShadowPos[i].loadVec4(new org.lwjgl.util.vector.Vector4f(0, 0, 0, 0));
            uCloudShadowAlpha[i].loadFloat(0.0f);
        }
    }

    public void loadToShadowSpaceMatrix(Matrix4f matrix) {
        toShadowMapSpace.loadMatrix(matrix);
    }
}

