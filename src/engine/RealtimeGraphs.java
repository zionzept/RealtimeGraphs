package engine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import gl.Entity;
import gl.FrameBuffer;
import gl.Shader;
import gl.Texture;
import window.GLApplication;
import window.SHotkey;
import window.SKeyboard;
import window.SMouse;
import window.Window;

public class RealtimeGraphs implements GLApplication {
	private static final double tau = Math.PI * 2;
	private static final double FADE_DT = 0.1;
	
	private Shader shader;
	
	private Texture fade_texture;
	private Texture circle_texture;
	private Texture arrow_texture;
	private Texture point_texture;
	
	private FrameBuffer drawing_fbo;
	private FrameBuffer path_fbo;
	
	private Entity fader;
	private Entity drawing;
	private Entity point;
	
	private LinkedList<Chain> chains;
	private LinkedList<Pen> pens;
	private LinkedList<Point> points;
	
	private Vector4f white = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
	private Vector4f gray = new Vector4f(0.6f, 0.8f, 0.8f, 0.1f);
	private Vector4f amber = new Vector4f(1.0f, 0.5f, 0.0f, 0.6f);
	private Vector4f red = new Vector4f(1.0f, 0.0f, 0.0f, 0.6f);
	private Vector4f cyan = new Vector4f(0.2f, 1.0f, 1.0f, 1.0f);
	private Vector4f green = new Vector4f(0.2f, 1.0f, 0.1f, 1.0f);
	private Vector4f shade = new Vector4f(1.0f, 1.0f, 1.0f, 0.05f);
	private Vector4f magenta = new Vector4f(1.0f, 0.0f, 1.0f, 1.0f);

	private ReentrantLock mod_lock;
	
	private Chain fourier;
	private Chain fourier_transition;
	private Pen fourier_pen;
	
	private Point highlight_point;
	private boolean dragging_point;
	
	private double blink_time = 10;
	
	private LinkedList<Point> bootleg_rect_points = new LinkedList<>();
	
	public static void main(String[] args) {
		RealtimeGraphs light_engine = new RealtimeGraphs();
		Window.create(light_engine, "Light Engine");
	}
	
	@Override
	public void init() {
		
		Matrix4f projection = new Matrix4f();
		projection.ortho2D(0, Window.w, 0, Window.h);
		
		shader = new Shader("plain", "plain", "plain");
		
		Runnable uniforms_strat = new Runnable() {
			@Override
			public void run() {
				shader.setUniform("projection", projection);
			}
		};
		shader.setUniforms(uniforms_strat);
		
		circle_texture = new Texture("circle.png");
		arrow_texture = new Texture("arrow.png");
		point_texture = new Texture("point.png");
		fade_texture = new Texture("black.png");
	
		point = new Entity();
		point.setShader(shader);
		point.setTexture(point_texture);
		point.scale(10);
		
		fader = new Entity();
		fader.setShader(shader);
		fader.setTexture(fade_texture);
		fader.translation_x = Window.w/2;
		fader.translation_y = Window.h/2;
		fader.scaling_x = Window.w/2;
		fader.scaling_y = Window.h/2;
		
		this.points = new LinkedList<Point>();
		this.chains = new LinkedList<Chain>();
		this.pens = new LinkedList<Pen>();
		
		drawing_fbo = new FrameBuffer();
		drawing = new Entity();
		drawing.setShader(shader);
		drawing.setTexture(drawing_fbo.texture);
		drawing.scaling_x = Window.w/2;
		drawing.scaling_y = Window.h/2;
		drawing.translation_x = Window.w/2;
		drawing.translation_y = Window.h/2;

		fourier = new Chain(shader, circle_texture, arrow_texture, new Vector4f(0.4f, 1.0f, 1.0f, 0.00f), new Vector4f(1.0f, 0.7f, 0.5f, 0.4f));
		fourier.setPos(Window.w/2, Window.h/2);
		chains.add(fourier);
		
		fourier_pen = new Pen(magenta, shader, point_texture);
		fourier_pen.target(fourier);
		pens.add(fourier_pen);
		
		mod_lock = new ReentrantLock();
		
		registerInput();
		
		dt(1d);
		pt = 0;
		rpt = 0;
	}
	
	

	@Override
	public void close() {
		drawing_fbo.delete();
	}
	
	private double lt;
	private double dt;
	private double pt;
	private double rpt;
	private void dt(double factor) {
		double ct = System.currentTimeMillis();
		dt = (ct - lt)/1000d;
		rpt += dt;
		dt *= factor;
		pt += dt;
		lt = ct;
	}

	private double fade_t;
	private double m_factor = 1;
	private int prev_point_counter = 0;
	
	@Override
	public void update() {
		mod_lock.lock();
		
		double mx = SMouse.getX();
		double my = Window.h - SMouse.getY();
		
		if (dragging_point) {
			if (!SMouse.isPressed(GLFW.GLFW_MOUSE_BUTTON_1)) {
				dragging_point = false;
			} else if (highlight_point.move(mx, my)) {
				updateFourierVectors(1E-8);
			}
		} else {
			highlight_point = null;
			for (Point p : points) {
				if (Math.hypot(mx - p.x, my - p.y) < 5) {
					highlight_point = p;
					break;
				}
			}
			if (highlight_point != null) {
				dragging_point = SMouse.isPressed(GLFW.GLFW_MOUSE_BUTTON_1);
			}
			
		}
		
		
		double factor = m_factor;
		double slow_start = 2;
		if (rpt < slow_start) {
			double x = rpt / slow_start;
			factor *= x*x*(3-2*x);
		}
		dt(factor);
		fade_t += dt;
		if (chains.isEmpty()) {
			pt = 0;
		}
		
		// blink points
		double cycle_loc = pt / tau;
		cycle_loc = cycle_loc - (int)cycle_loc;
		int point_counter = (int)(cycle_loc * points.size());
		if (points.size() > 0 && prev_point_counter != point_counter) {
			prev_point_counter = point_counter;
			points.get(point_counter).blink(pt, pt+blink_time/points.size());
		}
		
		
		
		Matrix4f transform = new Matrix4f();
		for (Chain e : this.chains) {
			e.update(pt);
		}
		{
			drawing_fbo.bind();
			if (fade_t >= FADE_DT) {
				fade_t = 0;
				shader.setUniform("tint", shade);
				fader.render(transform);
			}
			
			for (Pen pen : pens) {
				pen.render(transform);
			}
			
			
			// draw
			Vector vec = new Vector(1, 0, 0, shader, circle_texture, arrow_texture);
			Chain chain = new Chain(shader, circle_texture, arrow_texture, amber, amber);
			chain.add(vec);
			Pen pen = new Pen(green, shader, point_texture);
			pen.target(chain);

			while (bootleg_rect_points.size() >= 4) {
				Point[] p = new Point[4];
				for (int i = 0; i < 4; ++i) {
					p[i] = bootleg_rect_points.removeFirst();
				}
				vec.end_x = p[0].x;
				vec.end_y = p[0].y;
				pen.skip();
				vec.end_x = p[1].x;
				vec.end_y = p[1].y;
				pen.render(transform);
				vec.end_x = p[2].x;
				vec.end_y = p[2].y;
				pen.render(transform);
				vec.end_x = p[3].x;
				vec.end_y = p[3].y;
				pen.render(transform);
				vec.end_x = p[0].x;
				vec.end_y = p[0].y;
				pen.render(transform);
			}
			
			
			drawing_fbo.unbind();
		}
		shader.setUniform("tint", white);
		drawing.render(transform);
		
		for (Chain e : this.chains) {
			e.render(transform);
		}
		
		shader.setUniform("tint", amber);
		Iterator<Point> point_itr = points.iterator();
		Point p;
		Vector4f color;
		double blink_value;
		if (point_itr.hasNext()) {
			p = point_itr.next();
			color = p == highlight_point ? cyan : red;
			blink_value = p.blink_value(pt);
			if (blink_value > 0) {
				color = mix(color, white, (float)blink_value);
			}
			while (true) {
				point.translation_x = (float)p.x;
				point.translation_y = (float)p.y;
				shader.setUniform("tint", color);
				point.render(transform);				
				if (!point_itr.hasNext()) {
					break;
				}
				p = point_itr.next();
				color = p == highlight_point ? cyan : amber;
				blink_value = p.blink_value(pt);
				if (blink_value > 0) {
					color = mix(color, white, (float)blink_value);
				}
			}
		}
		
		mod_lock.unlock();
	}
	
	private Vector4f mix(Vector4f a, Vector4f b, float x) {
		float y = 1-x;
		return new Vector4f(y*a.x + x*b.x, y*a.y + x*b.y, y*a.z + x*b.z, y*a.w + x*b.w);
	}

	private void updateFourierVectors(double dt) {
		double x = Window.w/2d;
		double y = Window.h/2d;
		
		LinkedList<Point> fpoints = Fourier.transform(points, x, y);
		
		mod_lock.lock();
		fourier_transition = new Chain(shader, circle_texture, arrow_texture, gray, white, fpoints, 0.5);
		fourier_transition.setPos(x, y);
		fourier.set_transition(fourier_transition, pt, pt+dt);
		flushUpdate();
		fourier.sort(Vector.rot_abs_compare);
		
		mod_lock.unlock();
	}
	
	private void wipe() {
		mod_lock.lock();
		points.clear();
		pens.clear();
		chains.clear();
		drawing_fbo.clear();
		mod_lock.unlock();
	}
	
	final int SPACING = 40;
	
	private void registerInput() {
		SKeyboard.addHotkey(new SHotkey(GLFW.GLFW_KEY_G) {
			@Override
			public void addRequirements() {
			}
			@Override
			public void actuation() {
				mod_lock.lock();
				points.clear();
				int s = 12;
				HashSet<IntPoint> set = new HashSet<>();
				for (int i = 0; i < 50; ++i) {
					int x = (int)(Math.random()*2*s) - s;
					int y = (int)(Math.random()*2*s) - s;
					IntPoint p = new IntPoint(x, y);
					if (set.add(p)) {
						points.add(new Point(x*SPACING+Window.w*0.5, y*SPACING+Window.h*0.5));
					}
				}
				System.out.println(set.size());
				
				// find rectangles
				LinkedList<Point> rect_points = new LinkedList<>();
				HashMap<Pair<Integer, Integer>, LinkedList<Pair<IntPoint, IntPoint>>> level_lines = new HashMap<>();
				for (IntPoint a : set) {
					for (IntPoint b : set) {
						if (a.x == b.x && a.y < b.y) {
							Pair<IntPoint, IntPoint> al = new Pair<IntPoint, IntPoint>(a, b);
							Pair<Integer, Integer> level = new Pair<Integer, Integer>(a.y, b.y);
							LinkedList<Pair<IntPoint, IntPoint>> line_list = level_lines.get(level);
							if (line_list == null) {
								line_list = new LinkedList<>();
								level_lines.put(level, line_list);
							} else {
								for (Pair<IntPoint, IntPoint> bl : line_list) {
									rect_points.add(new Point(al.a.x*SPACING+Window.w*0.5, al.a.y*SPACING+Window.h*0.5));
									rect_points.add(new Point(al.b.x*SPACING+Window.w*0.5, al.b.y*SPACING+Window.h*0.5));
									rect_points.add(new Point(bl.b.x*SPACING+Window.w*0.5, bl.b.y*SPACING+Window.h*0.5));
									rect_points.add(new Point(bl.a.x*SPACING+Window.w*0.5, bl.a.y*SPACING+Window.h*0.5));
								}
							}
							line_list.add(al);
						}
					}
				}
				
				// pass out and handle in render
				bootleg_rect_points = rect_points;
				
				mod_lock.unlock();
			}
		});
		
		SKeyboard.addHotkey(new SHotkey(GLFW.GLFW_KEY_L) {
			@Override
			public void addRequirements() {
			}
			@Override
			public void actuation() {
				mod_lock.lock();
				points.clear();
				int s = 10;
				int point_count = 50;
				HashSet<IntPoint> set = new HashSet<>();
				for (int i = 0; i < point_count; ++i) {
					int x = (int)(Math.random()*2*s) - s;
					int y = (int)(Math.random()*2*s) - s;
					IntPoint p = new IntPoint(x, y);
					if (set.add(p)) {
						if (SKeyboard.isPressed(GLFW.GLFW_KEY_LEFT_SHIFT))
								points.add(new Point(x*SPACING+Window.w*0.5, y*SPACING+Window.h*0.5));
					}
				}
				
//				int[] xx = new int[] {0,5,-1,4};
//				int[] yy = new int[] {0,1,5,6};
//				for (int i = 0; i < 4; ++i) {
//					int x = xx[i];
//					int y = yy[i];
//					iPoint p = new iPoint(x,y);
//					set.add(p);
//					points.add(new Point(x*SPACING+Window.w*0.5, y*SPACING+Window.h*0.5));
//				}
				
				
				
				System.out.println(set.size());
				
				
				int rects = 0;
				
				// find rectangles
				LinkedList<Point> rect_points = new LinkedList<>();
				HashMap<Triplet<Integer, Integer, Integer>, LinkedList<Pair<IntPoint, IntPoint>>> rect_diags = new HashMap<>(point_count);
				for (IntPoint a : set) {
					for (IntPoint b : set) {
						if (a.x < b.x || (a.x == b.x && a.y < b.y)) {
							Pair<IntPoint, IntPoint> diag_a = new Pair<IntPoint, IntPoint>(a, b);
							Triplet<Integer, Integer, Integer> rect_key = new Triplet<>(a.x + b.x, a.y + b.y, (b.x-a.x) * (b.x-a.x) + (b.y-a.y) * (b.y-a.y));
							LinkedList<Pair<IntPoint, IntPoint>> diag_list = rect_diags.get(rect_key);
							if (diag_list == null) {
								diag_list = new LinkedList<>();
								rect_diags.put(rect_key, diag_list);
							} else {
								for (Pair<IntPoint, IntPoint> diag_b : diag_list) {
									rects++;
									rect_points.add(new Point(diag_a.a.x*SPACING+Window.w*0.5, diag_a.a.y*SPACING+Window.h*0.5));
									rect_points.add(new Point(diag_b.a.x*SPACING+Window.w*0.5, diag_b.a.y*SPACING+Window.h*0.5));
									rect_points.add(new Point(diag_a.b.x*SPACING+Window.w*0.5, diag_a.b.y*SPACING+Window.h*0.5));
									rect_points.add(new Point(diag_b.b.x*SPACING+Window.w*0.5, diag_b.b.y*SPACING+Window.h*0.5));
									
								}
							}
							diag_list.add(diag_a);
						}
					}
				}
				
				System.out.println(rects + " rectangles found");
				
				// pass out and handle in render
				if (SKeyboard.isPressed(GLFW.GLFW_KEY_LEFT_SHIFT))
					bootleg_rect_points = rect_points;
				
				mod_lock.unlock();
			}
		});
		
		SKeyboard.addHotkey(new SHotkey(GLFW.GLFW_KEY_S) {
			@Override
			public void addRequirements() {
			}
			@Override
			public void actuation() {
				double m_exp = 0.1*Window.h - SMouse.getY();
				m_factor = Math.pow(1.02, m_exp);
			}
		});
		SKeyboard.addHotkey(new SHotkey(GLFW.GLFW_KEY_T) {
			@Override
			public void addRequirements() {
			}
			@Override
			public void actuation() {
				pt = 0;
				flushUpdate();
			}
		});
		SKeyboard.addHotkey(new SHotkey(GLFW.GLFW_KEY_W) {
			@Override
			public void addRequirements() {
			}
			@Override
			public void actuation() {
				wipe();
			}
		});
		SKeyboard.addHotkey(new SHotkey(GLFW.GLFW_KEY_P) {
			@Override
			public void addRequirements() {
			}
			@Override
			public void actuation() {
				mod_lock.lock();
				points.clear();
				updateFourierVectors(1);
				mod_lock.unlock();
			}
		});
		SKeyboard.addHotkey(new SHotkey(GLFW.GLFW_KEY_PERIOD) {
			@Override
			public void addRequirements() {
			}
			@Override
			public void actuation() {
				if (points.isEmpty()) {
					return;
				}
				mod_lock.lock();
				points.removeLast();
				updateFourierVectors(0.1);
				mod_lock.unlock();
			}
		});
		SKeyboard.addHotkey(new SHotkey(GLFW.GLFW_KEY_COMMA) {
			@Override
			public void addRequirements() {
			}
			@Override
			public void actuation() {
				if (points.isEmpty()) {
					return;
				}
				mod_lock.lock();
				points.removeFirst();
				updateFourierVectors(1);
				mod_lock.unlock();
			}
		});
		SKeyboard.addHotkey(new SHotkey(GLFW.GLFW_KEY_E) {
			@Override
			public void addRequirements() {
			}
			@Override
			public void actuation() {
				mod_lock.lock();
				points.add(new Point(SMouse.getX(), Window.h-SMouse.getY()));
				updateFourierVectors(1);
				mod_lock.unlock();
			}
		});
		SKeyboard.addHotkey(new SHotkey(GLFW.GLFW_KEY_B) {
			@Override
			public void addRequirements() {
			}
			@Override
			public void actuation() {
				mod_lock.lock();
				LinkedList<Point> rev_points = new LinkedList<Point>();
				for (Point point : points) {
					rev_points.addFirst(point);
				}
				points.addAll(rev_points);
				mod_lock.unlock();
			}
		});
		
		SKeyboard.addHotkey(new SHotkey(GLFW.GLFW_KEY_F) {
			@Override
			public void addRequirements() {
			}
			@Override
			public void actuation() {
				updateFourierVectors(1);
			}
		});
		SKeyboard.addHotkey(new SHotkey(GLFW.GLFW_KEY_X) {
			@Override
			public void addRequirements() {
			}

			@Override
			public void actuation() {
				mod_lock.lock();
				fourier.set_transition(fourier.flatten_x(), pt, pt+2);
				flushUpdate();
				mod_lock.unlock();
			}
			
		});
		SKeyboard.addHotkey(new SHotkey(GLFW.GLFW_KEY_Y) {
			@Override
			public void addRequirements() {
			}

			@Override
			public void actuation() {
				mod_lock.lock();
				fourier.set_transition(fourier.flatten_y(), pt, pt+2);
				flushUpdate();
				mod_lock.unlock();
			}
			
		});
		SKeyboard.addHotkey(new SHotkey(GLFW.GLFW_KEY_1) {
			@Override
			public void addRequirements() {
			}
			@Override
			public void actuation() {
				mod_lock.lock();
				scene1();
				mod_lock.unlock();
			}
		});
		SKeyboard.addHotkey(new SHotkey(GLFW.GLFW_KEY_2) {
			@Override
			public void addRequirements() {
			}
			@Override
			public void actuation() {
				mod_lock.lock();
				scene2();
				mod_lock.unlock();
			}
		});
		SKeyboard.addHotkey(new SHotkey(GLFW.GLFW_KEY_3) {
			@Override
			public void addRequirements() {
			}
			@Override
			public void actuation() {
				mod_lock.lock();
				scene3();
				mod_lock.unlock();
			}
		});
	}
	
	private void flushUpdate() {
		for (Chain e : this.chains) {
			e.update(pt);
		}
		for (Pen p : pens) {
			p.skip();
		}
	}
	
	private void scene1() {
		Chain chain;
		Pen pen;
		
		int len = 100;
		double sym = 2d;
		double s = 100;
		
		LinkedList<Chain> chains = new LinkedList<Chain>();
		chain = new Chain(shader, circle_texture, arrow_texture, gray, white);
		chains.add(chain);
		
		chain.add(new Vector(50, Math.PI, -7d, shader, circle_texture, arrow_texture));
		chain.rotate(-Math.PI/8);
		for (int j = 0; j < 2; j++) {
			for (int i = 0; i < len; i++) {
				double radius = s / (sym*i+1);
				double rot_start = 0;
				double rot_speed = sym*i+1;
				chain.add(new Vector(radius, rot_start, rot_speed, shader, circle_texture, arrow_texture));
			}
			for (int i = 0; i < len; i++) {
				double radius = s / (sym*i+1);
				double rot_start = Math.PI;
				double rot_speed = -(sym*i+1);
				chain.add(new Vector(radius, rot_start, rot_speed, shader, circle_texture, arrow_texture));
			}
			chain.phase(-Math.PI/2);
			chain.rotate(Math.PI/2);
		}
		chain.phase(Math.PI/32);
		chain.rotate(Math.PI/2);
		for (int j = 0; j < 2; j++) {
			for (int i = 0; i < len; i++) {
				double radius = s / (sym*i+1);
				double rot_start = 0;
				double rot_speed = sym*i+1;
				chain.add(new Vector(radius, rot_start, rot_speed, shader, circle_texture, arrow_texture));
			}
			for (int i = 0; i < len; i++) {
				double radius = s / (sym*i+1);
				double rot_start = Math.PI;
				double rot_speed = -(sym*i+1);
				chain.add(new Vector(radius, rot_start, rot_speed, shader, circle_texture, arrow_texture));
			}
			chain.phase(-Math.PI/2);
			chain.rotate(Math.PI/2);
		}
		chain.phase(-Math.PI/64);
		chain.rotate(-Math.PI/4);
		
		pen = new Pen(green, shader, point_texture);
		pen.target(chain);
		pens.add(pen);
		
		System.out.println("sz: " + chain.size());
		chain = chain.reduce();
		System.out.println("sz: " + chain.size());
		chain.sort(Vector.middle_out_rot_compare);
		chains.add(chain);
		pen = new Pen(red, shader, point_texture);
		pen.target(chain);
		pens.add(pen);
		
		double sx = Window.w / 2;
		double sy = Window.h / 2;
		
		sx -= 400;
		for (Chain e : chains) {
			e.setPos(sx, sy);
			sx += 800;
		}
		this.chains.addAll(chains);
		flushUpdate();
	}
	
	private void scene2() {
		Chain chain;
		Pen pen;
		
		chain = new Chain(shader, circle_texture, arrow_texture, gray, white);
		double sx = Window.w / 2;
		double sy = Window.h / 2;
		chain.setPos(sx, sy);
		Vector epi = new Vector(100, 0, 1, shader, circle_texture, arrow_texture);
		epi.transition(300, -2, pt, pt+1);
		chain.add(epi);
		
		pen = new Pen(green, shader, point_texture);
		pen.target(chain);
		pens.add(pen);
		
		this.chains.add(chain);
		
		flushUpdate();
	}
	
	private void scene3() {
		Chain chain;
		Pen pen;
		
		chain = new Chain(shader, circle_texture, arrow_texture, gray, white);
		double sx = Window.w / 2;
		double sy = Window.h / 2;
		chain.setPos(sx, sy);
		Vector epi;
		double rad = 1;
		int n = 1000;
		for (int i = -n; i <= n; ++i) {
			if (i == 0) {
				continue;
			}
			double s = i/10d;
			double rs = 0;
			double r = rad*1/(s*s+1);
			if (r<0) {
				rad *= -1;
				rs = Math.PI;
			}
			System.out.println("ASD:  "  +  r +  ", " + rs);
			epi = new Vector(50.0/i, -Math.PI, i, shader, circle_texture, arrow_texture);
			
			chain.add(epi);
			//epi = new Epicycle(15, 0, -i, shader, circle_texture, arrow_texture);
			//epicycles.add(epi);
		}
		
		
		pen = new Pen(green, shader, point_texture);
		pen.target(chain);
		pens.add(pen);
		
		chain.sort(Vector.rot_abs_compare);
		this.chains.add(chain);
		
		flushUpdate();
	}
}
