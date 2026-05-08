#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D BloomSampler;
uniform sampler2D HistorySampler;
uniform float Time;
uniform float Intensity;
uniform vec2 Resolution;
uniform float CameraMotion;
uniform float TintStrength;
uniform float DimStrength;
uniform float DesaturationStrength;
uniform float BlurStrength;
uniform float VignetteStrength;
uniform float BloomStrength;
uniform float HazeStrength;
uniform float DistortionStrength;
uniform float BlackoutStrength;
uniform float FinalFade;
uniform float HistoryStrength;
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

vec3 adjustSaturation(vec3 color, float amount) {
    float gray = luminance(color);
    return mix(vec3(gray), color, amount);
}

vec3 sampleSoftBlur(vec2 uv, vec2 texel, float radius) {
    vec2 offset = texel * radius;
    vec3 sum = texture(DiffuseSampler, uv).rgb * 0.26;
    sum += texture(DiffuseSampler, uv + vec2( offset.x, 0.0)).rgb * 0.12;
    sum += texture(DiffuseSampler, uv + vec2(-offset.x, 0.0)).rgb * 0.12;
    sum += texture(DiffuseSampler, uv + vec2(0.0,  offset.y)).rgb * 0.12;
    sum += texture(DiffuseSampler, uv + vec2(0.0, -offset.y)).rgb * 0.12;
    sum += texture(DiffuseSampler, uv + vec2( offset.x,  offset.y)).rgb * 0.065;
    sum += texture(DiffuseSampler, uv + vec2(-offset.x, -offset.y)).rgb * 0.065;
    sum += texture(DiffuseSampler, uv + vec2( offset.x, -offset.y)).rgb * 0.065;
    sum += texture(DiffuseSampler, uv + vec2(-offset.x,  offset.y)).rgb * 0.065;
    return sum;
}

void main() {
    vec2 resolution = max(Resolution, vec2(1.0));
    vec2 texel = 1.0 / max(InSize, vec2(1.0));
    vec2 centered = texCoord - vec2(0.5);
    vec2 aspectCentered = vec2(centered.x * (resolution.x / resolution.y), centered.y);
    vec2 radialDir = safeNormalize(aspectCentered + vec2(0.0001, -0.0001));
    float radius = length(aspectCentered);
    float edge = smoothstep(0.18, 0.92, radius);
    float outerEdge = smoothstep(0.42, 1.04, radius);
    float centerMask = 1.0 - smoothstep(0.00, 0.28, radius);
    float tunnel = smoothstep(0.18, 0.98, radius);
    float hazeMask = 1.0 - smoothstep(0.0, 0.70, radius);
    float slowWave = 0.5 + 0.5 * sin(Time * 0.22 + sin(Time * 0.07 + 0.8) * 0.72);

    float distortionWave = sin(Time * 0.31 + radius * 8.0) * 0.60 + sin(Time * 0.13 + radius * 4.5 + 1.8) * 0.40;
    vec2 warpedUv = texCoord + radialDir * distortionWave * DistortionStrength * edge * 0.0028;
    warpedUv += vec2(sin(Time * 0.11 + radius * 3.2), cos(Time * 0.09 + radius * 3.6)) * DistortionStrength * 0.0009;
    warpedUv = clamp(warpedUv, vec2(0.001), vec2(0.999));

    vec3 scene = texture(DiffuseSampler, warpedUv).rgb;
    vec3 originalScene = texture(DiffuseSampler, texCoord).rgb;
    float sceneLum = luminance(scene);
    float shadowMask = 1.0 - smoothstep(0.16, 0.62, sceneLum);
    float midMask = smoothstep(0.10, 0.76, sceneLum) * (1.0 - smoothstep(0.76, 1.0, sceneLum));
    float highlightMask = smoothstep(0.54, 1.0, sceneLum);

    vec3 color = adjustSaturation(scene, 1.0 - DesaturationStrength);
    vec3 chemicalMultiply = vec3(0.46, 1.18, 1.08);
    vec3 chemicalWash = vec3(0.012, 0.46, 0.40);
    vec3 clinicalHighlight = vec3(0.44, 1.0, 0.88);
    color = mix(color, color * chemicalMultiply, saturate(TintStrength * (0.30 + 0.50 * midMask + 0.16 * shadowMask)));
    color += chemicalWash * TintStrength * (0.18 + 0.44 * midMask + 0.18 * shadowMask);
    color += clinicalHighlight * TintStrength * highlightMask * 0.050;
    color = mix(color, vec3(luminance(color)) * vec3(0.58, 1.02, 0.96), DesaturationStrength * 0.24);

    float blurRadius = (0.70 + 5.60 * BlurStrength) * (0.38 + 0.62 * edge) * (0.96 + 0.04 * slowWave);
    vec3 softBlur = sampleSoftBlur(warpedUv, texel, blurRadius);
    vec3 edgeBlur = sampleSoftBlur(warpedUv, texel, blurRadius * 1.85);
    color = mix(color, softBlur, saturate((0.07 + 0.36 * edge) * BlurStrength));
    color = mix(color, edgeBlur, saturate(outerEdge * outerEdge * BlurStrength * 0.42));
    color = mix(color, originalScene, centerMask * (0.08 + 0.10 * (1.0 - BlurStrength)));

    vec3 bloom = texture(BloomSampler, texCoord).rgb;
    color += bloom * vec3(0.22, 0.96, 0.78) * BloomStrength * (0.54 + 0.18 * slowWave);

    vec3 history = texture(HistorySampler, texCoord).rgb;
    float historyMix = HistoryStrength * (0.38 + 0.44 * edge + 0.18 * CameraMotion);
    color = mix(color, history, saturate(historyMix));

    float hazeDrift = 0.5 + 0.5 * sin(Time * 0.17 + radius * 5.4);
    color += vec3(0.018, 0.30, 0.26) * hazeMask * HazeStrength * (0.40 + 0.32 * hazeDrift);
    color += vec3(0.010, 0.20, 0.18) * centerMask * HazeStrength * (0.20 + 0.20 * slowWave);

    float contrastLoss = 0.10 * Intensity + 0.16 * BlurStrength + 0.08 * HazeStrength;
    color = (color - 0.5) * max(0.58, 1.0 - contrastLoss) + 0.5;
    color *= 1.0 - DimStrength * (0.28 + 0.40 * tunnel + 0.12 * FinalFade);

    float vignette = smoothstep(0.20, 0.96, radius);
    float tunnelDarken = VignetteStrength * (0.34 + 0.18 * Intensity + 0.22 * FinalFade);
    color *= 1.0 - vignette * tunnelDarken;
    color += vec3(0.004, 0.135, 0.120) * vignette * VignetteStrength * (0.24 + 0.24 * Intensity);
    color = mix(color, vec3(luminance(color)) * vec3(0.50, 0.92, 0.88), vignette * DesaturationStrength * 0.22);

    color = mix(color, vec3(0.0, 0.035, 0.032), saturate(FinalFade * 0.88));
    color = mix(color, vec3(0.0), saturate(BlackoutStrength));
    color = clamp(color, 0.0, 1.0);

    fragColor = vec4(color, 1.0);
}
