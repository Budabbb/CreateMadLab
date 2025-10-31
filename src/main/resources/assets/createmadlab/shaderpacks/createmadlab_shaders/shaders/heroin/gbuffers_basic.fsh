#version 120

varying vec4 color;

const int GL_LINEAR = 9729;
const int GL_EXP = 2048;

uniform int fogMode;
uniform float frameTimeCounter;

void main() {

    float totalDuration = 180.0;
    float fadeInDuration = 10.0;
    float peakDuration = 150.0;
    float fadeOutDuration = 20.0;

    float elapsed = mod(frameTimeCounter, totalDuration);
    float effectIntensity = 0.0;

    if (elapsed < fadeInDuration) {
        effectIntensity = elapsed / fadeInDuration;
    } else if (elapsed < fadeInDuration + peakDuration) {
        effectIntensity = 1.0;
    } else {
        float fadeOutElapsed = elapsed - (fadeInDuration + peakDuration);
        effectIntensity = 1.0 - (fadeOutElapsed / fadeOutDuration);
    }

    gl_FragData[0] = color;
    gl_FragData[1] = vec4(vec3(gl_FragCoord.z), 1.0);

    if (effectIntensity > 0.0) {
        vec3 warmTint = vec3(1.5, 1.2, 0.5); 
        gl_FragData[0].rgb *= mix(vec3(1.0), warmTint, effectIntensity * 0.9);

        gl_FragData[0].rgb *= mix(1.0, 1.4, effectIntensity * 0.5);

        float luminance = dot(gl_FragData[0].rgb, vec3(0.299, 0.587, 0.114));
        vec3 sepia = vec3(luminance) * vec3(1.3, 1.1, 0.7);
        gl_FragData[0].rgb = mix(gl_FragData[0].rgb, sepia, effectIntensity * 0.4);
    }

    if (fogMode == GL_EXP) {
        gl_FragData[0].rgb = mix(gl_FragData[0].rgb, gl_Fog.color.rgb, 1.0 - clamp(exp(-gl_Fog.density * gl_FogFragCoord), 0.0, 1.0));
    } else if (fogMode == GL_LINEAR) {
        gl_FragData[0].rgb = mix(gl_FragData[0].rgb, gl_Fog.color.rgb, clamp((gl_FogFragCoord - gl_Fog.start) * gl_Fog.scale, 0.0, 1.0));
    }
}