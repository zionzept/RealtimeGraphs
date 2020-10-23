package engine;

import java.util.Comparator;
import java.util.LinkedList;

import gl.Entity;
import gl.Shader;
import gl.Texture;

public class Vector {
	
	public static final Comparator<Vector> random_compare = new Comparator<Vector>() {
		@Override
		public int compare(Vector o1, Vector o2) {
			return Math.random() < 0.5 ? -1 : 1;
		}
	};
	
	public static final Comparator<Vector> radius_compare = new Comparator<Vector>() {
		@Override
		public int compare(Vector o1, Vector o2) {
			double dr = (o1.transition ? o1.radius_target : o1.radius) - (o2.transition ? o2.radius_target : o2.radius);
			if (dr > 0) {
				return -1;
			}
			if (dr < 0) {
				return 1;
			}
			dr = o1.rot_speed - o2.rot_speed;
			if (dr > 0) {
				return 1;
			}
			if (dr < 0) {
				return -1;
			}
			return 0;
		}
	};
	
	public static final Comparator<Vector> rot_compare = new Comparator<Vector>() {
		@Override
		public int compare(Vector o1, Vector o2) {
			double dr = o1.rot_speed - o2.rot_speed;
			if (dr < 0) {
				return -1;
			}
			if (dr > 0) {
				return 1;
			}
			return 0;
		}
	};
	
	public static final Comparator<Vector> rot_abs_compare = new Comparator<Vector>() {
		@Override
		public int compare(Vector o1, Vector o2) {
			if (Math.abs(o1.rot_speed) < Math.abs(o2.rot_speed)) {
				return -1;
			}
			return 1;
		}
	};
	
	public static final Comparator<Vector> middle_out_rot_compare = new Comparator<Vector>() {
		@Override
		public int compare(Vector o1, Vector o2) {
			double rs1 = o1.rot_speed;
			double rs2 = o2.rot_speed;
			if (rs1 < 0) {
				rs1 = -rs1*1E100;
			}
			if (rs2 < 0) {
				rs2 = -rs2*1E100;
			}
			if (rs1 > rs2) {
				return 1;
			}
			return -1;
		}
	};

	public double radius;
	public double rot_start;
	public double rot_speed;
	
	public final Entity circle;
	public final Entity arrow;
	
	public double start_x;
	public double start_y;
	public double end_x;
	public double end_y;
	public double rot;
	
	private Shader shader;
	private Texture circle_texture;
	private Texture arrow_texture;
	
	public Vector(Vector epicycle) {
		this(epicycle.radius, epicycle.rot_start, epicycle.rot_speed, epicycle.shader, epicycle.circle_texture, epicycle.arrow_texture);
	}
	
	public Vector (double radius, double rot_start, double rot_speed, Shader shader, Texture circle_texture, Texture arrow_texture) {
		this.radius = radius;
		this.rot_start = rot_start;
		this.rot_speed = rot_speed;
		
		this.shader = shader;
		this.circle_texture = circle_texture;
		this.arrow_texture = arrow_texture;
		
		circle = new Entity();
		circle.setShader(shader);
		circle.setTexture(circle_texture);
		
		arrow = new Entity();
		arrow.setShader(shader);
		arrow.setTexture(arrow_texture);
		
		set_scale(radius);
	}
	
	boolean transition;
	private double radius_start;
	private double radius_target;
	private double rot_start_start;
	private double rot_start_target;
	private double transition_start_time;
	private double transition_end_time;
	
	private double x0;
	private double x1;
	private double y0;
	private double y1;
	
	public void transition(double radius_target, double rot_start_target, double start_time, double end_time) {
		radius_start = radius;
		this.radius_target = radius_target;
		rot_start_start = rot_start;
		this.rot_start_target = rot_start_target;
		if (rot_start_target - rot_start_start > Math.PI) {
			rot_start_start += 2*Math.PI;
		} else if (rot_start_target - rot_start_start < -Math.PI) {
			rot_start_start -= 2*Math.PI;
		}
		x0 = radius * Math.cos(rot_start);
		y0 = radius * Math.sin(rot_start);
		x1 = radius_target * Math.cos(rot_start_target);
		y1 = radius_target * Math.sin(rot_start_target);
		
		transition_start_time = start_time;
		transition_end_time = end_time;
		transition = true;
	}
	
	public void update(double start_x, double start_y, double pt) {
		if (transition) {
			if (pt >= transition_end_time) {
				radius = radius_target;
				rot_start = rot_start_target;
				set_scale(radius);
				transition = false;
			} else {
				double t = 1 - (transition_end_time - pt) / (transition_end_time - transition_start_time);
				double f = t * t * t * (t * (t * 6 - 15) + 10);
				
				double x = f*x1 + (1-f)*x0;
				double y = f*y1 + (1-f)*y0;
				
				double r = Math.sqrt(x*x + y*y);
				double a = Math.atan2(y, x);
				
				//set_scale(r);
				//rot_start = a;
				
				set_scale(f*radius_target + (1-f) * radius_start);
				rot_start = f*rot_start_target + (1-f) * rot_start_start;
			}
		}
		this.start_x = start_x;
		this.start_y = start_y;
		this.rot = rot_start + pt * rot_speed;
		this.end_x = start_x + radius * Math.cos(rot);
		this.end_y = start_y + radius * Math.sin(rot);
		circle.rotation = (float)rot;
		circle.translation_x = (float)start_x;
		circle.translation_y = (float)start_y;
		arrow.rotation = (float)rot;
		arrow.translation_x = (float)start_x;
		arrow.translation_y = (float)start_y;
	}
	
	public void set_scale(double scale) {
		radius = scale;
		float entity_scale = (float)scale;
		circle.scaling_x = entity_scale;
		circle.scaling_y = entity_scale;
		double min_x = 2;
		arrow.scaling_x = (float)(min_x*Math.pow(2.718281828904590, -entity_scale/min_x)+entity_scale);
		double min_y = 20;
		double entity_scale_y = Math.pow(entity_scale, 0.8);
		arrow.scaling_y = (float)(min_y*Math.pow(2.718281828904590, -entity_scale_y/min_y)+entity_scale_y);
	}
	
	public void scale(double scale) {
		set_scale(this.radius * scale);
	}
}
