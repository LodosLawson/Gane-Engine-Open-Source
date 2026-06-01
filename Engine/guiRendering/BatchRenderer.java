package guiRendering;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * UI elemanlarını tek bir draw call ile çizmek için VBO tabanlı dinamik Batch Renderer.
 */
public class BatchRenderer {
    public static final int MAX_QUADS = 2000;
    private static final int VERTEX_SIZE = 8; // x, y, u, v, r, g, b, a
    private static final int QUAD_SIZE = 4 * VERTEX_SIZE; // 32 floats
    
    private final int vaoId;
    private final int vboId;
    private final int eboId;
    
    private final FloatBuffer vertexBuffer;
    private final float[] vertexArray;
    public int quadCount = 0;
    public int currentTextureId = -1;
    private boolean drawing = false;

    public BatchRenderer() {
        // VAO Oluştur
        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        // VBO Oluştur (Dinamik güncelleme için)
        vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        vertexBuffer = BufferUtils.createFloatBuffer(MAX_QUADS * QUAD_SIZE);
        vertexArray = new float[MAX_QUADS * QUAD_SIZE];
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, MAX_QUADS * QUAD_SIZE * 4, GL15.GL_DYNAMIC_DRAW);

        // EBO (Index Buffer) Oluştur (Karelerin çizim sırası için)
        eboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(MAX_QUADS * 6);
        for (int i = 0; i < MAX_QUADS; i++) {
            int offset = i * 4;
            indexBuffer.put(offset + 0);
            indexBuffer.put(offset + 1);
            indexBuffer.put(offset + 2);
            indexBuffer.put(offset + 2);
            indexBuffer.put(offset + 3);
            indexBuffer.put(offset + 0);
        }
        indexBuffer.flip();
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15.GL_STATIC_DRAW);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void begin() {
        if (drawing) {
            throw new IllegalStateException("BatchRenderer zaten çizim modunda!");
        }
        drawing = true;
        quadCount = 0;
        currentTextureId = -1;
    }

    public void drawQuad(float x, float y, float w, float h, Color color, int textureId) {
        if (!drawing) {
            throw new IllegalStateException("BatchRenderer çizim modunda değil! begin() çağrılmalı.");
        }
        
        // Doku değiştiğinde veya limit aşıldığında birikmiş çizimleri GPU'ya yolla
        if (textureId != currentTextureId || quadCount >= MAX_QUADS) {
            flush();
            currentTextureId = textureId;
        }

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        drawQuad(x, y, w, h, 0f, 0f, 1f, 1f, r, g, b, a);
    }

    public void drawQuad(float x, float y, float w, float h, float u0, float v0, float u1, float v1, float r, float g, float b, float a) {
        int index = quadCount * QUAD_SIZE;

        // Vertex 0: Sol-Üst
        vertexArray[index++] = x;
        vertexArray[index++] = y;
        vertexArray[index++] = u0;
        vertexArray[index++] = v0;
        vertexArray[index++] = r;
        vertexArray[index++] = g;
        vertexArray[index++] = b;
        vertexArray[index++] = a;

        // Vertex 1: Sağ-Üst
        vertexArray[index++] = x + w;
        vertexArray[index++] = y;
        vertexArray[index++] = u1;
        vertexArray[index++] = v0;
        vertexArray[index++] = r;
        vertexArray[index++] = g;
        vertexArray[index++] = b;
        vertexArray[index++] = a;

        // Vertex 2: Sağ-Alt
        vertexArray[index++] = x + w;
        vertexArray[index++] = y + h;
        vertexArray[index++] = u1;
        vertexArray[index++] = v1;
        vertexArray[index++] = r;
        vertexArray[index++] = g;
        vertexArray[index++] = b;
        vertexArray[index++] = a;

        // Vertex 3: Sol-Alt
        vertexArray[index++] = x;
        vertexArray[index++] = y + h;
        vertexArray[index++] = u0;
        vertexArray[index++] = v1;
        vertexArray[index++] = r;
        vertexArray[index++] = g;
        vertexArray[index++] = b;
        vertexArray[index++] = a;

        quadCount++;
    }

    public void flush() {
        if (quadCount == 0) return;

        // Vertex verilerini NIO buffer'ına kopyala ve GPU'ya yolla
        vertexBuffer.clear();
        vertexBuffer.put(vertexArray, 0, quadCount * QUAD_SIZE);
        vertexBuffer.flip();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertexBuffer);

        // Dokuyu bağla
        if (currentTextureId > 0) {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTextureId);
        } else {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
        }

        // VAO'yu bağla ve çizimi yap
        GL30.glBindVertexArray(vaoId);
        
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
        
        GL11.glVertexPointer(2, GL11.GL_FLOAT, VERTEX_SIZE * 4, 0);
        GL11.glTexCoordPointer(2, GL11.GL_FLOAT, VERTEX_SIZE * 4, 2 * 4);
        GL11.glColorPointer(4, GL11.GL_FLOAT, VERTEX_SIZE * 4, 4 * 4);
        
        GL11.glDrawElements(GL11.GL_TRIANGLES, quadCount * 6, GL11.GL_UNSIGNED_INT, 0);
        
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
        
        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        quadCount = 0;
    }

    public void end() {
        if (!drawing) {
            throw new IllegalStateException("BatchRenderer çizim modunda değil!");
        }
        flush();
        drawing = false;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public void cleanUp() {
        GL30.glDeleteVertexArrays(vaoId);
        GL15.glDeleteBuffers(vboId);
        GL15.glDeleteBuffers(eboId);
    }
}
