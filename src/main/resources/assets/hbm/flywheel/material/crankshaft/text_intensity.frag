// NTM backport: 1.21.1 intensity glyph sheets (rendertype_text_intensity: texture(Sampler0, uv).rrrr * vertexColor).
void flw_materialFragment() {
    flw_fragColor = flw_vertexColor * flw_sampleColor.rrrr;
}
