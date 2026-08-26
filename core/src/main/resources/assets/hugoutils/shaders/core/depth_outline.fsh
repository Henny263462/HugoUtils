#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float alpha = texture(Sampler0, texCoord0).a;
    if (alpha < 0.1) {
        discard;
    }
    fragColor = vec4(vertexColor.rgb * ColorModulator.rgb, vertexColor.a * ColorModulator.a);
}
