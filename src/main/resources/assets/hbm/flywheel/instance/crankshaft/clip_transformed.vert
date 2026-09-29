// Ported from CrankShaft (MIT, Copyright (c) 2026 movblock); modified by SecretAgent12 (NTM 1.21.1 backport) for Flywheel 1.0 / Minecraft 1.21.1.
// backport: CrankShaft writes the backend varying _flw_clipData read by its clip cutout shaders; Flywheel 1.0 has none,
// so this type declares its own output, read by the hbm:material/crankshaft/clip_*.frag material shaders.
out vec2 hbm_clipData;

void flw_instanceVertex(in FlwInstance i) {
    vec3 slidPos = flw_vertexPos.xyz + i.slide;
    flw_vertexPos = i.pose * vec4(slidPos, flw_vertexPos.w);
    flw_vertexNormal = transpose(inverse(mat3(i.pose))) * flw_vertexNormal;
    flw_vertexColor *= i.color;
    flw_vertexOverlay = i.overlay;
    flw_vertexLight = max(vec2(i.light) / 256.0, flw_vertexLight);
    hbm_clipData = vec2(dot(i.plane.xyz, slidPos), i.plane.w);
}
