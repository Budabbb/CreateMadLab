#version 120

uniform sampler2D texture;
uniform sampler2D lightmap;

varying vec4 color;
varying vec4 texcoord;
varying vec4 lmcoord;

const int GL_LINEAR = 9729;
const int GL_EXP = 2048;

uniform int fogMode;
uniform float frameTimeCounter;

void main() {
    // Fade timing: 10s fade-in, 150s peak, 20s fade-out (total 180s/3min)
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

    // Reduced blur effect
    vec4 baseColor = texture2D(texture, texcoord.st) * texture2D(lightmap, lmcoord.st) * color;

    if (effectIntensity > 0.0) {
        // Reduced blur amount
        float blurAmount = 0.0015 * effectIntensity;
        vec4 blurColor = baseColor;
        float samples = 1.0;

        // 8-sample blur for reduced effect
        for(int x = -2; x <= 2; x++) {
            for(int y = -2; y <= 2; y++) {
                if(x != 0 || y != 0) {
                    vec4 sampleColor = texture2D(texture, texcoord.st + vec2(x, y) * blurAmount) * texture2D(lightmap, lmcoord.st) * color;
                    // Only accumulate color, preserve original alpha
                    blurColor.rgb += sampleColor.rgb;
                    samples += 1.0;
                }
            }
        }

        blurColor.rgb /= samples; // Average the color samples
        blurColor.a = baseColor.a; // Preserve original alpha

        // Mix between original and blurred (40% blur at peak - reduced)
        baseColor = mix(baseColor, blurColor, effectIntensity * 0.4);
    }

    gl_FragData[0] = baseColor;
    gl_FragData[1] = vec4(vec3(gl_FragCoord.z), 1.0);

    // STRONG golden warm tint
    if (effectIntensity > 0.0) {
        vec3 warmTint = vec3(1.5, 1.2, 0.5); // Very strong golden
        gl_FragData[0].rgb *= mix(vec3(1.0), warmTint, effectIntensity * 0.9);

        // Add brightness and warmth
        gl_FragData[0].rgb *= mix(1.0, 1.4, effectIntensity * 0.5);

        // Sepia-like effect for extra warmth
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