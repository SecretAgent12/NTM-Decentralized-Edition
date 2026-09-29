#version 150

// backport: 1.21.1 form of the 26.x hbm:core/particle_nofog fragment shader; AlphaCutout replaces ALPHA_CUTOUT (default 0.1).


uniform sampler2D Sampler0;

uniform vec4 ColorModulator;

uniform float AlphaCutout;


in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a <= 0.0 || color.a < AlphaCutout) {
        discard;
    }
    fragColor = color;
}
