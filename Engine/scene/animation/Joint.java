package scene.animation;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Matrix4f;

public class Joint {

	public final int index;
	public final String name;
	public final List<Joint> children = new ArrayList<>();

	private Matrix4f animatedTransform = new Matrix4f();
	private final Matrix4f localBindTransform = new Matrix4f();
	private final Matrix4f inverseBindTransform = new Matrix4f();

	public Joint(int index, String name, Matrix4f localBindTransform, Matrix4f inverseBindTransform) {
		this.index = index;
		this.name = name;
		this.localBindTransform.load(localBindTransform);
		this.inverseBindTransform.load(inverseBindTransform);
	}

	public void addChild(Joint child) {
		this.children.add(child);
	}

	public Matrix4f getAnimatedTransform() {
		return animatedTransform;
	}

	public void setAnimatedTransform(Matrix4f animatedTransform) {
		this.animatedTransform = animatedTransform;
	}

	public Matrix4f getInverseBindTransform() {
		return inverseBindTransform;
	}

	public Matrix4f getLocalBindTransform() {
		return localBindTransform;
	}
}
