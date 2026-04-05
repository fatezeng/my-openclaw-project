package com.example.shunxing;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * IMU传感器管理器
 * 负责获取和处理IMU传感器数据，用于校正姿态估计
 */
public class IMUSensorManager implements SensorEventListener {
    private static final String TAG = "IMUSensorManager";
    private static IMUSensorManager instance;
    
    // 传感器管理器
    private SensorManager sensorManager;
    // 加速度传感器
    private Sensor accelerometer;
    // 陀螺仪传感器
    private Sensor gyroscope;
    // 磁力计传感器
    private Sensor magnetometer;
    
    // 传感器数据
    private float[] accelerometerData = new float[3];
    private float[] gyroscopeData = new float[3];
    private float[] magnetometerData = new float[3];
    
    // 校准标志
    private boolean isCalibrated = false;
    // 校准偏移值
    private float[] calibrationOffsets = new float[3];
    
    /**
     * 获取单例实例
     * @return IMUSensorManager 实例
     */
    public static synchronized IMUSensorManager getInstance() {
        if (instance == null) {
            instance = new IMUSensorManager();
        }
        return instance;
    }
    
    /**
     * 初始化传感器
     * @param context 上下文
     */
    public void initialize(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        
        if (sensorManager != null) {
            // 获取加速度传感器
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            // 获取陀螺仪传感器
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            // 获取磁力计传感器
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            
            // 注册传感器监听器
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
            if (gyroscope != null) {
                sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
            }
            if (magnetometer != null) {
                sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
            
            Log.i(TAG, "IMU sensors initialized");
        } else {
            Log.e(TAG, "SensorManager not available");
        }
    }
    
    /**
     * 校准传感器
     */
    public void calibrate() {
        // 这里使用简单的校准方法
        // 实际应用中可能需要更复杂的校准算法
        calibrationOffsets[0] = accelerometerData[0];
        calibrationOffsets[1] = accelerometerData[1];
        calibrationOffsets[2] = accelerometerData[2] - 9.81f; // 减去重力加速度
        
        isCalibrated = true;
        Log.i(TAG, "IMU sensors calibrated");
    }
    
    /**
     * 获取校正后的加速度数据
     * @return 校正后的加速度数据
     */
    public float[] getCalibratedAccelerometerData() {
        if (!isCalibrated) {
            calibrate();
        }
        
        float[] calibratedData = new float[3];
        calibratedData[0] = accelerometerData[0] - calibrationOffsets[0];
        calibratedData[1] = accelerometerData[1] - calibrationOffsets[1];
        calibratedData[2] = accelerometerData[2] - calibrationOffsets[2];
        
        return calibratedData;
    }
    
    /**
     * 获取陀螺仪数据
     * @return 陀螺仪数据
     */
    public float[] getGyroscopeData() {
        return gyroscopeData;
    }
    
    /**
     * 获取磁力计数据
     * @return 磁力计数据
     */
    public float[] getMagnetometerData() {
        return magnetometerData;
    }
    
    /**
     * 校正姿态估计
     * @param waistSize 原始腰围估计值
     * @param waistHeight 原始腰高估计值
     * @param waistCurvature 原始腰部弧度估计值
     * @return 校正后的测量结果数组 [腰围, 腰高, 腰部弧度]
     */
    public float[] correctPoseEstimation(float waistSize, float waistHeight, float waistCurvature) {
        float[] correctedResults = new float[3];
        
        try {
            // 获取校正后的加速度数据
            float[] accData = getCalibratedAccelerometerData();
            
            // 计算加速度的 magnitude
            float accMagnitude = (float) Math.sqrt(
                    accData[0] * accData[0] + 
                    accData[1] * accData[1] + 
                    accData[2] * accData[2]
            );
            
            // 如果加速度 magnitude 过大，说明有抖动
            if (accMagnitude > 2.0f) { // 阈值可调整
                // 使用平滑算法校正
                correctedResults[0] = waistSize * 0.9f + correctedResults[0] * 0.1f; // 腰围
                correctedResults[1] = waistHeight * 0.9f + correctedResults[1] * 0.1f; // 腰高
                correctedResults[2] = waistCurvature * 0.9f + correctedResults[2] * 0.1f; // 腰部弧度
            } else {
                // 没有抖动，直接使用原始值
                correctedResults[0] = waistSize;
                correctedResults[1] = waistHeight;
                correctedResults[2] = waistCurvature;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error correcting pose estimation: " + e.getMessage());
            // 出错时使用原始值
            correctedResults[0] = waistSize;
            correctedResults[1] = waistHeight;
            correctedResults[2] = waistCurvature;
        }
        
        return correctedResults;
    }
    
    /**
     * 释放传感器资源
     */
    public void release() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        Log.i(TAG, "IMU sensors released");
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, accelerometerData, 0, 3);
                break;
            case Sensor.TYPE_GYROSCOPE:
                System.arraycopy(event.values, 0, gyroscopeData, 0, 3);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, magnetometerData, 0, 3);
                break;
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 传感器精度变化时的处理
        Log.i(TAG, "Sensor accuracy changed: " + sensor.getType() + " - " + accuracy);
    }
}
