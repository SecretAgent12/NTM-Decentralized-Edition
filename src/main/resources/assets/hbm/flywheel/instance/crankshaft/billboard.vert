// Ported from CrankShaft (MIT, Copyright (c) 2026 movblock); modified by SecretAgent12 (NTM 1.21.1 backport) for Flywheel 1.0 / Minecraft 1.21.1.
void flw_instanceVertex(in FlwInstance i) {
    mat3 billboard = transpose(mat3(flw_view));
    flw_vertexPos.xyz = i.position + billboard * (flw_vertexPos.xyz * i.size);
    flw_vertexNormal = billboard * flw_vertexNormal;
    flw_vertexColor *= i.color;
    flw_vertexOverlay = i.overlay;
    flw_vertexLight = max(vec2(i.light) / 256.0, flw_vertexLight);
    flw_vertexTexCoord = i.uvRegion.xy + flw_vertexTexCoord * i.uvRegion.zw;
}
