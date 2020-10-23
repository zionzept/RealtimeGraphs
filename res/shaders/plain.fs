#version 460

uniform sampler2D texture;
uniform vec4 tint = vec4(1.0f, 1.0f, 1.0f, 1.0f);

in vec2 uv;
in vec3 frag_pos;
in vec3 frag_normal;

out vec4 fragColor;

void main() {
    
    fragColor =  texture2D(texture, uv) * tint;
}
