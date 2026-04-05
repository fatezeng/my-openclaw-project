package com.example.shunxing.pose;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.shunxing.BuildConfig;

import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.framework.image.BitmapImageBuilder;

import java.io.IOException;

/**
 * 姿势检测器，封装 MediaPipe 姿势检测功能
 */
public class PoseDetector {
    private static final String TAG = "PoseDetector";
    
    private Context context;
    private PoseLandmarker poseLandmarker;
    private boolean isInitialized = false;
    
    /**
     * 构造函数
     * @param context 上下文
     */
    public PoseDetector(Context context) {
        this.context = context;
    }
    
    /**
     * 初始化姿势检测器
     * @param callback 初始化回调
     */
    public void initialize(InitializeCallback callback) {
        try {
            // 检查模型文件是否存在
            boolean modelExists = checkModelFileExists("pose_landmarker_full.task");
            if (!modelExists) {
                throw new Exception("Model file not found: pose_landmarker_full.task");
            }
            
            // 创建 BaseOptions
            BaseOptions baseOptions = BaseOptions.builder()
                    .setModelAssetPath("pose_landmarker_full.task")
                    .build();
            
            // 创建 PoseLandmarkerOptions
            PoseLandmarker.PoseLandmarkerOptions options = PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(RunningMode.IMAGE)
                    .setNumPoses(1)
                    .setMinPoseDetectionConfidence(0.5f)
                    .setMinPosePresenceConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .build();
            
            // 创建 PoseLandmarker
            poseLandmarker = PoseLandmarker.createFromOptions(context, options);
            isInitialized = true;
            
            if (callback != null) {
                callback.onSuccess();
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error initializing pose detector: " + e.getMessage());
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }
    
    /**
     * 检查模型文件是否存在
     * @param modelPath 模型文件路径
     * @return 是否存在
     */
    private boolean checkModelFileExists(String modelPath) {
        try {
            // 检查 assets 目录中是否存在模型文件
            android.content.res.AssetManager assetManager = context.getAssets();
            String[] files = assetManager.list("");
            if (files != null) {
                for (String file : files) {
                    if (file.equals(modelPath)) {
                        return true;
                    }
                }
            }
            if (BuildConfig.DEBUG) Log.w(TAG, "Model file not found in assets: " + modelPath);
            return false;
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error checking model file existence: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检测姿势
     * @param bitmap 输入图片
     * @param rotationDegrees 旋转角度
     * @return 姿势检测结果
     */
    public PoseLandmarkerResult detectPose(Bitmap bitmap, int rotationDegrees) {
        if (!isInitialized || poseLandmarker == null) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Pose detector not initialized");
            return null;
        }
        
        try {
            // 根据旋转角度旋转 Bitmap
            Bitmap rotatedBitmap = bitmap;
            if (rotationDegrees != 0) {
                android.graphics.Matrix matrix = new android.graphics.Matrix();
                matrix.postRotate(rotationDegrees);
                rotatedBitmap = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
            
            // 创建 MPImage
            MPImage mpImage = new BitmapImageBuilder(rotatedBitmap).build();
            
            // 检测姿势
            return poseLandmarker.detect(mpImage);
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error detecting pose: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 释放资源
     */
    public void close() {
        try {
            if (poseLandmarker != null) {
                poseLandmarker.close();
                poseLandmarker = null;
                if (BuildConfig.DEBUG) Log.d(TAG, "Pose landmarker closed");
            }
            isInitialized = false;
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error closing pose detector: " + e.getMessage());
        }
    }
    
    /**
     * 计算腰部尺寸
     * @param result 姿势检测结果
     * @param width 图像宽度
     * @param height 图像高度
     * @param focalLengthPixels 焦距像素值
     * @param estimatedDistance 估计距离（米）
     * @return 腰部尺寸（厘米）
     */
    public float calculateWaistSize(PoseLandmarkerResult result, int width, int height, float focalLengthPixels, float estimatedDistance) {
        try {
            if (result == null || result.landmarks().isEmpty()) {
                if (BuildConfig.DEBUG) Log.w(TAG, "No landmarks detected for waist size calculation");
                return 0f;
            }
            
            // 获取第一个人的关键点
            java.util.List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark> landmarks = result.landmarks().get(0);
            
            // 检查是否有足够的关键点
            if (landmarks.size() < 25) {
                if (BuildConfig.DEBUG) Log.w(TAG, "Not enough landmarks for waist size calculation: " + landmarks.size());
                return 0f;
            }
            
            // 提取腰部关键点（左髋和右髋）
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark leftHip = landmarks.get(23);
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark rightHip = landmarks.get(24);
            
            // 计算髋部宽度（像素）
            float leftHipX = leftHip.x() * width;
            float rightHipX = rightHip.x() * width;
            float hipWidthPixels = Math.abs(rightHipX - leftHipX);
            
            // 已知骨盆实际宽度（米）
            final float PELVIS_ACTUAL_WIDTH = 0.28f;
            
            if (hipWidthPixels > 0 && focalLengthPixels > 0 && estimatedDistance > 0) {
                // 根据髋部像素宽度和实际距离估算髋部实际宽度（米）
                float hipActualWidth = (hipWidthPixels * estimatedDistance) / focalLengthPixels;
                // 假设腰围 = 髋部实际宽度 × 1.2（经验比例）
                float waistActualWidth = hipActualWidth * 1.2f;
                return waistActualWidth * 100; // 转厘米
            } else {
                // 若距离缺失，回退到固定比例（仅用于相册图片等场景）
                if (BuildConfig.DEBUG) Log.w(TAG, "Cannot calculate accurate waist size, using fallback method");
                return (PELVIS_ACTUAL_WIDTH * 1.2f) * 100;
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error calculating waist size: " + e.getMessage());
            return 0f;
        }
    }
    
    /**
     * 计算腰部尺寸（旧方法，兼容调用）
     * @param result 姿势检测结果
     * @param width 图像宽度
     * @param height 图像高度
     * @param focalLengthPixels 焦距像素值
     * @return 腰部尺寸（厘米）
     * @deprecated 请使用带 estimatedDistance 参数的版本
     */
    @Deprecated
    public float calculateWaistSize(PoseLandmarkerResult result, int width, int height, float focalLengthPixels) {
        return calculateWaistSize(result, width, height, focalLengthPixels, 1.5f); // 默认1.5米
    }
    
    /**
     * 计算腰部高度
     * @param result 姿势检测结果
     * @param width 图像宽度
     * @param height 图像高度
     * @param focalLengthPixels 焦距像素值
     * @param estimatedDistance 估计距离（米）
     * @return 腰部高度（厘米）
     */
    public float calculateWaistHeight(PoseLandmarkerResult result, int width, int height, float focalLengthPixels, float estimatedDistance) {
        try {
            if (result == null || result.landmarks().isEmpty()) {
                if (BuildConfig.DEBUG) Log.w(TAG, "No landmarks detected for waist height calculation");
                return 0f;
            }
            
            // 获取第一个人的关键点
            java.util.List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark> landmarks = result.landmarks().get(0);
            
            // 检查是否有足够的关键点
            if (landmarks.size() < 25) {
                if (BuildConfig.DEBUG) Log.w(TAG, "Not enough landmarks for waist height calculation: " + landmarks.size());
                return 0f;
            }
            
            // 提取腰部关键点（左髋）
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark leftHip = landmarks.get(23);
            
            // 计算腰部高度（像素）
            float hipY = leftHip.y() * height;
            
            // 使用公式：实际高度（米） = （像素高度 × 实际距离） / 焦距（像素）
            if (focalLengthPixels > 0) {
                float safeDistance = estimatedDistance > 0 ? estimatedDistance : 1.5f; // 默认1.5米
                float heightMeters = (hipY * safeDistance) / focalLengthPixels;
                // 转换为厘米
                return heightMeters * 100;
            } else {
                if (BuildConfig.DEBUG) Log.w(TAG, "Cannot calculate waist height: focalLengthPixels=" + focalLengthPixels);
                return 0f;
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error calculating waist height: " + e.getMessage());
            return 0f;
        }
    }
    
    /**
     * 计算腰部弧度
     * @param result 姿势检测结果
     * @param width 图像宽度
     * @param height 图像高度
     * @return 腰部弧度（度）
     */
    public float calculateWaistCurvature(PoseLandmarkerResult result, int width, int height) {
        try {
            if (result == null || result.landmarks().isEmpty()) {
                if (BuildConfig.DEBUG) Log.w(TAG, "No landmarks detected for waist curvature calculation");
                return 0f;
            }
            
            // 获取第一个人的关键点
            java.util.List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark> landmarks = result.landmarks().get(0);
            
            // 检查是否有足够的关键点
            if (landmarks.size() < 25) {
                if (BuildConfig.DEBUG) Log.w(TAG, "Not enough landmarks for waist curvature calculation: " + landmarks.size());
                return 0f;
            }
            
            // 计算左侧角度
            float leftAngle = calculateSideAngle(landmarks, width, height, 11, 23, 25); // 左肩、左髋、左膝
            
            // 计算右侧角度
            float rightAngle = calculateSideAngle(landmarks, width, height, 12, 24, 26); // 右肩、右髋、右膝
            
            // 计算平均角度
            return (leftAngle + rightAngle) / 2f;
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error calculating waist curvature: " + e.getMessage());
            return 0f;
        }
    }
    
    /**
     * 计算一侧的角度
     * @param landmarks 关键点列表
     * @param width 图像宽度
     * @param height 图像高度
     * @param shoulderIndex 肩膀索引
     * @param hipIndex 髋部索引
     * @param kneeIndex 膝盖索引
     * @return 角度（度）
     */
    private float calculateSideAngle(java.util.List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark> landmarks, int width, int height, int shoulderIndex, int hipIndex, int kneeIndex) {
        try {
            // 提取相关关键点
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark shoulder = landmarks.get(shoulderIndex);
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark hip = landmarks.get(hipIndex);
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark knee = landmarks.get(kneeIndex);
            
            // 计算三点之间的角度
            float shoulderX = shoulder.x() * width;
            float shoulderY = shoulder.y() * height;
            float hipX = hip.x() * width;
            float hipY = hip.y() * height;
            float kneeX = knee.x() * width;
            float kneeY = knee.y() * height;
            
            // 计算向量
            float v1x = shoulderX - hipX;
            float v1y = shoulderY - hipY;
            float v2x = kneeX - hipX;
            float v2y = kneeY - hipY;
            
            // 计算角度
            double dotProduct = v1x * v2x + v1y * v2y;
            double magnitude1 = Math.sqrt(v1x * v1x + v1y * v1y);
            double magnitude2 = Math.sqrt(v2x * v2x + v2y * v2y);
            
            if (magnitude1 == 0 || magnitude2 == 0) {
                return 0f;
            }
            
            double cosine = dotProduct / (magnitude1 * magnitude2);
            cosine = Math.max(-1.0, Math.min(1.0, cosine)); // 确保在有效范围内
            double angle = Math.acos(cosine);
            
            // 转换为度
            return (float) Math.toDegrees(angle);
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error calculating side angle: " + e.getMessage());
            return 0f;
        }
    }
    
    /**
     * 初始化回调
     */
    public interface InitializeCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
}