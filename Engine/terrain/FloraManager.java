package terrain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import scene.Scene;
import terrain.flat.FlatTerrain;

public class FloraManager {

    public static boolean useBatching = true;
    private static final float CHUNK_SIZE = 50.0f;
    private static int grassRadius = 8; // 400 units (8 * 50)
    private static int treeRadius = 8;  // 400 units (8 * 50)

    public static void setRenderDistanceScale(float scale) {
        grassRadius = Math.max(1, Math.round(3.0f * scale));
        treeRadius = Math.max(2, Math.round(6.0f * scale));
    }

    private static List<List<scene.GameObject>> treeTemplates = new ArrayList<>();
    private static List<List<scene.GameObject>> grassTemplates = new ArrayList<>();
    private static List<List<scene.GameObject>> bushTemplates = new ArrayList<>();

    public static void setTemplates(List<List<scene.GameObject>> trees, List<List<scene.GameObject>> grass, List<List<scene.GameObject>> bushes) {
        treeTemplates = trees;
        grassTemplates = grass;
        bushTemplates = bushes;
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
        Map<scene.GameObject, List<scene.InstanceData>> floraInstances = new HashMap<>();
        
        public void addInstance(scene.GameObject template, scene.InstanceData data) {
            floraInstances.computeIfAbsent(template, k -> new ArrayList<>()).add(data);
        }
    }

    public static void update(Scene scene) {
        if (scene == null || scene.getCamera() == null) {
            return;
        }

        FlatTerrain flatTerrain = null;
        for (ITerrain t : scene.getTerrains()) {
            if (t instanceof FlatTerrain) {
                flatTerrain = (FlatTerrain) t;
                break;
            }
        }

        if (flatTerrain == null) return;

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

        Vector3f camPos = scene.getCamera().getPosition();
        int ccx = (int) floor(camPos.x / CHUNK_SIZE);
        int ccz = (int) floor(camPos.z / CHUNK_SIZE);

        // Clear existing instanced lists for all templates
        clearInstances(scene, treeTemplates);
        clearInstances(scene, grassTemplates);
        clearInstances(scene, bushTemplates);
        scene.clearUnbatchedFlora();

        frustum.update(scene.getCamera().getProjectionViewMatrix());

        float waterHeight = 0.0f;
        if (!scene.getWater().isEmpty()) {
            waterHeight = scene.getWater().get(0).getHeight();
        }

        boolean generatedThisFrame = false;

        for (int dx = -treeRadius; dx <= treeRadius; dx++) {
            for (int dz = -treeRadius; dz <= treeRadius; dz++) {
                int cx = ccx + dx;
                int cz = ccz + dz;

                float centerX = cx * CHUNK_SIZE + CHUNK_SIZE / 2.0f;
                float centerZ = cz * CHUNK_SIZE + CHUNK_SIZE / 2.0f;
                float centerY = flatTerrain.getHeightAt(centerX, centerZ);

                if (!frustum.isPointInside(centerX, centerY, centerZ, 70.0f)) {
                    continue;
                }

                long key = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
                FloraChunk chunk = chunkCache.get(key);

                if (chunk == null) {
                    if (generatedThisFrame) continue;
                    chunk = generateChunk(flatTerrain, cx, cz, waterHeight);
                    chunkCache.put(key, chunk);
                    generatedThisFrame = true;
                }

                // Add chunks to scene
                for (Map.Entry<scene.GameObject, List<scene.InstanceData>> entry : chunk.floraInstances.entrySet()) {
                    scene.GameObject template = entry.getKey();
                    boolean isGrass = false;
                    for (List<scene.GameObject> group : grassTemplates) {
                        if (group.contains(template)) {
                            isGrass = true; break;
                        }
                    }
                    
                    if (isGrass && (Math.abs(dx) > grassRadius || Math.abs(dz) > grassRadius)) {
                        continue; // Skip grass outside grassRadius
                    }
                    
                    if (useBatching) {
                        scene.getInstancedEntities().get(template.getModel()).get(template.getSkin()).addAll(entry.getValue());
                    } else {
                        for (scene.InstanceData id : entry.getValue()) {
                            scene.Entity e = new scene.Entity(template.getModel(), template.getSkin());
                            org.lwjgl.util.vector.Matrix4f m = id.getTransform();
                            e.setPosition(new Vector3f(m.m30, m.m31, m.m32));
                            scene.getUnbatchedFlora().add(e);
                        }
                    }
                }
            }
        }

        chunkCache.entrySet().removeIf(entry -> {
            long key = entry.getKey();
            int cx = (int) (key >> 32);
            int cz = (int) key;
            return Math.abs(cx - ccx) > treeRadius + 4 || Math.abs(cz - ccz) > treeRadius + 4;
        });
    }
    
    private static void clearInstances(Scene scene, List<List<scene.GameObject>> templates) {
        for (List<scene.GameObject> group : templates) {
            for (scene.GameObject template : group) {
                scene.getInstancedEntities()
                    .computeIfAbsent(template.getModel(), m -> new HashMap<>())
                    .computeIfAbsent(template.getSkin(), s -> new ArrayList<>())
                    .clear();
            }
        }
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

		// Direct terrain lookup

        long chunkSeed = ((long) cx * 341873128712L) ^ ((long) cz * 132897987541L) ^ terrain.getSeed();
        Random rand = new Random(chunkSeed);
        Vector3f normal = new Vector3f();

        float seedOffsetX = terrain.getSeed() % 10000;
        float seedOffsetZ = terrain.getSeed() / 10000;

        // GRASS
        if (!grassTemplates.isEmpty()) {
            List<org.lwjgl.util.vector.Vector2f> grassPoints = generatePoissonDiskSamples(CHUNK_SIZE, CHUNK_SIZE, 1.5f, 15, rand);
            for (org.lwjgl.util.vector.Vector2f pt : grassPoints) {
                float gx = cx * CHUNK_SIZE + pt.x;
                float gz = cz * CHUNK_SIZE + pt.y;
                float gy = terrain.getHeightAndNormal(gx, gz, normal);

                if (gy >= waterHeight + 1.5f && normal.y >= 0.55f) {
                    float grassDensityNoise = noise2D(gx / 150.0f + seedOffsetX, gz / 150.0f + seedOffsetZ);
                    if (grassDensityNoise > -0.1f) {
                        List<scene.GameObject> templateGroup = grassTemplates.get(rand.nextInt(grassTemplates.size()));
                        Matrix4f matrix = new Matrix4f();
                        matrix.setIdentity();
                        Matrix4f.translate(new Vector3f(gx, gy, gz), matrix, matrix);
                        Matrix4f.rotate((float) Math.toRadians(rand.nextFloat() * 360f), new Vector3f(0, 1, 0), matrix, matrix);
                        
                        float scale = 0.4f + rand.nextFloat() * 0.3f; 
                        Matrix4f.scale(new Vector3f(scale, scale, scale), matrix, matrix);
                        
                        for (scene.GameObject part : templateGroup) {
                            Matrix4f instanceMatrix = new Matrix4f(matrix);
                            if (part.getBaseOffset().x != 0 || part.getBaseOffset().y != 0 || part.getBaseOffset().z != 0) {
                                Matrix4f.translate(part.getBaseOffset(), instanceMatrix, instanceMatrix);
                            }
                            chunk.addInstance(part, new scene.InstanceData(instanceMatrix, 0));
                        }
                    }
                }
            }
        }

        // TREES
        if (!treeTemplates.isEmpty()) {
            List<org.lwjgl.util.vector.Vector2f> treePoints = generatePoissonDiskSamples(CHUNK_SIZE, CHUNK_SIZE, 25.0f, 20, rand);
            for (org.lwjgl.util.vector.Vector2f pt : treePoints) {
                float gx = cx * CHUNK_SIZE + pt.x;
                float gz = cz * CHUNK_SIZE + pt.y;
                float gy = terrain.getHeightAndNormal(gx, gz, normal);

                if (gy >= waterHeight + 3.0f && normal.y >= 0.85f) {
                    float forestNoise = noise2D(gx / 400.0f + seedOffsetX, gz / 400.0f + seedOffsetZ);
                    if (forestNoise > -0.05f) {
                        List<scene.GameObject> templateGroup = treeTemplates.get(rand.nextInt(treeTemplates.size()));
                        Matrix4f tMatrix = new Matrix4f();
                        tMatrix.setIdentity();
                        Matrix4f.translate(new Vector3f(gx, gy, gz), tMatrix, tMatrix);
                        Matrix4f.rotate((float) Math.toRadians(rand.nextFloat() * 360f), new Vector3f(0, 1, 0), tMatrix, tMatrix);
                        Matrix4f.rotate((float) Math.toRadians(90.0f), new Vector3f(1, 0, 0), tMatrix, tMatrix); // Inverted Z-up to Y-up (+90)
                        
                        float tScale = 0.15f + rand.nextFloat() * 0.15f; // Boyutlar küçültüldü
                        Matrix4f.scale(new Vector3f(tScale, tScale, tScale), tMatrix, tMatrix);
                        
                        for (scene.GameObject part : templateGroup) {
                            Matrix4f instanceMatrix = new Matrix4f(tMatrix);
                            if (part.getBaseOffset().x != 0 || part.getBaseOffset().y != 0 || part.getBaseOffset().z != 0) {
                                Matrix4f.translate(part.getBaseOffset(), instanceMatrix, instanceMatrix);
                            }
                            chunk.addInstance(part, new scene.InstanceData(instanceMatrix, 0));
                        }
                    }
                }
            }
        }
        
        // BUSHES
        if (!bushTemplates.isEmpty()) {
            List<org.lwjgl.util.vector.Vector2f> bushPoints = generatePoissonDiskSamples(CHUNK_SIZE, CHUNK_SIZE, 20.0f, 15, rand);
            for (org.lwjgl.util.vector.Vector2f pt : bushPoints) {
                float gx = cx * CHUNK_SIZE + pt.x;
                float gz = cz * CHUNK_SIZE + pt.y;
                float gy = terrain.getHeightAndNormal(gx, gz, normal);

                if (gy >= waterHeight + 2.0f && normal.y >= 0.65f) {
                    List<scene.GameObject> templateGroup = bushTemplates.get(rand.nextInt(bushTemplates.size()));
                    Matrix4f bMatrix = new Matrix4f();
                    bMatrix.setIdentity();
                    Matrix4f.translate(new Vector3f(gx, gy, gz), bMatrix, bMatrix);
                    Matrix4f.rotate((float) Math.toRadians(rand.nextFloat() * 360f), new Vector3f(0, 1, 0), bMatrix, bMatrix);
                    
                    float bScale = 0.8f + rand.nextFloat() * 1.0f; 
                    Matrix4f.scale(new Vector3f(bScale, bScale, bScale), bMatrix, bMatrix);
                    
                    for (scene.GameObject part : templateGroup) {
                        Matrix4f instanceMatrix = new Matrix4f(bMatrix);
                        if (part.getBaseOffset().x != 0 || part.getBaseOffset().y != 0 || part.getBaseOffset().z != 0) {
                            Matrix4f.translate(part.getBaseOffset(), instanceMatrix, instanceMatrix);
                        }
                        chunk.addInstance(part, new scene.InstanceData(instanceMatrix, 0));
                    }
                }
            }
        }

        return chunk;
    }

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
}
