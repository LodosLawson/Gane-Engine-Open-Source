package terrain;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import scene.Scene;
import textures.Texture;
import utils.ICamera;
import utils.MyFile;

public class GrassField {

    private int vaoId = 0;
    private int vertexVbo = 0;
    private int normalVbo = 0;
    private int uvVbo = 0;
    private int indexVbo = 0;
    private int indexCount = 0;

    private int grassTexId = 0;
    private GrassShader shader;
    private ITerrain terrain;

    private Vector4f clipPlane = new Vector4f(0, -1, 0, 100000f);

    public GrassField(ITerrain terrain, int count) {
        this.terrain = terrain;
        this.shader = new GrassShader();
        this.grassTexId = loadTex("res/grass.png");
        
        generateGrassMesh(count);
    }

    public void setClipPlane(Vector4f plane) {
        this.clipPlane = plane;
    }

    private void generateGrassMesh(int count) {
        List<Float> positions = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Float> uvs = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        Random rand = new Random(12345L);
        float width = terrain.getWidth();
        float depth = terrain.getDepth();

        int placed = 0;
        int maxAttempts = count * 15;
        int attempts = 0;

        while (placed < count && attempts < maxAttempts) {
            attempts++;
            
            float rx = rand.nextFloat() * width - width * 0.5f;
            float rz = rand.nextFloat() * depth - depth * 0.5f;

            float y = terrain.getHeightAt(rx, rz);
            if (y <= 1.0f) {
                continue;
            }

            float eps = 1.0f;
            float yL = terrain.getHeightAt(rx - eps, rz);
            float yR = terrain.getHeightAt(rx + eps, rz);
            float yD = terrain.getHeightAt(rx, rz - eps);
            float yU = terrain.getHeightAt(rx, rz + eps);
            float slope = (float) Math.sqrt((yL - yR) * (yL - yR) + (yD - yU) * (yD - yU));

            if (slope > 0.6f) {
                continue;
            }

            float w = 1.2f + rand.nextFloat() * 0.8f;
            float h = 1.5f + rand.nextFloat() * 1.0f;
            float angle = rand.nextFloat() * (float) Math.PI;

            int baseIndex = placed * 8;

            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            float cos2 = (float) Math.cos(angle + Math.PI / 4.0);
            float sin2 = (float) Math.sin(angle + Math.PI / 4.0);

            // Quad 1
            // Bottom left
            positions.add(rx - w * cos); positions.add(y); positions.add(rz - w * sin);
            normals.add(-sin); normals.add(0f); normals.add(cos);
            uvs.add(0f); uvs.add(1f);

            // Bottom right
            positions.add(rx + w * cos); positions.add(y); positions.add(rz + w * sin);
            normals.add(-sin); normals.add(0f); normals.add(cos);
            uvs.add(1f); uvs.add(1f);

            // Top right
            positions.add(rx + w * cos); positions.add(y + h); positions.add(rz + w * sin);
            normals.add(-sin); normals.add(0f); normals.add(cos);
            uvs.add(1f); uvs.add(0f);

            // Top left
            positions.add(rx - w * cos); positions.add(y + h); positions.add(rz - w * sin);
            normals.add(-sin); normals.add(0f); normals.add(cos);
            uvs.add(0f); uvs.add(0f);

            // Quad 2
            // Bottom left
            positions.add(rx - w * cos2); positions.add(y); positions.add(rz - w * sin2);
            normals.add(-sin2); normals.add(0f); normals.add(cos2);
            uvs.add(0f); uvs.add(1f);

            // Bottom right
            positions.add(rx + w * cos2); positions.add(y); positions.add(rz + w * sin2);
            normals.add(-sin2); normals.add(0f); normals.add(cos2);
            uvs.add(1f); uvs.add(1f);

            // Top right
            positions.add(rx + w * cos2); positions.add(y + h); positions.add(rz + w * sin2);
            normals.add(-sin2); normals.add(0f); normals.add(cos2);
            uvs.add(1f); uvs.add(0f);

            // Top left
            positions.add(rx - w * cos2); positions.add(y + h); positions.add(rz - w * sin2);
            normals.add(-sin2); normals.add(0f); normals.add(cos2);
            uvs.add(0f); uvs.add(0f);

            // Indices Quad 1
            indices.add(baseIndex); indices.add(baseIndex + 1); indices.add(baseIndex + 2);
            indices.add(baseIndex); indices.add(baseIndex + 2); indices.add(baseIndex + 3);

            // Indices Quad 2
            indices.add(baseIndex + 4); indices.add(baseIndex + 5); indices.add(baseIndex + 6);
            indices.add(baseIndex + 4); indices.add(baseIndex + 6); indices.add(baseIndex + 7);

            placed++;
        }

        System.out.println("[GrassField] " + placed + " adet cimen VBO'su olusturuldu (Hedef: " + count + ", Deneme: " + attempts + ")");

        if (indices.isEmpty()) return;

        float[] posArr = toFloatArray(positions);
        float[] normArr = toFloatArray(normals);
        float[] uvArr = toFloatArray(uvs);
        int[] idxArr = toIntArray(indices);

        indexCount = idxArr.length;

        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        vertexVbo = storeDataInVbo(0, 3, posArr);
        normalVbo = storeDataInVbo(1, 3, normArr);
        uvVbo = storeDataInVbo(2, 2, uvArr);
        indexVbo = storeIndexVbo(idxArr);

        GL30.glBindVertexArray(0);
    }

    public void render(ICamera camera, Scene scene) {
        render(camera, scene, null, 0);
    }

    public void render(ICamera camera, Scene scene, Matrix4f toShadowSpace, int shadowMapTexId) {
        if (indexCount == 0) return;

        shader.start();

        Matrix4f model = new Matrix4f();
        model.setIdentity();

        shader.loadMatrices(model, camera.getViewMatrix(), camera.getProjectionMatrix());
        shader.loadClipPlane(clipPlane);

        float time = 0.0f;
        if (scene.getSky() != null && scene.getSky() instanceof skybox.atmosphere.AtmosphereSky) {
            time = ((skybox.atmosphere.AtmosphereSky) scene.getSky()).getTime();
        } else {
            time = (float) (System.currentTimeMillis() % 1000000L) / 1000.0f;
        }
        shader.loadTime(time);
        
        Vector3f wind = scene.getWindVelocity();
        shader.loadWindDir(new Vector2f(wind.x, wind.z));

        shader.loadLight(scene.getLightDirection(), scene.getLightColor(), scene.getAmbientLight());
        shader.loadFogParams(scene.getFogColor(), scene.getFogDensity(), scene.getFogStart());
        shader.loadCameraPos(camera.getPosition());

        if (toShadowSpace != null) {
            shader.loadToShadowSpaceMatrix(toShadowSpace);
        }

        // Load Cloud Shadows
        if (scene.getSky() != null && scene.getSky() instanceof skybox.atmosphere.AtmosphereSky) {
            skybox.atmosphere.AtmosphereSky sky = (skybox.atmosphere.AtmosphereSky) scene.getSky();
            shader.loadCloudShadowData(sky.getTime(), new org.lwjgl.util.vector.Vector2f(scene.getWindVelocity().x, scene.getWindVelocity().z), sky.isCloudsEnabled(), sky.getClusters());
        } else {
            shader.loadCloudShadowData(0f, new org.lwjgl.util.vector.Vector2f(0, 0), false, null);
        }

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, grassTexId);

        if (shadowMapTexId != 0) {
            GL13.glActiveTexture(GL13.GL_TEXTURE6);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, shadowMapTexId);
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);

        GL30.glBindVertexArray(vaoId);
        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glEnableVertexAttribArray(2);

        GL11.glDrawElements(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0);

        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);
        GL30.glBindVertexArray(0);

        shader.stop();
    }

    public void cleanUp() {
        shader.cleanUp();
        if (vaoId != 0) GL30.glDeleteVertexArrays(vaoId);
        if (vertexVbo != 0) GL15.glDeleteBuffers(vertexVbo);
        if (normalVbo != 0) GL15.glDeleteBuffers(normalVbo);
        if (uvVbo != 0) GL15.glDeleteBuffers(uvVbo);
        if (indexVbo != 0) GL15.glDeleteBuffers(indexVbo);
        if (grassTexId != 0) GL11.glDeleteTextures(grassTexId);
    }

    private int storeDataInVbo(int attrib, int size, float[] data) {
        int vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        FloatBuffer buf = BufferUtils.createFloatBuffer(data.length);
        buf.put(data).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(attrib, size, GL11.GL_FLOAT, false, 0, 0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        return vbo;
    }

    private int storeIndexVbo(int[] data) {
        int vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vbo);
        IntBuffer buf = BufferUtils.createIntBuffer(data.length);
        buf.put(data).flip();
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);
        return vbo;
    }

    private int loadTex(String path) {
        try {
            return Texture.newTexture(new MyFile(path)).anisotropic().create().textureId;
        } catch (Exception e) {
            System.err.println("[GrassField] Texture yuklenemedi: " + path);
            return 0;
        }
    }

    private float[] toFloatArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = list.get(i);
        return arr;
    }

    private int[] toIntArray(List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = list.get(i);
        return arr;
    }
}

