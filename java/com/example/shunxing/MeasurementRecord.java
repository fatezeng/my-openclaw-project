package com.example.shunxing;

import java.io.Serializable;
import java.util.Date;

/**
 * 测量记录类
 * 用于存储腰部测量结果
 */
public class MeasurementRecord implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private int id; // 记录ID
    private float waistSize; // 腰围，单位：cm
    private float waistHeight; // 腰部高度，单位：cm
    private float waistCurvature; // 腰部弧度，单位：度
    private Date timestamp; // 测量时间
    private String imagePath; // 测量时的照片路径（可选）
    
    /**
     * 构造方法
     * @param id 记录ID
     * @param waistSize 腰围，单位：cm
     * @param waistHeight 腰部高度，单位：cm
     * @param waistCurvature 腰部弧度，单位：度
     */
    public MeasurementRecord(int id, float waistSize, float waistHeight, float waistCurvature) {
        this.id = id;
        this.waistSize = waistSize;
        this.waistHeight = waistHeight;
        this.waistCurvature = waistCurvature;
        this.timestamp = new Date();
        this.imagePath = null;
    }
    
    /**
     * 获取记录ID
     * @return 记录ID
     */
    public int getId() {
        return id;
    }
    
    /**
     * 设置记录ID
     * @param id 记录ID
     */
    public void setId(int id) {
        this.id = id;
    }
    
    /**
     * 获取腰围
     * @return 腰围，单位：cm
     */
    public float getWaistSize() {
        return waistSize;
    }
    
    /**
     * 设置腰围
     * @param waistSize 腰围，单位：cm
     */
    public void setWaistSize(float waistSize) {
        this.waistSize = waistSize;
    }
    
    /**
     * 获取腰部高度
     * @return 腰部高度，单位：cm
     */
    public float getWaistHeight() {
        return waistHeight;
    }
    
    /**
     * 设置腰部高度
     * @param waistHeight 腰部高度，单位：cm
     */
    public void setWaistHeight(float waistHeight) {
        this.waistHeight = waistHeight;
    }
    
    /**
     * 获取腰部弧度
     * @return 腰部弧度，单位：度
     */
    public float getWaistCurvature() {
        return waistCurvature;
    }
    
    /**
     * 设置腰部弧度
     * @param waistCurvature 腰部弧度，单位：度
     */
    public void setWaistCurvature(float waistCurvature) {
        this.waistCurvature = waistCurvature;
    }
    
    /**
     * 获取测量时间
     * @return 测量时间
     */
    public Date getTimestamp() {
        return timestamp;
    }
    
    /**
     * 设置测量时间
     * @param timestamp 测量时间
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * 获取测量时的照片路径
     * @return 照片路径
     */
    public String getImagePath() {
        return imagePath;
    }
    
    /**
     * 设置测量时的照片路径
     * @param imagePath 照片路径
     */
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    
    @Override
    public String toString() {
        return "MeasurementRecord{" +
                "id=" + id +
                ", waistSize=" + waistSize +
                ", waistHeight=" + waistHeight +
                ", waistCurvature=" + waistCurvature +
                ", timestamp=" + timestamp +
                '}';
    }
}