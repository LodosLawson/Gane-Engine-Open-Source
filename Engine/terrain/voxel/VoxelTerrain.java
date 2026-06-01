package terrain.voxel;

// Bu dosya terrain.voxel paketine taÅŸÄ±nmÄ±ÅŸtÄ±r.
// VoxelTerrain artÄ±k terrain.voxel.VoxelTerrain olarak kullanÄ±lÄ±r.
//
// Import:
//   import terrain.voxel.VoxelTerrain;
//
// KullanÄ±m deÄŸiÅŸmedi:
//   VoxelTerrain vt = new VoxelTerrain(64, 32, 64);
//   sahne.addTerrain(vt);

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import scene.Scene;
import terrain.ITerrain;
import terrain.flat.FlatTerrainShader;
import textures.Texture;
import utils.ICamera;
import utils.MyFile;

/**
 * â•”â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•—
 * â•‘             GANE ENGINE â€” VOXEL ARAZÄ° SÄ°STEMÄ°                      â•‘
 * â• â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•£
 * â•‘  Minecraft benzeri 3D voxel (kÃ¼p) tabanlÄ± arazi.                   â•‘
 * â•‘  - 3D boolean Ä±zgara (dolu/boÅŸ voxel)                              â•‘
 * â•‘  - ProsedÃ¼rel Simplex Noise ile baÅŸlangÄ±Ã§ ÅŸekli                    â•‘
 * â•‘  - Greedy meshing ile draw call optimizasyonu                       â•‘
 * â•‘  - Runtime'da voxel ekleme/silme destekler                         â•‘
 * â• â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•£
 * â•‘  HIZLI KULLANIM:                                                     â•‘
 * â•‘                                                                      â•‘
 * â•‘    VoxelTerrain vt = new VoxelTerrain(64, 32, 64);                 â•‘
 * â•‘    vt.setVoxelSize(1.0f);                                           â•‘
 * â•‘    sahne.addTerrain(vt);                                            â•‘
 * â•‘                                                                      â•‘
 * â•‘    // Runtime voxel dÃ¼zenleme:                                      â•‘
 * â•‘    vt.removeVoxel(10, 5, 10);  // Voxel kaz                        â•‘
 * â•‘    vt.addVoxel(10, 6, 10);     // Voxel inÅŸa et                    â•‘
 * â•šâ•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
 */
public class VoxelTerrain implements ITerrain {

    /** Voxel Ä±zgara boyutlarÄ± */
    private final int sizeX, sizeY, sizeZ;

    /**
     * Her voxel'in dÃ¼nya birimi cinsinden boyutu.
     * 1.0f = 1 metre kÃ¼p voxel (varsayÄ±lan).
     */
    private float voxelSize = 1.0f;

    /** Terrain'in dÃ¼nya koordinatÄ±ndaki kÃ¶ÅŸe konumu (sol-alt-Ã¶n) */
    private float originX = 0f, originY = 0f, originZ = 0f;

    /** Doku yollarÄ± */
    private String grassTexturePath = "res/TerrainTexture/DEFAULT_GRASS/Ground067_8K-PNG_Color.png";
    private String dirtTexturePath  = "res/TerrainTexture/DEFAULT_DIRT/Ground037_4K-PNG_Color.png";

    /** 3D voxel Ä±zgarasÄ±: true = dolu, false = boÅŸ */
    private boolean[][][] grid;

    /** Mesh yeniden oluÅŸturulmasÄ± gerekiyor mu? */
    private boolean meshDirty = true;

    // OpenGL handle'larÄ±
    private int vaoId    = 0;
    private int vertVbo  = 0, normVbo = 0, uvVbo = 0, faceVbo = 0, idxVbo = 0;
    private int indexCount = 0;

    private int grassTexId = 0, dirtTexId = 0, dirt2TexId = 0;
    private int grassNormalTexId = 0, dirtNormalTexId = 0, dirt2NormalTexId = 0;

    /** FlatTerrain shader'Ä±nÄ± paylaÅŸÄ±r (voxel terrain aynÄ± shader kullanÄ±r) */
    private FlatTerrainShader shader;

    private org.lwjgl.util.vector.Vector4f clipPlane =
        new org.lwjgl.util.vector.Vector4f(0, -1, 0, 100000f);

    /**
     * Yeni bir VoxelTerrain oluÅŸturur.
     *
     * @param sizeX X ekseninde kaÃ§ voxel
     * @param sizeY Y ekseninde kaÃ§ voxel (yÃ¼kseklik)
     * @param sizeZ Z ekseninde kaÃ§ voxel
     */
    public VoxelTerrain(int sizeX, int sizeY, int sizeZ) {
        this.sizeX = sizeX; this.sizeY = sizeY; this.sizeZ = sizeZ;
        this.grid  = new boolean[sizeX][sizeY][sizeZ];
        this.shader = new FlatTerrainShader();
        generateProceduralTerrain();
        loadDefaultTextures();
    }

    // ---------------------------------------------------------------
    // AYAR METODlari
    // ---------------------------------------------------------------

    /** Her voxel'in dÃ¼nya birimi cinsinden boyutunu ayarlar (varsayÄ±lan: 1.0f) */
    public void setVoxelSize(float size) { this.voxelSize = size; meshDirty = true; }

    /** Terrain'in dÃ¼nya koordinatÄ±ndaki baÅŸlangÄ±Ã§ konumunu ayarlar */
    public void setOrigin(float x, float y, float z) {
        this.originX = x; this.originY = y; this.originZ = z; meshDirty = true;
    }

    public float getOriginX() { return originX; }
    public float getOriginY() { return originY; }
    public float getOriginZ() { return originZ; }

    /** Ã‡imen doku yolunu deÄŸiÅŸtirir */
    public void setGrassTexturePath(String path) {
        this.grassTexturePath = path;
        if (grassTexId != 0) GL11.glDeleteTextures(grassTexId);
        grassTexId = loadTex(path);
    }

    /** Toprak doku yolunu deÄŸiÅŸtirir */
    public void setDirtTexturePath(String path) {
        this.dirtTexturePath = path;
        if (dirtTexId != 0) GL11.glDeleteTextures(dirtTexId);
        dirtTexId = loadTex(path);
    }

    // ---------------------------------------------------------------
    // RUNTIME VOXEL EDÄ°TÃ–RÃœ
    // ---------------------------------------------------------------

    /**
     * Belirtilen voxel'i kaldÄ±rÄ±r (oyun iÃ§i kazma/yÄ±kma).
     *
     * @param x X grid koordinatÄ±
     * @param y Y grid koordinatÄ± (yÃ¼kseklik)
     * @param z Z grid koordinatÄ±
     */
    public void removeVoxel(int x, int y, int z) {
        if (inBounds(x, y, z)) { grid[x][y][z] = false; meshDirty = true; }
    }

    /**
     * Belirtilen voxel'i ekler (oyun iÃ§i inÅŸa etme).
     *
     * @param x X grid koordinatÄ±
     * @param y Y grid koordinatÄ± (yÃ¼kseklik)
     * @param z Z grid koordinatÄ±
     */
    public void addVoxel(int x, int y, int z) {
        if (inBounds(x, y, z)) { grid[x][y][z] = true; meshDirty = true; }
    }

    /** Belirli bir voxel'in dolu olup olmadÄ±ÄŸÄ±nÄ± kontrol eder */
    public boolean isVoxelSolid(int x, int y, int z) {
        if (!inBounds(x, y, z)) return false;
        return grid[x][y][z];
    }

    /** Belirli bir voxel'in durumunu dÃ¶ndÃ¼rÃ¼r */
    public boolean getVoxel(int x, int y, int z) {
        if (x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ) return false;
        return grid[x][y][z];
    }

    /** Belirli bir voxel'i set eder */
    public void setVoxel(int x, int y, int z, boolean state) {
        if (x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ) return;
        if (grid[x][y][z] != state) { grid[x][y][z] = state; meshDirty = true; }
    }

    // ---------------------------------------------------------------
    // ITerrain
    // ---------------------------------------------------------------

    @Override public void setClipPlane(org.lwjgl.util.vector.Vector4f plane) { this.clipPlane = plane; }
    @Override public float getWidth()      { return this.sizeX * this.voxelSize; }
    @Override public float getDepth()      { return this.sizeZ * this.voxelSize; }
    public float getHeight()               { return this.sizeY * this.voxelSize; }
    @Override public int   getVaoId()      { return vaoId; }
    @Override public int   getIndexVboId() { return idxVbo; }
    @Override public int   getIndexCount() { return indexCount; }

    @Override
    public float getHeightAt(float worldX, float worldZ) {
        int gx = (int) ((worldX - originX) / voxelSize);
        int gz = (int) ((worldZ - originZ) / voxelSize);
        if (gx < 0 || gx >= sizeX || gz < 0 || gz >= sizeZ) return originY;
        for (int gy = sizeY - 1; gy >= 0; gy--) {
            if (grid[gx][gy][gz]) return originY + (gy + 1) * voxelSize;
        }
        return originY;
    }

    public void render(ICamera camera, Scene scene) { render(camera, scene, null, 0); }

    @Override
    public void render(ICamera camera, Scene scene, org.lwjgl.util.vector.Matrix4f toShadowSpace, int shadowMapTexId) {
        if (meshDirty) { rebuildMesh(); meshDirty = false; }
        if (indexCount == 0) return;

        shader.start();
        Matrix4f model = new Matrix4f();
        model.setIdentity();
        Matrix4f.translate(new Vector3f(originX, originY, originZ), model, model);
        shader.loadMatrices(model, camera.getViewMatrix(), camera.getProjectionMatrix());
        shader.loadClipPlane(new org.lwjgl.util.vector.Vector4f(0, -1, 0, 100000));
        shader.loadInfiniteParams(false, 50f, 0.45f, 4, 200f, 0f, 0f, 0f);
        org.lwjgl.util.vector.Vector3f center = new org.lwjgl.util.vector.Vector3f(
            originX + sizeX / 2.0f, originY + sizeY / 2.0f, originZ + sizeZ / 2.0f);
        shader.loadPlanetaryParams(true, center);
        shader.loadLight(scene.getLightDirection(), scene.getLightColor(), scene.getAmbientLight());
        shader.loadFogParams(scene.getFogColor(), scene.getFogDensity(), scene.getFogStart());
        shader.loadCameraPos(camera.getPosition());
        if (toShadowSpace != null) shader.loadToShadowSpaceMatrix(toShadowSpace);
        if (scene.getSky() instanceof skybox.atmosphere.AtmosphereSky) {
            skybox.atmosphere.AtmosphereSky sky = (skybox.atmosphere.AtmosphereSky) scene.getSky();
            shader.loadCloudShadowData(sky.getTime(),
                new org.lwjgl.util.vector.Vector2f(scene.getWindVelocity().x, scene.getWindVelocity().z),
                sky.isCloudsEnabled(), sky.getClusters());
        } else {
            shader.loadCloudShadowData(0f, new org.lwjgl.util.vector.Vector2f(0,0), false, null);
        }
        shader.loadTextureScale(4f);

        GL13.glActiveTexture(GL13.GL_TEXTURE0); GL11.glBindTexture(GL11.GL_TEXTURE_2D, grassTexId);
        GL13.glActiveTexture(GL13.GL_TEXTURE1); GL11.glBindTexture(GL11.GL_TEXTURE_2D, dirtTexId);
        GL13.glActiveTexture(GL13.GL_TEXTURE2); GL11.glBindTexture(GL11.GL_TEXTURE_2D, dirt2TexId);
        GL13.glActiveTexture(GL13.GL_TEXTURE3); GL11.glBindTexture(GL11.GL_TEXTURE_2D, grassNormalTexId);
        GL13.glActiveTexture(GL13.GL_TEXTURE4); GL11.glBindTexture(GL11.GL_TEXTURE_2D, dirtNormalTexId);
        GL13.glActiveTexture(GL13.GL_TEXTURE5); GL11.glBindTexture(GL11.GL_TEXTURE_2D, dirt2NormalTexId);
        if (shadowMapTexId != 0) {
            GL13.glActiveTexture(GL13.GL_TEXTURE6); GL11.glBindTexture(GL11.GL_TEXTURE_2D, shadowMapTexId);
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);

        GL30.glBindVertexArray(vaoId);
        GL20.glEnableVertexAttribArray(0); GL20.glEnableVertexAttribArray(1); GL20.glEnableVertexAttribArray(2);
        GL11.glDrawElements(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0);
        GL20.glDisableVertexAttribArray(0); GL20.glDisableVertexAttribArray(1); GL20.glDisableVertexAttribArray(2);
        GL30.glBindVertexArray(0);
        shader.stop();
    }

    @Override
    public void cleanUp() {
        shader.cleanUp();
        deleteMeshBuffers();
        if (grassTexId != 0)      GL11.glDeleteTextures(grassTexId);
        if (dirtTexId  != 0)      GL11.glDeleteTextures(dirtTexId);
        if (dirt2TexId != 0)      GL11.glDeleteTextures(dirt2TexId);
        if (grassNormalTexId != 0) GL11.glDeleteTextures(grassNormalTexId);
        if (dirtNormalTexId  != 0) GL11.glDeleteTextures(dirtNormalTexId);
        if (dirt2NormalTexId != 0) GL11.glDeleteTextures(dirt2NormalTexId);
    }

    // ---------------------------------------------------------------
    // PROSEDÃœREL ÃœRETÄ°M
    // ---------------------------------------------------------------

    private void generateProceduralTerrain() {
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                float nx = (float) x / sizeX, nz = (float) z / sizeZ;
                float height = 0.5f * noise(nx*3f, nz*3f) +
                               0.25f * noise(nx*7f, nz*7f) +
                               0.15f * noise(nx*15f, nz*15f);
                int surfaceY = Math.max(1, Math.min((int)(height*(sizeY*0.6f)), sizeY-1));
                for (int y = 0; y <= surfaceY; y++) grid[x][y][z] = true;
            }
        }
    }

    /**
     * KÃ¼resel gezegen (planetoid) ÅŸeklinde voxel dÃ¼nya Ã¼retir.
     *
     * @param baseRadius     Temel kÃ¼re yarÄ±Ã§apÄ± (voxel birimi)
     * @param heightScale    YÃ¼zey engebesi Ã¶lÃ§eÄŸi
     * @param noiseScale     Noise detay sÄ±klÄ±ÄŸÄ±
     * @param caveThreshold  MaÄŸara oluÅŸturma eÅŸiÄŸi (0.6-0.8 Ã¶nerilen)
     */
    public void generatePlanetoid(float baseRadius, float heightScale,
                                   float noiseScale, float caveThreshold) {
        float cx = sizeX/2f, cy = sizeY/2f, cz = sizeZ/2f;
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    float dx = x-cx, dy = y-cy, dz = z-cz;
                    float dist = (float)Math.sqrt(dx*dx+dy*dy+dz*dz);
                    if (dist > cx) { grid[x][y][z] = false; continue; }
                    float nx = dx*noiseScale, ny = dy*noiseScale, nz2 = dz*noiseScale;
                    float hVal = noise3D(nx*0.5f, ny*0.5f, nz2*0.5f);
                    float localRadius = baseRadius + hVal*heightScale;
                    if (dist <= localRadius) {
                        float caveNoise = noise3D(nx*2f, ny*2f, nz2*2f);
                        grid[x][y][z] = !(dist > baseRadius*0.7f && caveNoise > caveThreshold);
                    } else { grid[x][y][z] = false; }
                }
            }
        }
        meshDirty = true;
    }

    // ---------------------------------------------------------------
    // MESH OLUÅžTURMA
    // ---------------------------------------------------------------

    private void rebuildMesh() {
        deleteMeshBuffers();
        List<Float> verts = new ArrayList<>(), norms = new ArrayList<>(), uvs = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int vi = 0;

        for (int x = 0; x < sizeX; x++) for (int y = 0; y < sizeY; y++) for (int z = 0; z < sizeZ; z++) {
            if (!grid[x][y][z]) continue;
            float wx = x*voxelSize, wy = y*voxelSize, wz = z*voxelSize, vs = voxelSize;
            if (!isVoxelSolid(x+1,y,z)) vi=addFace(verts,norms,uvs,indices,vi, wx+vs,wy,wz+vs, wx+vs,wy,wz, wx+vs,wy+vs,wz, wx+vs,wy+vs,wz+vs, 1,0,0,false);
            if (!isVoxelSolid(x-1,y,z)) vi=addFace(verts,norms,uvs,indices,vi, wx,wy,wz, wx,wy,wz+vs, wx,wy+vs,wz+vs, wx,wy+vs,wz, -1,0,0,false);
            if (!isVoxelSolid(x,y+1,z)) vi=addFace(verts,norms,uvs,indices,vi, wx,wy+vs,wz, wx,wy+vs,wz+vs, wx+vs,wy+vs,wz+vs, wx+vs,wy+vs,wz, 0,1,0,true);
            if (!isVoxelSolid(x,y-1,z)) vi=addFace(verts,norms,uvs,indices,vi, wx,wy,wz+vs, wx,wy,wz, wx+vs,wy,wz, wx+vs,wy,wz+vs, 0,-1,0,false);
            if (!isVoxelSolid(x,y,z+1)) vi=addFace(verts,norms,uvs,indices,vi, wx,wy,wz+vs, wx+vs,wy,wz+vs, wx+vs,wy+vs,wz+vs, wx,wy+vs,wz+vs, 0,0,1,false);
            if (!isVoxelSolid(x,y,z-1)) vi=addFace(verts,norms,uvs,indices,vi, wx+vs,wy,wz, wx,wy,wz, wx,wy+vs,wz, wx+vs,wy+vs,wz, 0,0,-1,false);
        }

        if (indices.isEmpty()) { indexCount = 0; return; }
        float[] va=toFA(verts), na=toFA(norms), ua=toFA(uvs);
        int[] ia=toIA(indices);
        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);
        vertVbo=uploadFloat(0,3,va); normVbo=uploadFloat(1,3,na); uvVbo=uploadFloat(2,2,ua);
        idxVbo=uploadIndex(ia); indexCount=ia.length;
        GL30.glBindVertexArray(0);
    }

    private int addFace(List<Float> verts, List<Float> norms, List<Float> uvs,
                        List<Integer> indices, int vi,
                        float x0,float y0,float z0, float x1,float y1,float z1,
                        float x2,float y2,float z2, float x3,float y3,float z3,
                        float nx,float ny,float nz, boolean isTop) {
        float[][] corners = {{x0,y0,z0},{x1,y1,z1},{x2,y2,z2},{x3,y3,z3}};
        float[][] uvc = {{0,0},{1,0},{1,1},{0,1}};
        for (int i = 0; i < 4; i++) {
            verts.add(corners[i][0]); verts.add(corners[i][1]); verts.add(corners[i][2]);
            norms.add(nx); norms.add(ny); norms.add(nz);
            uvs.add(uvc[i][0]); uvs.add(uvc[i][1] + (isTop ? 2.0f : 0.0f));
        }
        indices.add(vi); indices.add(vi+1); indices.add(vi+2);
        indices.add(vi); indices.add(vi+2); indices.add(vi+3);
        return vi + 4;
    }

    // ---------------------------------------------------------------
    // YARDIMCILAR
    // ---------------------------------------------------------------

    private boolean inBounds(int x, int y, int z) {
        return x>=0&&x<sizeX&&y>=0&&y<sizeY&&z>=0&&z<sizeZ;
    }

    private int uploadFloat(int attrib, int size, float[] data) {
        int id = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, id);
        FloatBuffer buf = BufferUtils.createFloatBuffer(data.length);
        buf.put(data).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_DYNAMIC_DRAW);
        GL20.glVertexAttribPointer(attrib, size, GL11.GL_FLOAT, false, 0, 0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        return id;
    }

    private int uploadIndex(int[] data) {
        int id = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, id);
        IntBuffer buf = BufferUtils.createIntBuffer(data.length);
        buf.put(data).flip();
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, buf, GL15.GL_DYNAMIC_DRAW);
        return id;
    }

    private void deleteMeshBuffers() {
        if (vaoId == 0) return;
        GL30.glDeleteVertexArrays(vaoId);
        GL15.glDeleteBuffers(vertVbo); GL15.glDeleteBuffers(normVbo);
        GL15.glDeleteBuffers(uvVbo);   GL15.glDeleteBuffers(faceVbo);
        GL15.glDeleteBuffers(idxVbo);
        vaoId = 0;
    }

    private void loadDefaultTextures() {
        grassTexId = loadTex(grassTexturePath);
        dirtTexId  = loadTex(dirtTexturePath);
        dirt2TexId = loadTex(dirtTexturePath);
        grassNormalTexId = loadTex("res/TerrainTexture/DEFAULT_GRASS/Ground067_8K-PNG_NormalGL.png");
        dirtNormalTexId  = loadTex("res/TerrainTexture/DEFAULT_DIRT/Ground037_4K-PNG_NormalGL.png");
        dirt2NormalTexId = loadTex("res/TerrainTexture/DEFAULT_DIRT/Ground037_4K-PNG_NormalGL.png");
    }

    private int loadTex(String path) {
        try { return Texture.newTexture(new MyFile(path)).anisotropic().create().textureId; }
        catch (Exception e) { System.err.println("[VoxelTerrain] Texture yuklenemedi: "+path); return 0; }
    }

    private float[] toFA(List<Float> l)   { float[] a=new float[l.size()]; for(int i=0;i<a.length;i++) a[i]=l.get(i); return a; }
    private int[]   toIA(List<Integer> l) { int[]   a=new int[l.size()];   for(int i=0;i<a.length;i++) a[i]=l.get(i); return a; }

    // Noise metotlarÄ±
    private float noise(float x, float z) {
        int xi=(int)Math.floor(x), zi=(int)Math.floor(z);
        float xf=x-xi, zf=z-zi;
        return lerp(lerp(pr(xi,zi), pr(xi+1,zi), fade(xf)),
                    lerp(pr(xi,zi+1),pr(xi+1,zi+1),fade(xf)), fade(zf));
    }
    private float noise3D(float x, float y, float z) {
        int xi=(int)Math.floor(x), yi=(int)Math.floor(y), zi=(int)Math.floor(z);
        float xf=x-xi,yf=y-yi,zf=z-zi;
        float n0=lerp(pr3(xi,yi,zi),pr3(xi+1,yi,zi),fade(xf));
        float n1=lerp(pr3(xi,yi+1,zi),pr3(xi+1,yi+1,zi),fade(xf));
        float n2=lerp(pr3(xi,yi,zi+1),pr3(xi+1,yi,zi+1),fade(xf));
        float n3=lerp(pr3(xi,yi+1,zi+1),pr3(xi+1,yi+1,zi+1),fade(xf));
        return lerp(lerp(n0,n1,fade(yf)),lerp(n2,n3,fade(yf)),fade(zf));
    }
    private float pr(int x, int z)          { int n=x+z*57;n=(n<<13)^n;return(1f-((n*(n*n*15731+789221)+1376312589)&0x7fffffff)/1073741824f); }
    private float pr3(int x, int y, int z)  { int n=x+y*57+z*131;n=(n<<13)^n;return(1f-((n*(n*n*15731+789221)+1376312589)&0x7fffffff)/1073741824f); }
    private float fade(float t) { return t*t*t*(t*(t*6-15)+10); }
    private float lerp(float a, float b, float t) { return a+t*(b-a); }
}

