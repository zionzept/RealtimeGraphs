package engine;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import gl.Entity;
import gl.Shader;
import gl.Texture;

public class Pen {

	private Chain epicycles;
	private Vector4f color;
	private Shader shader;
	private Entity tip;
	
	private double px;
	private double py;
	
	public Pen(Vector4f color, Shader shader, Texture tip_texture) {
		this.color = color;
		this.shader = shader;
		
		tip = new Entity();
		tip.setShader(shader);
		tip.setTexture(tip_texture);
		tip.scale(3);
	}
	
	public void target(Chain epicycles) {
		this.epicycles = epicycles;
		skip();
	}
	
	
	/**
	 * updates position of pen without drawing
	 */
	public void skip() {
		if (epicycles == null || epicycles.isEmpty()) {
			return;
		}
		px = epicycles.getLast().end_x;
		py = epicycles.getLast().end_y;
	}
	
	public void render(Matrix4f transform) {
		if (epicycles == null || epicycles.isEmpty()) {
			return;
		}
		shader.setUniform("tint", color); // line color
		Vector epi = epicycles.getLast();
		double dx = epi.end_x-px;
		double dy = epi.end_y-py;
		double precision = 0.5;
		while (dx*dx+dy*dy > precision * precision) {
			double a = Math.atan2(dy, dx);
			px += precision * Math.cos(a);
			py += precision * Math.sin(a);
			tip.translation_x = (float)px;
			tip.translation_y = (float)py;
			tip.render(transform);
			dx = epi.end_x-px;
			dy = epi.end_y-py;
		}
	//	shader.setUniform("tint", color); different color for frame points?
    //  or rather, is this last part even needed...
		px = epi.end_x;
		py = epi.end_y;
		tip.translation_x = (float)px;
		tip.translation_y = (float)py;
		tip.render(transform);
	}
	
}
