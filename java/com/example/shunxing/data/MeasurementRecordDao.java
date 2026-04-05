package com.example.shunxing.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * 测量记录数据访问对象
 */
@Dao
public interface MeasurementRecordDao {
    @Insert
    long insert(MeasurementRecord record);
    
    @Update
    void update(MeasurementRecord record);
    
    @Delete
    void delete(MeasurementRecord record);
    
    @Query("SELECT * FROM measurement_records ORDER BY timestamp DESC")
    List<MeasurementRecord> getAllRecords();
    
    @Query("SELECT * FROM measurement_records WHERE id = :id")
    MeasurementRecord getRecordById(int id);
    
    @Query("DELETE FROM measurement_records")
    void deleteAllRecords();
    
    @Query("SELECT COUNT(*) FROM measurement_records")
    int getRecordCount();
}