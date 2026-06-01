package terrain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import scene.Scene;
import scene.Model;
import scene.Skin;
import objects.Grass3D;
import objects.Tree3D;
import terrain.flat.FlatTerrain;

/**
 * Dinamik, chunk-tabanlı çimen ve ağaç (flora) yönetim sınıfı.
 * Kameranın etrafındaki aktif chunk'ları tespit edip deterministik olarak
 * flora üretir ve sahnede instanced render listesine ekler.
 */
public class FloraManager {

    public static boolean useBatching = true;
    private static final float CHUNK_SIZE = 50.0f;
    private static int grassRadius = 3; 
    private static int treeRadius = 4;  // Önceden 6 idi, 81 chunk'a indirdik.

    public static void setRenderDistanceScale(float scale) {
        grassRadius = Math.max(1, Math.round(3.0f * scale));
        treeRadius = Math.max(2, Math.round(4.0f * scale));
    }

    private static final Map<Long, FloraChunk> chunkCache = new HashMap<>();
    private static FlatTerrain lastTerrain = null;
    private static long lastSeed = -1;
    private static float lastMaxHeight = -1;
    private static float lastRoughness = -1;
    private static int lastOctaves = -1;
    private static float lastScale = -1;
    private static final utils.Frustum frustum = new utils.Frustum();

    private static class FloraChunk {
        int cx, cz;
        List<scene.InstanceData> grass = new ArrayList<>();
        List<scene.InstanceData> trees = new ArrayList<>();
    }

    public static void update(Scene scene) {
        if (scene == null || scene.getCamera() == null) {
            return;
        }

        // Sahnede FlatTerrain var mı kontrol et
        FlatTerrain flatTerrain = null;
        for (ITerrain t : scene.getTerrains()) {
            if (t instanceof FlatTerrain) {
                flatTerrain = (FlatTerrain) t;
                break;
            }
        }

        if (flatTerrain == null) {
            return;
        }

        if (lastTerrain != flatTerrain
                || lastSeed != flatTerrain.getSeed()
                || lastMaxHeight != flatTerrain.getPMaxHeight()
                || lastRoughness != flatTerrain.getPRoughness()
                || lastOctaves != flatTerrain.getPOctaves()
                || lastScale != flatTerrain.getPScale()) {
            chunkCache.clear();
            lastTerrain = flatTerrain;
            lastSeed = flatTerrain.getSeed();
            lastMaxHeight = flatTerrain.getPMaxHeight();
            lastRoughness = flatTerrain.getPRoughness();
            lastOctaves = flatTerrain.getPOctaves();
            lastScale = flatTerrain.getPScale();
        }

        // Kameranın dünya koordinatları ve hangi chunk'ta olduğu
        Vector3f camPos = scene.getCamera().getPosition();
        int ccx = (int) floor(camPos.x / CHUNK_SIZE);
        int ccz = (int) floor(camPos.z / CHUNK_SIZE);

        // Sahnedeki instanced çimen ve ağaç listelerini al ve sıfırla
        Model grassModel = Grass3D.getGrassModel();
        Skin grassSkin = Grass3D.getGrassSkin();
        Model treeModel = Tree3D.getTreeModel();
        Skin treeSkin = Tree3D.getTreeSkin();

        List<scene.InstanceData> grassList = scene.getInstancedEntities()
                .computeIfAbsent(grassModel, m -> new HashMap<>())
                .computeIfAbsent(grassSkin, s -> new ArrayList<>());
        grassList.clear();

        List<scene.InstanceData> treeListLod0 = scene.getInstancedEntities()
                .computeIfAbsent(treeModel, m -> new HashMap<>())
                .computeIfAbsent(treeSkin, s -> new ArrayList<>());
        treeListLod0.clear();

        Model treeModelLod1 = Tree3D.getTreeModelLod1();
        List<scene.InstanceData> treeListLod1 = scene.getInstancedEntities()
                .computeIfAbsent(treeModelLod1, m -> new HashMap<>())
                .computeIfAbsent(treeSkin, s -> new ArrayList<>());
        treeListLod1.clear();

        scene.clearUnbatchedFlora();

        // Update frustum
        frustum.update(scene.getCamera().getProjectionViewMatrix());

        float waterHeight = 0.0f;
        if (!scene.getWater().isEmpty()) {
            waterHeight = scene.getWater().get(0).getHeight();
        }

        boolean generatedThisFrame = false;

        // Maksimum yarıçap olan 8 (Ağaç yarıçapı) kadar chunk'ları dön
        for (int dx = -treeRadius; dx <= treeRadius; dx++) {
            for (int dz = -treeRadius; dz <= treeRadius; dz++) {
                int cx = ccx + dx;
                int cz = ccz + dz;

                // Chunk center for culling
                float centerX = cx * CHUNK_SIZE + CHUNK_SIZE / 2.0f;
                float centerZ = cz * CHUNK_SIZE + CHUNK_SIZE / 2.0f;
                float centerY = flatTerrain.getHeightAt(centerX, centerZ);

                // Cull chunks outside frustum (a radius of 60.0f is enough for a 50x50 chunk with height variations)
                // This prevents adding chunks that are outside the camera view, eliminating the need for per-instance culling.
                if (!frustum.isPointInside(centerX, centerY, centerZ, 60.0f)) {
                    continue;
                }

                long key = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
                FloraChunk chunk = chunkCache.get(key);

                if (chunk == null) {
                    if (generatedThisFrame) {
                        continue; // Defer generation of this chunk to future frames
                    }
                    chunk = generateChunk(flatTerrain, cx, cz, waterHeight);
                    chunkCache.put(key, chunk);
                    generatedThisFrame = true;
                }

                // Çimenler daha yakın mesafede çizilir (GRASS_RADIUS)
                if (Math.abs(dx) <= grassRadius && Math.abs(dz) <= grassRadius) {
                    if (useBatching) {
                        grassList.addAll(chunk.grass);
                    } else {
                        for (scene.InstanceData id : chunk.grass) {
                            scene.Entity e = new scene.Entity(grassModel, grassSkin);
                            org.lwjgl.util.vector.Matrix4f m = id.getTransform();
                            e.setPosition(new Vector3f(m.m30, m.m31, m.m32));
                            // Basit tutmak için rotasyon ve ölçeği atlıyoruz veya default bırakıyoruz
                            scene.getUnbatchedFlora().add(e);
                        }
                    }
                }

                // Ağaçlar daha uzak mesafede çizilir (TREE_RADIUS)
                if (useBatching) {
                    if (scene.isLodEnabled()) {
                        float lodDistSq = 80.0f * 80.0f; // Yüksek poly ağaçlar için mesafeyi düşürdük
                        for (scene.InstanceData id : chunk.trees) {
                            org.lwjgl.util.vector.Matrix4f m = id.getTransform();
                            float tx = m.m30 - camPos.x;
                            float ty = m.m31 - camPos.y;
                            float tz = m.m32 - camPos.z;
                            float distSq = tx * tx + ty * ty + tz * tz;
                            
                            if (distSq < lodDistSq) {
                                treeListLod0.add(id);
                            } else {
                                treeListLod1.add(id);
                            }
                        }
                    } else {
                        // LOD kapalıysa tümünü en yüksek performansı veren low-poly (LOD1) çizelim
                        treeListLod1.addAll(chunk.trees);
                    }
                } else {
                    for (scene.InstanceData id : chunk.trees) {
                        Model activeTreeModel = treeModel;
                        if (scene.isLodEnabled()) {
                            org.lwjgl.util.vector.Matrix4f m = id.getTransform();
                            float tx = m.m30 - camPos.x;
                            float ty = m.m31 - camPos.y;
                            float tz = m.m32 - camPos.z;
                            float distSq = tx * tx + ty * ty + tz * tz;
                            if (distSq >= 200.0f * 200.0f) {
                                activeTreeModel = treeModelLod1;
                            }
                        } else {
                            activeTreeModel = treeModelLod1;
                        }
                        scene.Entity e = new scene.Entity(activeTreeModel, treeSkin);
                        org.lwjgl.util.vector.Matrix4f m = id.getTransform();
                        e.setPosition(new Vector3f(m.m30, m.m31, m.m32));
                        scene.getUnbatchedFlora().add(e);
                    }
                }
            }
        }

        // Bellek sızıntısını engellemek için çok uzaktaki chunk'ları temizle
        chunkCache.entrySet().removeIf(entry -> {
            long key = entry.getKey();
            int cx = (int) (key >> 32);
            int cz = (int) key;
            return Math.abs(cx - ccx) > treeRadius + 4 || Math.abs(cz - ccz) > treeRadius + 4;
        });
    }

    private static float floor(float x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }

    private static float hash2D(float px, float pz) {
        int ix = (int) Math.floor(px + 1000000.0f);
        int iz = (int) Math.floor(pz + 1000000.0f);

        ix = ix % 100000;
        iz = iz % 100000;

        int n = ix + iz * 57;
        n = (n << 13) ^ n;
        int nn = (n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff;
        return 1.0f - ((float) nn / 1073741824.0f);
    }

    private static float noise2D(float x, float z) {
        float xi = floor(x);
        float zi = floor(z);
        float xf = x - xi;
        float zf = z - zi;

        float ux = xf * xf * (3.0f - 2.0f * xf);
        float uz = zf * zf * (3.0f - 2.0f * zf);

        float a = hash2D(xi, zi);
        float b = hash2D(xi + 1.0f, zi);
        float c = hash2D(xi, zi + 1.0f);
        float d = hash2D(xi + 1.0f, zi + 1.0f);

        return a + ux * (b - a) + uz * (c - a) + ux * uz * (a - b - c + d);
    }

    private static FloraChunk generateChunk(FlatTerrain terrain, int cx, int cz, float waterHeight) {
        FloraChunk chunk = new FloraChunk();
        chunk.cx = cx;
        chunk.cz = cz;

        // Create height cache for this chunk to speed up generation 9x!
        ChunkHeightCache heightCache = new ChunkHeightCache(terrain, cx, cz);

        // Deterministik seed (Arazinin kendi tohumunu da kullan)
        long chunkSeed = ((long) cx * 341873128712L) ^ ((long) cz * 132897987541L) ^ terrain.getSeed();
        Random rand = new Random(chunkSeed);
        Vector3f normal = new Vector3f();

        float seedOffsetX = terrain.getSeed() % 10000;
        float seedOffsetZ = terrain.getSeed() / 10000;

        // GRASS POISSON DISK SAMPLING
        // Çimen mesafesi FPS optimizasyonu için 4.5'ten 5.5'e çıkarıldı
        List<org.lwjgl.util.vector.Vector2f> grassPoints = generatePoissonDiskSamples(CHUNK_SIZE, CHUNK_SIZE, 5.5f, 15,
                rand);
        for (org.lwjgl.util.vector.Vector2f pt : grassPoints) {
            float rRot = rand.nextFloat();
            float rScale = rand.nextFloat();

            float gx = cx * CHUNK_SIZE + pt.x;
            float gz = cz * CHUNK_SIZE + pt.y;
            float gy = heightCache.getHeightAndNormal(gx, gz, normal);

            // 1. Okyanus ve kumsal kontrolü (Suyun altında veya yakınında çıkmasın)
            if (gy >= waterHeight + 1.5f) {
                // 2. Eğim kontrolü (Zemin çok dikse çimen büyümez)
                if (normal.y >= 0.55f) {
                    // 3. Çimen Biyom kontrolü
                    float grassDensityNoise = noise2D(gx / 150.0f + seedOffsetX, gz / 150.0f + seedOffsetZ);
                    if (grassDensityNoise > -0.1f) {
                        float grassTextureIndex = rand.nextInt(4);
                        Matrix4f matrix = new Matrix4f();
                        matrix.setIdentity();
                        Matrix4f.translate(new Vector3f(gx, gy, gz), matrix, matrix);
                        Matrix4f.rotate((float) Math.toRadians(rand.nextFloat() * 360f), new Vector3f(0, 1, 0), matrix, matrix);
                        float scale = 1.6f + rand.nextFloat() * 1.6f; // Grass slightly bigger to compensate for lower density
                        Matrix4f.scale(new Vector3f(scale, scale, scale), matrix, matrix);
                        chunk.grass.add(new scene.InstanceData(matrix, grassTextureIndex));
                    }
                }
            }
        }

        // TREE POISSON DISK SAMPLING
        // Ağaçların mesafesi FPS için 35'ten 40'a çıkarıldı
        List<org.lwjgl.util.vector.Vector2f> treePoints = generatePoissonDiskSamples(CHUNK_SIZE, CHUNK_SIZE, 40.0f, 20,
                rand);
        for (org.lwjgl.util.vector.Vector2f pt : treePoints) {
            float gx = cx * CHUNK_SIZE + pt.x;
            float gz = cz * CHUNK_SIZE + pt.y;
            float gy = heightCache.getHeightAndNormal(gx, gz, normal);

            // 1. Okyanus ve kumsal kontrolü
            if (gy >= waterHeight + 4.0f) {
                // 2. Eğim kontrolü
                if (normal.y >= 0.70f) {
                    // 3. Orman Biyom kontrolü
                    float forestNoise = noise2D(gx / 400.0f + seedOffsetX, gz / 400.0f + seedOffsetZ);
                    if (forestNoise > -0.05f) {
                        Matrix4f tMatrix = new Matrix4f();
                        tMatrix.setIdentity();
                        Matrix4f.translate(new Vector3f(gx, gy, gz), tMatrix, tMatrix);
                        Matrix4f.rotate((float) Math.toRadians(rand.nextFloat() * 360f), new Vector3f(0, 1, 0), tMatrix, tMatrix);
                        float tScale = 0.6f + rand.nextFloat() * 0.4f; // Trees smaller
                        Matrix4f.scale(new Vector3f(tScale, tScale, tScale), tMatrix, tMatrix);
                        float treeTextureIndex = rand.nextInt(4);
                        chunk.trees.add(new scene.InstanceData(tMatrix, treeTextureIndex));
                    }
                }
            }
        }

        return chunk;
    }

    /**
     * Bridson's Poisson Disk Sampling Algoritması (Fast 2D)
     */
    private static List<org.lwjgl.util.vector.Vector2f> generatePoissonDiskSamples(float width, float height,
            float minRadius, int k, Random rand) {
        List<org.lwjgl.util.vector.Vector2f> points = new ArrayList<>();
        float cellSize = minRadius / (float) Math.sqrt(2);
        int gridWidth = (int) Math.ceil(width / cellSize);
        int gridHeight = (int) Math.ceil(height / cellSize);
        int[][] grid = new int[gridWidth][gridHeight];
        for (int i = 0; i < gridWidth; i++)
            java.util.Arrays.fill(grid[i], -1);

        List<org.lwjgl.util.vector.Vector2f> activeList = new ArrayList<>();
        org.lwjgl.util.vector.Vector2f p0 = new org.lwjgl.util.vector.Vector2f(rand.nextFloat() * width,
                rand.nextFloat() * height);
        points.add(p0);
        activeList.add(p0);
        grid[(int) (p0.x / cellSize)][(int) (p0.y / cellSize)] = 0;

        while (!activeList.isEmpty()) {
            int activeIndex = rand.nextInt(activeList.size());
            org.lwjgl.util.vector.Vector2f p = activeList.get(activeIndex);
            boolean found = false;

            for (int i = 0; i < k; i++) {
                float angle = rand.nextFloat() * (float) Math.PI * 2;
                float r = minRadius + rand.nextFloat() * minRadius;
                float nx = p.x + r * (float) Math.cos(angle);
                float ny = p.y + r * (float) Math.sin(angle);

                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    int col = (int) (nx / cellSize);
                    int row = (int) (ny / cellSize);
                    boolean ok = true;
                    int searchRadius = 2;

                    for (int c = Math.max(0, col - searchRadius); c <= Math.min(gridWidth - 1,
                            col + searchRadius); c++) {
                        for (int r2 = Math.max(0, row - searchRadius); r2 <= Math.min(gridHeight - 1,
                                row + searchRadius); r2++) {
                            int ptIndex = grid[c][r2];
                            if (ptIndex != -1) {
                                org.lwjgl.util.vector.Vector2f pt = points.get(ptIndex);
                                float dx = pt.x - nx;
                                float dy = pt.y - ny;
                                if (dx * dx + dy * dy < minRadius * minRadius) {
                                    ok = false;
                                    break;
                                }
                            }
                        }
                        if (!ok)
                            break;
                    }

                    if (ok) {
                        found = true;
                        org.lwjgl.util.vector.Vector2f newPt = new org.lwjgl.util.vector.Vector2f(nx, ny);
                        points.add(newPt);
                        activeList.add(newPt);
                        grid[col][row] = points.size() - 1;
                        break;
                    }
                }
            }

            if (!found) {
                activeList.remove(activeIndex);
            }
        }
        return points;
    }

    public static void clearCache() {
        chunkCache.clear();
    }

    private static class ChunkHeightCache {
        private final FlatTerrain terrain;
        private final float gridSquareSize;
        private final int minCol;
        private final int minRow;
        private final float[][] heights = new float[16][16];
        private final boolean[][] evaluated = new boolean[16][16];

        private final float pScale;
        private final float pOffsetX;
        private final float pOffsetZ;
        private final int pOctaves;
        private final float pRoughness;
        private final float pMaxHeight;
        private final float baseHeight;

        public ChunkHeightCache(FlatTerrain terrain, int cx, int cz) {
            this.terrain = terrain;
            this.gridSquareSize = terrain.getWidth() / 256.0f;
            this.minCol = (int) floor((cx * CHUNK_SIZE) / gridSquareSize) - 2;
            this.minRow = (int) floor((cz * CHUNK_SIZE) / gridSquareSize) - 2;

            this.pScale = terrain.getPScale();
            this.pOffsetX = terrain.getPOffsetX();
            this.pOffsetZ = terrain.getPOffsetZ();
            this.pOctaves = terrain.getPOctaves();
            this.pRoughness = terrain.getPRoughness();
            this.pMaxHeight = terrain.getPMaxHeight();
            this.baseHeight = terrain.getBaseHeight();
        }

        private float getCornerHeight(int col, int row) {
            float X = col * gridSquareSize;
            float Z = row * gridSquareSize;

            // Eğer terrain sonsuz değilse ve sınırların dışındaysak, baseHeight döndür
            if (!terrain.isInfinite()) {
                float halfW = terrain.getWidth() * 0.5f;
                float halfD = terrain.getDepth() * 0.5f;
                if (X < -halfW || X > halfW || Z < -halfD || Z > halfD) {
                    return baseHeight;
                }
            }

            int localCol = col - minCol;
            int localRow = row - minRow;
            if (localCol < 0 || localCol >= 16 || localRow < 0 || localRow >= 16) {
                return terrain.getHeightAt(X, Z);
            }
            if (!evaluated[localCol][localRow]) {
                heights[localCol][localRow] = terrain.getHeightAt(X, Z);
                evaluated[localCol][localRow] = true;
            }
            return heights[localCol][localRow];
        }

        public float getHeightAndNormal(float worldX, float worldZ, Vector3f outNormal) {
            // Eğer terrain sonsuz değilse ve sınırların dışındaysak ya da doğrudan arazi
            // verisini kullanmak istiyorsak, terrain'den doğrudan sorgula.
            // Bu sayede heightmap veya ada sönümlemesi (falloff) doğru bir şekilde hesaba
            // katılır.
            if (!terrain.isInfinite()) {
                return terrain.getHeightAndNormal(worldX, worldZ, outNormal);
            }

            float xVal = worldX / gridSquareSize;
            float zVal = worldZ / gridSquareSize;

            int col = (int) floor(xVal);
            int row = (int) floor(zVal);

            float hTL = getCornerHeight(col, row);
            float hTR = getCornerHeight(col + 1, row);
            float hBL = getCornerHeight(col, row + 1);
            float hBR = getCornerHeight(col + 1, row + 1);

            float xCoord = xVal - col;
            float zCoord = zVal - row;

            float answer;
            if (xCoord <= (1.0f - zCoord)) {
                answer = barryCentric(new Vector3f(0, hTL, 0),
                        new Vector3f(1, hTR, 0),
                        new Vector3f(0, hBL, 1),
                        new org.lwjgl.util.vector.Vector2f(xCoord, zCoord));
                outNormal.set(hTL - hTR, gridSquareSize, hTL - hBL);
            } else {
                answer = barryCentric(new Vector3f(1, hTR, 0),
                        new Vector3f(1, hBR, 1),
                        new Vector3f(0, hBL, 1),
                        new org.lwjgl.util.vector.Vector2f(xCoord, zCoord));
                outNormal.set(hBL - hBR, gridSquareSize, hTR - hBR);
            }
            outNormal.normalise();
            return answer;
        }

        private float barryCentric(Vector3f p1, Vector3f p2, Vector3f p3, org.lwjgl.util.vector.Vector2f pos) {
            float det = (p2.z - p3.z) * (p1.x - p3.x) + (p3.x - p2.x) * (p1.z - p3.z);
            float l1 = ((p2.z - p3.z) * (pos.x - p3.x) + (p3.x - p2.x) * (pos.y - p3.z)) / det;
            float l2 = ((p3.z - p1.z) * (pos.x - p3.x) + (p1.x - p3.x) * (pos.y - p3.z)) / det;
            float l3 = 1.0f - l1 - l2;
            return l1 * p1.y + l2 * p2.y + l3 * p3.y;
        }
    }
}
