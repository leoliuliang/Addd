#include <jni.h>
#include <string>

extern "C"{
#include "gif_lib.h"
}
#include "android/bitmap.h"
#include <android/log.h>
#define  LOG_TAG    "liuliang"
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#define  dispose(ext) (((ext)->Bytes[0] & 0x1c) >> 2)
#define  trans_index(ext) ((ext)->Bytes[3])
#define  transparency(ext) ((ext)->Bytes[0] & 1)

//定义一个宏，将argb,转换为比如 FF6512E0 这种值
#define argb(a,r,g,b) ( ((a) & 0xff) << 24 ) | ( ((b) & 0xff) << 16 ) | ( ((g) & 0xff) << 8 ) | ((r) & 0xff)

/** 结构体记录播放状态 */
struct GifBean{
    int current_frame;//当前帧数
    int total_frame;//总帧数
    int *delays;
};


extern "C"
JNIEXPORT jlong JNICALL
Java_com_mxkj_gifframework_GifHandler_loadGif(JNIEnv *env, jclass clazz, jstring path_) {
    const char* path = env->GetStringUTFChars(path_,0);

    //打开gif文件，需要的所有数据都可以在返回的GifFileType里去取，error为-1就打开失败
    int error;
    GifFileType *fileType = DGifOpenFileName(path,&error);

    //初始化缓冲区，Call DGifOpenFileName() or DGifOpenFileHandle() first to initialize I/O
    DGifSlurp(fileType);

    GifBean *gifBean = static_cast<GifBean *>(malloc(sizeof(GifBean)));
    //malloc出来的对象，在内存区域可能有脏数据，所以先memset清空
    memset(gifBean,0, sizeof(GifBean));

    //绑定数据，将gifBean和UserData进行绑定
    fileType->UserData = gifBean;

    //初始化
    gifBean->current_frame = 0;
    gifBean->total_frame = fileType->ImageCount;

    //new出来的对象都要手动释放掉
    env->ReleaseStringUTFChars(path_,path);

    return reinterpret_cast<jlong>(fileType);

}

int drawFrame1(GifFileType *gifFileType, AndroidBitmapInfo info, void *pixels) {
    GifBean *gifBean = static_cast<GifBean *>(gifFileType->UserData);
    //获取当前帧的SaveImage, 此SaveImage不是存的像素（因为存像素的话内存会爆表的），所以不能直接用，需要解压操作
    SavedImage savedImage = gifFileType->SavedImages[gifBean->current_frame];

    //图像分为两部分，1.像素；2.描述（有相关信息，比如宽高，偏移量等），所看到的gif图片不一定就是那么大，有可能左边、上边会空出来一截（就叫偏移量）
    GifImageDesc gifImageDesc = savedImage.ImageDesc;

    //临时索引，记录每一行的首地址
    int *line;
    //像素转一下为int类型，实际就是bitmap
    int *px = (int *)pixels;

    //临时记录 没个像素点
    int pointPixel;
    //像素压缩数据
    GifByteType gifByteType;
    //从图片描述里 获取颜色字典
    ColorMapObject *colorMapObject = gifImageDesc.ColorMap;
    //颜色值 rgb
    GifColorType gifColorType;

    //解压, 从减去偏移量的位置开始，一帧一帧的获取并渲染，从左上角开始，第一行遍历完后遍历第二行，一直往下遍历完一帧图片
    for (int y = gifImageDesc.Top; y < gifImageDesc.Top + gifImageDesc.Height; ++y) {
        //每次遍历完一行，将下一行的首地址赋值给line
        line = px;
        for (int x = gifImageDesc.Left; x < gifImageDesc.Left + gifImageDesc.Width; ++x) {
            //定位像素
            pointPixel = (y - gifImageDesc.Top) * gifImageDesc.Width + (x - gifImageDesc.Left);
            //得到每一个像素的压缩数据（颜色值的索引）, 不能直接使用, 需要一个颜色字典
            gifByteType = savedImage.RasterBits[pointPixel];
            //从颜色字典里得到对应的颜色值 rgb ; 也就是像素
            gifColorType = colorMapObject->Colors[gifByteType];
            //给line赋值，这里的line就是bitmap在底层的表现
            line[x] = argb(255,gifColorType.Red,gifColorType.Green,gifColorType.Blue);
        }
        //这里换行，因为每个像素4个字节，所以要除4 ，也可以直接转char类型，所以可以用px = (int *)(px + info.stride/4); 或者 px = (int *)((char*)px + info.stride)
        //info.stride就是每一行的像素
        px = (int *)((char*)px + info.stride);
    }
    GraphicsControlBlock gcb;//获取控制信息
    DGifSavedExtensionToGCB(gifFileType,gifBean->current_frame,&gcb);
    int delay=gcb.DelayTime * 10;
    LOGE("delay %d",delay);
    return delay;
}

int drawFrame(GifFileType* gif,AndroidBitmapInfo  info,   void* pixels,  bool force_dispose_1) {
    GifColorType *bg;
    GifColorType *color;
    SavedImage * frame;
    ExtensionBlock * ext = 0;
    GifImageDesc * frameInfo;
    ColorMapObject * colorMap;

    int *line;
    int width, height,x,y,j,loc,n,inc,p;
    void* px;
    GifBean *gifBean = static_cast<GifBean *>(gif->UserData);
    width = gif->SWidth;
    height = gif->SHeight;
    frame = &(gif->SavedImages[gifBean->current_frame]);
    frameInfo = &(frame->ImageDesc);
    if (frameInfo->ColorMap) {
        colorMap = frameInfo->ColorMap;
    } else {
        colorMap = gif->SColorMap;
    }
    bg = &colorMap->Colors[gif->SBackGroundColor];

    for (j=0; j<frame->ExtensionBlockCount; j++) {
        if (frame->ExtensionBlocks[j].Function == GRAPHICS_EXT_FUNC_CODE) {
            ext = &(frame->ExtensionBlocks[j]);
            break;
        }
    }
    // For dispose = 1, we assume its been drawn
    px = pixels;
    if (ext && dispose(ext) == 1 && force_dispose_1 && gifBean->current_frame > 0) {
        gifBean->current_frame=gifBean->current_frame-1,
                drawFrame(gif , info, pixels,  true);
    }else if (ext && dispose(ext) == 2 && bg) {
        for (y=0; y<height; y++) {
            line = (int*) px;
            for (x=0; x<width; x++) {
                line[x] = argb(255, bg->Red, bg->Green, bg->Blue);
            }
            px = (int *) ((char*)px + info.stride);
        }
    } else if (ext && dispose(ext) == 3 && gifBean->current_frame > 1) {
        gifBean->current_frame=gifBean->current_frame-2,
                drawFrame(gif,  info, pixels,  true);
    }
    px = pixels;
    if (frameInfo->Interlace) {
        n = 0;
        inc = 8;
        p = 0;
        px = (int *) ((char*)px + info.stride * frameInfo->Top);
        for (y=frameInfo->Top; y<frameInfo->Top+frameInfo->Height; y++) {
            for (x=frameInfo->Left; x<frameInfo->Left+frameInfo->Width; x++) {
                loc = (y - frameInfo->Top)*frameInfo->Width + (x - frameInfo->Left);
                if (ext && frame->RasterBits[loc] == trans_index(ext) && transparency(ext)) {
                    continue;
                }
                color = (ext && frame->RasterBits[loc] == trans_index(ext)) ? bg : &colorMap->Colors[frame->RasterBits[loc]];
                if (color)
                    line[x] = argb(255, color->Red, color->Green, color->Blue);
            }
            px = (int *) ((char*)px + info.stride * inc);
            n += inc;
            if (n >= frameInfo->Height) {
                n = 0;
                switch(p) {
                    case 0:
                        px = (int *) ((char *)pixels + info.stride * (4 + frameInfo->Top));
                        inc = 8;
                        p++;
                        break;
                    case 1:
                        px = (int *) ((char *)pixels + info.stride * (2 + frameInfo->Top));
                        inc = 4;
                        p++;
                        break;
                    case 2:
                        px = (int *) ((char *)pixels + info.stride * (1 + frameInfo->Top));
                        inc = 2;
                        p++;
                }
            }
        }
    }else {
        px = (int *) ((char*)px + info.stride * frameInfo->Top);
        for (y=frameInfo->Top; y<frameInfo->Top+frameInfo->Height; y++) {
            line = (int*) px;
            for (x=frameInfo->Left; x<frameInfo->Left+frameInfo->Width; x++) {
                loc = (y - frameInfo->Top)*frameInfo->Width + (x - frameInfo->Left);
                if (ext && frame->RasterBits[loc] == trans_index(ext) && transparency(ext)) {
                    continue;
                }
                color = (ext && frame->RasterBits[loc] == trans_index(ext)) ? bg : &colorMap->Colors[frame->RasterBits[loc]];
                if (color)
                    line[x] = argb(255, color->Red, color->Green, color->Blue);
            }
            px = (int *) ((char*)px + info.stride);
        }
    }
    GraphicsControlBlock gcb;//获取控制信息
    DGifSavedExtensionToGCB(gif,gifBean->current_frame,&gcb);
    int delay=gcb.DelayTime * 10;
//    LOGE("delay %d",delay);
    return delay;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_mxkj_gifframework_GifHandler_getWidth(JNIEnv *env, jclass clazz, jlong gif_handler) {
    //指针是可以互相强转的
    GifFileType * gifFileType = reinterpret_cast<GifFileType *>(gif_handler);
    return gifFileType->SWidth;

}extern "C"
JNIEXPORT jint JNICALL
Java_com_mxkj_gifframework_GifHandler_getHeight(JNIEnv *env, jclass clazz, jlong gif_handler) {
    GifFileType * gifFileType = reinterpret_cast<GifFileType *>(gif_handler);
    return gifFileType->SHeight;

}extern "C"
JNIEXPORT jint JNICALL
Java_com_mxkj_gifframework_GifHandler_updateFrame(JNIEnv *env, jclass clazz, jlong gif_handler,
                                                  jobject bitmap) {

    GifFileType *gifFileType = reinterpret_cast<GifFileType *>(gif_handler);

    //获取java传过来的bitmap信息
    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env,bitmap,&info);
    int width = info.width;
    int height = info.height;

    //将bitmap转换为像素（二维数组），并给bitmap上锁不让其他线程（包括java线程）操作
    void *pixels;
    AndroidBitmap_lockPixels(env, bitmap, &pixels);

    //绘制
//    int delay = drawFrame(gifFileType,info,pixels, false); //完整
    int delay = drawFrame1(gifFileType,info,pixels); //主要逻辑，也可播放

    //解锁
    AndroidBitmap_unlockPixels(env,bitmap);


    //通过上面loadGif绑定了的（fileType->UserData = gifBean），指针可以相互强转
    GifBean *gifBean = static_cast<GifBean *>(gifFileType->UserData);
    //绘制一帧往后加一帧
    gifBean->current_frame++;
    //不能一直加，操作总帧数就从头开始，循环播放
    if(gifBean->current_frame >= gifBean->total_frame - 1){
        gifBean->current_frame = 0;
    }

    return delay;
}