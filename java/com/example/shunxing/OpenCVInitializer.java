package com.example.shunxing;

import android.content.Context;
import android.util.Log;

import org.opencv.android.OpenCVLoader;

/**
 * OpenCV初始化工具类
 * 负责加载和初始化OpenCV库
 */
public class OpenCVInitializer {

    private static final String TAG = "OpenCVInitializer";
    private static boolean isInitialized = false;

    /**
     * 初始化OpenCV库
     * @param context 上下文
     * @param callback 初始化回调
     */
    public static void initialize(Context context, final OpenCVInitCallback callback) {
        if (isInitialized) {
            if (callback != null) {
                callback.onSuccess();
            }
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 加载OpenCV库
                    boolean success = OpenCVLoader.initLocal();
                    if (success) {
                        Log.i(TAG, "OpenCV loaded successfully");
                        isInitialized = true;
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    } else {
                        Log.e(TAG, "OpenCV loading failed: initLocal() returned false");
                        if (callback != null) {
                            callback.onFailure();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "OpenCV loading failed: " + e.getMessage());
                    if (callback != null) {
                        callback.onFailure();
                    }
                }
            }
        }).start();
    }

    /**
     * 检查OpenCV是否已初始化
     * @return 是否已初始化
     */
    public static boolean isInitialized() {
        return isInitialized;
    }

    /**
     * OpenCV初始化回调接口
     */
    public interface OpenCVInitCallback {
        void onSuccess();
        void onFailure();
    }
}
