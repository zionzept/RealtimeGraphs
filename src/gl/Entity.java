package gl;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

import org.joml.Matrix4f;

public class Entity {
	
	private Shader shader;
	private Texture texture;
	
	public float scaling_x;
	public float scaling_y;
	public float rotation;
	public float translation_x;
	public float translation_y;
	
	protected boolean dead;
	
	private static final StaticMesh square = new StaticMesh(
			new float[] {-1, -1, 0, 1, -1, 0, 1, 1, 0, -1, 1, 0}, 
			new float[] {0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1}, 
			new float[] {0, 0, 1, 0, 1, 1, 0, 1}, 
			new int[] {0, 1, 2, 0, 2, 3}
			);
	
	public Entity() {
		scaling_x = 1;
		scaling_y = 1;
	}
	
	public void setShader(Shader shader) {
		this.shader = shader;
	}
	
	public void setTexture(Texture texture) {
		this.texture = texture;
	}

	public void render(Matrix4f transform) {
		texture.bind(0);
		Matrix4f new_transform = new Matrix4f();
		transform.mulAffine(getTransform(), new_transform);
		shader.bind();
		shader.setUniform("world", new_transform);
		square.render();
	}
	
	public Matrix4f getTransform() { // can automatically precalc for performance on stationary, can probably combine cleanup for EntityNode on render to use calculated transform of render call in this class then.
		Matrix4f t0 = new Matrix4f();
		Matrix4f t1;
		// translate
		t0 = new Matrix4f();
		t0.m30(translation_x);
		t0.m31(translation_y);
		t0.m32(0);
		// rotation_z
		t1 = new Matrix4f();
		t1.m00((float) cos(rotation));
		t1.m01((float) sin(rotation));
		t1.m10((float) -sin(rotation));
		t1.m11((float) cos(rotation));
		t0.mulAffine(t1);
		/**
		// rotation_y 
		t1 = new Matrix4f();
		t1.m00((float) cos(rotation_y));
		t1.m02((float) -sin(rotation_y));
		t1.m20((float) sin(rotation_y));
		t1.m22((float) cos(rotation_y));
		t0.mulAffine(t1);
		// rotation_x
		t1 = new Matrix4f();
		t1.m11((float) cos(rotation_x));
		t1.m12((float) sin(rotation_x));
		t1.m21((float) -sin(rotation_x));
		t1.m22((float) cos(rotation_x));
		t0.mulAffine(t1);
		**/
		// scale
		t1 = new Matrix4f();
		t1.m00(scaling_x);
		t1.m11(scaling_y);
		t1.m22(0);
		t0.mulAffine(t1);
		return t0;
	}

	public void scale(float s) {
		scaling_x *= s;
		scaling_y *= s;
	}
	
}
