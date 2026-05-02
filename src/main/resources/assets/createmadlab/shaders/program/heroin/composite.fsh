#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D BloomSampler;
uniform sampler2D HistorySampler;
uniform float EffectTime;
uniform float Intensity;
uniform vec2 Resolution;
uniform float CameraMotion;
uniform float SedationWave;
uniform float Warmth;
uniform float BlurStrength;
uniform float VignetteStrength;
uniform float TrailStrength;
uniform float BloomStrength;
uniform vec2 InSize;

in vec2 texCoord;

out vec4 fragColor;

float saturate(float value) {
    return clamp(value, 0.0, 1.0);
}

float luminance(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

vec2 safeNormalize(vec2 value) {
    float len = max(length(value), 0.0001);
    return value / len;
}

vec3 sampleDreamBlur(vec2 uv, vec2 texel, float radius) {
    vec2 offset = texel * radius;
    vec3 sum = texture(DiffuseSampler, uv).rgb * 0.24;
    sum += texture(DiffuseSampler, uv + vec2( offset.x, 0.0)).rgb * 0.12;
    sum += texture(DiffuseSampler, uv + vec2(-offset.x, 0.0)).rgb * 0.12;
    sum += texture(DiffuseSampler, uv + vec2(0.0,  offset.y)).rgb * 0.12;
    sum += texture(DiffuseSampler, uv + vec2(0.0, -offset.y)).rgb * 0.12;
    sum += texture(DiffuseSampler, uv + vec2( offset.x,  offset.y)).rgb * 0.07;
    sum += texture(DiffuseSampler, uv + vec2(-offset.x, -offset.y)).rgb * 0.07;
    sum += texture(DiffuseSampler, uv + vec2( offset.x, -offset.y)).rgb * 0.07;
    sum += texture(DiffuseSampler, uv + vec2(-offset.x,  offset.y)).rgb * 0.07;
    return sum;
}

void main() {
    vec2 resolution = max(Resolution, vec2(1.0));
    vec2 texel = 1.0 / max(InSize, vec2(1.0));
    vec2 centered = texCoord - vec2(0.5);
    vec2 aspectCentered = vec2(centered.x * (resolution.x / resolution.y), centered.y);
    vec2 radialDir = safeNormalize(aspectCentered + vec2(0.0001, -0.0001));
    float radius = length(aspectCentered);
    float edge = smoothstep(0.18, 0.90, radius);
    float centerMask = 1.0 - smoothstep(0.00, 0.26, radius);
    float breathing = 0.5 + 0.5 * sin(EffectTime * 0.45 + sin(EffectTime * 0.11 + 0.9) * 0.8);

    vec3 scene = texture(DiffuseSampler, texCoord).rgb;
    float sceneLum = luminance(scene);
    float midMask = smoothstep(0.15, 0.65, sceneLum);
    float highMask = smoothstep(0.55, 1.0, sceneLum);

    vec3 warmMid = vec3(1.08, 0.98, 0.82);
    vec3 warmHigh = vec3(1.18, 1.08, 0.80);
    vec3 color = scene * mix(vec3(1.0), warmMid, midMask * Warmth);
    color += warmHigh * highMask * 0.06 * Warmth;

    float blurRadius = (1.0 + 6.0 * BlurStrength) * (0.18 + 0.82 * edge) * (0.92 + 0.08 * breathing);
    vec3 dreamBlur = sampleDreamBlur(texCoord, texel, blurRadius);
    vec3 edgeBlur = sampleDreamBlur(texCoord, texel, blurRadius * 1.8);
    color = mix(color, dreamBlur, saturate((0.04 + 0.64 * edge) * BlurStrength));
    color = mix(color, edgeBlur, saturate(edge * edge * BlurStrength * 0.35));
    color = mix(color, scene, centerMask * 0.16 * Intensity);

    vec3 bloom = texture(BloomSampler, texCoord).rgb;
    vec3 warmBloom = bloom * vec3(1.18, 1.05, 0.80);
    color += warmBloom * BloomStrength * (0.82 + 0.18 * SedationWave);

    float centerGlow = 1.0 - smoothstep(0.0, 0.78, radius);
    color += vec3(0.18, 0.11, 0.04) * centerGlow * (0.08 + 0.14 * Warmth) * (0.78 + 0.22 * SedationWave);

    vec3 history = texture(HistorySampler, texCoord).rgb;
    vec2 smearOffset = radialDir * (0.0010 + 0.0045 * BlurStrength) * (0.35 + 0.65 * edge) * (0.35 + 0.65 * CameraMotion);
    vec3 smear = scene * 0.40;
    smear += texture(DiffuseSampler, clamp(texCoord - smearOffset, vec2(0.001), vec2(0.999))).rgb * 0.24;
    smear += texture(DiffuseSampler, clamp(texCoord - smearOffset * 2.0, vec2(0.001), vec2(0.999))).rgb * 0.18;
    smear += texture(DiffuseSampler, clamp(texCoord - smearOffset * 3.0, vec2(0.001), vec2(0.999))).rgb * 0.10;
    smear += history * 0.08;
    float historyMix = CameraMotion * TrailStrength * (0.10 + 0.10 * SedationWave);
    float smearMix = CameraMotion * TrailStrength * (0.08 + 0.12 * edge);
    color = mix(color, mix(color, history, 0.55), saturate(historyMix));
    color = mix(color, smear, saturate(smearMix));

    float softenedLum = luminance(color);
    color = mix(color, vec3(softenedLum), 0.05 * Intensity + 0.03 * edge * Intensity);
    color = (color - 0.5) * (1.0 - 0.10 * Intensity - 0.06 * BlurStrength) + 0.5;

    vec3 north = texture(DiffuseSampler, clamp(texCoord + vec2(0.0, texel.y), vec2(0.001), vec2(0.999))).rgb;
    vec3 south = texture(DiffuseSampler, clamp(texCoord - vec2(0.0, texel.y), vec2(0.001), vec2(0.999))).rgb;
    vec3 east = texture(DiffuseSampler, clamp(texCoord + vec2(texel.x, 0.0), vec2(0.001), vec2(0.999))).rgb;
    vec3 west = texture(DiffuseSampler, clamp(texCoord - vec2(texel.x, 0.0), vec2(0.001), vec2(0.999))).rgb;
    vec3 softened = (north + south + east + west + color) * 0.20;
    color = mix(color, softened, 0.08 * Intensity + 0.16 * edge * BlurStrength);

    float vignette = smoothstep(0.22, 0.90, radius);
    vec3 vignetteTint = vec3(0.11, 0.05, 0.02);
    color *= 1.0 - vignette * VignetteStrength * 0.46;
    color += vignetteTint * vignette * VignetteStrength * 0.26;
    color = mix(color, color * vec3(0.96, 0.92, 0.88), vignette * VignetteStrength * 0.34);
    color = mix(color, vec3(luminance(color)), vignette * 0.10 * VignetteStrength);
    color = clamp(color, 0.0, 1.0);

    fragColor = vec4(color, 1.0);
}
