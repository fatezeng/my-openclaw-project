package com.example.shunxing;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ModelGenerationWorker extends Worker {

    private static final String TAG = "ModelGenerationWorker";
    public static final String KEY_TASK_ID = "task_id";
    public static final String KEY_MODEL_PATH = "model_path";
    public static final String KEY_STATUS = "status";
    public static final String KEY_ERROR = "error";

    public ModelGenerationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            // 获取任务ID
            String taskId = getInputData().getString(KEY_TASK_ID);
            if (taskId == null) {
                return Result.failure(createOutputData("failed", "任务ID为空"));
            }

            Log.d(TAG, "开始处理任务: " + taskId);

            // 模拟模型生成过程
            // 在实际应用中，这里应该调用Kiri Engine SDK的API来查询任务状态
            simulateModelGeneration(taskId);

            // 模拟生成模型文件路径
            String modelPath = createMockModelFile();

            Log.d(TAG, "模型生成完成，保存路径: " + modelPath);

            // 返回成功结果
            return Result.success(createOutputData("success", null, modelPath));

        } catch (Exception e) {
            Log.e(TAG, "后台处理失败: " + e.getMessage());
            return Result.failure(createOutputData("failed", e.getMessage()));
        }
    }

    private void simulateModelGeneration(String taskId) throws InterruptedException {
        // 模拟模型生成的不同阶段
        Log.d(TAG, "开始上传照片...");
        Thread.sleep(2000); // 模拟上传时间

        Log.d(TAG, "开始重建模型...");
        Thread.sleep(5000); // 模拟重建时间

        Log.d(TAG, "开始优化模型...");
        Thread.sleep(3000); // 模拟优化时间

        Log.d(TAG, "模型生成完成");
    }

    private String createMockModelFile() throws IOException {
        // 创建模型存储目录
        File dir = new File(getApplicationContext().getExternalFilesDir("3DModels"), "generated");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 创建模拟模型文件
        String fileName = "waist_model_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date()) + ".glb";
        File modelFile = new File(dir, fileName);

        // 创建空文件
        if (!modelFile.createNewFile()) {
            throw new IOException("无法创建模型文件");
        }

        return modelFile.getAbsolutePath();
    }

    private Data createOutputData(String status, String error) {
        return new Data.Builder()
                .putString(KEY_STATUS, status)
                .putString(KEY_ERROR, error)
                .build();
    }

    private Data createOutputData(String status, String error, String modelPath) {
        return new Data.Builder()
                .putString(KEY_STATUS, status)
                .putString(KEY_ERROR, error)
                .putString(KEY_MODEL_PATH, modelPath)
                .build();
    }
}