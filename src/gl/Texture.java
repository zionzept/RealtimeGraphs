package gl;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameterf;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL30;

import window.Window;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL46.*;


public class Texture {
	
	public final int id;
	public final int w;
	public final int h;
	
	public Texture(String filename) {
		this(filename.split("\\."));
	}
	
	private Texture(String[] filename) {
		this(filename[0], filename[1]);
	}
	
	public Texture(String filename, String format) {
		format = format.toLowerCase();
		BufferedImage bi = null;
		File file = null;
		if (format.equals("png")) {
			try {
				file = new File("res/textures/" + filename + ".png");
				bi = ImageIO.read(file);
			} catch(IOException e) {
				System.out.println("Can't find file:");
				System.out.println(file.getAbsolutePath());
				e.printStackTrace();
				id = -1;
				w = 0; h = 0;
				return;
			}
		} else if (format.equals("jpg")) {
			try {
				file = new File("res/textures/" + filename + ".jpg");
				bi = ImageIO.read(file);
			} catch(IOException e) {
				System.out.println("Can't find file:");
				System.out.println(file.getAbsolutePath());
				e.printStackTrace();
				id = -1;
				w = 0; h = 0;
				return;
			}
		}
		w = bi.getWidth();
		h = bi.getHeight();
		
		int[] pixels_raw = new int[w * h * 4];
		pixels_raw = bi.getRGB(0, 0, w, h, null, 0, w);
		ByteBuffer pixels = BufferUtils.createByteBuffer(w * h * 4);
		
		for (int i = 0; i < w; i++) {
			for (int j = 0; j < h; j++) {
				int pixel = pixels_raw[i*h + j];
				pixels.put((byte)((pixel >> 16) & 0xFF)); //RED
				pixels.put((byte)((pixel >> 8) & 0xFF)); //GREEN
				pixels.put((byte)(pixel & 0xFF)); //BLUE
				pixels.put((byte)((pixel >> 24) & 0xFF)); //ALPHA
			}
		}
		pixels.flip();
		id = glGenTextures();
		
		glBindTexture(GL_TEXTURE_2D, id);
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_GENERATE_MIPMAP, GL_TRUE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
		glBindTexture(GL_TEXTURE_2D, 0);
		
	}
	
	public Texture(int width, int height) {
		w = width;
		h = height;
		ByteBuffer pixels = BufferUtils.createByteBuffer(w * h * 4);
		id = glGenTextures();
		
		glBindTexture(GL_TEXTURE_2D, id);
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_GENERATE_MIPMAP, GL_TRUE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
		glBindTexture(GL_TEXTURE_2D, 0);
	}
	
	public void bind(int sampler) {
		glActiveTexture(GL_TEXTURE0 + sampler);
		glBindTexture(GL_TEXTURE_2D, id);
	}
}
