#version 150

// backport: additive geometry fades out inside fog.
#moj_import <hbm:backport_fog.glsl>

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;

in float vertexDistance;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    fragColor = vertexColor * ColorModulator * hbm_fog_fade(vertexDistance, FogStart, FogEnd);
}
