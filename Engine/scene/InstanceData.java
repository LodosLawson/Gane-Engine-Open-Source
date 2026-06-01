package scene;

import org.lwjgl.util.vector.Matrix4f;

/**
 * Instanced Rendering için GPU'ya gönderilecek verileri tutar.
 */
public class InstanceData {
    private Matrix4f transform;
    private float textureOffsetIndex;

    public InstanceData(Matrix4f transform, float textureOffsetIndex) {
        this.transform = transform;
        this.textureOffsetIndex = textureOffsetIndex;
    }

    public Matrix4f getTransform() {
        return transform;
    }

    public float getTextureOffsetIndex() {
        return textureOffsetIndex;
    }
}
