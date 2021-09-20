#extension GL_OES_EGL_image_external : require

precision mediump float;
varying vec2 aCoord;
uniform samplerExternalOES vTexture;
void main(){
//Opengl 自带函数
    vec4 rgba = texture2D(vTexture,aCoord);
//    灰色  滤镜
    float color=(rgba.r + rgba.g + rgba.b) / 3.0;
    vec4 tempColor=vec4(color,color,color,1);
    gl_FragColor=tempColor;

}