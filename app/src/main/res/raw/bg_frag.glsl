uniform vec2 uResolution;
uniform float uAnimTime;
uniform vec4 uBound;
uniform float uTranslateY;
uniform vec3 uPoints[4];
uniform vec4 uColors[4];
uniform float uAlphaMulti;
uniform float uNoiseScale;
uniform float uPointOffset;
uniform float uPointRadiusMulti;
uniform float uSaturateOffset;
uniform float uLightOffset;
uniform float uAlphaOffset;
uniform float uShadowColorMulti;
uniform float uShadowColorOffset;
uniform float uShadowNoiseScale;
uniform float uShadowOffset;

vec3 hsl2rgb(in vec3 c) {
    vec3 rgb = clamp(abs(mod(c.x*6.0+vec3(0.0,4.0,2.0),6.0)-3.0)-1.0,0.0,1.0);
    return c.z + c.y * (rgb-0.5)*(1.0-abs(2.0*c.z-1.0));
}

vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0,-1.0/3.0,2.0/3.0,-1.0);
    vec4 p = mix(vec4(c.bg,K.wz),vec4(c.gb,K.xy),step(c.b,c.g));
    vec4 q = mix(vec4(p.xyw,c.r),vec4(c.r,p.yzx),step(p.x,c.r));
    float d = q.x - min(q.w,q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z+(q.w-q.y)/(6.0*d+e)), d/(q.x+e), q.x);
}

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0,2.0/3.0,1.0/3.0,3.0);
    vec3 p = abs(fract(c.xxx+K.xyz)*6.0-K.www);
    return c.z * mix(K.xxx, clamp(p-K.xxx,0.0,1.0), c.y);
}

float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx)*0.13);
    p3 += dot(p3,p3.yzx+3.333);
    return fract((p3.x+p3.y)*p3.z);
}

float perlin(vec2 x) {
    vec2 i = floor(x);
    vec2 f = fract(x);
    float a = hash(i);
    float b = hash(i+vec2(1,0));
    float c = hash(i+vec2(0,1));
    float d = hash(i+vec2(1,1));
    vec2 u = f*f*(3.0-2.0*f);
    return mix(a,b,u.x) + (c-a)*u.y*(1.0-u.x) + (d-b)*u.x*u.y;
}

float gradientNoise(vec2 uv) {
    return fract(52.9829189 * fract(dot(uv, vec2(0.06711056,0.00583715))));
}

vec4 main(vec2 fragCoord) {
    vec2 vUv = fragCoord / uResolution;
    vUv.y = 1.0 - vUv.y;

    vec2 uv = vUv;
    uv -= vec2(0.0, uTranslateY);
    uv -= uBound.xy;
    uv /= uBound.zw;

    vec4 color = vec4(0.0);

    float noiseValue = perlin(vUv * uNoiseScale + vec2(-uAnimTime));

    for (int i = 0; i < 4; i++) {
        vec4 pc = uColors[i];
        pc.rgb *= pc.a;

        vec2 p = uPoints[i].xy;
        float r = uPoints[i].z * uPointRadiusMulti;

        float id = float(i);
        float t = uAnimTime * (0.5 + id * 0.1) + id * 25.0; 
        
        p.x += (sin(t) * 0.7 + sin(t * 0.4) * 0.3) * uPointOffset;
        
        p.y += (cos(t * 0.8) * 0.6 + cos(t * 0.2) * 0.4) * uPointOffset;

        float d = distance(uv, p);
        float pct = smoothstep(r, 0.0, d);

        color.rgb = mix(color.rgb, pc.rgb, pct);
        color.a   = mix(color.a, pc.a, pct);
    }

    float n = smoothstep(0.0, 1.0, noiseValue);
    color.rgb /= max(color.a, 1e-4);

    vec3 hsv = rgb2hsv(color.rgb);
    hsv.y = mix(hsv.y, 0.0, n * uSaturateOffset);
    color.rgb = hsv2rgb(hsv);

    color.rgb += n * uLightOffset;
    color.a = clamp(color.a * uAlphaMulti, 0.0, 1.0);

    color += (10.0/255.0)*gradientNoise(fragCoord) - (5.0/255.0);

    return vec4(color.rgb * color.a, color.a);
}
