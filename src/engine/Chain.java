package engine;

import java.util.Iterator;
import java.util.LinkedList;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import gl.Entity;
import gl.Shader;
import gl.Texture;

public class Chain extends LinkedList<Vector> {
	private static final long serialVersionUID = 1L;
	
	private Shader shader;
	private Texture circle_texture;
	private Texture arrow_texture;
	
	private Vector4f circle_color;
	private Vector4f arrow_color;
	
	private double x;
	private double y;
	
	public Chain(Chain epicycles) {
		this(epicycles.shader, epicycles.circle_texture, epicycles.arrow_texture, epicycles.circle_color, epicycles.arrow_color);
		this.x = epicycles.x;
		this.y = epicycles.y;
		for (Vector e : epicycles) {
			add(new Vector(e));
		}
	}
	
	//shenanigans
	int monkaS_fx = 0;
	
	private double[] distribution(int size) {
		double[] d = new double[size];
		double g = 0;
		for (int i = 0; i < size; ++i) {
			double q = Math.random();
			d[i] = q;
			g += q;
		}
		for (int i = 0; i < size; ++i) {
			d[i] /= g;
		}
		return d;
	}
	
	public Chain(Shader shader, Texture circle_texture, Texture arrow_texture, Vector4f circle_color, Vector4f arrow_color, LinkedList<Point> f_points, double shift) {
		this(shader, circle_texture, arrow_texture, circle_color, arrow_color);
		
		int i = 0;
		
		Point[] point = new Point[f_points.size()];
		int sh = (int)(f_points.size()*shift);
		for (Point p : f_points) {
			int index = (i + sh) % point.length;
			point[index] = p;
			i++;
		}
		
		int rot = -sh;
		
		//shenanigans
		int points = point.length;
		
		for (Point p : point) {
			double radius = Math.hypot(p.x, p.y);
			double rot_start = Math.atan2(p.y, p.x);
			//shenanigans
			int monkaS = monkaS_fx*2+1;
			double[] monkaSjk = distribution(monkaS);
			StringBuilder sb = new StringBuilder();
			sb.append("chain:\n");
			for (int monka = 0; monka < monkaS; ++monka) {
				
				sb.append("\ti:" + monka + "\t sjk:" + monkaSjk[monka]);
				add(new Vector(radius*monkaSjk[monka], rot_start, rot+points*(monka-monkaS/2), shader, circle_texture, arrow_texture));
			}
			
			rot++;
		}
	}
	
	public Chain(Shader shader, Texture circle_texture, Texture arrow_texture, Vector4f circle_color, Vector4f arrow_color) {
		super();
		this.shader = shader;
		this.circle_texture = circle_texture;
		this.arrow_texture = arrow_texture;
		this.circle_color = circle_color;
		this.arrow_color = arrow_color;
	}
	
	public void setPos(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	public void rotate(double d_rot) {
		for (Vector epicycle : this) {
			epicycle.rot_start += d_rot;
		}
	}
	
	public void phase(double phase) {
		for (Vector epicycle : this) {
			epicycle.rot_start += phase * epicycle.rot_speed;
		}
	}
	
	public void scale(double scale) {
		for (Vector epicycle : this) {
			epicycle.scale(scale);
		}
	}
	
	public void scale_rot(double scale) {
		for (Vector epicycle : this) {
			epicycle.rot_speed *= scale;
		}
	}
	
	public void update(double pt) {
		double sx = x;
		double sy = y;
		Iterator<Vector> itr = iterator();
		while (itr.hasNext()) {
			Vector epicycle = itr.next();
			if (epicycle.radius == 0 && epicycle.transition == false) {
				itr.remove();
				continue;
			}
			epicycle.update(sx, sy, pt);
			sx = epicycle.end_x;
			sy = epicycle.end_y;
		}
	}
	
	public Chain flatten_x() {
		Chain red = new Chain(this);
		red.scale(0.5);
		Chain flip = new Chain(red);
		for (Vector e : flip) {
			e.rot_start = Math.PI - e.rot_start;
		}
		flip.scale_rot(-1);
		flip.addAll(red);
		flip = flip.reduce();
		return flip;
	}
	
	public Chain flatten_y() {
		Chain red = new Chain(this);
		red.scale(0.5);
		Chain flip = new Chain(red);
		for (Vector e : flip) {
			e.rot_start = -e.rot_start;
		}
		flip.scale_rot(-1);
		flip.addAll(red);
		flip = flip.reduce();
		return flip;
	}
	
	public Chain reduce() {
		LinkedList<Vector> copy = new LinkedList<Vector>();
		for (Vector epicycle : this) {
			copy.add(epicycle);
		}
		copy.sort(Vector.rot_compare);
		
		Chain reduced = new Chain(shader, circle_texture, arrow_texture, circle_color, arrow_color);
		Iterator<Vector> itr = copy.iterator();
		
		if (!itr.hasNext()) {
			return this;
		}
		Vector sample = itr.next();
		LinkedList<Vector> batch = new LinkedList<Vector>();
		batch.add(sample);
		while (itr.hasNext()) {
			Vector next = itr.next();
			if (Math.abs(next.rot_speed - sample.rot_speed) < 1E-12) {
				batch.add(next);
			}
			else {
				if (batch.size() == 1) {
					reduced.add(batch.getFirst());
				} else {
					reduced.add(combine(batch));				
				}
				sample = next;
				batch = new LinkedList<Vector>();
				batch.add(sample);
			}
		}
		if (batch.size() == 1) {
			reduced.add(batch.getFirst());
		} else {
			reduced.add(combine(batch));				
		}
		return reduced;
	}
	
	public void set_transition(LinkedList<Vector> epicycles, double start_time, double end_time) {
		sort(Vector.rot_compare);
		epicycles.sort(Vector.rot_compare);
		LinkedList<Vector> mag = new LinkedList<Vector>();
		Iterator<Vector> itr1 = iterator();
		Iterator<Vector> itr2 = epicycles.iterator();
		
		Vector e = null;
		Vector o = null;
		
		if (itr1.hasNext()) {
			e = itr1.next();
		}
		if (itr2.hasNext()) {
			o = itr2.next();
		}
		
		do {
			if (o != null && (e == null || e.rot_speed > o.rot_speed)) { // add
				Vector a = new Vector(0, o.rot_start, o.rot_speed, shader, circle_texture, arrow_texture);
				a.transition(o.radius, o.rot_start, start_time, end_time);
				mag.add(a);
				if (itr2.hasNext()) {
					o = itr2.next();
				} else {
					o = null;
				}
			} else if (e != null && o != null && e.rot_speed == o.rot_speed) { // transition
				e.transition(o.radius, o.rot_start, start_time, end_time);
				if (itr1.hasNext()) {
					e = itr1.next();
				} else {
					e = null;
				}
				if (itr2.hasNext()) {
					o = itr2.next();
				} else {
					o = null;
				}
			} else if (e != null && (o == null || e.rot_speed < o.rot_speed)){
				e.transition(0, e.rot_start, start_time, end_time); // remove
				if (itr1.hasNext()) {
					e = itr1.next();
				} else {
					e = null;
				}
			}
		} while (e != null || o != null);
		addAll(mag);
		sort(Vector.rot_abs_compare);
	}
	
	/**
	 * Combines several epicycles of the same rotation into one epicycle.
	 * 
	 * @param epicycles
	 * @return
	 */
	public Vector combine(LinkedList<Vector> epicycles) {
		double rot_speed = epicycles.getFirst().rot_speed;
		double x = 0;
		double y = 0;
		for (Vector epicycle : epicycles) {
			x += epicycle.radius * Math.cos(epicycle.rot_start);
			y += epicycle.radius * Math.sin(epicycle.rot_start);
		}
		double radius = Math.hypot(x,y);
		double rot_start = Math.atan2(y,x);
		return new Vector(radius, rot_start, rot_speed, shader, circle_texture, arrow_texture);
	}
	
	public void render(Matrix4f transform) {
		shader.setUniform("tint", circle_color);
		for (Vector epicycle : this) {
			Entity c = epicycle.circle;
			if (c.scaling_x > 0.1) {
				c.render(transform);
			}
		}
		shader.setUniform("tint", arrow_color);
		for (Vector epicycle : this) {
			Entity a = epicycle.arrow;
			if (a.scaling_x > 0.1) {
				a.render(transform);
			}
		}
	}
}
