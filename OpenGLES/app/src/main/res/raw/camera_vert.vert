//把顶点左边给vPosition
//vec4是gpu里的数据类型，4个字节
attribute vec4 vPosition;

//接收纹理坐标，接收采样器采样图片的坐标，就是摄像头捕获的数据的坐标
attribute vec4 vCoord;

//oepngl 和 camera的坐标不一样，需要进行矩阵变换
uniform mat4 vMatrix;

//传给片元着色器 像素点, 变量名aCoord要和camera_frag.frag里的一样
varying vec2 aCoord;

void main(){
    //给gpu顶点程序变量gl_Position赋值, gpu就知道要渲染的形状了
    gl_Position = vPosition;

    //矩阵变换
    aCoord= (vMatrix * vCoord).xy;
}