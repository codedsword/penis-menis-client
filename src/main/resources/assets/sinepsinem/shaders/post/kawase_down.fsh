#version 330

// Dual Kawase blur, downsample step (Marius Bjorge, "Bandwidth-Efficient Rendering", 2015).
// Renders into a target half the size of the input; bilinear filtering does a 2x2 average per tap.

uniform sampler2D InSampler;

layout(std140) uniform BlurInfo {
    vec2 HalfTexel;
    float Offset;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 o = HalfTexel * Offset;
    vec4 sum = texture(InSampler, texCoord) * 4.0;
    sum += texture(InSampler, texCoord - o);
    sum += texture(InSampler, texCoord + o);
    sum += texture(InSampler, texCoord + vec2(o.x, -o.y));
    sum += texture(InSampler, texCoord - vec2(o.x, -o.y));
    fragColor = vec4((sum / 8.0).rgb, 1.0);
}
