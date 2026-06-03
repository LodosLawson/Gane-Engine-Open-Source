package shadows;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import terrain.flat.FlatTerrain;
import terrain.planet.PlanetTerrain;
import terrain.voxel.VoxelTerrain;
import scene.Skin;
import scene.Model;
import scene.Entity;
import scene.Scene;
import utils.ICamera;

public class ShadowMapMasterRenderer {

    private static final int SHADOW_MAP_SIZE = 4096; // Performans ve kalite için 4096'ya çıkarıldı.
    private static final int MAX_INSTANCES = 500000;
    private static final int INSTANCE_DATA_LENGTH = 17;
    private static final FloatBuffer buffer = BufferUtils.createFloatBuffer(MAX_INSTANCES * INSTANCE_DATA_LENGTH);
    private static final float[] tempArray = new float[MAX_INSTANCES * INSTANCE_DATA_LENGTH];

    private ShadowFrameBuffer shadowFbo;
    private ShadowShader shader;
    private ShadowInstancedShader instancedShader;
    private ShadowTerrainShader terrainShader;
    private openglObjects.Vbo instancedVbo;
    private ShadowBox shadowBox;
    private Matrix4f projectionMatrix = new Matrix4f();
    private Matrix4f lightViewMatrix = new Matrix4f();
    private Matrix4f projectionViewMatrix = new Matrix4f();
    private Matrix4f offset = createOffset();

    public ShadowMapMasterRenderer(ICamera camera) {
        shader = new ShadowShader();
        instancedShader = new ShadowInstancedShader();
        terrainShader = new ShadowTerrainShader();
        shadowBox = new ShadowBox(lightViewMatrix, camera);
        shadowFbo = new ShadowFrameBuffer(SHADOW_MAP_SIZE, SHADOW_MAP_SIZE);

        instancedVbo = openglObjects.Vbo.create(org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER);
        instancedVbo.bind();
        instancedVbo.allocateData(MAX_INSTANCES * INSTANCE_DATA_LENGTH);
        instancedVbo.unbind();

        shader.connectTextureUnits();
        instancedShader.connectTextureUnits();
    }

    public void render(Scene scene, ICamera camera) {
        Vector3f sunPosition = scene.getLightDirection();
        if (sunPosition == null)
            return;

        // 1. Işığın dünyadaki doğrusal yönünü hesapla
        Vector3f lightDirection = new Vector3f(sunPosition.x, sunPosition.y, sunPosition.z);

        // 2. IŞIK KAMERASININ MERKEZİNİ AYARLA:
        // Sabit bir nokta yerine, ana kameranın dünyadaki anlık pozisyonunu merkez
        // alıyoruz.
        Vector3f cameraPos = scene.getCamera().getPosition();

        // 3. MATRİS GÜNCELLEME SIRASI (EN KRİTİK KISIM):
        // Önce ışığın bakış matrisini (Light View), kameranın olduğu konuma göre inşa
        // et.
        updateLightViewMatrix(lightDirection, cameraPos);

        // 4. ŞİMDİ ShadowBox'ı bu yeni oluşturduğumuz güncel matrisle besle.
        // Böylece gölge kutusunun köşeleri tamamen doğru ışık uzayına eşlenir!
        shadowBox.update(lightViewMatrix);

        // --- TEXEL SNAPPING (GÖLGE TİTREMESİNİ DURDURAN KISIM) ---
        float shadowMapSize = (float) SHADOW_MAP_SIZE;
        float worldTexelSize = shadowBox.getWidth() / shadowMapSize;

        float snappedMinX = (float) (Math.floor(shadowBox.getMinX() / worldTexelSize) * worldTexelSize);
        float snappedMaxX = (float) (Math.floor(shadowBox.getMaxX() / worldTexelSize) * worldTexelSize);
        float snappedMinY = (float) (Math.floor(shadowBox.getMinY() / worldTexelSize) * worldTexelSize);
        float snappedMaxY = (float) (Math.floor(shadowBox.getMaxY() / worldTexelSize) * worldTexelSize);
        // ---------------------------------------------------------

        // 5. ShadowBox'tan gelen dinamik ve sıkı sınırlara (tight bounds) göre
        // projeksiyon matrisini güncelle
        updateOrthoProjectionMatrix(snappedMinX, snappedMaxX, snappedMinY, snappedMaxY,
                -shadowBox.getMaxZ(), -shadowBox.getMinZ());

        // 6. İki matrisi çarpıp nihai gölge uzay matrisini oluştur
        /*
         * Matrix4f.mul(projectionMatrix, lightViewMatrix, projectionViewMatrix);
         * shadowFbo.bindFrameBuffer();
         * GL11.glEnable(GL11.GL_DEPTH_TEST);
         * // Gölge çiziminde "Shadow Acne" (kendi kendine gölge yapma titremesi)
         * hatasını
         * // engellemek için
         * // objelerin güneşe bakan (ön) yüzeylerini çizmiyoruz, arka yüzeylerinin
         * // derinliğini alıyoruz.
         * GL11.glEnable(GL11.GL_CULL_FACE);
         * GL11.glCullFace(GL11.GL_FRONT);
         * GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
         */
        Matrix4f.mul(projectionMatrix, lightViewMatrix, projectionViewMatrix);
        shadowFbo.bindFrameBuffer();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        // GL11.glEnable(GL11.GL_VERSION); // Removed invalid state call
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK); // Gölge kopmasını (Peter Panning) engellemek için GL_BACK yapıldı
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

        // --- Render standard and shiny entities ---
        shader.start();

        renderEntities(scene.getAllEntities());
        renderEntities(scene.getUnbatchedFlora());
        renderEntities(scene.getShinyEntities());
        renderTerrains(scene.getTerrains());

        shader.stop();

        // --- Render instanced entities (trees, grass etc.) ---
        if (!scene.getInstancedEntities().isEmpty()) {
            instancedShader.start();
            GL11.glDisable(GL11.GL_CULL_FACE);
            renderInstancedEntities(scene.getInstancedEntities());
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glCullFace(GL11.GL_BACK);
            instancedShader.stop();
        }

        // Normal çizim döngüsünün bozulmaması için yüzey ayıklamasını eski haline getir
        GL11.glCullFace(GL11.GL_BACK);
        GL11.glDisable(GL11.GL_CULL_FACE);
        shadowFbo.unbindFrameBuffer();
    }

    private void renderEntities(List<Entity> entities) {
        for (Entity entity : entities) {
            if (entity.getModel() == null)
                continue;

            // Gölge Haritası Optimizasyonu: Sadece ışığın etki alanındaki objeleri çiz
            if (!shadowBox.isPointInside(entity.getPosition(), entity.getCullingRadius())) {
                continue;
            }

            Model model = entity.getModel();
            Skin skin = entity.getSkin();

            bindModel(model);
            if (skin != null) {
                bindSkin(skin);
            }
            prepareInstance(entity);
            model.getVao().bindIndexBuffer();
            GL11.glDrawElements(GL11.GL_TRIANGLES, model.getVao().getIndexCount(), GL11.GL_UNSIGNED_INT, 0);
            unbindModel(model);
        }
    }

    private void renderInstancedEntities(Map<Model, Map<Skin, List<scene.InstanceData>>> instancedEntities) {
        instancedShader.projectionViewMatrix.loadMatrix(projectionViewMatrix);

        for (Model model : instancedEntities.keySet()) {
            boolean isGrass = false;
            try {
                isGrass = (model == objects.Grass3D.getGrassModel());
            } catch (Throwable t) {}
            
            // OPTIMIZASYON: Cimenlerin (Grass) golge uretmesini engelle. 
            // Cok fazla alpha-discard icerdigi icin shadow map cizimini asiri yavaslatiyor!
            if (isGrass) continue;

            openglObjects.Vao vao = model.getVao();

            // Setup instanced VBO if not already pointing to our VBO (to support switching
            // with main pass VBO)
            instancedVbo.bind();
            vao.addInstancedAttribute(vao.id, instancedVbo.getId(), 3, 4, INSTANCE_DATA_LENGTH, 0);
            vao.addInstancedAttribute(vao.id, instancedVbo.getId(), 4, 4, INSTANCE_DATA_LENGTH, 4);
            vao.addInstancedAttribute(vao.id, instancedVbo.getId(), 5, 4, INSTANCE_DATA_LENGTH, 8);
            vao.addInstancedAttribute(vao.id, instancedVbo.getId(), 6, 4, INSTANCE_DATA_LENGTH, 12);
            vao.addInstancedAttribute(vao.id, instancedVbo.getId(), 7, 1, INSTANCE_DATA_LENGTH, 16);
            instancedVbo.unbind();

            // Bind VAO attributes for position (0), textureCoords (1), and normals (2)
            // Attribute 2 must be enabled even though shadow shader doesn't use normals,
            // because the VAO interleaved layout stores pos(0)/tex(1)/norm(2) and we
            // need the divisor state to be clean for the instanced attributes (3-6).
            vao.bind(0, 1, 2);

            GL20.glEnableVertexAttribArray(3);
            GL20.glEnableVertexAttribArray(4);
            GL20.glEnableVertexAttribArray(5);
            GL20.glEnableVertexAttribArray(6);
            GL20.glEnableVertexAttribArray(7);

            for (Skin skin : instancedEntities.get(model).keySet()) {
                bindSkin(skin);
                instancedShader.numberOfRows.loadFloat((float) skin.getNumberOfRows());

                List<scene.InstanceData> allData = instancedEntities.get(model).get(skin);
                buffer.clear();
                int count = 0;
                int index = 0;

                for (scene.InstanceData data : allData) {
                    if (count >= MAX_INSTANCES) {
                        break;
                    }
                    Matrix4f mat = data.getTransform();
                    
                    // Gölge Haritası Optimizasyonu: Instanced objeleri ele
                    float radius = 10.0f; // Tahmini bir kapsama alanı (ağaç/çimen)
                    // Pozisyon transform matrisinin m30, m31, m32 elemanlarındandır
                    if (!shadowBox.isPointInside(mat.m30, mat.m31, mat.m32, radius)) {
                        continue;
                    }
                    
                    tempArray[index++] = mat.m00;
                    tempArray[index++] = mat.m01;
                    tempArray[index++] = mat.m02;
                    tempArray[index++] = mat.m03;
                    tempArray[index++] = mat.m10;
                    tempArray[index++] = mat.m11;
                    tempArray[index++] = mat.m12;
                    tempArray[index++] = mat.m13;
                    tempArray[index++] = mat.m20;
                    tempArray[index++] = mat.m21;
                    tempArray[index++] = mat.m22;
                    tempArray[index++] = mat.m23;
                    tempArray[index++] = mat.m30;
                    tempArray[index++] = mat.m31;
                    tempArray[index++] = mat.m32;
                    tempArray[index++] = mat.m33;
                    tempArray[index++] = data.getTextureOffsetIndex();
                    count++;
                }

                if (count == 0)
                    continue;

                buffer.put(tempArray, 0, count * INSTANCE_DATA_LENGTH);
                buffer.flip();

                instancedVbo.bind();
                instancedVbo.updateData(buffer);
                vao.bindIndexBuffer();

                GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, vao.getIndexCount(), GL11.GL_UNSIGNED_INT, 0, count);
            }

            GL20.glDisableVertexAttribArray(3);
            GL20.glDisableVertexAttribArray(4);
            GL20.glDisableVertexAttribArray(5);
            GL20.glDisableVertexAttribArray(6);
            GL20.glDisableVertexAttribArray(7);

            instancedVbo.unbind();
            vao.unbind(0, 1, 2);
        }
    }

    private void renderTerrains(List<terrain.ITerrain> terrains) {
        for (terrain.ITerrain terrain : terrains) {
            if (terrain instanceof terrain.flat.FlatTerrain) {
                terrain.flat.FlatTerrain ft = (terrain.flat.FlatTerrain) terrain;
                terrainShader.start();
                terrainShader.projectionViewMatrix.loadMatrix(projectionViewMatrix);

                Matrix4f transformationMatrix = new Matrix4f();
                transformationMatrix.setIdentity();

                // --- FIX: Apply the same transformation as in FlatTerrain.render ---
                if (ft.isInfinite()) {
                    // This logic matches FlatTerrain.java lines 660-670
                    utils.ICamera camera = shadowBox.getCamera();
                    float camX = camera.getPosition().x;
                    float camZ = camera.getPosition().z;
                    float vertexSpacingX = ft.getWidth() / ft.getGridCount();
                    float vertexSpacingZ = ft.getDepth() / ft.getGridCount();

                    float gridSnapX = (float) Math.floor(camX / vertexSpacingX) * vertexSpacingX;
                    float gridSnapZ = (float) Math.floor(camZ / vertexSpacingZ) * vertexSpacingZ;

                    Matrix4f.translate(new Vector3f(gridSnapX, 0, gridSnapZ), transformationMatrix,
                            transformationMatrix);
                } else {
                    Matrix4f.translate(new Vector3f(0, ft.getBaseHeight(), 0), transformationMatrix,
                            transformationMatrix);
                }

                terrainShader.modelMatrix.loadMatrix(transformationMatrix);

                // Load terrain displacement uniforms
                terrainShader.uMaxHeight.loadFloat(ft.getMaxHeight());
                terrainShader.uRoughness.loadFloat(ft.getRoughness());
                terrainShader.uOctaves.loadInt(ft.getOctaves());
                terrainShader.uScale.loadFloat(ft.getScale());
                terrainShader.uOffsetX.loadFloat(ft.getOffsetX());
                terrainShader.uOffsetZ.loadFloat(ft.getOffsetZ());
                terrainShader.uBaseHeight.loadFloat(ft.getBaseHeight());
                terrainShader.uInfinite.loadFloat(ft.isInfinite() ? 1.0f : 0.0f);

                GL30.glBindVertexArray(terrain.getVaoId());
                GL20.glEnableVertexAttribArray(0);
                if (terrain.getIndexCount() == 0) continue;
                org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER, terrain.getIndexVboId());
                GL11.glDrawElements(GL11.GL_TRIANGLES, terrain.getIndexCount(), GL11.GL_UNSIGNED_INT, 0);
                GL20.glDisableVertexAttribArray(0);
                GL30.glBindVertexArray(0);
                terrainShader.stop();
                shader.start();
            } else {
                GL30.glBindVertexArray(terrain.getVaoId());
                GL20.glEnableVertexAttribArray(0);
                GL20.glEnableVertexAttribArray(1);

                Matrix4f transformationMatrix = new Matrix4f();
                transformationMatrix.setIdentity();

                if (terrain instanceof PlanetTerrain) {
                    transformationMatrix.translate(((PlanetTerrain) terrain).getPosition());
                    org.lwjgl.util.vector.Vector3f rot = ((PlanetTerrain) terrain).getRotation();
                    transformationMatrix.rotate((float) Math.toRadians(rot.y), new Vector3f(0, 1, 0));
                    transformationMatrix.rotate((float) Math.toRadians(rot.x), new Vector3f(1, 0, 0));
                    transformationMatrix.rotate((float) Math.toRadians(rot.z), new Vector3f(0, 0, 1));
                } else if (terrain instanceof VoxelTerrain) {
                    VoxelTerrain vt = (VoxelTerrain) terrain;
                    transformationMatrix.translate(new Vector3f(vt.getOriginX(), vt.getOriginY(), vt.getOriginZ()));
                }

                Matrix4f mvpMatrix = new Matrix4f();
                Matrix4f.mul(projectionViewMatrix, transformationMatrix, mvpMatrix);
                shader.mvpMatrix.loadMatrix(mvpMatrix);

                if (terrain.getIndexCount() == 0) continue;
                org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER, terrain.getIndexVboId());
                GL11.glDrawElements(GL11.GL_TRIANGLES, terrain.getIndexCount(), GL11.GL_UNSIGNED_INT, 0);

                GL20.glDisableVertexAttribArray(0);
                GL20.glDisableVertexAttribArray(1);
                GL30.glBindVertexArray(0);
            }
        }
    }

    private void bindModel(Model model) {
        GL30.glBindVertexArray(model.getVao().id);
        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1); // Texture coords for alpha testing
    }

    private void bindSkin(Skin skin) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, skin.getDiffuseTexture().textureId);
    }

    private void unbindModel(Model model) {
        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL30.glBindVertexArray(0);
    }

    private void prepareInstance(Entity entity) {
        Matrix4f transform = new Matrix4f();
        transform.setIdentity();
        Matrix4f.translate(entity.getPosition(), transform, transform);
        if (entity.getRotation().x != 0)
            Matrix4f.rotate((float) Math.toRadians(entity.getRotation().x), new Vector3f(1, 0, 0), transform,
                    transform);
        if (entity.getRotation().y != 0)
            Matrix4f.rotate((float) Math.toRadians(entity.getRotation().y), new Vector3f(0, 1, 0), transform,
                    transform);
        if (entity.getRotation().z != 0)
            Matrix4f.rotate((float) Math.toRadians(entity.getRotation().z), new Vector3f(0, 0, 1), transform,
                    transform);
        if (entity.getScale() != 1.0f)
            Matrix4f.scale(new Vector3f(entity.getScale(), entity.getScale(), entity.getScale()), transform, transform);

        Matrix4f mvpMatrix = new Matrix4f();
        Matrix4f.mul(projectionViewMatrix, transform, mvpMatrix);
        shader.mvpMatrix.loadMatrix(mvpMatrix);
    }

    /**
     * Creates the orthographic projection matrix for the light using general
     * bounds.
     */
    private void updateOrthoProjectionMatrix(float left, float right, float bottom, float top, float near, float far) {
        projectionMatrix.setIdentity();
        projectionMatrix.m00 = 2f / (right - left);
        projectionMatrix.m11 = 2f / (top - bottom);
        projectionMatrix.m22 = -2f / (far - near);
        projectionMatrix.m30 = -(right + left) / (right - left);
        projectionMatrix.m31 = -(top + bottom) / (top - bottom);
        projectionMatrix.m32 = -(far + near) / (far - near);
        projectionMatrix.m33 = 1f;
    }

    private void updateLightViewMatrix(Vector3f direction, Vector3f center) {
        direction.normalise();

        Vector3f back = new Vector3f(-direction.x, -direction.y, -direction.z);
        back.normalise();

        Vector3f up = new Vector3f(0, 1, 0);
        if (Math.abs(back.x) < 0.0001f && Math.abs(back.z) < 0.0001f) {
            up.set(0, 0, back.y > 0 ? 1 : -1);
        }

        Vector3f right = Vector3f.cross(up, back, null);
        right.normalise();

        Vector3f actualUp = Vector3f.cross(back, right, null);
        actualUp.normalise();

        lightViewMatrix.setIdentity();

        // LWJGL2'de matris notasyonu: m[sutun][satir]
        // Orijinal View Matrix column assignment (ShadowBox ile uyumlu):
        // Sutun 0: right.x, right.y, right.z
        // Sutun 1: up.x, up.y, up.z
        // Sutun 2: back.x, back.y, back.z
        lightViewMatrix.m00 = right.x;
        lightViewMatrix.m01 = actualUp.x;
        lightViewMatrix.m02 = back.x;

        lightViewMatrix.m10 = right.y;
        lightViewMatrix.m11 = actualUp.y;
        lightViewMatrix.m12 = back.y;

        lightViewMatrix.m20 = right.z;
        lightViewMatrix.m21 = actualUp.z;
        lightViewMatrix.m22 = back.z;

        Vector3f negativeCenter = new Vector3f(-center.x, -center.y, -center.z);
        Matrix4f.translate(negativeCenter, lightViewMatrix, lightViewMatrix);
    }

    public Matrix4f getToShadowMapSpaceMatrix() {
        Matrix4f m = new Matrix4f();
        Matrix4f.mul(offset, projectionViewMatrix, m);
        return m;
    }

    public int getShadowMap() {
        return shadowFbo.getShadowMap();
    }

    public ShadowBox getShadowBox() {
        return shadowBox;
    }

    public void cleanUp() {
        shader.cleanUp();
        instancedShader.cleanUp();
        instancedVbo.delete();
        shadowFbo.cleanUp();
    }

    /**
     * Creates a matrix that offsets the coordinates from [-1, 1] to [0, 1] so they
     * can be used to sample the shadow map texture.
     */
    private static Matrix4f createOffset() {
        Matrix4f offset = new Matrix4f();
        offset.translate(new Vector3f(0.5f, 0.5f, 0.5f));
        offset.scale(new Vector3f(0.5f, 0.5f, 0.5f));
        return offset;
    }
}
