#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec2 texCoord0;
out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    // Compensate the horizontal offset for the active projection's aspect
    // ratio. This keeps the outline round and reduces over-expansion on small
    // held-item models without relying on unavailable framebuffer uniforms.
    float horizontalScale = abs(ProjMat[0][0] / ProjMat[1][1]);
    gl_Position.xy += vec2(
        OUTLINE_X * horizontalScale,
        OUTLINE_Y
    ) * 0.0007 * gl_Position.w;
    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    texCoord0 = UV0;
    vertexColor = Color;
}
