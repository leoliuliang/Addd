package com.mxkj.gifframework;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.mxkj.gifframework.utils.AssetsUtil;
import com.mxkj.gifframework.utils.PermissionUtil;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    ImageView imageView;
    String path;
    GifHandler gifHandler;
    Bitmap bitmap;

    @SuppressLint("HandlerLeak")
    Handler myHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            //在handler里不断的循环渲染，就可以达到播放效果
            int delay = gifHandler.updateFrame(bitmap);
            imageView.setImageBitmap(bitmap);
            myHandler.sendEmptyMessageDelayed(1,delay);
//            Log.i("----->","delay: "+delay+  "  bitmap: "+bitmap.getRowBytes());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.imageview);
        PermissionUtil.checkPermission(this);
        path = new AssetsUtil(this).assetsToSdcard("demogif.gif");

    }

    public void btnLoad(View view) {
        gifHandler = GifHandler.load(path);
        int width = gifHandler.getWidth();
        int height = gifHandler.getHeight();
        Log.i("----->","width: "+width +"  height:  "+height);
        //拿到宽高后就可以创建bitmap了
        bitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        //需要通知 C 渲染完成第一帧, 接着通过handle继续通知后面渲染的帧
        int delay = gifHandler.updateFrame(bitmap);
        imageView.setImageBitmap(bitmap);
        myHandler.sendEmptyMessageDelayed(1,delay);
    }
}
