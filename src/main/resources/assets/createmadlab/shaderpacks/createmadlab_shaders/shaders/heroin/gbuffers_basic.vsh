// "The Wave", a wavy world shader that makes the world move like a rolling ocean.
// So yeah, possible seasickness warning.
// By Beed28

#version 120 

varying vec4 texcoord;
varying vec4 color;
varying vec4 lmcoord;

uniform float frameTimeCounter;
uniform vec3 cameraPosition;
uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;

void main() {

    texcoord = gl_TextureMatrix[0] * gl_MultiTexCoord0;
    vec4 position = gl_ModelViewMatrix * gl_Vertex;

    position = gbufferModelViewInverse * position;
    vec3 worldpos = position.xyz + cameraPosition;

    float PI = 3.14159265358979323846264;

    float totalDuration = 180.0;
    float fadeInDuration = 10.0;
    float peakDuration = 150.0;
    float fadeOutDuration = 20.0;

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

    if (gl_Color.a == 0.4) {
        float y = position.y;
        float z = position.z;
        float om = cos(2.0 * PI * (frameTimeCounter / 5.50 + (worldpos.z) / 256.0)) / 12.0 * fadeIntensity;
        position.y = z*sin(om)+y*cos(om);
        position.z = z*cos(om)-y*sin(om);
    }
    if (gl_Color.a == 0.4) {
        float y = position.y;
        float x = position.x;
        float om = cos(2.0 * PI * (frameTimeCounter / 5.50 + (worldpos.x) / 384.0)) / 12.0 * fadeIntensity;
        position.y = x*sin(om)+y*cos(om);
        position.x = x*cos(om)-y*sin(om);
    }

    if (gl_Color.a != 0.4) {
        float y = position.y;
        float z = position.z;
        float om = cos(2.0 * PI * (frameTimeCounter / 5.50 + (worldpos.z) / 9999.0)) / 12.0 * fadeIntensity;
        position.y = z*sin(om)+y*cos(om);
        position.z = z*cos(om)-y*sin(om);
    }
    if (gl_Color.a != 0.4) {
        float y = position.y;
        float x = position.x;
        float om = cos(2.0 * PI * (frameTimeCounter / 5.50 + (worldpos.x) / 9999.0)) / 12.0 * fadeIntensity;
        position.y = x*sin(om)+y*cos(om);
        position.x = x*cos(om)-y*sin(om);
    }

    position = gbufferModelView * position;

    gl_Position = gl_ProjectionMatrix * position;

    color = gl_Color;

    texcoord = gl_TextureMatrix[0] * gl_MultiTexCoord0;

    lmcoord = gl_TextureMatrix[1] * gl_MultiTexCoord1;

    gl_FogFragCoord = gl_Position.z;
}