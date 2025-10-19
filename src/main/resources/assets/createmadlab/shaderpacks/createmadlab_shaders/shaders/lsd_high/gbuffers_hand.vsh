#version 120

const float Terrain = 1.; //Terrain distortion level
const float Hand = 1.; //Hand distortion level
const float Offset = 1.; //Camera height offset

const float Amount = .8; //Wave distortion intensity
const float Frequency = .8; //Wave frequency
const float Speed = .8; //Wave animation speed

attribute vec2 mc_Entity;

uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;
uniform vec3 cameraPosition;
uniform float frameTimeCounter;

varying vec4 color;
varying vec3 model;
varying vec3 world;
varying vec2 coord0;
varying vec2 coord1;
varying float id;

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
vec3 off(vec3 p)
{
    vec3 wave = cos(p.zxy*.5*Frequency+frameTimeCounter*Speed)*Amount;
    return (value3(p)*.4+value3(p/2.)*.6+value3(p/8.)-1.)*Terrain+wave;
}

void main()
{
    // Standardized fade timer - 6 minutes total: 2m fade in, 3m peak, 1m fade out
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

    vec3 pos = (gl_ModelViewMatrix * gl_Vertex).xyz;
    pos = mat3(gbufferModelViewInverse) * pos  + gbufferModelViewInverse[3].xyz;
    model = pos+cameraPosition;

    // Apply vertex distortions with fade intensity
    vec3 distortion = off(pos+cameraPosition) * Hand * fadeIntensity;
    pos += distortion;
    pos.y -= off(cameraPosition-vec3(0,1,0)).y*Offset*Hand * fadeIntensity;

    gl_Position = gl_ProjectionMatrix * gbufferModelView * vec4(pos,1);
    gl_FogFragCoord = length(pos);

    color = gl_Color;
    world = pos;
    coord0 = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    coord1 = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    id = mc_Entity.x;
}