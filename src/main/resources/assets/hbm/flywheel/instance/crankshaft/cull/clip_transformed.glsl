// Ported from CrankShaft (MIT, Copyright (c) 2026 movblock); modified by SecretAgent12 (NTM 1.21.1 backport) for Flywheel 1.0 / Minecraft 1.21.1.
#include "flywheel:util/matrix.glsl"

void flw_transformBoundingSphere(in FlwInstance i, inout vec3 center, inout float radius) {
    center += i.slide;
    transformBoundingSphere(i.pose, center, radius);
}
