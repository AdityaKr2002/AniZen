//!DESC Unsharp Mask 5x5 (Luma)
//!HOOK MAIN
//!BIND HOOKED
//!WIDTH HOOKED.w
//!HEIGHT HOOKED.h

#define SHARPEN_AMOUNT 1.0

vec4 hook() {
    vec2 pt = HOOKED_pt;
    vec4 c = HOOKED_texOff(0.0);
    
    vec3 blur = vec3(0.0);
    
    // 5x5 Box Blur
    blur += HOOKED_texOff(vec2(-2.0*pt.x, -2.0*pt.y)).rgb;
    blur += HOOKED_texOff(vec2(-1.0*pt.x, -2.0*pt.y)).rgb;
    blur += HOOKED_texOff(vec2( 0.0*pt.x, -2.0*pt.y)).rgb;
    blur += HOOKED_texOff(vec2( 1.0*pt.x, -2.0*pt.y)).rgb;
    blur += HOOKED_texOff(vec2( 2.0*pt.x, -2.0*pt.y)).rgb;
    
    blur += HOOKED_texOff(vec2(-2.0*pt.x, -1.0*pt.y)).rgb;
    blur += HOOKED_texOff(vec2(-1.0*pt.x, -1.0*pt.y)).rgb;
    blur += HOOKED_texOff(vec2( 0.0*pt.x, -1.0*pt.y)).rgb;
    blur += HOOKED_texOff(vec2( 1.0*pt.x, -1.0*pt.y)).rgb;
    blur += HOOKED_texOff(vec2( 2.0*pt.x, -1.0*pt.y)).rgb;

    blur += HOOKED_texOff(vec2(-2.0*pt.x,  0.0*pt.y)).rgb;
    blur += HOOKED_texOff(vec2(-1.0*pt.x,  0.0*pt.y)).rgb;
    blur += c.rgb; // Center pixel
    blur += HOOKED_texOff(vec2( 1.0*pt.x,  0.0*pt.y)).rgb;
    blur += HOOKED_texOff(vec2( 2.0*pt.x,  0.0*pt.y)).rgb;

    blur += HOOKED_texOff(vec2(-2.0*pt.x,  1.0*pt.y)).rgb;
    blur += HOOKED_texOff(vec2(-1.0*pt.x,  1.0*pt.y)).rgb;
    blur += HOOKED_texOff(vec2( 0.0*pt.x,  1.0*pt.y)).rgb;
    blur += HOOKED_texOff(vec2( 1.0*pt.x,  1.0*pt.y)).rgb;
    blur += HOOKED_texOff(vec2( 2.0*pt.x,  1.0*pt.y)).rgb;

    blur += HOOKED_texOff(vec2(-2.0*pt.x,  2.0*pt.y)).rgb;
    blur += HOOKED_texOff(vec2(-1.0*pt.x,  2.0*pt.y)).rgb;
    blur += HOOKED_texOff(vec2( 0.0*pt.x,  2.0*pt.y)).rgb;
    blur += HOOKED_texOff(vec2( 1.0*pt.x,  2.0*pt.y)).rgb;
    blur += HOOKED_texOff(vec2( 2.0*pt.x,  2.0*pt.y)).rgb;
    
    blur /= 25.0;
    
    // Extract Luma (Brightness) using Rec.709 coefficients
    float luma_orig = dot(c.rgb, vec3(0.2126, 0.7152, 0.0722));
    float luma_blur = dot(blur, vec3(0.2126, 0.7152, 0.0722));
    
    // Calculate unsharp difference
    float diff = luma_orig - luma_blur;
    
    // Apply difference scaled by amount directly to RGB channels
    // This perfectly mimics FFmpeg `unsharp=5:5:amount:5:5:0` (Luma only sharpening)
    vec3 res = c.rgb + (diff * SHARPEN_AMOUNT);
    
    return vec4(clamp(res, 0.0, 1.0), c.a);
}