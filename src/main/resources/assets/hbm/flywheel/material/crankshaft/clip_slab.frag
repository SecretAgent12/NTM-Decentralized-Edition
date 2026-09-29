// Ported from CrankShaft (MIT, Copyright (c) 2026 movblock); modified by SecretAgent12 (NTM 1.21.1 backport) for Flywheel 1.0 / Minecraft 1.21.1.
// backport: CrankShaft's cutout/clip_slab.glsl as a material fragment shader (see instance/crankshaft/clip_transformed.vert);
// the alpha test (< 0.1) stays with the material's ONE_TENTH cutout.
in vec2 hbm_clipData;

void flw_materialFragment() {
    if (abs(hbm_clipData.x) > hbm_clipData.y) {
        discard;
    }
}
