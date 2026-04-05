package com.example.shunxing.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 测量记录管理器，使用 Room 数据库存储
 */
public class MeasurementRecordManager {
    private static final String TAG = "MeasurementRecordManager";
    private static MeasurementRecordManager instance;
    private MeasurementRecordDao recordDao;
    private ExecutorService executorService;
    private Handler mainHandler;
    
    private MeasurementRecordManager(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        recordDao = db.measurementRecordDao();
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public static synchronized MeasurementRecordManager getInstance(Context context) {
        if (instance == null) {
            instance = new MeasurementRecordManager(context);
        }
        return instance;
    }
    
    /**
     * 记录回调接口
     */
    public interface RecordsCallback {
        void onResult(List<MeasurementRecord> records);
    }
    
    /**
     * 记录回调接口（单个记录）
     */
    public interface RecordCallback {
        void onResult(MeasurementRecord record);
    }
    
    /**
     * 保存测量记录
     * @param record 测量记录
     * @return 是否保存成功
     * @deprecated 请使用异步方式保存记录
     */
    @Deprecated
    public boolean saveRecord(MeasurementRecord record) {
        try {
            Future<Long> future = executorService.submit(() -> recordDao.insert(record));
            long id = future.get();
            boolean success = id > 0;
            if (success) {
                Log.d(TAG, "Record saved successfully with ID: " + id);
            }
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error saving record: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取所有测量记录（同步）
     * @return 测量记录列表
     * @deprecated 请使用 getAllRecordsAsync() 避免阻塞UI线程
     */
    @Deprecated
    public List<MeasurementRecord> getAllRecords() {
        try {
            Future<List<MeasurementRecord>> future = executorService.submit(() -> recordDao.getAllRecords());
            return future.get();
        } catch (Exception e) {
            Log.e(TAG, "Error getting all records: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }
    
    /**
     * 获取所有测量记录（异步）
     * @param callback 回调
     */
    public void getAllRecordsAsync(RecordsCallback callback) {
        executorService.execute(() -> {
            try {
                List<MeasurementRecord> records = recordDao.getAllRecords();
                mainHandler.post(() -> callback.onResult(records));
            } catch (Exception e) {
                Log.e(TAG, "Error getting all records: " + e.getMessage());
                mainHandler.post(() -> callback.onResult(java.util.Collections.emptyList()));
            }
        });
    }
    
    /**
     * 根据 ID 获取测量记录（同步）
     * @param id 记录 ID
     * @return 测量记录
     * @deprecated 请使用 getRecordByIdAsync() 避免阻塞UI线程
     */
    @Deprecated
    public MeasurementRecord getRecordById(int id) {
        try {
            Future<MeasurementRecord> future = executorService.submit(() -> recordDao.getRecordById(id));
            return future.get();
        } catch (Exception e) {
            Log.e(TAG, "Error getting record by id: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 根据 ID 获取测量记录（异步）
     * @param id 记录 ID
     * @param callback 回调
     */
    public void getRecordByIdAsync(int id, RecordCallback callback) {
        executorService.execute(() -> {
            try {
                MeasurementRecord record = recordDao.getRecordById(id);
                mainHandler.post(() -> callback.onResult(record));
            } catch (Exception e) {
                Log.e(TAG, "Error getting record by id: " + e.getMessage());
                mainHandler.post(() -> callback.onResult(null));
            }
        });
    }
    
    /**
     * 更新测量记录
     * @param record 测量记录
     */
    public void updateRecord(MeasurementRecord record) {
        executorService.execute(() -> {
            try {
                recordDao.update(record);
            } catch (Exception e) {
                Log.e(TAG, "Error updating record: " + e.getMessage());
            }
        });
    }
    
    /**
     * 删除测量记录
     * @param record 测量记录
     */
    public void deleteRecord(MeasurementRecord record) {
        executorService.execute(() -> {
            try {
                recordDao.delete(record);
            } catch (Exception e) {
                Log.e(TAG, "Error deleting record: " + e.getMessage());
            }
        });
    }
    
    /**
     * 清空所有测量记录
     */
    public void clearAllRecords() {
        executorService.execute(() -> {
            try {
                recordDao.deleteAllRecords();
            } catch (Exception e) {
                Log.e(TAG, "Error clearing all records: " + e.getMessage());
            }
        });
    }
    
    /**
     * 获取记录数量
     * @return 记录数量
     * @deprecated 请使用异步方式获取记录数量
     */
    @Deprecated
    public int getRecordCount() {
        try {
            Future<Integer> future = executorService.submit(() -> recordDao.getRecordCount());
            return future.get();
        } catch (Exception e) {
            Log.e(TAG, "Error getting record count: " + e.getMessage());
            return 0;
        }
    }
}