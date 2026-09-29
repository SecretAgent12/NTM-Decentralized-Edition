// backport: 1.21.1 fog helpers for the hbm programs (one fog range: FogStart/FogEnd, FogShape 0 = sphere,
// 1 = cylinder), standing in for 26.x's fog.glsl (environmental + render-distance ranges).

float hbm_fog_distance(vec3 pos, int shape) {
    if (shape == 0) {
        return length(pos);
    }
    return max(length(pos.xz), abs(pos.y));
}

vec4 hbm_linear_fog(vec4 inColor, float vertexDistance, float fogStart, float fogEnd, vec4 fogColor) {
    if (vertexDistance <= fogStart) {
        return inColor;
    }
    float fogValue = vertexDistance < fogEnd ? smoothstep(fogStart, fogEnd, vertexDistance) : 1.0;
    return vec4(mix(inColor.rgb, fogColor.rgb, fogValue * fogColor.a), inColor.a);
}

// multiplier fading additive geometry to nothing inside fog (26.x: 1.0 - total_fog_value)
float hbm_fog_fade(float vertexDistance, float fogStart, float fogEnd) {
    if (vertexDistance <= fogStart) {
        return 1.0;
    } else if (vertexDistance >= fogEnd) {
        return 0.0;
    }
    return smoothstep(fogEnd, fogStart, vertexDistance);
}
