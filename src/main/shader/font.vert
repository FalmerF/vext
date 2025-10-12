#version 450

layout(push_constant) uniform PushConstants {
    mat4 matrix;
    vec3 inColor;
} pushConstants;
layout(binding = 1) readonly buffer GlyphBuffer {
    float data[];
} glyphBuffer;

layout(location = 0) in int glyphIndex;
layout(location = 1) in float offsetX;

layout(location = 0) out vec2 fragTexCoord;
layout(location = 1) out vec3 fragColor;
layout(location = 2) flat out int _glyphIndex;

vec2 positions[4] = vec2[](
vec2(0, -1),
vec2(1, -1),
vec2(0, 0),
vec2(1, 0)
);

void main() {
    int dataOffset = glyphIndex * 7;

    mat4 translation = mat4(1.0);
    translation[3][0] = offsetX;
    translation[3][1] = -glyphBuffer.data[dataOffset + 2];

    mat4 scaling = mat4(1.0);
    scaling[0][0] = glyphBuffer.data[dataOffset];
    scaling[1][1] = glyphBuffer.data[dataOffset + 1];

    mat4 movedMatrix = pushConstants.matrix * translation;
    movedMatrix = movedMatrix * scaling;

    gl_Position = movedMatrix * vec4(positions[gl_VertexIndex], 0.0, 1.0);

    fragTexCoord = positions[gl_VertexIndex] + vec2(0, 1);
    fragColor = pushConstants.inColor;

    _glyphIndex = glyphIndex;
}