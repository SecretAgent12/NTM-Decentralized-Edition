#version 150

// backport: 1.21.1 lightning vertex shader with fog distance (26.x core/rendertype_lightning).
#moj_import <hbm:backport_fog.glsl>

in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

out float vertexDistance;
out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexDistance = hbm_fog_distance(Position, FogShape);
    vertexColor = Color;
}
