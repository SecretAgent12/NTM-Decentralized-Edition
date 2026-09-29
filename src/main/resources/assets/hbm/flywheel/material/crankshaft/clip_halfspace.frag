// Ported from CrankShaft (MIT, Copyright (c) 2026 movblock); modified by SecretAgent12 (NTM 1.21.1 backport) for Flywheel 1.0 / Minecraft 1.21.1.
// backport: CrankShaft's cutout/clip_halfspace.glsl as a material fragment shader (see clip_slab.frag).
in vec2 hbm_clipData;

void flw_materialFragment() {
    if (hbm_clipData.x > hbm_clipData.y) {
        discard;
    }
}
