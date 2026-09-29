#version 150

// backport: 1.21.1 form of hbm core/tracer_ribbon.vsh. 26.x defines become uniforms (MIN_RIBBON_WIDTH,
// RIBBON_FILTER_PADDING, EMISSIVE -> HbmEmissive); 1.21.1 OpenGL clip depth is -1..1.
in vec3 Position;
in vec3 OtherPosition;
in vec4 Color;
in vec4 OtherColor;
in vec2 Widths;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec2 ScreenSize;
uniform float HbmEmissive;
uniform float MinRibbonWidth;
uniform float RibbonFilterPadding;

vec4 sample_lightmap(sampler2D lightMap, ivec2 uv) {
    return HbmEmissive > 0.5 ? vec4(1.0) : texelFetch(lightMap, uv / 16, 0);
}

float fog_spherical_distance(vec3 pos) {
    return length(pos);
}

float fog_cylindrical_distance(vec3 pos) {
    return max(length(pos.xz), abs(pos.y));
}

#define MIN_RIBBON_WIDTH MinRibbonWidth
#define RIBBON_FILTER_PADDING RibbonFilterPadding
#define NTM_RIBBON_DEPTH_ZERO_TO_ONE 0
#define NTM_RIBBON_POSITION Position
#define NTM_RIBBON_OTHER_POSITION OtherPosition
#define NTM_RIBBON_COLOR Color
#define NTM_RIBBON_OTHER_COLOR OtherColor
#define NTM_RIBBON_WIDTHS Widths
#define NTM_RIBBON_SELECTOR UV0
#define NTM_RIBBON_LIGHT_COORDS UV2
#define NTM_RIBBON_MODEL_VIEW ModelViewMat
#define NTM_RIBBON_PROJECTION ProjMat
#define NTM_RIBBON_SCREEN ScreenSize
#define NTM_RIBBON_VANILLA
#define NTM_RIBBON_FOG
#moj_import <hbm:tracer_ribbon_vertex.glsl>

void main() {
    hbmRibbonVertex();
}
