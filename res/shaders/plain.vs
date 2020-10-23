#version 460

layout(location = 0) in vec3 vertex;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec2 tex_coords;

uniform mat4 world;
uniform mat4 projection;

out vec2 uv;
out vec3 frag_pos;
out vec3 frag_normal;

void main() {
    uv = tex_coords;

    frag_pos = (world * vec4(vertex, 1)).xyz;
    frag_normal = (world * vec4(normal, 0)).xyz;
    

    vec4 pos = projection * vec4(frag_pos, 1);
    gl_Position = pos;
}