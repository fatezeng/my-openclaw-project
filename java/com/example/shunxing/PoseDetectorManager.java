package com.example.shunxing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.core.Delegate;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import java.util.List;

/**
 * MediaPipe Pose管理器
 * 负责加载模型和执行姿势检测
 */
public class PoseDetectorManager {

    private static final String TAG = "PoseDetectorManager";
    private static final String MODEL_ASSET_PATH = "pose_landmarker_full.task";

    private static PoseDetectorManager instance;
    private PoseLandmarker poseLandmarker;
    private boolean isInitialized = false;

    /**
     * 获取单例实例
     * @return PoseDetectorManager实例
     */
    public static synchronized PoseDetectorManager getInstance() {
        if (instance == null) {
            instance = new PoseDetectorManager();
        }
        return instance;
    }

    /**
     * 初始化姿势检测器
     * @param context 上下文
     * @param callback 初始化回调
     */
    public void initialize(Context context, final PoseDetectorCallback callback) {
        if (isInitialized) {
            if (callback != null) {
                callback.onSuccess();
            }
            return;
        }

        try {
            // 配置BaseOptions
            BaseOptions baseOptions = BaseOptions.builder()
                    .setModelAssetPath(MODEL_ASSET_PATH)
                    .setDelegate(Delegate.CPU)
                    .build();

            // 配置PoseLandmarker选项
            PoseLandmarker.PoseLandmarkerOptions options = PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(RunningMode.IMAGE)
                    .setNumPoses(1)
                    .setMinPoseDetectionConfidence(0.5f)
                    .setMinPosePresenceConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .build();

            // 创建PoseLandmarker
            poseLandmarker = PoseLandmarker.createFromOptions(context, options);
            isInitialized = true;

            Log.i(TAG, "MediaPipe Pose initialized successfully");
            if (callback != null) {
                callback.onSuccess();
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize MediaPipe Pose: " + e.getMessage());
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }

    /**
     * 检测图像中的人体姿势
     * @param bitmap 输入图像
     * @return 姿势检测结果
     */
    public PoseLandmarkerResult detectPose(Bitmap bitmap) {
        if (!isInitialized || poseLandmarker == null) {
            Log.e(TAG, "Pose detector not initialized");
            return null;
        }

        try {
            // 创建MPImage
            MPImage image = new BitmapImageBuilder(bitmap).build();

            // 执行姿势检测
            PoseLandmarkerResult result = poseLandmarker.detect(image);
            
            Log.i(TAG, "Pose detection completed");
            return result;

        } catch (Exception e) {
            Log.e(TAG, "Error detecting pose: " + e.getMessage());
            return null;
        }
    }

    /**
     * 检测旋转后的图像中的人体姿势
     * @param bitmap 输入图像
     * @param rotationDegrees 旋转角度
     * @return 姿势检测结果
     */
    public PoseLandmarkerResult detectPose(Bitmap bitmap, int rotationDegrees) {
        if (!isInitialized || poseLandmarker == null) {
            Log.e(TAG, "Pose detector not initialized");
            return null;
        }

        try {
            // 旋转图像以匹配相机方向
            Bitmap rotatedBitmap = rotateBitmap(bitmap, rotationDegrees);

            // 创建MPImage
            MPImage image = new BitmapImageBuilder(rotatedBitmap).build();

            // 执行姿势检测
            PoseLandmarkerResult result = poseLandmarker.detect(image);
            
            Log.i(TAG, "Pose detection completed");
            return result;

        } catch (Exception e) {
            Log.e(TAG, "Error detecting pose: " + e.getMessage());
            return null;
        }
    }

    /**
     * 旋转Bitmap图像
     * @param bitmap 原始图像
     * @param rotationDegrees 旋转角度
     * @return 旋转后的图像
     */
    private Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees) {
        if (rotationDegrees == 0) {
            return bitmap;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /**
     * 释放资源
     */
    public void close() {
        if (poseLandmarker != null) {
            poseLandmarker.close();
            poseLandmarker = null;
        }
        isInitialized = false;
        Log.i(TAG, "Pose detector closed");
    }

    /**
     * 检查是否已初始化
     * @return 是否已初始化
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * 姿势检测回调接口
     */
    public interface PoseDetectorCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
}