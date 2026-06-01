package utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import openglObjects.Vao;
import scene.Entity;
import scene.Model;
import textures.Texture;

public class Text3DBuilder {

    public static Entity createTextEntity(String text, org.lwjgl.util.vector.Vector3f position) {
        int width = 1024;
        int height = 128;
        
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Seffaf arka plan
        g2d.setColor(new Color(0, 0, 0, 0));
        g2d.fillRect(0, 0, width, height);
        
        // Font ayarlari
        g2d.setFont(new Font("Arial", Font.BOLD, 42));
        FontMetrics fm = g2d.getFontMetrics();
        int x = (width - fm.stringWidth(text)) / 2;
        int y = (height - fm.getHeight()) / 2 + fm.getAscent();
        
        // Gölge (Glow/Shadow efekti)
        g2d.setColor(new Color(0, 150, 255, 180));
        g2d.drawString(text, x + 2, y + 2);
        g2d.drawString(text, x - 2, y - 2);
        
        // Asil metin
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, x, y);
        g2d.dispose();

        int[] pixels = new int[width * height];
        img.getRGB(0, 0, width, height, pixels, 0, width);
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int pixel = pixels[i * width + j];
                buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
                buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
                buffer.put((byte) (pixel & 0xFF));         // B
                buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
            }
        }
        buffer.flip();
        
        int tex = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        
        // Quad Vao olustur (Boyut orani 1024x128 = 8:1)
        float w = 400f; // Genislik 400 birim
        float h = 50f;  // Yukseklik 50 birim
        
        Vao quadVao = Vao.create();
        quadVao.bind();
        
        float[] positions = new float[]{ -w, h, 0,  -w, -h, 0,  w, -h, 0,  w, h, 0 };
        float[] uvs = new float[]{ 0, 0,  0, 1,  1, 1,  1, 0 };
        float[] normals = new float[]{ 0, 0, 1,  0, 0, 1,  0, 0, 1,  0, 0, 1 };
        int[] indices = new int[]{ 0, 1, 2,  2, 3, 0 };
        
        quadVao.storeData(indices, 4, positions, uvs, normals);
        quadVao.unbind();
        
        Model model = new Model(quadVao);
        Texture textureObj = Texture.createFromId(tex, width);
        scene.Skin skin = new scene.Skin(textureObj, null);
        skin.setTransparent(true);
        
        Entity entity = new Entity(model, skin);
        entity.setPosition(position);
        return entity;
    }
}
