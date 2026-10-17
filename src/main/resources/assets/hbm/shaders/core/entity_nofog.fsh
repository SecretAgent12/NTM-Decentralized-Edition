#version 150

// backport: 1.21.1 form of the 26.x entity fragment shader (none fog). AlphaCutout replaces the
// ALPHA_CUTOUT define (-1 = no discard); lightmap/overlay arrive as 1.0 when disabled.


uniform sampler2D Sampler0;

uniform vec4 ColorModulator;

uniform float AlphaCutout;
// backport: DISCARD_ZERO_ALPHA define as a uniform (1.0 = discard fully transparent fragments)
uniform float HbmDiscardZeroAlpha;


in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < AlphaCutout) {
        discard;
    }
    color *= vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color *= lightMapColor;
    if (HbmDiscardZeroAlpha > 0.5 && color.a <= 0.0) {
        discard;
    }
    fragColor = color;
}
