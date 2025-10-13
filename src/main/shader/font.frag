#version 450

layout(binding = 0) uniform sampler2D sdfTexture;
layout(binding = 1) readonly buffer GlyphBuffer {
    float data[];
} glyphBuffer;

layout(location = 0) in vec2 fragTexCoord;
layout(location = 1) in vec3 fragColor;
layout(location = 2) flat in int glyphIndex;
layout(location = 3) flat in float glyphScale;
layout(location = 4) flat in float sdfTexelSize;

layout(location = 0) out vec4 outColor;

void main() {
    int dataOffset = glyphIndex * 7;

    vec2 minUV = vec2(glyphBuffer.data[dataOffset + 3], glyphBuffer.data[dataOffset + 4]);
    vec2 maxUV = vec2(glyphBuffer.data[dataOffset + 5], glyphBuffer.data[dataOffset + 6]);

    vec2 size = maxUV - minUV;
    vec2 texCoord = minUV + (fragTexCoord * size);

    float distance = texture(sdfTexture, texCoord).r;

    float screenPixelSize = length(fwidth(texCoord) * sdfTexelSize);
    float smoothing = 0.001 * screenPixelSize * glyphScale;

    float alpha = smoothstep(0.49 - smoothing, 0.49 + smoothing, distance);

    outColor = vec4(fragColor, alpha);
}