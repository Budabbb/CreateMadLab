#version 120

uniform float blindness;
uniform int isEyeInWater;
uniform float frameTimeCounter;

varying vec4 color;

void main()
{
    float totalDuration = 360.0;    // 6 minutes total
    float fadeInDuration = 120.0;   // 2 minutes fade in
    float peakDuration = 180.0;     // 3 minutes peak intensity
    float fadeOutDuration = 60.0;   // 1 minute fade out

    float elapsed = mod(frameTimeCounter, totalDuration);
    float fadeIntensity = 0.0;

    if (elapsed < fadeInDuration) {
        fadeIntensity = elapsed / fadeInDuration;
    } else if (elapsed < fadeInDuration + peakDuration) {
        fadeIntensity = 1.0;
    } else {
        float fadeOutElapsed = elapsed - (fadeInDuration + peakDuration);
        fadeIntensity = 1.0 - (fadeOutElapsed / fadeOutDuration);
    }

    float fog = (isEyeInWater>0) ? 1.-exp(-gl_FogFragCoord * gl_Fog.density):
    clamp((gl_FogFragCoord-gl_Fog.start) * gl_Fog.scale, 0., 1.);

    vec4 col = color * vec4(vec3(1.-blindness),1);
    col.rgb = mix(col.rgb, gl_Fog.color.rgb, fog);
    gl_FragData[0] = col;
}