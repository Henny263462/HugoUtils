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
    // Inverted hull: the visible front surface stays untouched; only the expanded silhouette remains.
    if (gl_FrontFacing) {
        discard;
    }
    float textureAlpha = texture(Sampler0, texCoord0).a;
    if (textureAlpha < 0.1) {
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
    fragColor = vec4(
        vertexColor.rgb * ColorModulator.rgb,
        textureAlpha * vertexColor.a * ColorModulator.a * fogFade
    );
}
