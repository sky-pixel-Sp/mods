#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D MainSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform Globals {
    ivec3 CameraBlockPos;
    vec3 CameraOffset;
    vec2 ScreenSize;
    float GlintAlpha;
    float GameTime;
    int MenuBlurRadius;
    int UseRgss;
};
layout(std140) uniform SkyPalette {
    vec3 TopColor;
    vec3 NebulaColor;
    vec3 HorizonColor;
    float Intensity;
    float Variant;
};
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34,456.21));
    p += dot(p,p+45.32);
    return fract(p.x*p.y);
}
float noise2(vec2 p) {
    vec2 i=floor(p), f=fract(p);
    f=f*f*(3.0-2.0*f);
    return mix(mix(hash21(i),hash21(i+vec2(1,0)),f.x),
               mix(hash21(i+vec2(0,1)),hash21(i+vec2(1,1)),f.x),f.y);
}
float fbm(vec2 p) {
    float v=0.0, a=0.5;
    for(int i=0;i<5;i++){v+=noise2(p)*a;p=mat2(1.6,1.2,-1.2,1.6)*p+0.17;a*=0.5;}
    return v;
}
vec3 sky(vec2 uv) {
    float t=GameTime*125.663706;
    vec3 c=mix(HorizonColor,TopColor,smoothstep(0.02,0.98,uv.y));
    vec2 p=(uv-0.5)*vec2(4.4,3.0)+vec2(t*0.012,-t*0.008);
    float n=fbm(p*1.25+4.0)*0.62+fbm(p*2.0-7.0)*0.38;
    float cloud=smoothstep(0.36,0.76,n);
    float filament=smoothstep(0.53,0.82,fbm(p*3.1+12.4));
    c+=NebulaColor*cloud*Intensity*0.72;
    c+=mix(NebulaColor,vec3(1.0),0.25)*filament*Intensity*0.12;
    c+=HorizonColor*exp(-abs(uv.y-0.34)*8.0)*0.13;

    vec2 grid=uv*vec2(124.0,72.0), cell=floor(grid), local=fract(grid);
    vec2 offset=vec2(hash21(cell+1.7),hash21(cell+8.3));
    float d=length(local-(0.18+offset*0.64));
    float stars=(1.0-smoothstep(0.0,0.055,d))*step(0.986,hash21(cell+41.0));
    c+=vec3(0.68,0.77,1.0)*stars*(0.52+0.48*sin(t*1.3+hash21(cell+19.0)*6.28318));

    if(Variant<0.5) {
        float ribbon=sin(uv.x*12.0+n*9.0+t*0.22);
        float curtain=smoothstep(0.48,0.84,n+ribbon*0.08)*smoothstep(0.32,0.66,uv.y);
        c+=vec3(0.20,0.92,0.67)*curtain*0.42+vec3(0.30,0.54,1.0)*curtain*0.18;
    }
    if(Variant>6.5&&Variant<7.5) {
        vec2 q=(uv-vec2(0.72,0.57))*vec2(1.0,1.7); float r=length(q);
        float disc=1.0-smoothstep(0.105,0.12,r);
        float ring=1.0-smoothstep(0.012,0.032,abs(r-0.19+q.y*0.06));
        c=mix(c,vec3(0.92,0.69,0.47),disc*0.88)+vec3(0.92,0.67,0.47)*ring*0.72;
    }
    if(Variant>9.5&&Variant<10.5) {
        float x=fract(GameTime*0.7+0.15);
        vec2 q=uv-vec2(x,0.70-x*0.35);
        c+=vec3(0.72,0.88,1.0)*(exp(-dot(q,q)*15000.0)*1.3+
           exp(-abs(q.y+q.x*0.35)*210.0)*exp(-max(0.0,-q.x)*10.0)*0.45);
    }
    if(Variant>10.5&&Variant<11.5) {
        vec2 q=uv-vec2(0.5,0.48); float a=atan(q.y,q.x);
        c+=vec3(0.35,0.46,1.0)*pow(max(0.0,sin(a*43.0+t*0.4)),22.0)*
           smoothstep(0.04,0.62,length(q))*0.38;
    }
    if(Variant>16.5&&Variant<17.5) {
        vec2 q=(uv-vec2(0.5,0.53))*vec2(1.0,1.6); float r=length(q);
        float ring=1.0-smoothstep(0.018,0.052,abs(r-0.19));
        float core=1.0-smoothstep(0.095,0.12,r);
        c+=vec3(0.88,0.54,1.0)*ring*1.25;
        c=mix(c,vec3(0.002,0.001,0.008),core);
    }
    if(Variant>17.5&&Variant<18.5) {
        float bn=fbm(vec2(uv.x*24.0,uv.y*4.0+t*0.25));
        float bolt=1.0-smoothstep(0.008,0.035,abs(uv.x-0.54+(bn-0.5)*0.08));
        c+=vec3(0.56,0.68,1.0)*bolt*pow(max(0.0,sin(t*0.37)),18.0)*1.6;
    }
    if(Variant>3.5&&Variant<7.5) {
        float r=length((uv-vec2(0.73,0.43))*vec2(1.0,1.4));
        float sun=1.0-smoothstep(0.045,0.085,r), halo=exp(-r*15.0);
        c=mix(c,vec3(1.0,0.70,0.43),sun*0.88)+vec3(1.0,0.34,0.16)*halo*0.28;
    }
    if(Variant>15.5&&Variant<16.5) {
        float horizon=1.0-smoothstep(0.02,0.18,abs(uv.y-0.31));
        float gx=1.0-smoothstep(0.0,0.018,abs(fract(uv.x*18.0)-0.5));
        float gy=1.0-smoothstep(0.0,0.018,abs(fract((uv.y-0.31)*28.0)-0.5));
        c+=vec3(0.95,0.12,0.62)*max(gx,gy)*horizon*0.46;
    }
    if(Variant>18.5) {
        float wave=sin(uv.x*18.0+t*0.18+n*3.0)*0.5+0.5;
        float band=1.0-smoothstep(0.02,0.14,abs(uv.y-0.28-wave*0.035));
        c+=vec3(0.14,0.63,0.82)*band*0.38;
    }
    return pow(max(c,vec3(0.0)),vec3(0.92));
}
void main() {
    vec4 scene=texture(MainSampler,texCoord);
    float depth=texture(DepthSampler,texCoord).r;
    if(depth<0.999999){fragColor=scene;return;}
    fragColor=vec4(sky(texCoord),1.0);
}
