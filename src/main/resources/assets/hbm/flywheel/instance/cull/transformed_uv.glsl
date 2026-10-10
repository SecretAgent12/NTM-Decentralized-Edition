// backport-fix: BF-086 the same code as flywheel:instance/cull/transformed.glsl, but a file of its own. Flywheel 1.0
// caches compiled shaders by name and names a cull shader after its file only, while the code it compiles embeds the
// instance layout; types sharing a cull file therefore share the first one's layout. Keep one file per layout.
#include "flywheel:util/matrix.glsl"

void flw_transformBoundingSphere(in FlwInstance i, inout vec3 center, inout float radius) {
    transformBoundingSphere(i.pose, center, radius);
}
