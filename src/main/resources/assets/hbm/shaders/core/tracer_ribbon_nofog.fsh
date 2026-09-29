#version 150

// backport: 1.21.1 form of hbm core/tracer_ribbon_nofog.fsh.
uniform vec4 ColorModulator;

#moj_import <hbm:tracer_ribbon_fragment.glsl>

out vec4 fragColor;

void main() {
    float perspectiveT;
    vec4 color = hbmRibbonSurface(perspectiveT) * hbmRibbonLight * ColorModulator;
    color.rgb *= color.a;
    fragColor = color;
}
