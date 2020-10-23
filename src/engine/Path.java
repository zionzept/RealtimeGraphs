package engine;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_NORMAL_ARRAY;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_COORD_ARRAY;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.GL_VERTEX_ARRAY;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glEnableClientState;
import static org.lwjgl.opengl.GL11.glNormalPointer;
import static org.lwjgl.opengl.GL11.glTexCoordPointer;
import static org.lwjgl.opengl.GL11.glVertexPointer;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import gl.Entity;
import gl.Entity3;
import gl.Shader;

public class Path {

	private Chain chain;
	private Vector4f color;
	private Shader shader;
	private Entity3 ent;
	
	private double px;
	private double py;
	
	private float[][] verts;
	private float[][] normals;
	private float[][] texcoords;
	
	public Path(Vector4f color, Shader shader, int len) {
		this.color = color;
		this.shader = shader;
		
		ent = new Entity3();
		ent.setShader(shader);
		//ent.scale(3);
		
		verts = new float[len][3];
		normals = new float[len][3];
		texcoords = new float[len][2];
	}
	
	public void target(Chain epicycles) {
		this.chain = epicycles;
		cut();
	}
	
	public void cut() {
		float x = (float) chain.getLast().end_x;
		float y = (float) chain.getLast().end_y;
		float z = 0f;
		for (float[] point : verts) {
			point[0] = x;
			point[1] = y;
			point[2] = z;
		}
		
		for (float[] normal : normals) {
			normal[0] = 0;
			normal[1] = 1;
			normal[2] = 0;
		}
		
		for (float[] texcoord : texcoords) {
			texcoord[0] = 0;
			texcoord[1] = 0;
		}
	}
	
	public void update() {
		float x = (float) chain.getLast().end_x;
		float y = (float) chain.getLast().end_y;
		float z = 0f;
		verts[0][0] = x;
		verts[0][1] = y;
		verts[0][2] = z;
	}
	
	public void render(Matrix4f transform) {
		update(); // probably might aswell keep this called from here
		if (chain == null || chain.isEmpty()) {
			return;
		}
		shader.setUniform("tint", color); // line color

		
		texture.bind(0);
		Matrix4f new_transform = new Matrix4f();
		transform.mulAffine(getTransform(), new_transform);
		shader.bind();
		shader.setUniform("world", new_transform);
		square.render();
		
		glEnableVertexAttribArray(0);
		glEnableVertexAttribArray(1);
		glEnableVertexAttribArray(2);
		
		glEnableClientState(GL_VERTEX_ARRAY);
		glBindBuffer(GL_ARRAY_BUFFER, v_id);
		glVertexPointer(3, GL_FLOAT, 0, 0);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		
		glEnableClientState(GL_NORMAL_ARRAY);
		glBindBuffer(GL_ARRAY_BUFFER, n_id);
		glNormalPointer(GL_FLOAT, 0, 0);
		glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		
		glEnableClientState(GL_TEXTURE_COORD_ARRAY);
		glBindBuffer(GL_ARRAY_BUFFER, t_id);
		glTexCoordPointer(2, GL_FLOAT, 0, 0);
		glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0);
		
		
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, i_id);
		
		glDrawElements(GL_TRIANGLES, draw_count, GL_UNSIGNED_INT, 0);
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		
		glDisableVertexAttribArray(0);
		glDisableVertexAttribArray(1);
		glDisableVertexAttribArray(2);
		
		
	//	shader.setUniform("tint", color); different color for frame points?
    //  or rather, is this last part even needed...
		px = epi.end_x;
		py = epi.end_y;
		tip.translation_x = (float)px;
		tip.translation_y = (float)py;
		tip.render(transform);
	}
	
}
