package objConverter;

public class ModelData {
	
	private static final int DIMENSIONS = 3;

	private float[] vertices;
	private float[] textureCoords;
	private float[] normals;
	private int[] indices;
	private float furthestPoint;

	private float[] joints;
	private float[] weights;
	
	// Bounding Box sınırları (Orijinal model uzayında)
	private float minX, maxX, minY, maxY, minZ, maxZ;
	
	private byte[] embeddedTextureData;
	private boolean doubleSided = false;
	private boolean transparent = false;

	private String name;
	private scene.animation.Joint rootJoint;
	private int jointCount;
	private scene.animation.Animation animation;

	public ModelData(float[] vertices, float[] textureCoords, float[] normals, int[] indices, float furthestPoint) {
		this(vertices, textureCoords, normals, indices, null, null, furthestPoint, 0, 0, 0, 0, 0, 0);
	}

	public ModelData(float[] vertices, float[] textureCoords, float[] normals, int[] indices,
			float[] joints, float[] weights, float furthestPoint) {
		this(vertices, textureCoords, normals, indices, joints, weights, furthestPoint, 0, 0, 0, 0, 0, 0);
	}

	public ModelData(float[] vertices, float[] textureCoords, float[] normals, int[] indices,
			float furthestPoint, float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
		this(vertices, textureCoords, normals, indices, null, null, furthestPoint, minX, maxX, minY, maxY, minZ, maxZ);
	}

	public ModelData(float[] vertices, float[] textureCoords, float[] normals, int[] indices,
			float[] joints, float[] weights, float furthestPoint,
			float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
		this.vertices = vertices;
		this.textureCoords = textureCoords;
		this.normals = normals;
		this.indices = indices;
		this.joints = joints;
		this.weights = weights;
		this.furthestPoint = furthestPoint;
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;
		this.minZ = minZ;
		this.maxZ = maxZ;
	}

	public void setAnimationData(scene.animation.Joint rootJoint, int jointCount, scene.animation.Animation animation) {
		this.rootJoint = rootJoint;
		this.jointCount = jointCount;
		this.animation = animation;
	}
	
	public int getVertexCount(){
		return vertices.length/DIMENSIONS;
	}

	public float[] getVertices() {
		return vertices;
	}

	public float[] getTextureCoords() {
		return textureCoords;
	}

	public float[] getNormals() {
		return normals;
	}

	public int[] getIndices() {
		return indices;
	}

	public float getFurthestPoint() {
		return furthestPoint;
	}

	public byte[] getEmbeddedTextureData() {
		return embeddedTextureData;
	}

	public void setEmbeddedTextureData(byte[] embeddedTextureData) {
		this.embeddedTextureData = embeddedTextureData;
	}

	public boolean isDoubleSided() {
		return doubleSided;
	}

	public void setDoubleSided(boolean doubleSided) {
		this.doubleSided = doubleSided;
	}

	public boolean isTransparent() {
		return transparent;
	}

	public void setTransparent(boolean transparent) {
		this.transparent = transparent;
	}

	public float[] getJointIds() {
		return joints;
	}

	public float[] getVertexWeights() {
		return weights;
	}

	public scene.animation.Joint getRootJoint() {
		return rootJoint;
	}

	public int getJointCount() {
		return jointCount;
	}

	public float getMinX() { return minX; }
	public float getMaxX() { return maxX; }
	public float getMinY() { return minY; }
	public float getMaxY() { return maxY; }
	public float getMinZ() { return minZ; }
	public float getMaxZ() { return maxZ; }

	public scene.animation.Animation getAnimation() {
		return animation;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
