#version 120

const float Color = .8; //Rainbow intensity
const float Animation = .4; //Animation speed
const float Spread = .5; //Color Spread

uniform sampler2D texture;

uniform float frameTimeCounter;
uniform float blindness;

varying vec4 color;
varying vec3 world;
varying vec2 coord0;

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
    float totalDuration = 360.0;    
    float fadeInDuration = 120.0;   
    float peakDuration = 180.0;     
    float fadeOutDuration = 60.0;   

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

    float light = 1.-blindness;

    vec4 tex = texture2D(texture,coord0);
    vec3 col = value3(world*.04)*8.+value3((world+world.zxy)*.1*Spread)*3.+frameTimeCounter*Animation;
    vec4 finalColor = color * mix(tex, tex * vec4((cos(col)*.5+.5)*light,1), Color);

    vec4 normalColor = color * tex;
    finalColor.rgb = mix(normalColor.rgb, finalColor.rgb, fadeIntensity);

    gl_FragData[0] = finalColor;
}