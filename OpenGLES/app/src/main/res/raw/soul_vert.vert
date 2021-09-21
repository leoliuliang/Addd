attribute vec4 vPosition; //变量 float[4]  一个顶点  java传过来的

attribute vec2 vCoord;  //纹理坐标, 定义在java层的坐标数组
//varying  给片元 程序  传递   变量
varying vec2 aCoord;

void main(){
    //内置变量： 把坐标点赋值给gl_position 就Ok了。  图片 矩形
    gl_Position = vPosition;
//    目前坐标值  传递给aCoord, 不断运行，动态取值
    aCoord = vCoord.xy;
}