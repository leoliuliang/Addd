//使用采样器、必须写，固定的
#extension GL_OES_EGL_image_external : require


//所有float类型数据的精度是lowp
precision mediump float;

varying vec2 aCoord;

//采样器  uniform static
uniform samplerExternalOES vTexture;

void main(){
    //Opengl 自带函数texture2D, 采样出rgba颜色来
    vec4 rgba = texture2D(vTexture,aCoord);

    //给gpu变量片元颜色gl_FragColor赋值
    gl_FragColor = vec4(rgba.r,rgba.g,rgba.b,rgba.a);
}