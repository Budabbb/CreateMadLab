#version 150

uniform sampler2D DiffuseSampler;
uniform float Threshold;
uniform float Knee;

in vec2 texCoord;

out vec4 fragColor;

float luminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

void main() {
    vec3 scene = texture(DiffuseSampler, texCoord).rgb;
    float luma = luminance(scene);

    float knee = max(Knee, 0.0001);
    float soft = clamp((luma - Threshold + knee) / (2.0 * knee), 0.0, 1.0);
    soft = soft * soft * (3.0 - 2.0 * soft);
    float hard = max(luma - Threshold, 0.0);
    float mask = max(hard, soft);

    fragColor = vec4(scene * mask, 1.0);
}
