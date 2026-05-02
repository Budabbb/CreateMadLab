#version 150

uniform sampler2D DiffuseSampler;
uniform float TripTime;
uniform float Intensity;
uniform float DoseStrength;
uniform vec2 Resolution;
uniform float CameraMotion;
uniform float TripPhase;
uniform float PeakPulse;
uniform float SpikeStrength;
uniform float DistortionStrength;
uniform float TrailStrength;
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

vec2 safeNormalize(vec2 value) {
    float len = max(length(value), 0.0001);
    return value / len;
}

mat2 rotation(float angle) {
    float s = sin(angle);
    float c = cos(angle);
    return mat2(c, -s, s, c);
}

void main() {
    vec2 resolution = max(Resolution, vec2(1.0));
    vec2 texel = 1.0 / max(InSize, vec2(1.0));
    vec2 centered = texCoord - vec2(0.5);
    vec2 aspectCentered = vec2(centered.x * (resolution.x / resolution.y), centered.y);
    float radius = length(aspectCentered);
    vec2 radialDir = safeNormalize(aspectCentered + vec2(0.0001, -0.0001));
    vec2 tangentDir = vec2(-radialDir.y, radialDir.x);

    float edgeMask = smoothstep(0.14, 0.92, radius);
    float midMask = smoothstep(0.10, 0.55, radius) * (1.0 - smoothstep(0.76, 1.02, radius));
    float centerRelief = 1.0 - smoothstep(0.03, 0.24, radius);
    float brightnessMask = smoothstep(0.38, 0.82, luminance(texture(DiffuseSampler, texCoord).rgb));
    float highDose = smoothstep(0.95, 1.55, DoseStrength);
    float veryHighDose = smoothstep(1.90, 2.40, DoseStrength);
    float ultraDose = smoothstep(2.65, 3.35, DoseStrength);
    float spike = saturate(SpikeStrength);
    float screenAngle = atan(aspectCentered.y, aspectCentered.x);

    float chromaGate = smoothstep(mix(0.08, 0.05, highDose), mix(0.28, 0.20, highDose), Intensity);
    float breathingGate = smoothstep(mix(0.14, 0.10, highDose), mix(0.40, 0.32, highDose), Intensity);
    float crawlGate = smoothstep(mix(0.22, 0.16, highDose), mix(0.52, 0.40, highDose), Intensity);
    float geometryGate = smoothstep(mix(0.55, 0.34, highDose), mix(0.82, 0.62, highDose), Intensity);
    float colorRise = smoothstep(0.04, 0.22, TripPhase);
    float colorFade = 1.0 - smoothstep(0.88, 0.995, TripPhase);
    float colorEnvelope = colorRise * colorFade;
    float colorGate = smoothstep(0.10, 0.40, Intensity) * colorEnvelope;
    float colorPeak = smoothstep(0.72, 1.0, Intensity) * colorEnvelope;
    float trailGate = smoothstep(mix(0.42, 0.34, highDose), mix(0.70, 0.56, highDose), Intensity);
    float trailPhase = smoothstep(0.18, 0.34, TripPhase) * (1.0 - smoothstep(0.82, 0.96, TripPhase));
    float geometryPhase = smoothstep(mix(0.28, 0.20, highDose), mix(0.45, 0.36, highDose), TripPhase) * (1.0 - smoothstep(0.78, 0.94, TripPhase));
    float worldPatternGate = ultraDose * smoothstep(0.52, 0.80, Intensity) * smoothstep(0.18, 0.32, TripPhase) * (1.0 - smoothstep(0.86, 0.98, TripPhase));

    float breath = 0.5 + 0.5 * sin(TripTime * 1.08 + sin(TripTime * 0.19) * 1.6);
    float radialWave = sin(radius * 30.0 - TripTime * 1.90 + sin(aspectCentered.y * 17.0 + TripTime * 0.72));
    float secondaryWave = sin(radius * 16.0 + TripTime * 1.10 + sin(aspectCentered.x * 9.0 - TripTime * 0.33));
    float driftWave = sin(aspectCentered.x * 13.0 + aspectCentered.y * 17.0 - TripTime * 1.18);
    float distortionMask = saturate(max(edgeMask * 0.75, midMask) * (1.0 - centerRelief * 0.82));
    vec2 uv = texCoord;
    float warpStrength = (0.0105 + 0.0030 * PeakPulse) * DistortionStrength * breathingGate * (1.0 + 0.25 * highDose + 0.35 * veryHighDose + 0.78 * ultraDose + 1.20 * spike);
    uv += radialDir * (radialWave * 0.55 + secondaryWave * 0.30 + driftWave * 0.15) * warpStrength * distortionMask * (0.45 + 0.55 * breath);
    uv += tangentDir * (radialWave * 0.30 + secondaryWave * 0.70) * 0.0024 * DistortionStrength * breathingGate * edgeMask * (0.55 + 0.45 * breath) * (1.0 + 0.35 * highDose + 0.48 * ultraDose + 0.75 * spike);
    uv += aspectCentered * (0.0030 * breathingGate * DistortionStrength * (breath - 0.5)) * (0.25 + 0.75 * edgeMask) * (1.0 + 0.20 * highDose + 0.36 * ultraDose + 0.45 * spike);

    vec2 crawl = vec2(
        sin((uv.y + aspectCentered.y * 0.7) * 42.0 + TripTime * 1.30 + radius * 19.0),
        cos((uv.x - aspectCentered.x * 0.5) * 36.0 - TripTime * 1.12 - radius * 17.0)
    );
    uv += crawl * 0.0031 * crawlGate * DistortionStrength * (0.30 + 0.70 * midMask) * (0.58 + 0.42 * breath) * (1.0 + 0.18 * highDose + 0.30 * veryHighDose + 0.40 * ultraDose + 0.55 * spike);
    uv += tangentDir * (crawl.x + crawl.y) * 0.0018 * crawlGate * DistortionStrength * edgeMask * (1.0 + 0.25 * highDose + 0.42 * ultraDose + 0.65 * spike);
    uv = clamp(uv, vec2(0.001), vec2(0.999));

    vec2 aberration = radialDir * (0.0020 + 0.0110 * chromaGate * DistortionStrength) * edgeMask * (1.0 + 0.40 * highDose + 0.30 * veryHighDose + 0.28 * ultraDose + 0.85 * spike);
    float r = texture(DiffuseSampler, clamp(uv + aberration, vec2(0.001), vec2(0.999))).r;
    float g = texture(DiffuseSampler, uv).g;
    float b = texture(DiffuseSampler, clamp(uv - aberration, vec2(0.001), vec2(0.999))).b;
    vec3 color = vec3(r, g, b);

    vec2 shimmerOffset = tangentDir * (0.0016 + 0.0028 * crawlGate) * DistortionStrength * (0.40 + 0.60 * midMask);
    vec3 shimmerA = texture(DiffuseSampler, clamp(uv + shimmerOffset, vec2(0.001), vec2(0.999))).rgb;
    vec3 shimmerB = texture(DiffuseSampler, clamp(uv - shimmerOffset * 0.75, vec2(0.001), vec2(0.999))).rgb;
    color = mix(color, mix(shimmerA, shimmerB, 0.45), (0.12 + 0.06 * highDose + 0.05 * spike) * crawlGate * (0.35 + 0.65 * midMask));

    vec3 north = texture(DiffuseSampler, clamp(uv + vec2(0.0, texel.y), vec2(0.001), vec2(0.999))).rgb;
    vec3 south = texture(DiffuseSampler, clamp(uv - vec2(0.0, texel.y), vec2(0.001), vec2(0.999))).rgb;
    vec3 east = texture(DiffuseSampler, clamp(uv + vec2(texel.x, 0.0), vec2(0.001), vec2(0.999))).rgb;
    vec3 west = texture(DiffuseSampler, clamp(uv - vec2(texel.x, 0.0), vec2(0.001), vec2(0.999))).rgb;
    vec3 neighborhood = (north + south + east + west) * 0.25;
    vec3 detail = color - neighborhood;
    color += detail * (0.14 + 0.16 * colorGate + 0.08 * PeakPulse * colorEnvelope);

    float saturationBoost = 1.0
        + (0.28 + 0.22 * PeakPulse) * colorGate
        + 0.14 * colorPeak
        + (0.14 * highDose + 0.06 * veryHighDose + 0.08 * ultraDose + 0.12 * spike) * colorEnvelope;
    float contrastBoost = 1.0
        + 0.10 * colorGate
        + 0.12 * Intensity * colorEnvelope
        + 0.10 * PeakPulse * colorEnvelope
        + (0.08 * highDose + 0.05 * veryHighDose + 0.08 * ultraDose + 0.12 * spike) * colorEnvelope;
    float hueDrift = (0.020 + 0.010 * highDose) * colorGate * sin(TripTime * 0.26 + radius * 9.0);
    color.r += hueDrift;
    color.g -= hueDrift * 0.45;
    color.b += hueDrift * 0.25;
    color = adjustSaturation(color, saturationBoost);
    color = adjustContrast(color, contrastBoost);
    color = mix(color, color * vec3(1.05, 1.03, 1.01), 0.10 * colorGate + 0.08 * PeakPulse * colorEnvelope);

    vec3 trail = vec3(0.0);
    vec2 trailDir = safeNormalize(mix(radialDir, tangentDir, 0.40));
    float trailDistance = (0.0030 + 0.0150 * TrailStrength) * (0.35 + 0.65 * edgeMask) * (1.0 + 0.30 * highDose + 0.35 * veryHighDose + 0.45 * ultraDose + 0.95 * spike);
    for (int i = 1; i <= 7; i++) {
        float stepFactor = float(i) / 7.0;
        vec2 sampleUv = clamp(uv - trailDir * trailDistance * stepFactor, vec2(0.001), vec2(0.999));
        trail += texture(DiffuseSampler, sampleUv).rgb * (1.0 - stepFactor * 0.10);
    }
    trail /= 5.2;
    color = mix(color, trail, (0.24 + 0.10 * highDose + 0.10 * veryHighDose + 0.12 * ultraDose + 0.22 * spike) * trailGate * trailPhase * TrailStrength * (0.35 + 0.65 * CameraMotion));

    float sectorCount = mix(6.0, 14.0, clamp(ultraDose + 0.55 * spike, 0.0, 1.0));
    float sectorAngle = 6.2831853 / sectorCount;
    float repeatedAngle = abs(mod(screenAngle + TripTime * (0.10 + 0.08 * ultraDose) + radius * (0.6 + 1.3 * spike) + sectorAngle * 0.5, sectorAngle) - sectorAngle * 0.5);
    float repeatedRadius = pow(clamp(radius, 0.0, 1.0), mix(1.02, 0.56, clamp(ultraDose + 0.45 * spike, 0.0, 1.0)));
    vec2 repeatedDir = vec2(cos(repeatedAngle), sin(repeatedAngle));
    vec2 repeatedAspect = repeatedDir * repeatedRadius;
    float inverseAspect = resolution.y / resolution.x;
    vec2 patternUvA = clamp(vec2(repeatedAspect.x * inverseAspect, repeatedAspect.y) * (0.92 - 0.16 * ultraDose - 0.10 * spike) + vec2(0.5), vec2(0.001), vec2(0.999));
    vec2 patternUvB = clamp(vec2(-repeatedAspect.x * inverseAspect, repeatedAspect.y) * (0.88 - 0.14 * ultraDose - 0.08 * spike) + vec2(0.5), vec2(0.001), vec2(0.999));
    vec2 patternUvC = clamp(vec2(repeatedAspect.y * inverseAspect, repeatedAspect.x) * (0.78 - 0.12 * ultraDose - 0.06 * spike) + vec2(0.5), vec2(0.001), vec2(0.999));
    vec2 patternUvD = clamp(vec2(-repeatedAspect.y * inverseAspect, repeatedAspect.x) * (0.66 - 0.12 * ultraDose - 0.08 * spike) + vec2(0.5), vec2(0.001), vec2(0.999));
    vec2 patternUvE = clamp(vec2(repeatedAspect.x * inverseAspect, -repeatedAspect.y) * (0.58 - 0.10 * ultraDose - 0.10 * spike) + vec2(0.5), vec2(0.001), vec2(0.999));
    vec3 worldPattern = texture(DiffuseSampler, patternUvA).rgb;
    worldPattern += texture(DiffuseSampler, patternUvB).rgb;
    worldPattern += texture(DiffuseSampler, patternUvC).rgb;
    worldPattern += texture(DiffuseSampler, patternUvD).rgb;
    worldPattern += texture(DiffuseSampler, patternUvE).rgb;
    worldPattern /= 5.0;
    vec3 worldPatternDeep = texture(DiffuseSampler, mix(patternUvA, patternUvD, 0.5 + 0.5 * sin(TripTime * 0.34 + radius * 16.0))).rgb;
    worldPatternDeep += texture(DiffuseSampler, mix(patternUvB, patternUvE, 0.5 + 0.5 * cos(TripTime * 0.29 - radius * 13.0))).rgb;
    worldPatternDeep += texture(DiffuseSampler, mix(patternUvC, patternUvD, 0.5 + 0.5 * sin(TripTime * 0.41 + screenAngle * 3.0))).rgb;
    worldPatternDeep /= 3.0;
    vec3 patternMorph = mix(worldPattern, worldPatternDeep, 0.45 + 0.35 * spike);
    patternMorph = adjustSaturation(patternMorph, 1.34 + 0.42 * ultraDose + 0.46 * spike);
    vec3 patternTint = mix(vec3(1.10, 0.84, 0.56), vec3(0.54, 1.02, 1.10), 0.5 + 0.5 * sin(repeatedAngle * sectorCount + repeatedRadius * 18.0 + TripTime * 0.12));
    patternMorph = mix(patternMorph, patternMorph * patternTint, 0.18 + 0.18 * spike);
    patternMorph += abs(patternMorph - vec3(luminance(patternMorph))) * (0.05 + 0.08 * ultraDose + 0.10 * spike);
    float patternCoverage = mix((0.18 + 0.82 * edgeMask) * (1.0 - centerRelief * 0.88), clamp(0.62 + 0.26 * midMask + 0.30 * centerRelief, 0.0, 1.0), clamp(ultraDose * (0.55 + 0.45 * spike), 0.0, 1.0));
    float worldPatternBlend = saturate(worldPatternGate * patternCoverage * (0.62 + 0.38 * PeakPulse + 0.95 * spike));
    color = mix(color, patternMorph, saturate(worldPatternBlend * (0.44 + 0.48 * ultraDose + 0.40 * spike)));
    color = mix(color, patternMorph, saturate(worldPatternBlend * spike * (0.42 + 0.48 * ultraDose)));
    color += abs(patternMorph - vec3(luminance(patternMorph))) * worldPatternBlend * (0.14 + 0.18 * ultraDose + 0.10 * spike);
    color = adjustSaturation(color, 1.0 + worldPatternBlend * (0.10 + 0.12 * ultraDose + 0.18 * spike));

    vec2 geometryUv = rotation(sin(TripTime * (0.08 + 0.03 * spike + 0.02 * ultraDose)) * (0.35 + 0.18 * veryHighDose + 0.22 * ultraDose + 0.32 * spike)) * aspectCentered;
    float angle = atan(geometryUv.y, geometryUv.x);
    float geoRadius = length(geometryUv);
    float petals = abs(sin(angle * mix(10.0, 15.0, highDose + 0.4 * veryHighDose + 0.4 * ultraDose) + TripTime * 0.18 + sin(TripTime * 0.09 + spike * 1.3))) *
        abs(sin(angle * mix(18.0, 32.0, highDose + 0.5 * veryHighDose + 0.5 * ultraDose) - TripTime * 0.12));
    float rings = abs(sin(geoRadius * mix(48.0, 74.0, highDose + 0.5 * veryHighDose + 0.55 * ultraDose) - TripTime * 0.95 + petals * 2.8));
    float lattice = abs(sin((geometryUv.x + geometryUv.y) * mix(26.0, 42.0, highDose + 0.6 * veryHighDose + 0.55 * ultraDose) + TripTime * 0.41)) *
        abs(cos((geometryUv.x - geometryUv.y) * mix(26.0, 42.0, highDose + 0.6 * veryHighDose + 0.55 * ultraDose) - TripTime * 0.37));
    float kaleido = abs(sin(angle * mix(6.0, 15.5, highDose + 0.6 * veryHighDose + 0.75 * ultraDose) + geoRadius * mix(12.0, 28.0, highDose + 0.7 * veryHighDose + 0.70 * ultraDose) - TripTime * mix(0.22, 0.58, highDose + 0.5 * veryHighDose + 0.45 * ultraDose)));
    float mirrored = abs(cos(angle * mix(2.5, 12.5, highDose + 0.5 * veryHighDose + 0.65 * ultraDose) - TripTime * 0.14));
    float mandala = smoothstep(mix(0.64, 0.52, veryHighDose + 0.45 * ultraDose), 0.92, petals * 0.42 + rings * 0.22 + kaleido * mix(0.18, 0.44, highDose + 0.4 * veryHighDose + 0.40 * ultraDose) + mirrored * mix(0.0, 0.28, highDose + 0.5 * veryHighDose + 0.40 * ultraDose));
    float geometry = (mandala * 0.66 + smoothstep(0.78, 0.96, lattice) * 0.34) *
        geometryGate * geometryPhase * brightnessMask * (0.28 + 0.72 * edgeMask) * (0.35 + 0.65 * PeakPulse + 0.60 * spike) * (1.0 + 0.50 * veryHighDose + 0.70 * ultraDose);
    vec3 geometryTint = mix(vec3(0.08, 0.16, 0.24), vec3(0.22, 0.20, 0.11), 0.5 + 0.5 * sin(TripTime * 0.12 + angle * 0.45));
    color += geometryTint * geometry * (0.28 + 0.24 * highDose + 0.16 * veryHighDose + 0.26 * ultraDose + 0.28 * spike);
    color += vec3(0.08, 0.04, 0.02) * smoothstep(0.68, 0.94, kaleido) * geometryGate * edgeMask * (0.10 + 0.16 * highDose + 0.18 * veryHighDose + 0.22 * ultraDose + 0.24 * spike);
    color += geometryTint.bgr * smoothstep(0.70, 0.95, abs(sin(angle * mix(8.0, 19.0, veryHighDose + 0.55 * ultraDose) - TripTime * 0.52 + geoRadius * 34.0)))
        * geometryGate * edgeMask * (spike + 0.30 * ultraDose) * (0.10 + 0.12 * veryHighDose + 0.18 * ultraDose);

    color += vec3(0.030, 0.024, 0.016) * PeakPulse * colorPeak * (1.0 + 0.22 * highDose + 0.22 * spike);
    color *= 1.0 - edgeMask * 0.030 * Intensity * (1.0 + 0.45 * spike);
    color = mix(color, color * vec3(1.05, 1.03, 1.00), (0.18 * PeakPulse + 0.10 * spike) * colorEnvelope);
    color = clamp(color, 0.0, 1.0);

    fragColor = vec4(color, 1.0);
}
