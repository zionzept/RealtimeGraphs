package gl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

import java.util.LinkedList;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import engine.Util;

public class Shader {
	
	public static final LinkedList<Shader> shaders = new LinkedList<Shader>();
	
	public static Shader phong;
	public static Shader wood;
	
	private int progID;
	private int vertID;
	private int fragID;
	private String name;
	private String vertPath;
	private String fragPath;
	private Runnable setUniforms;
	
	public Shader(String name, String vertPath, String fragPath) {
		this.name = name + " (" + vertPath + " : " + fragPath + ")";
		this.vertPath = vertPath;
		this.fragPath = fragPath;
		compile();
	}
	
	private void compile() {
		String vert = Util.loadAsString("res/shaders/" + vertPath + ".vs");
		String frag = Util.loadAsString("res/shaders/" + fragPath + ".fs");
		
		progID = glCreateProgram();
		int vertID = glCreateShader(GL_VERTEX_SHADER);
		int fragID = glCreateShader(GL_FRAGMENT_SHADER);
		
		glShaderSource(vertID, vert);
		glShaderSource(fragID, frag);
		
		glCompileShader(vertID);
		if(glGetShaderi(vertID, GL_COMPILE_STATUS) == GL_FALSE) {
			System.err.println(vertPath + ": Failed to compile vertex shader!");
			System.err.println(glGetShaderInfoLog(vertID));
		}
		
		glCompileShader(fragID);
		if(glGetShaderi(fragID, GL_COMPILE_STATUS) == GL_FALSE) {
			System.err.println(fragPath + ": Failed to compile fragment shader!");
			System.err.println(glGetShaderInfoLog(fragID));
		}
		
		glAttachShader(progID, vertID);
		glAttachShader(progID, fragID);
		
		glBindAttribLocation(progID, 0, "vertices");
		glBindAttribLocation(progID, 1, "textures");
		
		glLinkProgram(progID);
		if (glGetProgrami(progID, GL_LINK_STATUS) == GL_FALSE) {
			System.err.println("Failed to link shader program");
			System.err.println(glGetProgramInfoLog(progID));
		}
	}
	
	public void free() {
		glDeleteShader(vertID);
		glDeleteShader(fragID);
		glDeleteProgram(progID);
	}
	
	public void recompile() {
		free();
		compile();
	}
	
	public void setUniforms(Runnable run) {
		setUniforms = run;
	}
	
	public void setUniform(String name, int value) {
		int location = glGetUniformLocation(progID, name);
		if (location != -1) {
			glUniform1i(location, value);
		}
	}
	
	public void setUniform(String name, float value) {
		int location = glGetUniformLocation(progID, name);
		if (location != -1) {
			glUniform1f(location, value);
		}
	}
	
	public void setUniform(String name, Matrix4f value) {
		int location = glGetUniformLocation(progID, name);
		if (location != -1) {
			float[] f = new float[16];
			value.get(f);
			glUniformMatrix4fv(location, false, f);
		}
	}
	
	public void setUniform(String name, Vector3f value) {
		int location = glGetUniformLocation(progID, name);
		if (location != -1) {
			glUniform3f(location, value.x(), value.y(), value.z());
		}
	}
	
	public void setUniform(String name, Vector4f value) {
		int location = glGetUniformLocation(progID, name);
		if (location != -1) {
			glUniform4f(location, value.x(), value.y(), value.z(), value.w());
		}
	}
	
	public void setUniform(String name, boolean value) {
		int location = glGetUniformLocation(progID, name);
		if (location != -1) {
			glUniform1i(location, value ? 1 : 0);
		}
	}
	
	public void bind() {
		glUseProgram(progID);
		setUniforms.run();
	}
	
	@Override
	public String toString() {
		return name;
	}
}
