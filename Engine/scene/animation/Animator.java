package scene.animation;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.util.vector.Matrix4f;

import scene.Entity;

public class Animator {

	private final Entity entity;
	private final Joint rootJoint;
	private final int jointCount;
	
	private Animation currentAnimation;
	private float animationTime = 0;
	private boolean paused = false;
	private float playSpeed = 1.0f;

	public Animator(Entity entity, Joint rootJoint, int jointCount) {
		this.entity = entity;
		this.rootJoint = rootJoint;
		this.jointCount = jointCount;
		this.entity.setJointTransforms(new Matrix4f[jointCount]);
		for (int i = 0; i < jointCount; i++) {
			this.entity.getJointTransforms()[i] = new Matrix4f();
			this.entity.getJointTransforms()[i].setIdentity();
		}
	}

	public void doAnimation(Animation animation) {
		this.animationTime = 0;
		this.currentAnimation = animation;
	}

	public void update(float delta) {
		if (currentAnimation == null || paused) {
			return;
		}
		animationTime += delta * playSpeed;
		if (animationTime > currentAnimation.getLength()) {
			this.animationTime %= currentAnimation.getLength();
		}
		Map<Integer, Matrix4f> currentPose = calculateCurrentAnimationPose();
		applyPoseToJoints(currentPose, rootJoint, new Matrix4f());
	}

	public void pause() {
		this.paused = true;
	}

	public void resume() {
		this.paused = false;
	}

	public void setSpeed(float speed) {
		this.playSpeed = speed;
	}

	public boolean isPaused() {
		return paused;
	}

	public float getSpeed() {
		return playSpeed;
	}

	private Map<Integer, Matrix4f> calculateCurrentAnimationPose() {
		KeyFrame[] frames = getPreviousAndNextFrames();
		float progression = calculateProgression(frames[0], frames[1]);
		return interpolatePoses(frames[0], frames[1], progression);
	}

	private void applyPoseToJoints(Map<Integer, Matrix4f> currentPose, Joint joint, Matrix4f parentTransform) {
		Matrix4f currentLocalTransform = currentPose.get(joint.index);
		if (currentLocalTransform == null) {
			// fallback if this bone is not animated in this frame
			currentLocalTransform = joint.getLocalBindTransform();
		}
		
		Matrix4f currentTransform = new Matrix4f();
		Matrix4f.mul(parentTransform, currentLocalTransform, currentTransform);
		
		for (Joint childJoint : joint.children) {
			applyPoseToJoints(currentPose, childJoint, currentTransform);
		}
		
		// final bone transform = current * inverse bind
		Matrix4f.mul(currentTransform, joint.getInverseBindTransform(), currentTransform);
		
		if (joint.index >= 0 && joint.index < jointCount) {
			entity.getJointTransforms()[joint.index].load(currentTransform);
		}
	}

	private KeyFrame[] getPreviousAndNextFrames() {
		KeyFrame[] allFrames = currentAnimation.getKeyFrames();
		KeyFrame previousFrame = allFrames[0];
		KeyFrame nextFrame = allFrames[0];
		for (int i = 1; i < allFrames.length; i++) {
			nextFrame = allFrames[i];
			if (nextFrame.getTimeStamp() > animationTime) {
				break;
			}
			previousFrame = allFrames[i];
		}
		// loop logic
		if (nextFrame.getTimeStamp() <= animationTime && allFrames.length > 1) {
			nextFrame = allFrames[0]; 
		}
		return new KeyFrame[] { previousFrame, nextFrame };
	}

	private float calculateProgression(KeyFrame previousFrame, KeyFrame nextFrame) {
		float totalTime = nextFrame.getTimeStamp() - previousFrame.getTimeStamp();
		if (totalTime <= 0) {
			// loop point progression (e.g. from 2.0s to 0.0s)
			totalTime = (currentAnimation.getLength() - previousFrame.getTimeStamp()) + nextFrame.getTimeStamp();
		}
		float currentTime = animationTime - previousFrame.getTimeStamp();
		if (currentTime < 0) {
			currentTime = (currentAnimation.getLength() - previousFrame.getTimeStamp()) + animationTime;
		}
		
		if (totalTime <= 0) return 0f;
		return currentTime / totalTime;
	}

	private Map<Integer, Matrix4f> interpolatePoses(KeyFrame previousFrame, KeyFrame nextFrame, float progression) {
		Map<Integer, Matrix4f> currentPose = new HashMap<>();
		for (int jointId : previousFrame.getJointKeyFrames().keySet()) {
			JointTransform previousTransform = previousFrame.getJointKeyFrames().get(jointId);
			JointTransform nextTransform = nextFrame.getJointKeyFrames().get(jointId);
			if (nextTransform != null) {
				JointTransform currentTransform = JointTransform.interpolate(previousTransform, nextTransform, progression);
				currentPose.put(jointId, currentTransform.getLocalTransform());
			} else {
				currentPose.put(jointId, previousTransform.getLocalTransform());
			}
		}
		return currentPose;
	}

}
