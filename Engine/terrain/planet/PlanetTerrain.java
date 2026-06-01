package terrain.planet;

import terrain.ITerrain;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import utils.ICamera;

public class PlanetTerrain implements ITerrain {

    private int vaoId;
    private int vboVertex, vboNormal, vboUv, vboIndex;
    private int vertexCount;

    private List<Vector3f> vertices;
    private List<Vector3f> normals;
    private List<Vector2f> uvs;
    private List<Integer> indices;

    private float baseRadius;
    private Vector3f position = new Vector3f(0, 0, 0); // Center of the planet
    private Vector3f rotation = new Vector3f(0, 0, 0);

    // Texture ayarlarÃ„Â±
    private String grassTexturePath = "res/TerrainTexture/DEFAULT_GRASS/Ground067_8K-PNG_Color.png";
    private String dirtTexturePath = "res/DEFAULT_GROUND/Ground037_4K-PNG_Color.png";
    private String dirt2TexturePath = "res/DEFAULT_GROUND/Ground037_4K-PNG_Color.png";
    private String grassNormalPath = "res/TerrainTexture/DEFAULT_GRASS/Ground067_8K-PNG_NormalGL.png";
    private String dirtNormalPath = "res/DEFAULT_GROUND/Ground037_4K-PNG_NormalGL.png";
    private String dirt2NormalPath = "res/DEFAULT_GROUND/Ground037_4K-PNG_NormalGL.png";

    private terrain.flat.FlatTerrainShader shader;
    private int grassTex, dirtTex, dirt2Tex;
    private int grassNorm, dirtNorm, dirt2Norm;

    private Vector4f clipPlane = new Vector4f(0, -1, 0, 100000);

    public PlanetTerrain(float radius, int subdivisions, terrain.flat.FlatTerrainShader shader) {
        this.baseRadius = radius;
        this.shader = shader;

        loadDefaultTextures();
        generateIcoSphere(subdivisions);
        setupBuffers();
    }

    private void loadDefaultTextures() {
        grassTex = loadTex(grassTexturePath);
        dirtTex = loadTex(dirtTexturePath);
        dirt2Tex = loadTex(dirt2TexturePath);
        grassNorm = loadTex(grassNormalPath);
        dirtNorm = loadTex(dirtNormalPath);
        dirt2Norm = loadTex(dirt2NormalPath);
    }

    private int loadTex(String path) {
        try {
            return textures.Texture.newTexture(new utils.MyFile(path)).anisotropic().create().textureId;
        } catch (Exception e) {
            System.err.println("[PlanetTerrain] Texture yuklenemedi: " + path);
            return 0;
        }
    }

    private void generateIcoSphere(int recursionLevel) {
        vertices = new ArrayList<>();
        indices = new ArrayList<>();
        normals = new ArrayList<>();
        uvs = new ArrayList<>();

        float t = (float) ((1.0 + Math.sqrt(5.0)) / 2.0);

        addVertex(new Vector3f(-1, t, 0));
        addVertex(new Vector3f(1, t, 0));
        addVertex(new Vector3f(-1, -t, 0));
        addVertex(new Vector3f(1, -t, 0));

        addVertex(new Vector3f(0, -1, t));
        addVertex(new Vector3f(0, 1, t));
        addVertex(new Vector3f(0, -1, -t));
        addVertex(new Vector3f(0, 1, -t));

        addVertex(new Vector3f(t, 0, -1));
        addVertex(new Vector3f(t, 0, 1));
        addVertex(new Vector3f(-t, 0, -1));
        addVertex(new Vector3f(-t, 0, 1));

        // 5 faces around point 0
        indices.add(0);
        indices.add(11);
        indices.add(5);
        indices.add(0);
        indices.add(5);
        indices.add(1);
        indices.add(0);
        indices.add(1);
        indices.add(7);
        indices.add(0);
        indices.add(7);
        indices.add(10);
        indices.add(0);
        indices.add(10);
        indices.add(11);

        // 5 adjacent faces
        indices.add(1);
        indices.add(5);
        indices.add(9);
        indices.add(5);
        indices.add(11);
        indices.add(4);
        indices.add(11);
        indices.add(10);
        indices.add(2);
        indices.add(10);
        indices.add(7);
        indices.add(6);
        indices.add(7);
        indices.add(1);
        indices.add(8);

        // 5 faces around point 3
        indices.add(3);
        indices.add(9);
        indices.add(4);
        indices.add(3);
        indices.add(4);
        indices.add(2);
        indices.add(3);
        indices.add(2);
        indices.add(6);
        indices.add(3);
        indices.add(6);
        indices.add(8);
        indices.add(3);
        indices.add(8);
        indices.add(9);

        // 5 adjacent faces
        indices.add(4);
        indices.add(9);
        indices.add(5);
        indices.add(2);
        indices.add(4);
        indices.add(11);
        indices.add(6);
        indices.add(2);
        indices.add(10);
        indices.add(8);
        indices.add(6);
        indices.add(7);
        indices.add(9);
        indices.add(8);
        indices.add(1);

        Map<Long, Integer> middlePointIndexCache = new HashMap<>();

        for (int i = 0; i < recursionLevel; i++) {
            List<Integer> newIndices = new ArrayList<>();
            for (int j = 0; j < indices.size(); j += 3) {
                int v1 = indices.get(j);
                int v2 = indices.get(j + 1);
                int v3 = indices.get(j + 2);

                int a = getMiddlePoint(v1, v2, middlePointIndexCache);
                int b = getMiddlePoint(v2, v3, middlePointIndexCache);
                int c = getMiddlePoint(v3, v1, middlePointIndexCache);

                newIndices.add(v1);
                newIndices.add(a);
                newIndices.add(c);
                newIndices.add(v2);
                newIndices.add(b);
                newIndices.add(a);
                newIndices.add(v3);
                newIndices.add(c);
                newIndices.add(b);
                newIndices.add(a);
                newIndices.add(b);
                newIndices.add(c);
            }
            indices = newIndices;
        }

        for (int i = 0; i < vertices.size(); i++) {
            Vector3f v = vertices.get(i);
            Vector3f n = new Vector3f(v);
            if (n.lengthSquared() > 0)
                n.normalise();

            // Map to sphere radius
            v.x = n.x * baseRadius;
            v.y = n.y * baseRadius;
            v.z = n.z * baseRadius;

            normals.add(n);

            // Basic spherical UVs
            float u = (float) (0.5 + (Math.atan2(n.z, n.x) / (2 * Math.PI)));
            float vUv = (float) (0.5 - (Math.asin(n.y) / Math.PI));
            uvs.add(new Vector2f(u * 100f, vUv * 100f));
        }

        recalculateNormals();
    }

    private int addVertex(Vector3f v) {
        float length = (float) Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
        vertices.add(new Vector3f(v.x / length, v.y / length, v.z / length));
        return vertices.size() - 1;
    }

    private int getMiddlePoint(int p1, int p2, Map<Long, Integer> cache) {
        boolean firstIsSmaller = p1 < p2;
        long smallerIndex = firstIsSmaller ? p1 : p2;
        long greaterIndex = firstIsSmaller ? p2 : p1;
        long key = (smallerIndex << 32) + greaterIndex;

        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        Vector3f point1 = vertices.get(p1);
        Vector3f point2 = vertices.get(p2);
        Vector3f middle = new Vector3f(
                (point1.x + point2.x) / 2.0f,
                (point1.y + point2.y) / 2.0f,
                (point1.z + point2.z) / 2.0f);

        int i = addVertex(middle);
        cache.put(key, i);
        return i;
    }

    private void recalculateNormals() {
        // Zero all normals
        for (int i = 0; i < normals.size(); i++) {
            normals.get(i).set(0, 0, 0);
        }

        // Add face normals to vertex normals
        for (int i = 0; i < indices.size(); i += 3) {
            int i0 = indices.get(i);
            int i1 = indices.get(i + 1);
            int i2 = indices.get(i + 2);

            Vector3f v0 = vertices.get(i0);
            Vector3f v1 = vertices.get(i1);
            Vector3f v2 = vertices.get(i2);

            Vector3f edge1 = Vector3f.sub(v1, v0, null);
            Vector3f edge2 = Vector3f.sub(v2, v0, null);
            Vector3f faceNormal = Vector3f.cross(edge1, edge2, null);

            Vector3f.add(normals.get(i0), faceNormal, normals.get(i0));
            Vector3f.add(normals.get(i1), faceNormal, normals.get(i1));
            Vector3f.add(normals.get(i2), faceNormal, normals.get(i2));
        }

        // Normalize
        for (int i = 0; i < normals.size(); i++) {
            Vector3f n = normals.get(i);
            if (n.lengthSquared() > 0) {
                n.normalise();
            } else {
                Vector3f v = vertices.get(i);
                n.set(v);
                n.normalise();
            }
        }
    }

    private void setupBuffers() {
        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        vboVertex = GL15.glGenBuffers();
        vboNormal = GL15.glGenBuffers();
        vboUv = GL15.glGenBuffers();
        vboIndex = GL15.glGenBuffers();

        updateBuffers();
        GL30.glBindVertexArray(0);
    }

    public void updateBuffers() {
        recalculateNormals();

        float[] vArr = new float[vertices.size() * 3];
        float[] nArr = new float[normals.size() * 3];
        float[] uArr = new float[uvs.size() * 2];
        int[] iArr = new int[indices.size()];

        for (int i = 0; i < vertices.size(); i++) {
            vArr[i * 3] = vertices.get(i).x;
            vArr[i * 3 + 1] = vertices.get(i).y;
            vArr[i * 3 + 2] = vertices.get(i).z;

            nArr[i * 3] = normals.get(i).x;
            nArr[i * 3 + 1] = normals.get(i).y;
            nArr[i * 3 + 2] = normals.get(i).z;

            uArr[i * 2] = uvs.get(i).x;
            uArr[i * 2 + 1] = uvs.get(i).y;
        }

        for (int i = 0; i < indices.size(); i++) {
            iArr[i] = indices.get(i);
        }

        GL30.glBindVertexArray(vaoId);

        uploadFloatData(vboVertex, 0, 3, vArr);
        uploadFloatData(vboNormal, 1, 3, nArr);
        uploadFloatData(vboUv, 2, 2, uArr);

        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboIndex);
        IntBuffer buffer = BufferUtils.createIntBuffer(iArr.length);
        buffer.put(iArr);
        buffer.flip();
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        vertexCount = iArr.length;

        GL30.glBindVertexArray(0);
    }

    private void uploadFloatData(int vbo, int attr, int size, float[] data) {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);
        buffer.put(data);
        buffer.flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(attr, size, GL11.GL_FLOAT, false, 0, 0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    /**
     * Yerel koordinat sistemindeki (model space) bir noktayÃ„Â± yÃƒÂ¼kseltir veya
     * alÃƒÂ§altÃ„Â±r.
     * 
     * @param localHitPoint Modifikasyon merkezi (Local Space)
     * @param radius        Etki yarÃ„Â±ÃƒÂ§apÃ„Â±
     * @param amount        DeÃ„Å¸iÃ…Å¸im miktarÃ„Â± (Pozitif: yÃƒÂ¼kselt, Negatif: alÃƒÂ§alt)
     */
    public void modifyTerrain(Vector3f localHitPoint, float radius, float amount) {
        float radiusSq = radius * radius;
        boolean changed = false;

        for (int i = 0; i < vertices.size(); i++) {
            Vector3f v = vertices.get(i);
            float distSq = Vector3f.sub(v, localHitPoint, null).lengthSquared();

            if (distSq < radiusSq) {
                // UzaklÃ„Â±Ã„Å¸a gÃƒÂ¶re smooth etki (Gaussian / Linear falloff)
                float dist = (float) Math.sqrt(distSq);
                float falloff = 1.0f - (dist / radius);

                // Merkezden dÃ„Â±Ã…Å¸arÃ„Â±ya doÃ„Å¸ru normali bul (KÃƒÂ¼re olduÃ„Å¸u iÃƒÂ§in vektÃƒÂ¶rÃƒÂ¼n kendisi
                // normaldir)
                Vector3f dir = new Vector3f(v);
                if (dir.lengthSquared() > 0) {
                    dir.normalise();
                } else {
                    dir.set(0, 1, 0);
                }

                // DeÃ„Å¸iÃ…Å¸imi uygula
                float push = amount * falloff;
                v.x += dir.x * push;
                v.y += dir.y * push;
                v.z += dir.z * push;
                changed = true;
            }
        }

        if (changed) {
            updateBuffers();
        }
    }

    public void render(ICamera camera, Scene scene) {
        render(camera, scene, null, 0);
    }

    @Override
    public void render(ICamera camera, Scene scene, Matrix4f toShadowSpace, int shadowMapTexId) {
        shader.start();

        Matrix4f model = new Matrix4f();
        model.setIdentity();
        Matrix4f.translate(position, model, model);
        Matrix4f.rotate((float) Math.toRadians(rotation.x), new Vector3f(1, 0, 0), model, model);
        Matrix4f.rotate((float) Math.toRadians(rotation.y), new Vector3f(0, 1, 0), model, model);
        Matrix4f.rotate((float) Math.toRadians(rotation.z), new Vector3f(0, 0, 1), model, model);

        shader.loadMatrices(model, camera.getViewMatrix(), camera.getProjectionMatrix());
        shader.loadClipPlane(clipPlane);
        shader.loadInfiniteParams(false, 50f, 0.45f, 4, 200f, 0f, 0f, 0f);
        shader.loadPlanetaryParams(true, position);

        shader.loadLight(scene.getLightDirection(), scene.getLightColor(), scene.getAmbientLight());
        shader.loadFogParams(scene.getFogColor(), scene.getFogDensity(), scene.getFogStart());
        shader.loadCameraPos(camera.getPosition());

        if (toShadowSpace != null) {
            shader.loadToShadowSpaceMatrix(toShadowSpace);
        }

        if (scene.getSky() != null && scene.getSky() instanceof skybox.atmosphere.AtmosphereSky) {
            skybox.atmosphere.AtmosphereSky sky = (skybox.atmosphere.AtmosphereSky) scene.getSky();
            shader.loadCloudShadowData(sky.getTime(),
                    new Vector2f(scene.getWindVelocity().x, scene.getWindVelocity().z), sky.isCloudsEnabled(),
                    sky.getClusters());
        } else {
            shader.loadCloudShadowData(0f, new Vector2f(0, 0), false, null);
        }

        shader.loadTextureScale(100f);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, grassTex);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, dirtTex);
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, dirt2Tex);
        GL13.glActiveTexture(GL13.GL_TEXTURE3);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, grassNorm);
        GL13.glActiveTexture(GL13.GL_TEXTURE4);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, dirtNorm);
        GL13.glActiveTexture(GL13.GL_TEXTURE5);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, dirt2Norm);

        if (shadowMapTexId != 0) {
            GL13.glActiveTexture(GL13.GL_TEXTURE6);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, shadowMapTexId);
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);

        GL30.glBindVertexArray(vaoId);
        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glEnableVertexAttribArray(2);

        GL11.glDrawElements(GL11.GL_TRIANGLES, vertexCount, GL11.GL_UNSIGNED_INT, 0);

        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);
        GL30.glBindVertexArray(0);

        shader.stop();
    }

    @Override
    public float getHeightAt(float x, float z) {
        // FlatTerrain iÃƒÂ§in tasarlanmÃ„Â±Ã…Å¸. KÃƒÂ¼re iÃƒÂ§in X, Y, Z uzayÃ„Â±nda Raycast lazÃ„Â±m.
        // FiziÃ„Å¸in ÃƒÂ§alÃ„Â±Ã…Å¸masÃ„Â± iÃƒÂ§in kÃƒÂ¼re yÃƒÂ¼zeyine olan yaklaÃ…Å¸Ã„Â±k mesafeyi hesaplayabiliriz.
        // Ancak tam yÃƒÂ¼kseklik iÃƒÂ§in oyuncunun konumunu vermeliyiz.
        // Ã…Å¾imdilik sadece baseRadius dÃƒÂ¶ndÃƒÂ¼receÃ„Å¸iz.
        return baseRadius;
    }

    /**
     * Oyuncunun merkezden olan uzaklÃ„Â±Ã„Å¸Ã„Â±na gÃƒÂ¶re yÃƒÂ¼ksekliÃ„Å¸ini hesaplar.
     */
    public float getExactHeightAt(Vector3f localPosition) {
        // TODO: En yakÃ„Â±n ÃƒÂ¼ÃƒÂ§geni (veya vertex'i) bulup interpole etmek.
        // Ã…Å¾imdilik en yakÃ„Â±n vertex'in merkezden uzaklÃ„Â±Ã„Å¸Ã„Â±nÃ„Â± veriyoruz.
        if (vertices == null || vertices.isEmpty())
            return baseRadius;

        float minDistSq = Float.MAX_VALUE;
        Vector3f closest = null;

        for (Vector3f v : vertices) {
            float d = Vector3f.sub(v, localPosition, null).lengthSquared();
            if (d < minDistSq) {
                minDistSq = d;
                closest = v;
            }
        }
        return closest.length();
    }

    @Override
    public void setClipPlane(Vector4f plane) {
        this.clipPlane.set(plane);
    }

    @Override
    public float getWidth() {
        return baseRadius * 2;
    }

    @Override
    public float getDepth() {
        return baseRadius * 2;
    }

    @Override public int getVaoId() { return vaoId; }
    @Override public int getIndexVboId() { return vboIndex; }
    @Override public int getIndexCount() { return vertexCount; }

    @Override
    public void cleanUp() {
        GL30.glDeleteVertexArrays(vaoId);
        GL15.glDeleteBuffers(vboVertex);
        GL15.glDeleteBuffers(vboNormal);
        GL15.glDeleteBuffers(vboUv);
        GL15.glDeleteBuffers(vboIndex);
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f position) {
        this.position.set(position);
    }

    public Vector3f getRotation() {
        return rotation;
    }

    public void setRotation(Vector3f rotation) {
        this.rotation.set(rotation);
    }

    public float getBaseRadius() {
        return baseRadius;
    }
}




