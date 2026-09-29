// Ported from CrankShaft (MIT, Copyright (c) 2026 movblock); modified by SecretAgent12 (NTM 1.21.1 backport) for Flywheel 1.0 / Minecraft 1.21.1.
void flw_transformBoundingSphere(in FlwInstance i, inout vec3 center, inout float radius) {
    radius = (length(center) + radius) * i.size;
    center = i.position;
}
