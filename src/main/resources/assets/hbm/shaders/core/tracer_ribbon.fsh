#version 150

// backport: 1.21.1 form of hbm core/tracer_ribbon.fsh (one fog range; FogShape picks the distance).
#moj_import <hbm:backport_fog.glsl>

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform int FogShape;

#moj_import <hbm:tracer_ribbon_fragment.glsl>

out vec4 fragColor;

void main() {
    float perspectiveT;
    vec4 color = hbmRibbonSurface(perspectiveT) * hbmRibbonLight * ColorModulator;
    float fogDistance = FogShape == 0
            ? mix(hbmRibbonFog.x, hbmRibbonFog.y, perspectiveT)
            : mix(hbmRibbonFog.z, hbmRibbonFog.w, perspectiveT);
    color = hbm_linear_fog(color, fogDistance, FogStart, FogEnd, FogColor);
    color.rgb *= color.a;
    fragColor = color;
}
