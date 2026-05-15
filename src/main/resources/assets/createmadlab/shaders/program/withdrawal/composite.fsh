#version 150

uniform sampler2D DiffuseSampler;
uniform float Time;
uniform float Intensity;
uniform vec2 Resolution;
uniform float TintStrength;
uniform float DesaturationStrength;
uniform float VignetteStrength;
uniform float PulseStrength;
uniform float JitterStrength;
uniform float BlurStrength;
uniform vec2 InSize;

in vec2 texCoord;

out vec4 fragColor;

float saturate(float value) {
    return clamp(value, 0.0, 1.0);
}

float luminance(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

vec3 sampleScene(vec2 uv) {
    return texture(DiffuseSampler, clamp(uv, vec2(0.001), vec2(0.999))).rgb;
}

void main() {
    vec2 resolution = max(Resolution, vec2(1.0));
    vec2 texel = 1.0 / max(InSize, vec2(1.0));
    vec2 centered = texCoord - vec2(0.5);
    vec2 aspectCentered = vec2(centered.x * (resolution.x / resolution.y), centered.y);
    float radius = length(aspectCentered);

    float intensity = saturate(Intensity);
    float shiverA = sin(Time * 28.0 + texCoord.y * 93.0 + sin(Time * 3.1) * 0.7);
    float shiverB = cos(Time * 31.0 + texCoord.x * 81.0 + cos(Time * 2.7) * 0.5);
    vec2 jitter = vec2(shiverA, shiverB) * texel * JitterStrength * 1.35;
    vec2 uv = clamp(texCoord + jitter, vec2(0.001), vec2(0.999));

    vec3 color = sampleScene(uv);

    float blurRadius = BlurStrength * 2.15;
    vec3 blurred = color;
    blurred += sampleScene(uv + vec2(texel.x, 0.0) * blurRadius);
    blurred += sampleScene(uv - vec2(texel.x, 0.0) * blurRadius);
    blurred += sampleScene(uv + vec2(0.0, texel.y) * blurRadius);
    blurred += sampleScene(uv - vec2(0.0, texel.y) * blurRadius);
    blurred += sampleScene(uv + texel * vec2(0.70, 0.70) * blurRadius);
    blurred += sampleScene(uv - texel * vec2(0.70, 0.70) * blurRadius);
    blurred /= 7.0;
    color = mix(color, blurred, saturate(BlurStrength * 0.70));

    float gray = luminance(color);
    color = mix(color, vec3(gray), DesaturationStrength);

    vec3 coldTarget = vec3(gray * 0.68, gray * 0.80 + 0.015, gray * 0.93 + 0.030);
    color = mix(color, coldTarget, TintStrength);

    float pulse = 0.5 + 0.5 * sin(Time * 1.15 + sin(Time * 0.21) * 0.55);
    float vignette = smoothstep(0.32, 0.96, radius);
    color *= 1.0 - vignette * VignetteStrength * (0.82 + 0.18 * pulse);

    float centerDrain = smoothstep(0.08, 0.72, radius) * 0.026 * intensity;
    float weakPulse = (pulse - 0.5) * 0.018 * PulseStrength;
    color *= 1.0 - centerDrain - weakPulse;

    color = clamp(color, 0.0, 1.0);
    fragColor = vec4(color, 1.0);
}
