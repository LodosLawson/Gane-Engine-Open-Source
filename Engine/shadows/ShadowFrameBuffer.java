package shadows;

import java.nio.ByteBuffer;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

public class ShadowFrameBuffer {

    private final int WIDTH;
    private final int HEIGHT;

    private int fbo;
    private int shadowMap;

    /**
     * Initializes the frame buffer and shadow map of a certain size.
     * 
     * @param width
     *               - the width of the shadow map in pixels.
     * @param height
     *               - the height of the shadow map in pixels.
     */
    public ShadowFrameBuffer(int width, int height) {
        this.WIDTH = width;
        this.HEIGHT = height;
        initialiseFrameBuffer();
    }

    /**
     * Deletes the frame buffer and shadow map texture when the game closes.
     */
    public void cleanUp() {
        GL30.glDeleteFramebuffers(fbo);
        GL11.glDeleteTextures(shadowMap);
    }

    /**
     * Binds the frame buffer, setting it as the current render target. Anything
     * rendered after this will be rendered to this FBO, and therefore to the
     * shadow map.
     */
    public void bindFrameBuffer() {
        bindFrameBuffer(fbo, WIDTH, HEIGHT);
    }

    /**
     * Unbinds the frame buffer, setting the default frame buffer as the current
     * render target. Anything rendered after this will be rendered to the
     * screen.
     */
    public void unbindFrameBuffer() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glDrawBuffer(GL11.GL_BACK); // Restore default draw buffer for main render pass
        GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
    }

    /**
     * @return The ID of the shadow map texture. The ID will always be the
     *         same, even when the contents of the shadow map texture change
     *         each frame.
     */
    public int getShadowMap() {
        return shadowMap;
    }

    /**
     * Creates the frame buffer and adds its depth attachment texture.
     */
    private void initialiseFrameBuffer() {
        fbo = createFrameBuffer();
        shadowMap = createDepthBufferAttachment(WIDTH, HEIGHT);
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("Shadow FrameBuffer is not complete! Status: " + status);
        }
        unbindFrameBuffer();
    }

    /**
     * Binds the frame buffer as the current render target.
     * 
     * @param frameBuffer
     *                    - the frame buffer to bind.
     * @param width
     *                    - the width of the frame buffer.
     * @param height
     *                    - the height of the frame buffer.
     */
    private static void bindFrameBuffer(int frameBuffer, int width, int height) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, frameBuffer);
        GL11.glViewport(0, 0, width, height);
    }

    /**
     * Creates a frame buffer and binds it so that attachments can be added to
     * it. The draw buffer is set to none, indicating that we use no colour buffer.
     * 
     * @return The newly created frame buffer ID.
     */
    private static int createFrameBuffer() {
        int frameBuffer = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, frameBuffer);
        GL11.glDrawBuffer(GL11.GL_NONE);
        GL11.glReadBuffer(GL11.GL_NONE);
        return frameBuffer;
    }

    /**
     * Creates a depth buffer texture attachment.
     * 
     * @param width
     *               - the width of the texture.
     * @param height
     *               - the height of the texture.
     * @return The ID of the depth texture.
     */
    private static int createDepthBufferAttachment(int width, int height) {
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT24, width, height, 0,
                GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_BORDER);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_BORDER);

        java.nio.FloatBuffer borderColor = org.lwjgl.BufferUtils.createFloatBuffer(4);
        borderColor.put(new float[] { 1.0f, 1.0f, 1.0f, 1.0f });
        borderColor.flip();
        GL11.glTexParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_BORDER_COLOR, borderColor);

        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                GL11.GL_TEXTURE_2D, texture, 0);
        return texture;
    }
}
