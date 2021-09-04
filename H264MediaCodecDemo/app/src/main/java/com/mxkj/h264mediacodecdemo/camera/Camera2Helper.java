package com.mxkj.h264mediacodecdemo.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * 作者：created by 刘亮 on 2021/9/4 16:24
 */
public class Camera2Helper {
    private Context context;
    private Point previewViewSize;
    private Size mPreviewSize;
    private ImageReader mImageReader;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private CameraDevice mCameraDevice;
    private TextureView mTextureView;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CameraCaptureSession mCaptureSession;
    private Camera2Listener camera2Linstener;

    public Camera2Helper(Context context) {
        this.context = context;
        camera2Linstener = (Camera2Listener)context;
    }

    public synchronized void start(TextureView textureView) {
        mTextureView = textureView;

        //获取系统提供的摄像头管理类
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        try {

            //这个‘摄像头的配置信息,摄像头特征信息，传0 是后置摄像头
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics("0");
            //支持哪些格式
            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            //摄像头预览尺寸和textureView的尺寸不会完全一样，所以要寻找一个最合适的尺寸
            mPreviewSize = getBestSuppprtedSize(new ArrayList<Size>(Arrays.asList(map.getOutputSizes(SurfaceTexture.class))));

            //imagereader相当于一个桥梁，用于应用层和摄像头底层之间通信
            //camera2 获取的原始数据还是nv21，这里可以传YUV420是因为摄像头底层帮我们实现了nv21转yuv420
            //maxImages 传2 ，表示有2路输出，一是渲染到屏幕，二是输出保存到文件中
            mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.YUV_420_888, 2);

            //
            mBackgroundThread = new HandlerThread("CameraBackground");
            mBackgroundThread.start();
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
            //设置摄像头监听，让摄像头预览发生在子线程
            mImageReader.setOnImageAvailableListener(new OnImageAvailableListenerImpl(), mBackgroundHandler);

            //打开摄像头
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            cameraManager.openCamera("0", mDeviceStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }



    public synchronized void openCamera(){

    }

    private CameraDevice.StateCallback mDeviceStateCallback = new CameraDevice.StateCallback() {

        @Override//回调回来，代表打开摄像头成功
        public void onOpened(CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;

            //开始建立应用层和底层的会话
            createCameraPreviewSession();
        }

        @Override//摄像头断开
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override//摄像头打开错误
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };

    private void createCameraPreviewSession(){
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            //设置预览宽高
            texture.setDefaultBufferSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
            Surface surface = new Surface(texture);

            //请求预览
            mPreviewRequestBuilder =  mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //告诉摄像头底层我们要渲染到surface，第一路：预览
            mPreviewRequestBuilder.addTarget(surface);

            //自动对焦
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            //绑定后，最终才会回调到onImageAvailable，第二路：保存文件
            mPreviewRequestBuilder.addTarget(mImageReader.getSurface());

            //建立连接，目的是告诉底层有几路数据出口, 这里有两个surface就说明是2路
            mCameraDevice.createCaptureSession(Arrays.asList(surface,mImageReader.getSurface()),mCaptureStateCallback,mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.StateCallback mCaptureStateCallback = new CameraCaptureSession.StateCallback() {

        @Override//应用层和摄像头成功建立会话
        public void onConfigured(CameraCaptureSession session) {
            if (null == mCameraDevice){
                return;
            }
            mCaptureSession = session;
            try {
                //因为摄像头是一帧一帧的，所以重复请求会话
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {},mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }
    };

    private class OnImageAvailableListenerImpl implements ImageReader.OnImageAvailableListener{
        private byte[] y;
        private byte[] u;
        private byte[] v;

        //摄像头回调 应用层，类似camera1的 onPreviewFrame()
        @Override
        public void onImageAvailable(ImageReader reader) {
            //回调回来的数据是一帧一帧的，要获取到后消费，并close，才能进行下一帧处理，没有close的话预览一帧就会阻塞
            Image image = reader.acquireNextImage();

            //获取图像数组
            Image.Plane[] planes = image.getPlanes();
            if (y==null){
                //limit()是缓冲区所有的大小，position()是起始大小，相减就得到对应数据大小
                y = new byte[planes[0].getBuffer().limit() - planes[0].getBuffer().position()];
                u = new byte[planes[1].getBuffer().limit() - planes[1].getBuffer().position()];
                v = new byte[planes[2].getBuffer().limit() - planes[2].getBuffer().position()];
            }

            if (image.getPlanes()[0].getBuffer().remaining() == y.length){
                // 往yuv里放数据
                planes[0].getBuffer().get(y);
                planes[1].getBuffer().get(u);
                planes[2].getBuffer().get(v);
            }

            if (camera2Linstener != null){
                camera2Linstener.onPreview(y,u,v,mPreviewSize,planes[0].getRowStride());
            }


            image.close();
        }
    }


    public interface Camera2Listener{

        /**
         * 预览数据回调
         * @param y 预览数据，Y分量
         * @param u 预览数据，U分量
         * @param v 预览数据，V分量
         * @param previewSize 预览尺寸
         * @param stride 步长
         *
         * */
        void onPreview(byte[] y, byte[] u, byte[] v, Size previewSize, int stride);
    }



    /**
     * 获取最合适的尺寸
     * */
    private Size getBestSuppprtedSize(ArrayList<Size> sizes) {
        Point maxPreviewSize = new Point(1920, 1080);
        Point minPreviewSize = new Point(1280, 720);
        Size defaultSize = sizes.get(0);
        Size[] tempSizes = sizes.toArray(new Size[0]);
        Arrays.sort(tempSizes, new Comparator<Size>() {
            @Override
            public int compare(Size o1, Size o2) {
                if (o1.getWidth() > o2.getWidth()){
                    return -1;
                }else if(o1.getWidth() == o2.getWidth()){
                    return o1.getHeight() > o2.getHeight() ? -1 : 1;
                }else {
                    return 1;
                }
            }
        });
        sizes = new ArrayList<>(Arrays.asList(tempSizes));
        for (int i = sizes.size() -1 ; i>=0; i--){
            if (maxPreviewSize != null){
                if (sizes.get(i).getWidth() > maxPreviewSize.x || sizes.get(i).getHeight() > maxPreviewSize.y){
                    sizes.remove(i);
                    continue;
                }
            }
            if (minPreviewSize != null){
                if (sizes.get(i).getWidth() < minPreviewSize.x || sizes.get(i).getHeight() < minPreviewSize.y){
                    sizes.remove(i);
                }
            }
        }
        if (sizes.size() == 0){
            return defaultSize;
        }
        Size bestSize = sizes.get(0);
        float previewViewRatio;
        if (previewViewSize != null){
            previewViewRatio = (float)previewViewSize.x / (float)previewViewSize.y;
        }else{
            previewViewRatio = (float)bestSize.getWidth() / (float)bestSize.getHeight();
        }
        if (previewViewRatio > 1){
            previewViewRatio = 1 / previewViewRatio;
        }
        for (Size s : sizes){
            if (Math.abs((s.getHeight() / (float)s.getWidth()) - previewViewRatio) < Math.abs(bestSize.getHeight() / (float)bestSize.getWidth())){
                bestSize = s;
            }
        }
        return bestSize;
    }

}
