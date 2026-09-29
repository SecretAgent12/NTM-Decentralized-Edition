// Ported from CrankShaft (MIT, Copyright (c) 2026 movblock); modified by SecretAgent12 (NTM 1.21.1 backport) for Flywheel 1.0 / Minecraft 1.21.1.
// backport: CrankShaft multiplies by the 26.x ColorModulator uniform, which has no Flywheel 1.0 counterpart (vanilla keeps it white).
void flw_materialFragment() {
    flw_fragColor = flw_vertexColor;
}
