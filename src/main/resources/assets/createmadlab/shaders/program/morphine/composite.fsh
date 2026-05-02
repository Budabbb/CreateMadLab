#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D BloomSampler;
uniform float Time;
uniform float Intensity;
uniform float HealthFactor;
uniform float Pulse;
uniform vec2 Resolution;
uniform float Warmth;
uniform float VignetteStrength;
uniform float BlurStrength;
uniform float BloomStrength;
uniform float HazeStrength;
uniform float ContrastStrength;
uniform float RushStrength;
uniform float DebtIntensity;
uniform vec2 InSize;

in vec2 texCoord;

out vec4 fragColor;

float saturate(float value) {
    return clamp(value, 0.0, 1.0);
}

float luminance(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

vec3 adjustSaturation(vec3 color, float amount) {
    float gray = luminance(color);
    return mix(vec3(gray), color, amount);
}

vec3 adjustContrast(vec3 color, float amount) {
    return clamp((color - 0.5) * amount + 0.5, 0.0, 1.0);
}

vec3 sampleSoftBlur(vec2 uv, vec2 texel, float radius) {
    vec2 offset = texel * radius;
    vec3 sum = texture(DiffuseSampler, uv).rgb * 0.28;
    sum += texture(DiffuseSampler, uv + vec2( offset.x, 0.0)).rgb * 0.12;
    sum += texture(DiffuseSampler, uv + vec2(-offset.x, 0.0)).rgb * 0.12;
    sum += texture(DiffuseSampler, uv + vec2(0.0,  offset.y)).rgb * 0.12;
    sum += texture(DiffuseSampler, uv + vec2(0.0, -offset.y)).rgb * 0.12;
    sum += texture(DiffuseSampler, uv + vec2( offset.x,  offset.y)).rgb * 0.06;
    sum += texture(DiffuseSampler, uv + vec2(-offset.x, -offset.y)).rgb * 0.06;
    sum += texture(DiffuseSampler, uv + vec2( offset.x, -offset.y)).rgb * 0.06;
    sum += texture(DiffuseSampler, uv + vec2(-offset.x,  offset.y)).rgb * 0.06;
    return sum;
}

void main() {
    vec2 resolution = max(Resolution, vec2(1.0));
    vec2 texel = 1.0 / max(InSize, vec2(1.0));
    vec2 centered = texCoord - vec2(0.5);
    vec2 aspectCentered = vec2(centered.x * (resolution.x / resolution.y), centered.y);
    float radius = length(aspectCentered);
    float innerMask = 1.0 - smoothstep(0.0, 0.24, radius);
    float hazeMask = 1.0 - smoothstep(0.0, 0.58, radius);
    float edgeMask = smoothstep(0.18, 0.88, radius);
    float outerEdge = smoothstep(0.34, 1.02, radius);
    float tunnelMask = smoothstep(0.22, 0.92, radius);
    float injuryWeight = saturate(HealthFactor * 1.10 + DebtIntensity * 0.45);
    float breathWave = 0.5 + 0.5 * sin(Time * 0.82 + 0.35 * sin(Time * 0.31));
    float rushWave = 0.5 + 0.5 * sin(Time * 1.65 - 0.25);
    float surgeWave = clamp(mix(breathWave, rushWave, 0.58), 0.0, 1.0);
    float pulseWarm = clamp((0.18 + 0.82 * Pulse) * (0.72 + 0.28 * surgeWave) * (0.72 + 0.28 * HealthFactor), 0.0, 1.0);

    vec3 scene = texture(DiffuseSampler, texCoord).rgb;
    float sceneLum = luminance(scene);
    float shadowMask = 1.0 - smoothstep(0.18, 0.60, sceneLum);
    float midMask = smoothstep(0.10, 0.72, sceneLum) * (1.0 - smoothstep(0.72, 0.98, sceneLum));
    float highlightMask = smoothstep(0.56, 1.0, sceneLum);

    vec3 color = scene;
    float contrastAmount = 1.0 + ContrastStrength * (0.28 + 0.42 * RushStrength + 0.12 * surgeWave) - BlurStrength * 0.06 - HazeStrength * 0.08;
    color = adjustContrast(color, max(0.86, contrastAmount));
    color = adjustSaturation(color, 1.0 - 0.02 * Intensity + 0.05 * Warmth);

    color = mix(color, color * vec3(1.07, 0.97, 0.98), Warmth * shadowMask * 0.14);
    color = mix(color, color * vec3(1.22, 0.87, 0.90), Warmth * (0.24 + 0.60 * midMask));
    color += vec3(0.082, 0.022, 0.026) * Warmth * (0.28 + 0.72 * midMask);
    color += vec3(0.070, 0.026, 0.018) * Warmth * highlightMask * (0.26 + 0.62 * pulseWarm);
    color += vec3(0.060, 0.014, 0.018) * RushStrength * (0.28 + 0.72 * midMask);

    float blurRadius = 0.70 + 3.9 * BlurStrength + 0.95 * Pulse + 0.70 * HealthFactor + 0.45 * surgeWave;
    vec3 softBlur = sampleSoftBlur(texCoord, texel, blurRadius);
    vec3 edgeBlur = sampleSoftBlur(texCoord, texel, blurRadius * 1.65);
    color = mix(color, softBlur, saturate((0.08 + 0.28 * edgeMask) * BlurStrength));
    color = mix(color, edgeBlur, saturate(outerEdge * outerEdge * BlurStrength * (0.18 + 0.22 * injuryWeight + 0.10 * surgeWave)));
    color = mix(color, scene, innerMask * (0.14 + 0.24 * (1.0 - BlurStrength)));

    vec3 bloom = texture(BloomSampler, texCoord).rgb;
    vec3 warmBloom = bloom * vec3(1.20, 0.74, 0.62);
    color += warmBloom * BloomStrength * (0.70 + 0.34 * Pulse + 0.24 * RushStrength + 0.16 * surgeWave);

    float hazeDrift = 0.5 + 0.5 * sin(Time * 0.22 + radius * 5.8);
    color += vec3(0.138, 0.042, 0.046) * hazeMask * HazeStrength * (0.56 + 0.26 * hazeDrift + 0.34 * pulseWarm);
    color += vec3(0.102, 0.028, 0.032) * innerMask * HazeStrength * (0.28 + 0.46 * pulseWarm + 0.20 * injuryWeight);
    color += vec3(0.060, 0.014, 0.018) * innerMask * DebtIntensity * Pulse * 0.22;

    float vignette = smoothstep(0.22, 0.98, radius);
    float tunnelDarken = VignetteStrength * (0.24 + 0.16 * Pulse + 0.18 * injuryWeight + 0.06 * breathWave);
    color *= 1.0 - vignette * tunnelDarken;
    color += vec3(0.106, 0.012, 0.016) * vignette * VignetteStrength * (0.12 + 0.26 * Pulse + 0.22 * HealthFactor);
    color += vec3(0.070, 0.010, 0.014) * outerEdge * Warmth * (0.10 + 0.24 * injuryWeight + 0.06 * breathWave);
    color += vec3(0.060, 0.012, 0.016) * outerEdge * DebtIntensity * (0.08 + 0.14 * Pulse);
    color = mix(color, vec3(luminance(color)), vignette * 0.040 * VignetteStrength);

    color *= 1.0 - tunnelMask * 0.055 * Intensity * (0.86 + 0.28 * HealthFactor + 0.08 * breathWave);
    color = mix(color, scene, innerMask * 0.10);
    color = clamp(color, 0.0, 1.0);

    fragColor = vec4(color, 1.0);
}
