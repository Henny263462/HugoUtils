#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 textureColor = texture(Sampler0, texCoord0);
    if (textureColor.a < 0.1) {
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
    float strength = fogFade * GlintAlpha * vertexColor.a;
    float luminance = dot(textureColor.rgb, vec3(0.299, 0.587, 0.114));
    fragColor = vec4(vertexColor.rgb * ColorModulator.rgb * luminance * strength, textureColor.a);
}
