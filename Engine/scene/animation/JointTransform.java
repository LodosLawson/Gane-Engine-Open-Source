package scene.animation;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

public class JointTransform {

	private final Vector3f position;
	private final Quaternion rotation;

	public JointTransform(Vector3f position, Quaternion rotation) {
		this.position = position;
		this.rotation = rotation;
	}

	public Matrix4f getLocalTransform() {
		Matrix4f matrix = new Matrix4f();
		matrix.setIdentity();
		Matrix4f.translate(position, matrix, matrix);
		Matrix4f rotMatrix = quaternionToMatrix(rotation);
		Matrix4f.mul(matrix, rotMatrix, matrix);
		return matrix;
	}

	public static JointTransform interpolate(JointTransform frameA, JointTransform frameB, float progression) {
		Vector3f pos = interpolate(frameA.position, frameB.position, progression);
		Quaternion rot = interpolate(frameA.rotation, frameB.rotation, progression);
		return new JointTransform(pos, rot);
	}

	private static Vector3f interpolate(Vector3f start, Vector3f end, float progression) {
		float x = start.x + (end.x - start.x) * progression;
		float y = start.y + (end.y - start.y) * progression;
		float z = start.z + (end.z - start.z) * progression;
		return new Vector3f(x, y, z);
	}

	private static Quaternion interpolate(Quaternion a, Quaternion b, float blend) {
		Quaternion result = new Quaternion(0, 0, 0, 1);
		float dot = a.w * b.w + a.x * b.x + a.y * b.y + a.z * b.z;
		float blendI = 1f - blend;
		if (dot < 0) {
			result.w = blendI * a.w + blend * -b.w;
			result.x = blendI * a.x + blend * -b.x;
			result.y = blendI * a.y + blend * -b.y;
			result.z = blendI * a.z + blend * -b.z;
		} else {
			result.w = blendI * a.w + blend * b.w;
			result.x = blendI * a.x + blend * b.x;
			result.y = blendI * a.y + blend * b.y;
			result.z = blendI * a.z + blend * b.z;
		}
		result.normalise();
		return result;
	}

	private static Matrix4f quaternionToMatrix(Quaternion q) {
		Matrix4f matrix = new Matrix4f();
		matrix.m00 = (1.0f - 2.0f * (q.y * q.y + q.z * q.z));
		matrix.m01 = (2.0f * (q.x * q.y + q.z * q.w));
		matrix.m02 = (2.0f * (q.x * q.z - q.y * q.w));
		matrix.m03 = 0.0f;

		matrix.m10 = (2.0f * (q.x * q.y - q.z * q.w));
		matrix.m11 = (1.0f - 2.0f * (q.x * q.x + q.z * q.z));
		matrix.m12 = (2.0f * (q.y * q.z + q.x * q.w));
		matrix.m13 = 0.0f;

		matrix.m20 = (2.0f * (q.x * q.z + q.y * q.w));
		matrix.m21 = (2.0f * (q.y * q.z - q.x * q.w));
		matrix.m22 = (1.0f - 2.0f * (q.x * q.x + q.y * q.y));
		matrix.m23 = 0.0f;

		matrix.m30 = 0.0f;
		matrix.m31 = 0.0f;
		matrix.m32 = 0.0f;
		matrix.m33 = 1.0f;
		return matrix;
	}
}
