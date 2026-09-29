#version 150

// backport: 1.21.1 form of the 26.x core/entity vertex shader. The 26.x defines are uniforms:
// HbmFlags.x = cardinal lighting (!NO_CARDINAL_LIGHTING), .y = lightmap (!EMISSIVE),
// .z = overlay (!NO_OVERLAY), .w = APPLY_TEXTURE_MATRIX.
#moj_import <hbm:backport_light.glsl>
#moj_import <hbm:backport_fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 TextureMat;
uniform int FogShape;
uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;
uniform vec4 HbmFlags;

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexDistance = hbm_fog_distance(Position, FogShape);
    vertexColor = HbmFlags.x > 0.5 ? hbm_mix_light(Light0_Direction, Light1_Direction, Normal, Color) : Color;
    lightMapColor = HbmFlags.y > 0.5 ? texelFetch(Sampler2, UV2 / 16, 0) : vec4(1.0);
    overlayColor = HbmFlags.z > 0.5 ? texelFetch(Sampler1, UV1, 0) : vec4(1.0);
    texCoord0 = HbmFlags.w > 0.5 ? (TextureMat * vec4(UV0, 0.0, 1.0)).xy : UV0;
}
