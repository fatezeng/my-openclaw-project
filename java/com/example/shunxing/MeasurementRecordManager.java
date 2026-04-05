package com.example.shunxing;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 测量记录管理器
 * 负责存储和读取测量记录
 */
public class MeasurementRecordManager {

    private static final String TAG = "MeasurementRecordManager";
    private static final String PREFS_NAME = "measurement_records";
    private static final String RECORD_PREFIX = "record_";
    private static final int MAX_RECORDS = 10;
    
    private static MeasurementRecordManager instance;
    private SharedPreferences prefs;
    
    /**
     * 获取单例实例
     * @param context 上下文
     * @return MeasurementRecordManager实例
     */
    public static synchronized MeasurementRecordManager getInstance(Context context) {
        if (instance == null) {
            instance = new MeasurementRecordManager(context);
        }
        return instance;
    }
    
    /**
     * 构造方法
     * @param context 上下文
     */
    private MeasurementRecordManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * 保存测量记录
     * @param record 测量记录
     * @return 是否保存成功
     */
    public boolean saveRecord(MeasurementRecord record) {
        try {
            // 获取所有现有记录
            List<MeasurementRecord> records = getAllRecords();
            
            // 检查记录数是否已达到最大值
            if (records.size() >= MAX_RECORDS) {
                Log.i(TAG, "Measurement record limit reached");
                return false;
            }
            
            // 添加新记录
            records.add(0, record); // 新记录放在最前面
            
            // 重新保存所有记录
            clearAllRecords();
            for (int i = 0; i < records.size(); i++) {
                saveRecordToPrefs(i + 1, records.get(i));
            }
            
            Log.i(TAG, "Measurement record saved successfully");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving measurement record: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取所有测量记录
     * @return 测量记录列表
     */
    public List<MeasurementRecord> getAllRecords() {
        List<MeasurementRecord> records = new ArrayList<>();
        
        try {
            for (int i = 1; i <= MAX_RECORDS; i++) {
                MeasurementRecord record = getRecord(i);
                if (record != null) {
                    records.add(record);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting all measurement records: " + e.getMessage());
        }
        
        return records;
    }
    
    /**
     * 获取指定ID的测量记录
     * @param id 记录ID
     * @return 测量记录，如果不存在则返回null
     */
    public MeasurementRecord getRecord(int id) {
        try {
            String key = RECORD_PREFIX + id;
            String serializedRecord = prefs.getString(key, null);
            
            if (serializedRecord != null) {
                return deserializeRecord(serializedRecord);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting measurement record: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 删除指定ID的测量记录
     * @param id 记录ID
     */
    public void deleteRecord(int id) {
        try {
            String key = RECORD_PREFIX + id;
            prefs.edit().remove(key).apply();
            
            // 重新排序记录
            List<MeasurementRecord> records = getAllRecords();
            clearAllRecords();
            for (int i = 0; i < records.size(); i++) {
                saveRecordToPrefs(i + 1, records.get(i));
            }
            
            Log.i(TAG, "Measurement record deleted successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error deleting measurement record: " + e.getMessage());
        }
    }
    
    /**
     * 清空所有测量记录
     */
    public void clearAllRecords() {
        try {
            SharedPreferences.Editor editor = prefs.edit();
            for (int i = 1; i <= MAX_RECORDS; i++) {
                String key = RECORD_PREFIX + i;
                editor.remove(key);
            }
            editor.apply();
            
            Log.i(TAG, "All measurement records cleared");
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing all measurement records: " + e.getMessage());
        }
    }
    
    /**
     * 将测量记录保存到SharedPreferences
     * @param id 记录ID
     * @param record 测量记录
     */
    private void saveRecordToPrefs(int id, MeasurementRecord record) {
        try {
            String key = RECORD_PREFIX + id;
            String serializedRecord = serializeRecord(record);
            prefs.edit().putString(key, serializedRecord).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving record to prefs: " + e.getMessage());
        }
    }
    
    /**
     * 序列化测量记录
     * @param record 测量记录
     * @return 序列化后的字符串
     */
    private String serializeRecord(MeasurementRecord record) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(record);
        oos.close();
        baos.close();
        return android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT);
    }
    
    /**
     * 反序列化测量记录
     * @param serializedRecord 序列化后的字符串
     * @return 测量记录
     */
    private MeasurementRecord deserializeRecord(String serializedRecord) throws IOException, ClassNotFoundException {
        byte[] data = android.util.Base64.decode(serializedRecord, android.util.Base64.DEFAULT);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        MeasurementRecord record = (MeasurementRecord) ois.readObject();
        ois.close();
        bais.close();
        return record;
    }
}