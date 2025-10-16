#version 120

const float Shininess = 1.; //Water shine intensity

const float Color = 1.; //Rainbow intensity
const float Animation = .4; //Animation speed
const float Spread = .5; //Color Spread

uniform sampler2D texture;
uniform sampler2D lightmap;

uniform mat4 gbufferModelViewInverse;
uniform vec3 shadowLightPosition;
uniform float frameTimeCounter;
uniform float blindness;
uniform int isEyeInWater;

varying vec4 color;
varying vec3 model;
varying vec3 world;
varying vec2 coord0;
varying vec2 coord1;

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

    vec3 dir = normalize((gbufferModelViewInverse * vec4(shadowLightPosition,0)).xyz);
    float flip = clamp(dir.y/.1,-1.,1.); dir *= flip;
    vec3 norm = normalize(cross(dFdx(world),dFdy(world)));
    float lambert = dot(norm,dir)*.5+.5;

    float sun = exp((dot(reflect(normalize(world),norm),dir)-1.)*15.*(1.5-.5*flip));

    vec3 light = (lambert*.5+.5)*(1.-blindness) * texture2D(lightmap,coord1).rgb;
    vec4 tex = color * texture2D(texture,coord0);
    vec4 col = vec4(value3(model*.2*Spread)*8.+value3((model+model.zxy)*.4*Spread)*3.,1);
    col = mix(tex, vec4(cos(tex.rgb*3.+col.rgb+frameTimeCounter*Animation)*.5+.5,tex.a), Color) * vec4(light,1);

    // Fade in/out effect
    vec4 normalColor = tex * vec4(light, 1.0);
    col.rgb = mix(normalColor.rgb, col.rgb, fadeIntensity);

    gl_FragData[0] = col;
}