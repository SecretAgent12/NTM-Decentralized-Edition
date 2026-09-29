// Ported from CrankShaft (MIT, Copyright (c) 2026 movblock); modified by SecretAgent12 (NTM 1.21.1 backport) for Flywheel 1.0 / Minecraft 1.21.1.
bool flw_discardPredicate(vec4 color) {
    return flw_sampleColor.a > 0.1;
}
