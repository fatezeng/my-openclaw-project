package com.example.shunxing;

import android.app.Application;
import android.util.Log;

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";
    private static final String KIRI_APP_ID = "Qwe013794532694";
    private static final String KIRI_API_KEY = "kiri__btwBLwQIar0UZCzaXM0eFmVXeMvKdZ_6XS8b35h2kA";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // 创建通知通道
        NotificationHelper.createNotificationChannel(this);
        
        // 初始化Kiri Engine SDK（暂时注释掉，网络连接问题）
        /*try {
            com.kiriengine.KiriEngine.init(this, KIRI_APP_ID, KIRI_API_KEY);
            Log.d(TAG, "Kiri Engine SDK initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Kiri Engine SDK: " + e.getMessage());
        }*/
    }
}