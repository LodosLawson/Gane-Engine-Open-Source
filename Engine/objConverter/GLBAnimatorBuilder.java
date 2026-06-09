package objConverter;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

import scene.animation.Animation;
import scene.animation.Joint;
import scene.animation.JointTransform;
import scene.animation.KeyFrame;

public class GLBAnimatorBuilder {

	public static Joint[] parseSkeleton(JSONObject gltf, ByteBuffer binBuffer, float[] inverseBindMatrices, JSONArray jointsArray) {
		Joint[] allJoints = new Joint[jointsArray.length()];
		JSONArray nodes = gltf.getJSONArray("nodes");

		// 1. Create all joints
		for (int i = 0; i < jointsArray.length(); i++) {
			int nodeIndex = jointsArray.getInt(i);
			JSONObject node = nodes.getJSONObject(nodeIndex);
			String name = node.has("name") ? node.getString("name") : "Joint_" + i;

			// Extract local bind transform
			Matrix4f localBind = new Matrix4f();
			localBind.setIdentity();
			if (node.has("matrix")) {
				JSONArray mat = node.getJSONArray("matrix");
				localBind.m00 = (float) mat.getDouble(0); localBind.m01 = (float) mat.getDouble(1); localBind.m02 = (float) mat.getDouble(2); localBind.m03 = (float) mat.getDouble(3);
				localBind.m10 = (float) mat.getDouble(4); localBind.m11 = (float) mat.getDouble(5); localBind.m12 = (float) mat.getDouble(6); localBind.m13 = (float) mat.getDouble(7);
				localBind.m20 = (float) mat.getDouble(8); localBind.m21 = (float) mat.getDouble(9); localBind.m22 = (float) mat.getDouble(10); localBind.m23 = (float) mat.getDouble(11);
				localBind.m30 = (float) mat.getDouble(12); localBind.m31 = (float) mat.getDouble(13); localBind.m32 = (float) mat.getDouble(14); localBind.m33 = (float) mat.getDouble(15);
			} else {
				if (node.has("translation")) {
					JSONArray t = node.getJSONArray("translation");
					Vector3f trans = new Vector3f((float) t.getDouble(0), (float) t.getDouble(1), (float) t.getDouble(2));
					Matrix4f.translate(trans, localBind, localBind);
				}
				if (node.has("rotation")) {
					JSONArray r = node.getJSONArray("rotation");
					Quaternion quat = new Quaternion((float) r.getDouble(0), (float) r.getDouble(1), (float) r.getDouble(2), (float) r.getDouble(3));
					Matrix4f rotMat = quaternionToMatrix(quat);
					Matrix4f.mul(localBind, rotMat, localBind);
				}
				if (node.has("scale")) {
					JSONArray s = node.getJSONArray("scale");
					Vector3f scale = new Vector3f((float) s.getDouble(0), (float) s.getDouble(1), (float) s.getDouble(2));
					Matrix4f.scale(scale, localBind, localBind);
				}
			}

			// Extract inverse bind
			Matrix4f invBind = new Matrix4f();
			int offset = i * 16;
			invBind.m00 = inverseBindMatrices[offset + 0]; invBind.m01 = inverseBindMatrices[offset + 1]; invBind.m02 = inverseBindMatrices[offset + 2]; invBind.m03 = inverseBindMatrices[offset + 3];
			invBind.m10 = inverseBindMatrices[offset + 4]; invBind.m11 = inverseBindMatrices[offset + 5]; invBind.m12 = inverseBindMatrices[offset + 6]; invBind.m13 = inverseBindMatrices[offset + 7];
			invBind.m20 = inverseBindMatrices[offset + 8]; invBind.m21 = inverseBindMatrices[offset + 9]; invBind.m22 = inverseBindMatrices[offset + 10]; invBind.m23 = inverseBindMatrices[offset + 11];
			invBind.m30 = inverseBindMatrices[offset + 12]; invBind.m31 = inverseBindMatrices[offset + 13]; invBind.m32 = inverseBindMatrices[offset + 14]; invBind.m33 = inverseBindMatrices[offset + 15];

			allJoints[i] = new Joint(i, name, localBind, invBind);
		}

		// 2. Build Hierarchy
		for (int i = 0; i < jointsArray.length(); i++) {
			int nodeIndex = jointsArray.getInt(i);
			JSONObject node = nodes.getJSONObject(nodeIndex);
			if (node.has("children")) {
				JSONArray children = node.getJSONArray("children");
				for (int j = 0; j < children.length(); j++) {
					int childNodeIndex = children.getInt(j);
					// Find child joint index
					int childJointIndex = -1;
					for (int k = 0; k < jointsArray.length(); k++) {
						if (jointsArray.getInt(k) == childNodeIndex) {
							childJointIndex = k;
							break;
						}
					}
					if (childJointIndex != -1) {
						allJoints[i].addChild(allJoints[childJointIndex]);
					}
				}
			}
		}

		return allJoints;
	}

	static class Channel {
		int jointIndex;
		String path;
		float[] times;
		float[] values;
	}

	public static Animation parseAnimation(JSONObject gltf, ByteBuffer binBuffer, JSONArray jointsArray, Joint[] allJoints) {
		if (!gltf.has("animations")) return null;
		JSONArray animations = gltf.getJSONArray("animations");
		JSONObject anim = animations.getJSONObject(0);

		JSONArray channelsArray = anim.getJSONArray("channels");
		JSONArray samplersArray = anim.getJSONArray("samplers");

		java.util.List<Channel> channels = new java.util.ArrayList<>();
		java.util.TreeSet<Float> uniqueTimes = new java.util.TreeSet<>();

		for (int i = 0; i < channelsArray.length(); i++) {
			JSONObject channelJSON = channelsArray.getJSONObject(i);
			int samplerIndex = channelJSON.getInt("sampler");
			JSONObject target = channelJSON.getJSONObject("target");
			int targetNode = target.getInt("node");
			String path = target.getString("path");

			int jointIndex = -1;
			for (int j = 0; j < jointsArray.length(); j++) {
				if (jointsArray.getInt(j) == targetNode) {
					jointIndex = j;
					break;
				}
			}
			if (jointIndex == -1) continue;

			JSONObject sampler = samplersArray.getJSONObject(samplerIndex);
			int inputAccessor = sampler.getInt("input");
			int outputAccessor = sampler.getInt("output");

			float[] times = GLBFileLoader.readFloatArray(inputAccessor, gltf, binBuffer);
			float[] values = GLBFileLoader.readFloatArray(outputAccessor, gltf, binBuffer);

			for (float t : times) uniqueTimes.add(t);

			Channel ch = new Channel();
			ch.jointIndex = jointIndex;
			ch.path = path;
			ch.times = times;
			ch.values = values;
			channels.add(ch);
		}

		if (uniqueTimes.isEmpty()) return null;

		float length = uniqueTimes.last();
		KeyFrame[] frames = new KeyFrame[uniqueTimes.size()];
		int frameIdx = 0;

		for (Float time : uniqueTimes) {
			Map<Integer, JointTransform> pose = new HashMap<>();

			for (int jId = 0; jId < jointsArray.length(); jId++) {
				Vector3f trans = new Vector3f(0, 0, 0);
				Quaternion rot = new Quaternion(0, 0, 0, 1);
				Vector3f scale = new Vector3f(1, 1, 1);

				boolean hasAnim = false;
				for (Channel ch : channels) {
					if (ch.jointIndex == jId) {
						hasAnim = true;
						float[] sampled = sampleChannel(ch, time);
						if (ch.path.equals("translation")) {
							trans.set(sampled[0], sampled[1], sampled[2]);
						} else if (ch.path.equals("rotation")) {
							rot.set(sampled[0], sampled[1], sampled[2], sampled[3]);
						} else if (ch.path.equals("scale")) {
							scale.set(sampled[0], sampled[1], sampled[2]);
						}
					}
				}

				if (hasAnim) {
					// Apply scale if needed? GanEngine JointTransform currently ignores scale.
					pose.put(jId, new JointTransform(trans, rot));
				}
			}
			frames[frameIdx++] = new KeyFrame(time, pose);
		}

		return new Animation(length, frames);
	}

	private static float[] sampleChannel(Channel ch, float time) {
		float[] times = ch.times;
		float[] values = ch.values;
		int comp = ch.path.equals("rotation") ? 4 : 3;

		if (time <= times[0]) {
			float[] res = new float[comp];
			for (int i = 0; i < comp; i++) res[i] = values[i];
			return res;
		}
		if (time >= times[times.length - 1]) {
			float[] res = new float[comp];
			for (int i = 0; i < comp; i++) res[i] = values[(times.length - 1) * comp + i];
			return res;
		}

		for (int i = 0; i < times.length - 1; i++) {
			if (time >= times[i] && time <= times[i + 1]) {
				float t1 = times[i];
				float t2 = times[i + 1];
				float factor = (time - t1) / (t2 - t1);
				float[] res = new float[comp];
				
				if (ch.path.equals("rotation")) { // slerp
					Quaternion q1 = new Quaternion(values[i * 4], values[i * 4 + 1], values[i * 4 + 2], values[i * 4 + 3]);
					Quaternion q2 = new Quaternion(values[(i + 1) * 4], values[(i + 1) * 4 + 1], values[(i + 1) * 4 + 2], values[(i + 1) * 4 + 3]);
					// Use naive lerp for quaternion, followed by normalize (GLTF linear interpolation for quat is spherical linear, but normalized linear is an acceptable approximation)
					float dot = q1.x * q2.x + q1.y * q2.y + q1.z * q2.z + q1.w * q2.w;
					float blend = factor;
					float blendI = 1.0f - factor;
					if (dot < 0) {
						res[0] = blendI * q1.x - blend * q2.x;
						res[1] = blendI * q1.y - blend * q2.y;
						res[2] = blendI * q1.z - blend * q2.z;
						res[3] = blendI * q1.w - blend * q2.w;
					} else {
						res[0] = blendI * q1.x + blend * q2.x;
						res[1] = blendI * q1.y + blend * q2.y;
						res[2] = blendI * q1.z + blend * q2.z;
						res[3] = blendI * q1.w + blend * q2.w;
					}
					// Normalize
					float mag = (float) Math.sqrt(res[0]*res[0] + res[1]*res[1] + res[2]*res[2] + res[3]*res[3]);
					res[0] /= mag; res[1] /= mag; res[2] /= mag; res[3] /= mag;
				} else { // lerp
					for (int c = 0; c < comp; c++) {
						float v1 = values[i * comp + c];
						float v2 = values[(i + 1) * comp + c];
						res[c] = v1 + (v2 - v1) * factor;
					}
				}
				return res;
			}
		}
		return new float[comp];
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
