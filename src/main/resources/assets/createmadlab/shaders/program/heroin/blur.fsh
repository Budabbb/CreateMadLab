#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform vec2 BlurDir;
uniform float BlurScale;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 texel = (BlurDir / max(OutSize, vec2(1.0))) * max(BlurScale, 0.1);

    vec3 sum = vec3(0.0);
    sum += texture(DiffuseSampler, texCoord - texel * 3.0).rgb * 0.08;
    sum += texture(DiffuseSampler, texCoord - texel * 2.0).rgb * 0.12;
    sum += texture(DiffuseSampler, texCoord - texel * 1.0).rgb * 0.18;
    sum += texture(DiffuseSampler, texCoord).rgb * 0.24;
    sum += texture(DiffuseSampler, texCoord + texel * 1.0).rgb * 0.18;
    sum += texture(DiffuseSampler, texCoord + texel * 2.0).rgb * 0.12;
    sum += texture(DiffuseSampler, texCoord + texel * 3.0).rgb * 0.08;

    fragColor = vec4(sum, 1.0);
}
