#version 120

uniform float blindness;
uniform int isEyeInWater;
uniform float frameTimeCounter;

varying vec4 color;

void main()
{
    // Internal fade timer - 20s fade in, 3:27 full, 20s fade out
    float totalDuration = 247.0; // 3 minutes 47 seconds
    float fadeInDuration = 20.0;
    float fadeOutDuration = 20.0;
    float fullDuration = totalDuration - fadeInDuration - fadeOutDuration;

    float elapsed = mod(frameTimeCounter, totalDuration);
    float fadeIntensity = 0.0;

    if (elapsed < fadeInDuration) {
        // Fade in
        fadeIntensity = elapsed / fadeInDuration;
    } else if (elapsed < fadeInDuration + fullDuration) {
        // Full intensity
        fadeIntensity = 1.0;
    } else {
        // Fade out
        float fadeOutElapsed = elapsed - (fadeInDuration + fullDuration);
        fadeIntensity = 1.0 - (fadeOutElapsed / fadeOutDuration);
    }

    float fog = (isEyeInWater>0) ? 1.-exp(-gl_FogFragCoord * gl_Fog.density):
    clamp((gl_FogFragCoord-gl_Fog.start) * gl_Fog.scale, 0., 1.);

    vec4 col = color * vec4(vec3(1.-blindness),1);
    col.rgb = mix(col.rgb, gl_Fog.color.rgb, fog);
    gl_FragData[0] = col;
}