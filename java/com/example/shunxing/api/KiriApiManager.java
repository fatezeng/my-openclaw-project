package com.example.shunxing.api;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Kiri API 管理器，封装云端建模流程
 */
public class KiriApiManager {
    private static final String TAG = "KiriApiManager";
    private static KiriApiManager instance;
    
    private Context context;
    private KiriApi kiriApi;
    private MutableLiveData<ModelGenerationStatus> statusLiveData;
    private String currentTaskId;
    private Handler pollingHandler;
    private Runnable pollingRunnable;
    
    /**
     * 模型生成状态
     */
    public static class ModelGenerationStatus {
        public enum Status {
            IDLE,
            UPLOADING,
            PROCESSING,
            DOWNLOADING,
            SUCCESS,
            FAILED
        }
        
        public Status status;
        public String message;
        public String modelPath;
        public int progress;
        
        public ModelGenerationStatus(Status status, String message) {
            this.status = status;
            this.message = message;
            this.progress = 0;
        }
        
        public ModelGenerationStatus(Status status, String message, int progress) {
            this(status, message);
            this.progress = progress;
        }
        
        public ModelGenerationStatus(Status status, String message, String modelPath) {
            this(status, message);
            this.modelPath = modelPath;
        }
    }
    
    /**
     * 私有构造函数
     */
    private KiriApiManager(Context context) {
        try {
            this.context = context;
            kiriApi = ApiClient.getInstance().create(KiriApi.class);
            statusLiveData = new MutableLiveData<>();
            statusLiveData.setValue(new ModelGenerationStatus(ModelGenerationStatus.Status.IDLE, "就绪"));
            pollingHandler = new Handler();
            Log.d(TAG, "KiriApiManager initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing KiriApiManager: " + e.getMessage());
        }
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized KiriApiManager getInstance(Context context) {
        if (instance == null) {
            instance = new KiriApiManager(context);
        }
        return instance;
    }
    
    /**
     * 模型生成回调接口
     */
    public interface ModelGenerationCallback {
        void onProgress(String status);
        void onSuccess(String modelUrl);
        void onFailure(String error);
    }
    

    
    /**
     * 获取状态 LiveData
     */
    public LiveData<ModelGenerationStatus> getStatusLiveData() {
        return statusLiveData;
    }
    
    /**
     * 生成3D模型
     * @param imagePaths 图片路径列表
     * @param callback 生成回调
     */
    public void generate3DModel(List<String> imagePaths, ModelGenerationCallback callback) {
        try {
            Log.d(TAG, "Starting 3D model generation with " + imagePaths.size() + " images");
            
            // 准备图片文件
            List<MultipartBody.Part> imageParts = new java.util.ArrayList<>();
            for (String path : imagePaths) {
                File file = new File(path);
                if (file.exists()) {
                    RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
                    MultipartBody.Part part = MultipartBody.Part.createFormData("images", file.getName(), requestFile);
                    imageParts.add(part);
                } else {
                    Log.w(TAG, "Image file not found: " + path);
                }
            }
            
            if (imageParts.isEmpty()) {
                String error = "图片文件不存在，无法上传";
                Log.e(TAG, error);
                if (callback != null) {
                    callback.onFailure(error);
                }
                return;
            }
            
            // 调用 API 创建扫描任务
            Call<CreateScanResponse> call = kiriApi.createScan(imageParts);
            call.enqueue(new Callback<CreateScanResponse>() {
                @Override
                public void onResponse(Call<CreateScanResponse> call, Response<CreateScanResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        currentTaskId = response.body().getTaskId();
                        Log.d(TAG, "Scan task created with ID: " + currentTaskId);
                        if (callback != null) {
                            callback.onProgress("正在处理模型...");
                        }
                        startPollingTaskStatus(callback);
                    } else {
                        String error = "任务创建失败，请稍后重试";
                        Log.e(TAG, error + " - Code: " + response.code());
                        if (callback != null) {
                            callback.onFailure(error);
                        }
                    }
                }
                
                @Override
                public void onFailure(Call<CreateScanResponse> call, Throwable t) {
                    String error = "网络连接失败，请检查设置";
                    Log.e(TAG, error + ": " + t.getMessage());
                    if (callback != null) {
                        callback.onFailure(error);
                    }
                }
            });
        } catch (Exception e) {
            String error = "启动3D建模失败";
            Log.e(TAG, error + ": " + e.getMessage());
            if (callback != null) {
                callback.onFailure(error);
            }
        }
    }
    
    /**
     * 开始轮询任务状态
     */
    private void startPollingTaskStatus(ModelGenerationCallback callback) {
        final int[] pollingCount = {0};
        final int MAX_POLLING_COUNT = 60; // 5分钟超时
        
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (pollingCount[0] >= MAX_POLLING_COUNT) {
                    String error = "任务超时，请稍后重试";
                    Log.e(TAG, error);
                    if (callback != null) {
                        callback.onFailure(error);
                    }
                    return;
                }
                
                Call<ScanStatusResponse> call = kiriApi.getScanStatus(currentTaskId);
                call.enqueue(new Callback<ScanStatusResponse>() {
                    @Override
                    public void onResponse(Call<ScanStatusResponse> call, Response<ScanStatusResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ScanStatusResponse statusResponse = response.body();
                            String status = statusResponse.getStatus();
                            
                            if ("processing".equals(status)) {
                                String progressMessage = "正在处理模型... " + (pollingCount[0] * 5) + "秒";
                                Log.d(TAG, progressMessage);
                                if (callback != null) {
                                    callback.onProgress(progressMessage);
                                }
                                pollingCount[0]++;
                                pollingHandler.postDelayed(pollingRunnable, 5000);
                            } else if ("completed".equals(status)) {
                                String modelUrl = statusResponse.getModelUrl();
                                if (modelUrl != null && !modelUrl.isEmpty()) {
                                    Log.d(TAG, "Model generation completed, downloading from: " + modelUrl);
                                    if (callback != null) {
                                        callback.onProgress("正在下载模型...");
                                    }
                                    downloadModel(modelUrl, callback);
                                } else {
                                    String error = "模型生成成功，但下载地址无效";
                                    Log.e(TAG, error);
                                    if (callback != null) {
                                        callback.onFailure(error);
                                    }
                                }
                            } else if ("failed".equals(status)) {
                                String error = "模型生成失败: " + (statusResponse.getError() != null ? statusResponse.getError() : "未知错误");
                                Log.e(TAG, error);
                                if (callback != null) {
                                    callback.onFailure(error);
                                }
                            } else {
                                pollingCount[0]++;
                                pollingHandler.postDelayed(pollingRunnable, 5000);
                            }
                        } else {
                            pollingCount[0]++;
                            pollingHandler.postDelayed(pollingRunnable, 5000);
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<ScanStatusResponse> call, Throwable t) {
                        pollingCount[0]++;
                        pollingHandler.postDelayed(pollingRunnable, 5000);
                        Log.e(TAG, "Error polling task status: " + t.getMessage());
                    }
                });
            }
        };
        pollingHandler.postDelayed(pollingRunnable, 5000); // 每5秒轮询一次
    }
    
    /**
     * 下载模型文件
     * @param modelUrl 模型下载地址
     * @param callback 下载回调
     */
    private void downloadModel(String modelUrl, ModelGenerationCallback callback) {
        try {
            // 创建模型存储目录
            File modelDir = new File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), "WaistMeasurement/models");
            if (!modelDir.exists()) {
                modelDir.mkdirs();
                Log.d(TAG, "Created model directory: " + modelDir.getAbsolutePath());
            }
            
            // 生成文件名
            String fileName = "model_" + System.currentTimeMillis() + ".glb";
            File modelFile = new File(modelDir, fileName);
            Log.d(TAG, "Saving model to: " + modelFile.getAbsolutePath());
            
            // 使用 OkHttp 下载文件
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(modelUrl)
                    .build();
            
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                    if (response.isSuccessful()) {
                        // 保存文件
                        try (okhttp3.ResponseBody responseBody = response.body();
                             FileOutputStream fos = new FileOutputStream(modelFile)) {
                            if (responseBody != null) {
                                fos.write(responseBody.bytes());
                                fos.flush();
                            }
                            
                            Log.d(TAG, "Model downloaded successfully: " + modelFile.getAbsolutePath());
                            if (callback != null) {
                                callback.onSuccess(modelFile.getAbsolutePath());
                            }
                        } catch (Exception e) {
                            String error = "模型保存失败";
                            Log.e(TAG, error + ": " + e.getMessage());
                            if (callback != null) {
                                callback.onFailure(error);
                            }
                        }
                    } else {
                        String error = "模型下载失败";
                        Log.e(TAG, error + " - Code: " + response.code());
                        if (callback != null) {
                            callback.onFailure(error);
                        }
                    }
                }
                
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    String error = "网络连接失败，请检查设置";
                    Log.e(TAG, error + ": " + e.getMessage());
                    if (callback != null) {
                        callback.onFailure(error);
                    }
                }
            });
        } catch (Exception e) {
            String error = "模型下载失败";
            Log.e(TAG, error + ": " + e.getMessage());
            if (callback != null) {
                callback.onFailure(error);
            }
        }
    }
    
    /**
     * 取消模型生成
     */
    public void cancelModelGeneration() {
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
            Log.d(TAG, "Polling cancelled");
        }
        statusLiveData.setValue(new ModelGenerationStatus(ModelGenerationStatus.Status.IDLE, "已取消"));
        Log.d(TAG, "Model generation cancelled");
    }
}