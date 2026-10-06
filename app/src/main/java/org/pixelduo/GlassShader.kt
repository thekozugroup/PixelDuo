package org.pixelduo
import android.graphics.RuntimeShader

/** AGSL adaptation of chuspeeism/iphone-duo's screenColor: 9 weighted taps,
 * 36px progressive radius, projected UI rays and edge darkening. MIT notice bundled. */
internal object DuoGlassShader {
 val source="""
 uniform shader content;
 uniform shader mip1; uniform shader mip2; uniform shader mip3; uniform shader mip4; uniform shader mip5; uniform shader mip6; uniform shader mip7;
 uniform float aaStrength;
 half3 sampleLevel(float2 p,float lod){
  // A bounded trilinear prefilter reuses existing mip levels, including at zero blur.
  lod=max(lod,aaStrength);
  if(lod<1.0)return mix(content.eval(p).rgb,mip1.eval(p).rgb,half(lod));
  if(lod<2.0)return mix(mip1.eval(p).rgb,mip2.eval(p).rgb,half(lod-1.0));
  if(lod<3.0)return mix(mip2.eval(p).rgb,mip3.eval(p).rgb,half(lod-2.0));
  if(lod<4.0)return mix(mip3.eval(p).rgb,mip4.eval(p).rgb,half(lod-3.0));
  if(lod<5.0)return mix(mip4.eval(p).rgb,mip5.eval(p).rgb,half(lod-4.0));
  if(lod<6.0)return mix(mip5.eval(p).rgb,mip6.eval(p).rgb,half(lod-5.0));
  return mix(mip6.eval(p).rgb,mip7.eval(p).rgb,half(clamp(lod-6.0,0.0,1.0)));
 }
 uniform float2 texSize;
 uniform float2 origin;
 uniform float2 extent;
 uniform float2 sampleScale;
 uniform float2 sampleOffset;
 uniform float progress;
 uniform float foldRadians;
 uniform float inner;
 uniform float fallback;
 uniform float horizontal;
 uniform float reverse;
 uniform float intensity;
 uniform float blurStrength;
 uniform float seamOffset;
 uniform float reflectedCover;
 uniform float windowReveal;
 uniform float earlyStretch;
 uniform float endStretch;
 uniform float startupEasing;
 uniform float verticalCompression;
 float coverAt(float x,float t){
  float a=3.141592654-t,c=cos(a),sn=sin(a);
  float hx=-0.23396,hz=-0.550084,px=hx-x*7.73936;
  float anchor=(c*hx+sn*hz)*(-39.75052)/(-sn*hx+c*hz+0.275454-40.0);
  float point=(c*px+sn*hz)*(-39.75052)/(-sn*px+c*hz+0.275454-40.0);
  return (point-anchor)/(7.73936*39.75052/39.174462);
 }
 float earlyCover(float x,float t,float original){
  float joinAngle=1.047197551;
  if(t>=joinAngle)return original;
  float end=coverAt(x,joinAngle),distance=max(0.0,x-end);
  float endSlope=-(coverAt(x,joinAngle+0.0001)-coverAt(x,joinAngle-0.0001))/0.0002*joinAngle;
  // Slider preserves the original 0–100% scale and extrapolates to 300%.
  float startSlope=max(0.0,3.0*distance-endSlope)*mix(0.05,0.9,clamp(earlyStretch,0.0,3.0));
  float u=clamp(t/joinAngle,0.0,1.0),u2=u*u,u3=u2*u;
  float stretch=(u3-2.0*u2+u)*startSlope+(-2.0*u3+3.0*u2)*distance+(u3-u2)*endSlope;
  return x-stretch;
 }

 float innerAt(float x,float t,float span){
  float c=cos(t),sn=sin(t),z=-0.025974;
  float anchor=sn*z*(-39.75052)/(c*z+0.275454-40.0);
  float px=-x*7.89935;
  float point=(c*px+sn*z)*(-39.75052)/(-sn*px+c*z+0.275454-40.0);
  return (anchor-point)/7.89935;
 }
 float earlyInner(float x,float t,float original,float span){
  float joinAngle=1.047197551;
  if(t>=joinAngle)return original;
  float end=innerAt(x,joinAngle,span),distance=max(0.0,x-end);
  float endSlope=-(innerAt(x,joinAngle+0.0001,span)-innerAt(x,joinAngle-0.0001,span))/0.0002*joinAngle;
  // Slider preserves the original 0–100% scale and extrapolates to 300%.
  float startSlope=max(0.0,3.0*distance-endSlope)*mix(0.05,0.9,clamp(earlyStretch,0.0,3.0));
  float u=clamp(t/joinAngle,0.0,1.0),u2=u*u,u3=u2*u;
  float stretch=(u3-2.0*u2+u)*startSlope+(-2.0*u3+3.0*u2)*distance+(u3-u2)*endSlope;
  return x-stretch;
 }

 half4 main(float2 p) {
  float2 uv=(p-origin)/extent;
  if(any(lessThan(uv,float2(0))) || any(greaterThan(uv,float2(1)))) return half4(0);
  float axis=mix(uv.x,uv.y,horizontal);
  axis=mix(axis,1.0-axis,reverse);
  // Project the folded physical panel through the reference's fixed eye at z=40.
  // Work in canonical hinge coordinates, then map back to screenshot orientation.
  if(inner>0.5 && fallback<0.5 && axis>=0.5+seamOffset) return half4(0);
  float across=mix(uv.y,uv.x,horizontal);
  float a=foldRadians;
  float c=cos(a), sn=sin(a);
  float width=inner>0.5 ? 15.7987 : 7.73936;
  float px=inner>0.5 ? (-7.89935+axis*width) : (-0.23396-axis*width);
  if(inner>0.5 && fallback>0.5) px=(axis-1.0)*7.89935;
  float py=(1.0-across)*11.1035+0.34562-5.8974;
  float pz=inner>0.5 ? 0.24948 : -0.27463;
  if(inner<0.5 || px<0.0){float z=pz-0.275454;float oldX=px;px=c*oldX+sn*z;pz=-sn*oldX+c*z+0.275454;}
  float depth=(0.24948-40.0)/(pz-40.0);
  float projectedX=px*depth;
  float projectedY=py*depth;
  float mapped=(projectedX+7.89935)/15.7987;
  float edge=(mapped-0.5)/(-0.5);
  if(inner>0.5 && fallback>0.5){mapped=1.0+projectedX/7.89935;edge=1.0-mapped;}
  if(inner<0.5){
   float hx=-0.23396, hz=-0.27463-0.275454;
   float foldedX=c*hx+sn*hz;
   float foldedZ=-sn*hx+c*hz+0.275454;
   float anchorDepth=(0.24948-40.0)/(foldedZ-40.0);
   float anchor=foldedX*anchorDepth;
   // Keep the cover's fixed edge flush at both corners. Perspective is relative
   // to that edge, not to the reference model's offset screen plane.
   projectedY/=anchorDepth;
   float frameWidth=7.73936*(40.0-0.24948)/(40.0-0.825538);
   mapped=(projectedX-anchor)/frameWidth;
   edge=mapped;
  }
  float corrected=mix(mapped,1.0-mapped,reverse);
  float projectedAcross=1.0-(projectedY-(0.34562-5.8974))/11.1035;
  float2 sourceUV=horizontal>0.5 ? float2(projectedAcross,corrected) : float2(corrected,projectedAcross);
  // V2 smoothly advances cover stretch. Keep original projectedAcross,
  // edge shading and perspective. Beyond the early-curve join, the original
  // horizontal mapping passes through exactly.
  if(windowReveal>0.5){
   float limited=inner>0.5 ? max(mapped,axis) : earlyCover(axis,3.141592654-a,mapped);
   if(inner<0.5)limited/=mix(1.0,clamp(endStretch,0.8,1.5)/0.8,smoothstep(0.523598776,1.047197551,3.141592654-a));
   if(inner>0.5){
    float innerHinge=fallback>0.5 ? 1.0 : 0.5;
    if(axis<innerHinge){
     float x=(innerHinge-axis)/innerHinge;
     float distance=earlyInner(x,a,innerAt(x,a,innerHinge),innerHinge);
     // Low settings reduce the displacement from flat; they must never
     // divide the source distance by <1 and introduce inward compression.
     if(endStretch<0.8)distance=mix(x,clamp(distance,0.0,x),clamp(endStretch/0.8,0.0,1.0));
     else distance/=mix(1.0,clamp(endStretch,0.8,1.5)/0.8,smoothstep(0.523598776,1.047197551,a));
     limited=innerHinge-innerHinge*distance;
    }else limited=axis;
   }
   float correctedV2=mix(limited,1.0-limited,reverse);
   if(horizontal>0.5)sourceUV.y=correctedV2;else sourceUV.x=correctedV2;
  }
  float seamMask=0.0;
  if(inner>0.5 && fallback<0.5 && seamOffset>0.0){
   seamMask=1.0-smoothstep(max(0.5,0.5+seamOffset-0.05),0.5+seamOffset,axis);
   if(axis>=0.5)sourceUV=uv;
  }
  // Cover startup: reach opacity before separating the captured image from
  // the live image below it. Smooth geometry only within the first few degrees.
  if(windowReveal>0.5 && startupEasing>0.5)sourceUV=mix(uv,sourceUV,smoothstep(0.015,0.05,progress));
  // Preserve the exact original projection at the default, including rotation.
  if(verticalCompression!=1.0){
   float amount=clamp(verticalCompression,0.0,2.0);
   if(horizontal>0.5)sourceUV.x=uv.x+(sourceUV.x-uv.x)*amount;
   else sourceUV.y=uv.y+(sourceUV.y-uv.y)*amount;
  }
  sourceUV=sourceUV*sampleScale+sampleOffset;
  float shaderProgress=inner>0.5 ? clamp(a/1.570796327,0.0,1.0) : clamp((3.141592654-a)/1.570796327,0.0,1.0);
  float motion=smoothstep(0.0,1.0,shaderProgress);
  // Reduce radius and tap count for a bounded transition effect.
  float blurEdge=edge;
  if(inner>0.5 && fallback<0.5)blurEdge=(edge+2.0*seamOffset)/(1.0+2.0*seamOffset);
  if(reflectedCover>0.5)blurEdge=max(blurEdge,(2.0*seamOffset-axis)/(1.0+2.0*seamOffset));
  float radius=36.0*motion*pow(clamp(blurEdge,0.0,1.0),1.35);
  radius*=blurStrength*(inner>0.5?1.25:1.0);
  float2 footprint=max((0.5+aaStrength)/texSize,float2(radius)*0.75/texSize);
  half3 color=half3(0);
  if(radius<0.01){
   float2 coverage=smoothstep(-footprint,footprint,sourceUV)*(1.0-smoothstep(1.0-footprint,1.0+footprint,sourceUV));
   color=sampleLevel(clamp(sourceUV,float2(0),float2(1))*texSize,0.0)*half(coverage.x*coverage.y);
  }else{
  for(int y=-1;y<=1;y++) {
   for(int x=-1;x<=1;x++) {
    float wx=x==0?2.0:1.0;
    float wy=y==0?2.0:1.0;
    float2 sampleUV=sourceUV+float2(float(x),float(y))*radius/texSize;
    float2 coverage=smoothstep(-footprint,footprint,sampleUV)*(1.0-smoothstep(1.0-footprint,1.0+footprint,sampleUV));
    color+=sampleLevel(clamp(sampleUV,float2(0),float2(1))*texSize,log2(max(1.0,radius)))*half(coverage.x*coverage.y*wx*wy/16.0);
   }
  }
  }
  float effect=motion*pow(clamp((edge-0.2)/0.8,0.0,1.0),1.35);
  color*=half(1.0-min(1.0,effect*2.0*intensity));
  float alpha=smoothstep(0.0,(windowReveal>0.5 && startupEasing>0.5)?0.015:0.035,progress);
  if(inner>0.5 && fallback<0.5 && axis>=0.5)alpha*=seamMask;
  return half4(color*half(alpha),half(alpha));
 }
 """.trimIndent()
}
