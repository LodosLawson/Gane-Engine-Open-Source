package shadows;

import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import utils.ICamera;

public class ShadowBox {

    private static final float OFFSET = 200; // Kameranin arkasindan golge dusurebilecek objeler (agac, bulut)
                                             // icin ekstra Z payi. Cok buyuk deger derinlik hassasiyetini dusurur.
    private static final Vector4f UP = new Vector4f(0, 1, 0, 0);
    private static final Vector4f FORWARD = new Vector4f(0, 0, -1, 0);

    // Shadow box dimensions - Set to a balanced value for quality and coverage.
    private float shadowDistance = 500f; // Sıkılaştırılmış mesafe daha net gölgeler sağlar.

    private float minX, maxX;
    private float minY, maxY;
    private float minZ, maxZ;
    private Matrix4f lightViewMatrix;
    private ICamera cam;

    // Perspective properties
    private float farHeight, farWidth, nearHeight, nearWidth;

    public ShadowBox(Matrix4f lightViewMatrix, ICamera camera) {
        this.lightViewMatrix = lightViewMatrix;
        this.cam = camera;
        calculateWidthsAndHeights();
    }

    /**
     * Updates the bounds of the shadow box based on the light direction and the
     * camera's view frustum, to make sure that the box covers the smallest area
     * possible while still covering everything inside the camera's view
     * (within a certain range).
     */
    // update() metodunu bu şekilde güncelle:
    public void update(Matrix4f currentLightViewMatrix) {
        this.lightViewMatrix = currentLightViewMatrix; // Matrisi eşitle veya doğrudan kullan
        calculateWidthsAndHeights();

        float yaw = cam.getYaw(); // Kameranın Yaw (Sağa-Sola dönme) açısını alır
        float pitch = cam.getPitch(); // Kameranın Pitch (Yukarı-Aşağı bakma) açısını alır

        Vector3f forwardVector = new Vector3f(
                (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))),
                (float) (-Math.sin(Math.toRadians(pitch))),
                (float) (-Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))));
        forwardVector.normalise();

        // Right vektörü: Forward ile Dünya Up (0,1,0) vektörünün cross product'ıdır
        Vector3f rightVector = Vector3f.cross(forwardVector, new Vector3f(0, 1, 0), null);
        rightVector.normalise();

        // Up vektörü: Right ile Forward'ın cross product'ıdır
        Vector3f upVector = Vector3f.cross(rightVector, forwardVector, null);
        upVector.normalise();
        // -------------------------------------------------------------------------

        // Geri kalan kodlar aynen devam ediyor, dokunma:
        Vector3f farCenter = new Vector3f(cam.getPosition());
        Vector3f.add(farCenter, (Vector3f) new Vector3f(forwardVector).scale(shadowDistance), farCenter);
        Vector3f nearCenter = new Vector3f(cam.getPosition());
        Vector3f.add(nearCenter, (Vector3f) new Vector3f(forwardVector).scale(0.1f), nearCenter);

        Vector4f[] points = calculateFrustumVertices(upVector, rightVector, farCenter, nearCenter);

        boolean first = true;
        for (Vector4f point : points) {
            if (first) {
                minX = point.x;
                maxX = point.x;
                minY = point.y;
                maxY = point.y;
                minZ = point.z;
                maxZ = point.z;
                first = false;
                continue;
            }
            if (point.x > maxX)
                maxX = point.x;
            else if (point.x < minX)
                minX = point.x;

            if (point.y > maxY)
                maxY = point.y;
            else if (point.y < minY)
                minY = point.y;

            if (point.z > maxZ)
                maxZ = point.z;
            else if (point.z < minZ)
                minZ = point.z;
        }
        minZ -= OFFSET;
        maxZ += OFFSET;
    }

    /**
     * Calculates the center of the "view cuboid" in light space first, then
     * converts this to world space using the inverse light's view matrix.
     * 
     * @return The center of the "view cuboid" in world space.
     */
    public Vector3f getCenter() {
        float x = (minX + maxX) / 2f;
        float y = (minY + maxY) / 2f;
        float z = (minZ + maxZ) / 2f;
        Vector4f cen = new Vector4f(x, y, z, 1);
        Matrix4f invertedLight = new Matrix4f();
        Matrix4f.invert(lightViewMatrix, invertedLight);
        return new Vector3f(Matrix4f.transform(invertedLight, cen, null));
    }

    public float getWidth() {
        return maxX - minX;
    }

    public float getHeight() {
        return maxY - minY;
    }

    public float getLength() {
        return maxZ - minZ;
    }

    public float getMinX() {
        return minX;
    }

    public float getMaxX() {
        return maxX;
    }

    public float getMinY() {
        return minY;
    }

    public float getMaxY() {
        return maxY;
    }

    public float getMinZ() {
        return minZ;
    }

    public float getMaxZ() {
        return maxZ;
    }
    
    /**
     * Objenin ShadowBox (Gölge Haritası Sınırları) içinde kalıp kalmadığını test eder.
     * Bu sayede gölge oluşturmayacak kadar uzaktaki objelerin çizilmesi (CPU/GPU) önlenir.
     */
    public boolean isPointInside(float worldX, float worldY, float worldZ, float radius) {
        float lx = lightViewMatrix.m00 * worldX + lightViewMatrix.m10 * worldY + lightViewMatrix.m20 * worldZ + lightViewMatrix.m30;
        float ly = lightViewMatrix.m01 * worldX + lightViewMatrix.m11 * worldY + lightViewMatrix.m21 * worldZ + lightViewMatrix.m31;
        float lz = lightViewMatrix.m02 * worldX + lightViewMatrix.m12 * worldY + lightViewMatrix.m22 * worldZ + lightViewMatrix.m32;
        
        if (lx < minX - radius || lx > maxX + radius) return false;
        if (ly < minY - radius || ly > maxY + radius) return false;
        if (lz < minZ - radius || lz > maxZ + radius) return false;
        
        return true;
    }

    public boolean isPointInside(Vector3f worldPosition, float radius) {
        return isPointInside(worldPosition.x, worldPosition.y, worldPosition.z, radius);
    }

    public Vector3f calculateFrustumCenter() {
        Matrix4f viewMatrix = cam.getViewMatrix();
        Vector3f rightVector = new Vector3f(viewMatrix.m00, viewMatrix.m10, viewMatrix.m20);
        Vector3f upVector = new Vector3f(viewMatrix.m01, viewMatrix.m11, viewMatrix.m21);
        Vector3f forwardVector = new Vector3f(-viewMatrix.m02, -viewMatrix.m12, -viewMatrix.m22);

        rightVector.normalise();
        upVector.normalise();
        forwardVector.normalise();

        Vector3f farCenter = new Vector3f(cam.getPosition());
        Vector3f.add(farCenter, (Vector3f) new Vector3f(forwardVector).scale(shadowDistance), farCenter);
        Vector3f nearCenter = new Vector3f(cam.getPosition());
        Vector3f.add(nearCenter, (Vector3f) new Vector3f(forwardVector).scale(0.1f), nearCenter);

        Vector3f center = new Vector3f();
        Vector3f.add(farCenter, nearCenter, center);
        center.scale(0.5f);
        return center;
    }

    /**
     * Calculates the position of the vertex at each corner of the view frustum
     * in light space (8 vertices in total, so this returns 8 positions).
     */
    private Vector4f[] calculateFrustumVertices(Vector3f upVector, Vector3f rightVector, Vector3f farCenter,
            Vector3f nearCenter) {
        Vector3f downVector = new Vector3f(-upVector.x, -upVector.y, -upVector.z);
        Vector3f leftVector = new Vector3f(-rightVector.x, -rightVector.y, -rightVector.z);

        Vector3f farTop = Vector3f.add(farCenter, (Vector3f) new Vector3f(upVector).scale(farHeight), null);
        Vector3f farBottom = Vector3f.add(farCenter, (Vector3f) new Vector3f(downVector).scale(farHeight), null);
        Vector3f nearTop = Vector3f.add(nearCenter, (Vector3f) new Vector3f(upVector).scale(nearHeight), null);
        Vector3f nearBottom = Vector3f.add(nearCenter, (Vector3f) new Vector3f(downVector).scale(nearHeight), null);

        Vector4f[] points = new Vector4f[8];
        points[0] = calculateLightSpaceFrustumCorner(farTop, rightVector, farWidth);
        points[1] = calculateLightSpaceFrustumCorner(farTop, leftVector, farWidth);
        points[2] = calculateLightSpaceFrustumCorner(farBottom, rightVector, farWidth);
        points[3] = calculateLightSpaceFrustumCorner(farBottom, leftVector, farWidth);
        points[4] = calculateLightSpaceFrustumCorner(nearTop, rightVector, nearWidth);
        points[5] = calculateLightSpaceFrustumCorner(nearTop, leftVector, nearWidth);
        points[6] = calculateLightSpaceFrustumCorner(nearBottom, rightVector, nearWidth);
        points[7] = calculateLightSpaceFrustumCorner(nearBottom, leftVector, nearWidth);
        return points;
    }

    private Vector4f calculateLightSpaceFrustumCorner(Vector3f startPoint, Vector3f direction, float width) {
        Vector3f point = Vector3f.add(startPoint, (Vector3f) new Vector3f(direction).scale(width), null);
        Vector4f point4f = new Vector4f(point.x, point.y, point.z, 1f);
        Matrix4f.transform(lightViewMatrix, point4f, point4f);
        return point4f;
    }

    private void calculateWidthsAndHeights() {
        float aspectRatio = (float) Display.getWidth() / (float) Display.getHeight();

        // Using a larger FOV for the shadow box to ensure coverage
        float fov = 75f;
        farHeight = (float) (shadowDistance * Math.tan(Math.toRadians(fov / 2f)));
        farWidth = farHeight * aspectRatio;

        nearHeight = (float) (0.5f * Math.tan(Math.toRadians(fov / 2f)));
        nearWidth = nearHeight * aspectRatio;
    }

    public void setShadowDistance(float distance) {
        this.shadowDistance = distance;
        calculateWidthsAndHeights();
    }

    public float getShadowDistance() {
        return this.shadowDistance;
    }

    public utils.ICamera getCamera() {
        return this.cam;
    }
}
