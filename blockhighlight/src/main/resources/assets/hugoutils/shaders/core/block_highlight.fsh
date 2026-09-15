#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 texel = texture(Sampler0, texCoord0);
    if (texel.a < 0.1) {
        discard;
    }
    float fogFade = 1.0 - total_fog_value(
        sphericalVertexDistance,
        cylindricalVertexDistance,
        FogEnvironmentalStart,
        FogEnvironmentalEnd,
        FogRenderDistanceStart,
        FogRenderDistanceEnd
    );
    vec3 tintedTexture = mix(texel.rgb, texel.rgb * vertexColor.rgb, 0.72);
    fragColor = vec4(tintedTexture * ColorModulator.rgb, texel.a * vertexColor.a * fogFade);
}
