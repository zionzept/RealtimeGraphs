package gl;

import org.lwjgl.opengl.GL30;

import window.Window;

public class FrameBuffer {

	public final int id;
	public final Texture texture;
	private boolean clear;
	
	
	public FrameBuffer() {
		texture = new Texture(Window.w, Window.h);
		id = GL30.glGenFramebuffers();
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, id);
		GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_TEXTURE_2D, texture.id, 0);
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	}
	
	public void clear() {
		clear = true;
	}
	
	public void bind() {
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, id);
		if (clear) {
			GL30.glClear(GL30.GL_COLOR_BUFFER_BIT);
			clear = false;
		}
	}
	
	public void unbind() {
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	}
	
	public void delete() {
		GL30.glDeleteFramebuffers(id);
	}
}
