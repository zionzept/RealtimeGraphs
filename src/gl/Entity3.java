package gl;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Entity3 {
	
	public static final Entity3 NULL = new Entity3() {
		@Override
		public void render(Matrix4f transform) {}
		
		@Override
		public Matrix4f getTransform() {
			return new Matrix4f();
		}
		
		@Override
		public Vector3f getScaling() {
			return new Vector3f();
		}
	};
	
	protected Shader shader;
	protected StaticMesh mesh;
	//protected Material material;
	
	protected float scaling_x;
	protected float scaling_y;
	protected float scaling_z;
	protected float rotation_x;
	protected float rotation_y;
	protected float rotation_z;
	protected float translation_x;
	protected float translation_y;
	protected float translation_z;
	
	protected boolean dead;
	
	public Entity3() {
		//materials = new LinkedList<Material>();
		scaling_x = 1;
		scaling_y = 1;
		scaling_z = 1;
	}
	
	public float x() {
		return translation_x;
	}
	
	public float y() {
		return translation_y;
	}
	
	public float z() {
		return translation_z;
	}
	
	public void setShader(Shader shader) {
		this.shader = shader;
	}
	
	public void setMesh(StaticMesh mesh) {
		this.mesh = mesh;
	}

	public void render(Matrix4f transform) {
		Matrix4f new_transform = new Matrix4f();
		transform.mulAffine(getTransform(), new_transform);
		shader.bind();
		shader.setUniform("world", new_transform);
		
		//material.materialize(shader);
		mesh.render();
	}
	
	public Matrix4f getTransform() { // can automatically precalc for performance on stationary, can probably combine cleanup for EntityNode on render to use calculated transform of render call in this class then.
		Matrix4f t0 = new Matrix4f();
		Matrix4f t1;
		// translate
		t0 = new Matrix4f();
		t0.m30(translation_x);
		t0.m31(translation_y);
		t0.m32(translation_z);
		// rotation_z
		t1 = new Matrix4f();
		t1.m00((float) cos(rotation_z));
		t1.m01((float) sin(rotation_z));
		t1.m10((float) -sin(rotation_z));
		t1.m11((float) cos(rotation_z));
		t0.mulAffine(t1);
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
		// scale
		t1 = new Matrix4f();
		t1.m00(scaling_x);
		t1.m11(scaling_y);
		t1.m22(scaling_z);
		t0.mulAffine(t1);
		return t0;
	}
	
	public void setTransform(Entity3 e) {
		scaling_x = e.scaling_x;
		scaling_y = e.scaling_y;
		scaling_z = e.scaling_z;
		rotation_x = e.rotation_x;
		rotation_y = e.rotation_y;
		rotation_z = e.rotation_z;
		translation_x = e.translation_x;
		translation_y = e.translation_y;
		translation_z = e.translation_z;
	}

	public void scale(float x, float y, float z) {
		scaling_x *= x;
		scaling_y *= y;
		scaling_z *= z;
	}

	public void scale(float s) {
		scale(s, s, s);
	}
	
	public Vector3f getScaling() {
		return new Vector3f(scaling_x, scaling_y, scaling_z);
	}
	
	public void rotate(Vector3f rot) {
		rotation_x += rot.x;
		rotation_y += rot.y;
		rotation_z += rot.z;
	}
	
	public void rotate(float x, float y, float z) {
		rotation_x += x;
		rotation_y += y;
		rotation_z += z;
	}
	
	public void setRotation(float x, float y, float z) {
		rotation_x = x;
		rotation_y = y;
		rotation_z = z;
	}
	
	public void setRotation(Vector3f rot) {
		rotation_x = rot.x;
		rotation_y = rot.y;
		rotation_z = rot.z;
	}

	public void rotate_x(float r) {
		rotation_x += r;
	}

	public void rotate_y(float r) {
		rotation_y += r;
	}

	public void rotate_z(float r) {
		rotation_z += r;
	}
	
	public Vector3f getRotation() {
		return new Vector3f(rotation_x, rotation_y, rotation_z);
	}
	
	public void translate(Vector3f vec) {
		translation_x += vec.x;
		translation_y += vec.y;
		translation_z += vec.z;
	}

	public void translate(float x, float y, float z) {
		translation_x += x;
		translation_y += y;
		translation_z += z;
	}
	
	public void setTranslation(float x, float y, float z) {
		translation_x = x;
		translation_y = y;
		translation_z = z;
	}
	
	public void setTranslation(Vector3f vec) {
		translation_x = vec.x;
		translation_y = vec.y;
		translation_z = vec.z;
	}
	
	public Vector3f getTranslation() {
		return new Vector3f(translation_x, translation_y, translation_z);
	}
	
}
