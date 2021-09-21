//   纹理   当前上色点的坐标  aCoord  对应起来
varying highp vec2 aCoord;
uniform   sampler2D  vTexture;
//从java接收的变量，不断的变化 ， 告诉你条件 ，从cpu 传进来  scalePercent  >1 ，一直不断增加，灵魂放大效果
uniform highp float scalePercent;
//混合 透明度    ----》 有大变小   规律
uniform lowp float mixturePercent;
void main() {

//    lowp  vec4 textureColor  =  texture2D(vTexture,aCoord);
//中心点
    highp vec2 center=vec2(0.5,0.5);

    highp vec2 textureCoordinateToUse = aCoord;//临时变量 , 产生灵魂的坐标，最开始为aCoord原始采样点

    textureCoordinateToUse -= center;//[0.6,0.6]  -  [0.5, 0.5]   =    [0.1, 0.1]

    //y轴来说，采样点一定比 需要渲染 的坐标点要小

    textureCoordinateToUse = textureCoordinateToUse/scalePercent;//textureCoordinateToUse不断减小

    textureCoordinateToUse += center;//[0, -0.09] + [0.5,0.5] = [0.5,0.41] , 最终 实际是变大
//    [0.5,0.6]
    //    [0.5,0.6]
   gl_FragColor= texture2D(vTexture,aCoord);
////[0.5,0.59]   //原来绘制颜色

    //texture2D 采样器 ，是一个工具 ， 返回颜色数组 [r  , g ,b  ,a ], lowp表示低精度
    lowp vec4 textureColor = texture2D(vTexture, aCoord);
//      新采样颜色
    lowp vec4 textureColor2= texture2D(vTexture,textureCoordinateToUse);

    //线性混合 mixturePercent   1  --->0
    gl_FragColor= mix(textureColor,textureColor2,mixturePercent);

}