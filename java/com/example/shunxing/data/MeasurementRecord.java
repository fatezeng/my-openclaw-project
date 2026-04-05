package com.example.shunxing.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

import java.util.Date;

/**
 * 测量记录实体类
 */
@Entity(tableName = "measurement_records")
public class MeasurementRecord {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private float waistSize;
    private float waistHeight;
    private float waistCurvature;
    private long timestamp;
    private String modelPath;
    private String imagePath;
    
    public MeasurementRecord() {
        this.timestamp = new Date().getTime();
    }
    
    @Ignore
    public MeasurementRecord(int id, float waistSize, float waistHeight, float waistCurvature) {
        this.id = id;
        this.waistSize = waistSize;
        this.waistHeight = waistHeight;
        this.waistCurvature = waistCurvature;
        this.timestamp = new Date().getTime();
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public float getWaistSize() {
        return waistSize;
    }
    
    public void setWaistSize(float waistSize) {
        this.waistSize = waistSize;
    }
    
    public float getWaistHeight() {
        return waistHeight;
    }
    
    public void setWaistHeight(float waistHeight) {
        this.waistHeight = waistHeight;
    }
    
    public float getWaistCurvature() {
        return waistCurvature;
    }
    
    public void setWaistCurvature(float waistCurvature) {
        this.waistCurvature = waistCurvature;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getModelPath() {
        return modelPath;
    }
    
    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}