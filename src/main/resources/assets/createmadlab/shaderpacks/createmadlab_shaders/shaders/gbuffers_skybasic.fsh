#version 120

const float Color = .2; //Rainbow intensity
const float Animation = .4; //Animation speed
const float Spread = .5; //Color Spread

uniform sampler2D texture;

uniform float frameTimeCounter;
uniform float blindness;
uniform int isEyeInWater;

varying vec4 color;
varying vec3 world;

vec3 hash3(vec3 p)
{
    return fract(cos(p*mat3(-31.14,15.92,65.35,-89.79,-32.38,46.26,43.38,32.79,-02.88))*41.97);
}
vec3 value3(vec3 p)
{
    vec3 f = floor(p);
    vec3 s = p-f; s*= s*s*(3.-s-s);
    const vec2 o = vec2(0,1);
    return mix(mix(mix(hash3(f+o.xxx),hash3(f+o.yxx),s.x),
                   mix(hash3(f+o.xyx),hash3(f+o.yyx),s.x),s.y),
               mix(mix(hash3(f+o.xxy),hash3(f+o.yxy),s.x),
                   mix(hash3(f+o.xyy),hash3(f+o.yyy),s.x),s.y),s.z);
}
void main()
{
    // Internal fade timer - 1m fade in, 3m peak, 1m fade out
    float totalDuration = 300.0; // 5 minutes total
    float fadeInDuration = 60.0; // 1 minute
    float peakDuration = 180.0;  // 3 minutes
    float fadeOutDuration = 60.0; // 1 minute

    float elapsed = mod(frameTimeCounter, totalDuration);
    float fadeIntensity = 0.0;

    if (elapsed < fadeInDuration) {
        // Fade in (0 to 1 over 1 minute)
        fadeIntensity = elapsed / fadeInDuration;
    } else if (elapsed < fadeInDuration + peakDuration) {
        // Peak intensity (3 minutes)
        fadeIntensity = 1.0;
    } else {
        // Fade out (1 to 0 over 1 minute)
        float fadeOutElapsed = elapsed - (fadeInDuration + peakDuration);
        fadeIntensity = 1.0 - (fadeOutElapsed / fadeOutDuration);
    }

    float light = 1.-blindness;

    float fog = (isEyeInWater>0) ? 1.-exp(-gl_FogFragCoord * gl_Fog.density):
    clamp((gl_FogFragCoord-gl_Fog.start) * gl_Fog.scale, 0., 1.);

    vec4 col = vec4(vec3(value3(world*.04*Spread)*8.+value3((world+world.zxy)*.1*Spread)*3.),0)*light;
    col = mix(color,vec4(cos(color.rgb*3.+col.rgb+frameTimeCounter*Animation)*.5+.5,color.a),Color);
    col.rgb = mix(col.rgb, gl_Fog.color.rgb, fog);

    // Fade in/out effect
    vec4 normalColor = color;
    normalColor.rgb = mix(normalColor.rgb, gl_Fog.color.rgb, fog);
    col.rgb = mix(normalColor.rgb, col.rgb, fadeIntensity);

    gl_FragData[0] = col;
}