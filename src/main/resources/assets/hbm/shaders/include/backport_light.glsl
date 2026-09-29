// backport: 1.21.1 two-light diffuse shading (minecraft_mix_light).
#define HBM_LIGHT_POWER (0.6)
#define HBM_AMBIENT_LIGHT (0.4)

vec4 hbm_mix_light(vec3 lightDir0, vec3 lightDir1, vec3 normal, vec4 color) {
    lightDir0 = normalize(lightDir0);
    lightDir1 = normalize(lightDir1);
    float light0 = max(0.0, dot(lightDir0, normal));
    float light1 = max(0.0, dot(lightDir1, normal));
    float lightAccum = min(1.0, (light0 + light1) * HBM_LIGHT_POWER + HBM_AMBIENT_LIGHT);
    return vec4(color.rgb * lightAccum, color.a);
}
