// backport: 26.x fog filter (color, sphericalDistance, cylindricalDistance) fading by vanilla's total_fog_value;
// Flywheel 1.0's fog filter takes the colour only, with flw_distance and the linear fog range uniforms. Vanilla
// 1.21.1 linear fog is smoothstep(start, end, distance), so the fade is its complement.
vec4 flw_fogFilter(vec4 color) {
    if (flw_distance <= flw_fogRange.x) {
        return color;
    } else if (flw_distance >= flw_fogRange.y) {
        return vec4(0.0);
    }
    return color * smoothstep(flw_fogRange.y, flw_fogRange.x, flw_distance);
}
