#version 120

const float Radius = 12.; //Globe radius
const float Toggle = -1.; //Enable, disable or invert globe [-1. 0. 1.]

const float Terrain = .6; //Terrain distortion level
const float Offset = 1.; //Camera height offset

const float Amount = .5; //Wave distortion intensity
const float Frequency = .5; //Wave frequency
const float Speed = .5; //Wave animation speed

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

    float c = fract(pos.y+cameraPosition.y);
    c *= min(10.-c/.1,1.);

    // Apply vertex distortions with fade intensity
    vec3 distortion = off(pos+cameraPosition) * fadeIntensity;
    pos += distortion;
    vec3 h = pos+cameraPosition;
    pos.y -= off(cameraPosition-vec3(0,1,0)).y*Offset * fadeIntensity;
    float water = float(mc_Entity.x==1.);
    pos.y += ((cos(h.x*2.+h.y*1.+h.z*2.+frameTimeCounter*4.)*.1-.1)*c)*water*Terrain * fadeIntensity;
    world = pos;
    pos.y -= dot(pos.xz,pos.xz)/Radius/Radius*Toggle * fadeIntensity;

    gl_Position = gl_ProjectionMatrix * gbufferModelView * vec4(pos,1);
    gl_FogFragCoord = length(pos);

    color = gl_Color;
    coord0 = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    coord1 = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    id = mc_Entity.x;
}