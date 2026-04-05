package com.example.shunxing;

import android.util.Log;

import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import java.util.List;

/**
 * 腰部尺寸计算工具类
 * 负责基于MediaPipe Pose关键点计算腰部尺寸
 */
public class WaistMeasurementCalculator {

    private static final String TAG = "WaistMeasurementCalculator";
    
    // MediaPipe Pose关键点索引
    private static final int LEFT_HIP = 23;
    private static final int RIGHT_HIP = 24;
    private static final int LEFT_SHOULDER = 11;
    private static final int RIGHT_SHOULDER = 12;
    private static final int LEFT_ELBOW = 13;
    private static final int RIGHT_ELBOW = 14;
    private static final int LEFT_WRIST = 15;
    private static final int RIGHT_WRIST = 16;
    private static final int LEFT_KNEE = 25;
    private static final int RIGHT_KNEE = 26;
    
    // 单例实例
    private static WaistMeasurementCalculator instance;
    
    // 身高（用于比例计算）
    private float userHeight = 170.0f; // 默认值，单位：cm
    
    /**
     * 获取单例实例
     * @return WaistMeasurementCalculator实例
     */
    public static synchronized WaistMeasurementCalculator getInstance() {
        if (instance == null) {
            instance = new WaistMeasurementCalculator();
        }
        return instance;
    }
    
    /**
     * 设置用户身高
     * @param height 身高，单位：cm
     */
    public void setUserHeight(float height) {
        this.userHeight = height;
    }
    
    /**
     * 计算腰部尺寸
     * @param result 姿势检测结果
     * @param imageWidth 图像宽度
     * @param imageHeight 图像高度
     * @return 腰部尺寸，单位：cm
     */
    public float calculateWaistSize(PoseLandmarkerResult result, int imageWidth, int imageHeight) {
        if (result == null || result.landmarks().isEmpty()) {
            Log.e(TAG, "No pose landmarks found");
            return 0.0f;
        }
        
        try {
            // 计算髋部宽度
            float hipWidth = calculateHipWidth(result, imageWidth, imageHeight);
            
            // 计算肩宽
            float shoulderWidth = calculateShoulderWidth(result, imageWidth, imageHeight);
            
            Log.d(TAG, "Hip width: " + hipWidth + " cm, Shoulder width: " + shoulderWidth + " cm");
            
            // 估算腰部尺寸
            float waistSize = estimateWaistSize(hipWidth, shoulderWidth);
            
            // 如果计算结果为0，使用默认值
            if (waistSize <= 0) {
                Log.d(TAG, "Calculated waist size is zero, using default value");
                waistSize = 70.0f; // 默认腰围值
            }
            
            Log.i(TAG, "Calculated waist size: " + waistSize + " cm");
            return waistSize;
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating waist size: " + e.getMessage());
            return 70.0f; // 异常时返回默认值
        }
    }
    
    /**
     * 计算髋部宽度
     * @param result 姿势检测结果
     * @param imageWidth 图像宽度
     * @param imageHeight 图像高度
     * @return 髋部宽度，单位：cm
     */
    private float calculateHipWidth(PoseLandmarkerResult result, int imageWidth, int imageHeight) {
        try {
            // 获取左右髋部关键点坐标
            float leftHipX = getLandmarkCoordinate(result, LEFT_HIP, 0) * imageWidth;
            float rightHipX = getLandmarkCoordinate(result, RIGHT_HIP, 0) * imageWidth;
            
            // 计算髋部宽度（像素）
            float hipWidthPixels = Math.abs(rightHipX - leftHipX);
            
            // 转换为实际距离（cm）
            return convertPixelToCm(hipWidthPixels, imageWidth);
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating hip width: " + e.getMessage());
            return 0.0f;
        }
    }
    
    /**
     * 计算肩宽
     * @param result 姿势检测结果
     * @param imageWidth 图像宽度
     * @param imageHeight 图像高度
     * @return 肩宽，单位：cm
     */
    private float calculateShoulderWidth(PoseLandmarkerResult result, int imageWidth, int imageHeight) {
        try {
            // 获取左右肩膀关键点坐标
            float leftShoulderX = getLandmarkCoordinate(result, LEFT_SHOULDER, 0) * imageWidth;
            float rightShoulderX = getLandmarkCoordinate(result, RIGHT_SHOULDER, 0) * imageWidth;
            
            // 计算肩宽（像素）
            float shoulderWidthPixels = Math.abs(rightShoulderX - leftShoulderX);
            
            // 转换为实际距离（cm）
            return convertPixelToCm(shoulderWidthPixels, imageWidth);
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating shoulder width: " + e.getMessage());
            return 0.0f;
        }
    }
    
    /**
     * 将像素距离转换为实际距离（cm）
     * @param pixelDistance 像素距离
     * @param imageWidth 图像宽度
     * @return 实际距离，单位：cm
     */
    private float convertPixelToCm(float pixelDistance, int imageWidth) {
        // 这里使用简化的转换方法
        // 实际应用中需要考虑相机焦距、拍摄距离等因素
        
        // 假设标准身高的人在图像中占一定比例
        // 标准肩宽约为身高的1/4
        float estimatedShoulderWidth = userHeight * 0.25f;
        
        // 假设肩宽在图像中占一定比例的像素
        // 这里使用经验值，实际应用中需要校准
        float pixelToCmRatio = estimatedShoulderWidth / (imageWidth * 0.4f);
        
        return pixelDistance * pixelToCmRatio;
    }
    
    /**
     * 估算腰部尺寸
     * @param hipWidth 髋部宽度，单位：cm
     * @param shoulderWidth 肩宽，单位：cm
     * @return 腰部尺寸，单位：cm
     */
    private float estimateWaistSize(float hipWidth, float shoulderWidth) {
        // 基于人体比例估算腰部尺寸
        // 一般来说，腰部尺寸与髋部宽度和肩宽有关
        
        // 方法1：基于髋部宽度的比例
        float waistSize1 = hipWidth * 0.85f; // 假设腰部比髋部窄15%
        
        // 方法2：基于肩宽的比例
        float waistSize2 = shoulderWidth * 0.75f; // 假设腰部比肩宽窄25%
        
        // 方法3：平均值
        float waistSize3 = (waistSize1 + waistSize2) / 2.0f;
        
        // 选择合适的估算方法
        // 这里使用方法3作为最终结果
        return waistSize3;
    }
    
    /**
     * 计算腰部高度
     * @param result 姿势检测结果
     * @param imageWidth 图像宽度
     * @param imageHeight 图像高度
     * @return 腰部高度，单位：cm
     */
    public float calculateWaistHeight(PoseLandmarkerResult result, int imageWidth, int imageHeight) {
        if (result == null || result.landmarks().isEmpty()) {
            Log.e(TAG, "No pose landmarks found");
            return 0.0f;
        }
        
        try {
            // 获取髋关节和膝关节的y坐标
            float hipY = (getLandmarkCoordinate(result, LEFT_HIP, 1) + getLandmarkCoordinate(result, RIGHT_HIP, 1)) / 2.0f * imageHeight;
            float kneeY = (getLandmarkCoordinate(result, LEFT_KNEE, 1) + getLandmarkCoordinate(result, RIGHT_KNEE, 1)) / 2.0f * imageHeight;
            
            // 计算腰部高度（像素）
            float heightPixels = Math.abs(kneeY - hipY);
            
            // 转换为实际距离（cm）
            return convertPixelToCm(heightPixels, imageWidth);
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating waist height: " + e.getMessage());
            return 0.0f;
        }
    }
    
    /**
     * 计算腰部弧度
     * @param result 姿势检测结果
     * @param imageWidth 图像宽度
     * @param imageHeight 图像高度
     * @return 腰部弧度，单位：度
     */
    public float calculateWaistCurvature(PoseLandmarkerResult result, int imageWidth, int imageHeight) {
        if (result == null || result.landmarks().isEmpty()) {
            Log.e(TAG, "No pose landmarks found");
            return 0.0f;
        }
        
        try {
            // 获取肩膀、臀部和膝盖的y坐标
            float shoulderY = (getLandmarkCoordinate(result, LEFT_SHOULDER, 1) + getLandmarkCoordinate(result, RIGHT_SHOULDER, 1)) / 2.0f;
            float hipY = (getLandmarkCoordinate(result, LEFT_HIP, 1) + getLandmarkCoordinate(result, RIGHT_HIP, 1)) / 2.0f;
            float kneeY = (getLandmarkCoordinate(result, LEFT_KNEE, 1) + getLandmarkCoordinate(result, RIGHT_KNEE, 1)) / 2.0f;
            
            // 计算腰部弧度
            // 使用简单的方法：计算肩膀-臀部-膝盖形成的角度
            float curvature = calculateAngle(shoulderY, hipY, kneeY);
            
            return curvature;
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating waist curvature: " + e.getMessage());
            return 0.0f;
        }
    }
    
    /**
     * 计算三点形成的角度
     * @param y1 第一个点的y坐标
     * @param y2 第二个点的y坐标
     * @param y3 第三个点的y坐标
     * @return 角度，单位：度
     */
    private float calculateAngle(float y1, float y2, float y3) {
        // 计算向量
        float vector1 = y2 - y1;
        float vector2 = y3 - y2;
        
        // 计算角度
        float angle = (float) Math.toDegrees(Math.atan2(vector2, 1) - Math.atan2(vector1, 1));
        
        // 确保角度为正值
        if (angle < 0) {
            angle += 180;
        }
        
        return angle;
    }
    
    /**
     * 从MediaPipe结果中获取关键点坐标
     * @param result 姿势检测结果
     * @param landmarkIndex 关键点索引
     * @param dimension 维度（0: x, 1: y, 2: z）
     * @return 关键点坐标值
     */
    private float getLandmarkCoordinate(PoseLandmarkerResult result, int landmarkIndex, int dimension) {
        try {
            List<NormalizedLandmark> landmarks = result.landmarks().get(0);
            NormalizedLandmark landmark = landmarks.get(landmarkIndex);
            
            switch (dimension) {
                case 0:
                    return landmark.x();
                case 1:
                    return landmark.y();
                case 2:
                    return landmark.z();
                default:
                    return 0.0f;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting landmark coordinate: " + e.getMessage());
            return 0.0f;
        }
    }
}