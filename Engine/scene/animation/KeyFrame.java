package scene.animation;

import java.util.Map;

public class KeyFrame {

	private final float timeStamp;
	private final Map<Integer, JointTransform> pose;

	public KeyFrame(float timeStamp, Map<Integer, JointTransform> jointKeyFrames) {
		this.timeStamp = timeStamp;
		this.pose = jointKeyFrames;
	}

	public float getTimeStamp() {
		return timeStamp;
	}

	public Map<Integer, JointTransform> getJointKeyFrames() {
		return pose;
	}

}
