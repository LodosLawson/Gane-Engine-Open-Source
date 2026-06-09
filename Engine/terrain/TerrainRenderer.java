package terrain;

import scene.Scene;
import utils.ICamera;
import utils.Frustum;

/**
 * Sahnedeki tüm ITerrain nesnelerini sırayla çizen yönetici sınıf.
 * MasterRenderer tarafından ana çizim aşamasında (renderMainPass) çağrılır.
 *
 * Terrain entity'lerden önce, okyanustan önce çizilir (depth order).
 */
public class TerrainRenderer {

    private final Frustum frustum = new Frustum();

    /**
     * Sahnedeki tüm terrain'leri çizer.
     *
     * @param scene  Aktif sahne (terrain listesine erişmek için)
     * @param camera Aktif kamera
     */
    public void render(Scene scene, ICamera camera, org.lwjgl.util.vector.Matrix4f toShadowSpace, int shadowMapTexId) {
        if (scene.getTerrains().isEmpty())
            return;

        if (scene.isFrustumCullingEnabled()) {
            frustum.update(camera.getProjectionViewMatrix());
        }

        for (ITerrain terrain : scene.getTerrains()) {
            if (scene.isFrustumCullingEnabled()) {
                if (terrain instanceof terrain.flat.FlatTerrain) {
                    terrain.flat.FlatTerrain ft = (terrain.flat.FlatTerrain) terrain;
                    float width = ft.getWidth();
                    float depth = ft.getDepth();
                    float baseHeight = ft.getBaseHeight();
                    float maxHeight = ft.getMaxHeight();
                    
                    float minX, maxX, minZ, maxZ;
                    if (ft.isInfinite()) {
                        float camX = camera.getPosition().x;
                        float camZ = camera.getPosition().z;
                        float vspX = width / ft.getGridCount();
                        float vspZ = depth / ft.getGridCount();
                        float snapX = (float) Math.floor(camX / vspX) * vspX;
                        float snapZ = (float) Math.floor(camZ / vspZ) * vspZ;
                        
                        minX = snapX - width * 0.5f;
                        maxX = snapX + width * 0.5f;
                        minZ = snapZ - depth * 0.5f;
                        maxZ = snapZ + depth * 0.5f;
                    } else {
                        minX = -width * 0.5f;
                        maxX = width * 0.5f;
                        minZ = -depth * 0.5f;
                        maxZ = depth * 0.5f;
                    }
                    
                    float minY = baseHeight;
                    float maxY = baseHeight + maxHeight;
                    
                    if (!frustum.isBoxInside(minX, minY, minZ, maxX, maxY, maxZ)) {
                        continue;
                    }
                } else if (terrain instanceof terrain.voxel.VoxelTerrain) {
                    terrain.voxel.VoxelTerrain vt = (terrain.voxel.VoxelTerrain) terrain;
                    float minX = vt.getOriginX();
                    float maxX = minX + vt.getWidth();
                    float minY = vt.getOriginY();
                    float maxY = minY + vt.getHeight();
                    float minZ = vt.getOriginZ();
                    float maxZ = minZ + vt.getDepth();
                    
                    if (!frustum.isBoxInside(minX, minY, minZ, maxX, maxY, maxZ)) {
                        continue;
                    }
                } else if (terrain instanceof terrain.planet.PlanetTerrain) {
                    terrain.planet.PlanetTerrain pt = (terrain.planet.PlanetTerrain) terrain;
                    float radius = pt.getBaseRadius();
                    float padding = radius * 0.5f;
                    org.lwjgl.util.vector.Vector3f pos = pt.getPosition();
                    float minX = pos.x - radius - padding;
                    float maxX = pos.x + radius + padding;
                    float minY = pos.y - radius - padding;
                    float maxY = pos.y + radius + padding;
                    float minZ = pos.z - radius - padding;
                    float maxZ = pos.z + radius + padding;
                    
                    if (!frustum.isBoxInside(minX, minY, minZ, maxX, maxY, maxZ)) {
                        continue;
                    }
                }
            }
            if (terrain.getIndexCount() == 0) continue;
            org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER, terrain.getIndexVboId());
            terrain.render(camera, scene, toShadowSpace, shadowMapTexId);
        }
    }

    /**
     * TerrainRenderer'ın OpenGL kaynaklarını serbest bırakır.
     * Terrain'lerin kendi cleanUp()'ları Scene.delete() içinde çağrılır,
     * bu yüzden burada sadece renderer'a özel temizlik yapılır.
     */
    public void cleanUp() {
        // Terrain kaynakları Scene.delete() içinde ITerrain.cleanUp() üzerinden
        // temizlenir.
    }
}
