#version 150

// backport: 1.21.1 form of hbm core/text_fade.fsh; HbmGrayscale replaces IS_GRAYSCALE, AlphaCutout
// replaces ALPHA_CUTOUT (default 0.1).
#moj_import <hbm:backport_fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform float AlphaCutout;
uniform float HbmGrayscale;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(Sampler0, texCoord0);
    if (HbmGrayscale > 0.5) {
        texColor = texColor.rrrr;
    }
    vec4 color = texColor * vertexColor * ColorModulator;
    if (color.a < AlphaCutout) {
        discard;
    }
    fragColor = color * hbm_fog_fade(vertexDistance, FogStart, FogEnd);
}
