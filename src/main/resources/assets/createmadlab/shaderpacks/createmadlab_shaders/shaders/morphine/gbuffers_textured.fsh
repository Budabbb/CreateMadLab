#version 120

uniform sampler2D texture;

varying vec4 color;
varying vec4 texcoord;

const int GL_LINEAR = 9729;
const int GL_EXP = 2048;

uniform int fogMode;
uniform float frameTimeCounter;

void main() {

    float totalDuration = 30.0;
    float fadeInDuration = 5.0;
    float peakDuration = 20.0;
    float fadeOutDuration = 5.0;

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

    vec4 baseColor = texture2D(texture, texcoord.st) * color;

    if (effectIntensity > 0.0) {

        float blurAmount = 0.0015 * effectIntensity;
        vec4 blurColor = baseColor;
        float samples = 1.0;

        for(int x = -2; x <= 2; x++) {
            for(int y = -2; y <= 2; y++) {
                if(x != 0 || y != 0) {
                    vec4 sampleColor = texture2D(texture, texcoord.st + vec2(x, y) * blurAmount) * color;

                    blurColor.rgb += sampleColor.rgb;
                    samples += 1.0;
                }
            }
        }

        blurColor.rgb /= samples; 
        blurColor.a = baseColor.a; 

        baseColor = mix(baseColor, blurColor, effectIntensity * 0.4);
    }

    gl_FragData[0] = baseColor;
    gl_FragData[1] = vec4(vec3(gl_FragCoord.z), 1.0);

    if (effectIntensity > 0.0) {
        vec3 redTint = vec3(1.6, 0.3, 0.3); 
        gl_FragData[0].rgb *= mix(vec3(1.0), redTint, effectIntensity * 0.95);

        gl_FragData[0].rgb *= mix(1.0, 1.3, effectIntensity * 0.4);

        float luminance = dot(gl_FragData[0].rgb, vec3(0.299, 0.587, 0.114));
        vec3 redTone = vec3(luminance) * vec3(1.4, 0.5, 0.5);
        gl_FragData[0].rgb = mix(gl_FragData[0].rgb, redTone, effectIntensity * 0.5);
    }

    if (fogMode == GL_EXP) {
        gl_FragData[0].rgb = mix(gl_FragData[0].rgb, gl_Fog.color.rgb, 1.0 - clamp(exp(-gl_Fog.density * gl_FogFragCoord), 0.0, 1.0));
    } else if (fogMode == GL_LINEAR) {
        gl_FragData[0].rgb = mix(gl_FragData[0].rgb, gl_Fog.color.rgb, clamp((gl_FogFragCoord - gl_Fog.start) * gl_Fog.scale, 0.0, 1.0));
    }
}