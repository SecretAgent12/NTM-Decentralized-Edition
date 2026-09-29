// Ported from CrankShaft (MIT, Copyright (c) 2026 movblock); modified by SecretAgent12 (NTM 1.21.1 backport) for Flywheel 1.0 / Minecraft 1.21.1.
void flw_instanceVertex(in FlwInstance i) {
    flw_vertexPos = i.pose * flw_vertexPos;
    flw_vertexNormal = transpose(inverse(mat3(i.pose))) * flw_vertexNormal;
    flw_vertexColor *= i.color;
    flw_vertexOverlay = i.overlay;
    flw_vertexLight = max(vec2(i.light) / 256.0, flw_vertexLight);
    flw_vertexTexCoord = i.uvRegion.xy + flw_vertexTexCoord * i.uvRegion.zw + flw_vertexTexCoord.yx * i.uvShear;
}
