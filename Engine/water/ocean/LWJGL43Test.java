package water.ocean;

import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL15;

public class LWJGL43Test {
    public static void test() {
        GL43.glDispatchCompute(1, 1, 1);
        GL42.glBindImageTexture(0, 1, 0, false, 0, GL15.GL_READ_ONLY, org.lwjgl.opengl.GL30.GL_RG32F);
    }
}
