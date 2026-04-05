package com.example.shunxing.camera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraCharacteristics;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.app.ActivityManager;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 相机管理器，封装相机预览和帧捕获功能
 */
public class CameraManager {
    private static final String TAG = "CameraManager";
    
    private Context context;
    private LifecycleOwner lifecycleOwner;
    private PreviewView previewView;
    
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Camera camera;
    private ImageAnalysis imageAnalysis;
    private ExecutorService cameraExecutor;
    private HandlerThread imageAnalysisThread;
    private Handler imageAnalysisHandler;
    
    private OnFrameAvailableListener frameAvailableListener;
    private InitializeCallback initializeCallback;
    private boolean isCameraInitialized = false;
    
    // 分辨率设置
    private static final int[] RESOLUTION_WIDTHS = {640, 960, 1280};
    private static final int[] RESOLUTION_HEIGHTS = {480, 720, 720};
    private static final int[] RESOLUTION_WIDTHS_PORTRAIT = {480, 720, 720};
    private static final int[] RESOLUTION_HEIGHTS_PORTRAIT = {640, 960, 1280};
    private int currentResolutionLevel = 1; // 0: 低, 1: 中, 2: 高（默认使用中分辨率以提高性能）
    
    // 焦距像素值
    private float focalLengthPixels = 1000.0f;
    
    /**
     * 构造函数
     * @param context 上下文
     * @param lifecycleOwner 生命周期所有者
     * @param previewView 预览视图
     */
    public CameraManager(Context context, LifecycleOwner lifecycleOwner, PreviewView previewView) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.previewView = previewView;
        this.cameraExecutor = Executors.newSingleThreadExecutor();
        this.imageAnalysisThread = new HandlerThread("ImageAnalysisThread");
        this.imageAnalysisThread.start();
        this.imageAnalysisHandler = new Handler(imageAnalysisThread.getLooper());
    }
    
    /**
     * 初始化相机
     * @param callback 初始化回调
     */
    public void initializeCamera(InitializeCallback callback) {
        this.initializeCallback = callback;
        
        // 检查相机权限
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Camera permission not granted");
            if (callback != null) {
                callback.onFailure(new Exception("Camera permission not granted"));
            }
            return;
        }
        
        cameraProviderFuture = ProcessCameraProvider.getInstance(context);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                // 根据设备性能自动选择合适的分辨率
                autoSelectResolution();
                bindCamera(cameraProvider);
                calculateFocalLengthPixels();
                isCameraInitialized = true;
                if (callback != null) {
                    callback.onSuccess();
                }
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error initializing camera: " + e.getMessage());
                if (callback != null) {
                    callback.onFailure(e);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing camera: " + e.getMessage());
                if (callback != null) {
                    callback.onFailure(e);
                }
            }
        }, ContextCompat.getMainExecutor(context));
    }
    
    /**
     * 绑定相机
     */
    private void bindCamera(ProcessCameraProvider cameraProvider) {
        // 清除之前的绑定
        cameraProvider.unbindAll();
        
        // 创建预览用例
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        
        // 检测设备方向
        int rotation = context.getResources().getConfiguration().orientation;
        boolean isPortrait = rotation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        
        // 根据方向选择合适的分辨率
        int width, height;
        if (isPortrait) {
            width = RESOLUTION_WIDTHS_PORTRAIT[currentResolutionLevel];
            height = RESOLUTION_HEIGHTS_PORTRAIT[currentResolutionLevel];
        } else {
            width = RESOLUTION_WIDTHS[currentResolutionLevel];
            height = RESOLUTION_HEIGHTS[currentResolutionLevel];
        }
        
        // 创建图像分析用例
        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(width, height))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        
        // 设置分析器，在后台线程上运行
        imageAnalysis.setAnalyzer(new Executor() {
            @Override
            public void execute(Runnable command) {
                imageAnalysisHandler.post(command);
            }
        }, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@androidx.camera.core.ExperimentalGetImage ImageProxy image) {
                try {
                    // 将 ImageProxy 转换为 Bitmap
                    Bitmap bitmap = toBitmap(image);
                    if (bitmap != null && frameAvailableListener != null) {
                        frameAvailableListener.onFrameAvailable(bitmap, image.getImageInfo().getRotationDegrees());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error analyzing image: " + e.getMessage());
                    if (frameAvailableListener != null) {
                        frameAvailableListener.onFrameError(e);
                    }
                } finally {
                    image.close();
                }
            }
        });
        
        // 选择后置相机
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        
        // 绑定用例
        camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis);
    }
    
    /**
     * 将 ImageProxy 转换为 Bitmap
     */
    private Bitmap toBitmap(ImageProxy image) {
        try {
            // 获取 Image 对象
            Image img = image.getImage();
            if (img == null) {
                return null;
            }
            
            // 获取 YUV 数据并转换为 NV21 格式
            byte[] nv21 = YUV_420_888_to_NV21(img);
            if (nv21 == null) {
                return null;
            }
            
            // 创建 YuvImage
            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, img.getWidth(), img.getHeight(), null);
            
            // 转换为 Bitmap
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, img.getWidth(), img.getHeight()), 100, out);
            byte[] imageBytes = out.toByteArray();
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 将 YUV_420_888 格式转换为 NV21 格式
     * @param image Image 对象
     * @return NV21 格式的字节数组
     */
    private byte[] YUV_420_888_to_NV21(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int ySize = width * height;
        int uvSize = width * height / 4;
        
        byte[] nv21 = new byte[ySize + uvSize * 2];
        
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();
        
        int yRowStride = planes[0].getRowStride();
        int uRowStride = planes[1].getRowStride();
        int vRowStride = planes[2].getRowStride();
        
        int yPixelStride = planes[0].getPixelStride();
        int uPixelStride = planes[1].getPixelStride();
        int vPixelStride = planes[2].getPixelStride();
        
        // 复制 Y 通道
        int position = 0;
        if (yPixelStride == 1 && yRowStride == width) {
            yBuffer.get(nv21, 0, ySize);
            position += ySize;
        } else {
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    nv21[position++] = yBuffer.get(i * yRowStride + j * yPixelStride);
                }
            }
        }
        
        // 复制 UV 通道
        if (uPixelStride == 1 && uRowStride == width / 2) {
            uBuffer.get(nv21, position, uvSize);
            vBuffer.get(nv21, position + uvSize, uvSize);
        } else {
            for (int i = 0; i < height / 2; i++) {
                for (int j = 0; j < width / 2; j++) {
                    nv21[position++] = uBuffer.get(i * uRowStride + j * uPixelStride);
                    nv21[position++] = vBuffer.get(i * vRowStride + j * vPixelStride);
                }
            }
        }
        
        return nv21;
    }
    
    /**
     * 计算焦距像素值
     */
    private void calculateFocalLengthPixels() {
        try {
            // 使用 android.hardware.camera2.CameraManager 获取相机特性
            android.hardware.camera2.CameraManager cameraManager = (android.hardware.camera2.CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String cameraId = null;
            
            // 获取后置相机 ID
            for (String id : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id;
                    break;
                }
            }
            
            if (cameraId != null) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                
                // 获取镜头的物理焦距（毫米）
                float[] focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                float focalLength = focalLengths != null && focalLengths.length > 0 ? focalLengths[0] : 3.0f; // 默认 3.0mm
                
                // 获取传感器物理宽度（毫米）
                android.util.SizeF sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                float sensorWidth = sensorSize != null ? sensorSize.getWidth() : 4.0f; // 默认 4.0mm
                
                // 获取传感器像素阵列大小
                android.util.Size pixelArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
                int sensorPixelWidth = pixelArraySize != null ? pixelArraySize.getWidth() : 4000; // 默认值
                
                // 获取活动像素区域
                android.graphics.Rect activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                int activePixelWidth = activeArraySize != null ? activeArraySize.width() : sensorPixelWidth; // 默认使用像素阵列宽度
                
                // 获取预览画面的像素宽度
                int rotation = context.getResources().getConfiguration().orientation;
                boolean isPortrait = rotation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
                int previewWidth = isPortrait ? RESOLUTION_WIDTHS_PORTRAIT[currentResolutionLevel] : RESOLUTION_WIDTHS[currentResolutionLevel];
                
                // 计算缩放因子
                float scaleFactor = (float) previewWidth / activePixelWidth;
                
                // 计算焦距像素值（使用活动像素区域宽度）
                focalLengthPixels = (focalLength * activePixelWidth) / sensorWidth;
                
                // 应用缩放因子
                focalLengthPixels *= scaleFactor;
                
                Log.d(TAG, "Focal length pixels calculated: " + focalLengthPixels);
                Log.d(TAG, "Sensor pixel width: " + sensorPixelWidth + ", Active pixel width: " + activePixelWidth + ", Preview width: " + previewWidth);
                Log.d(TAG, "Scale factor: " + scaleFactor);
            } else {
                Log.e(TAG, "Back camera not found");
            }
            
            // 降级：如果焦距计算失败或值过小，使用经验公式
            int rotation = context.getResources().getConfiguration().orientation;
            boolean isPortrait = rotation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
            int previewWidth = isPortrait ? RESOLUTION_WIDTHS_PORTRAIT[currentResolutionLevel] : RESOLUTION_WIDTHS[currentResolutionLevel];
            
            if (focalLengthPixels <= 0) {
                // 降级：使用预览宽度 * 系数（如 1.2）
                focalLengthPixels = previewWidth * 1.2f;
                Log.w(TAG, "Fallback focal length: " + focalLengthPixels);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating focal length pixels: " + e.getMessage());
            // 异常情况下使用降级方案
            int rotation = context.getResources().getConfiguration().orientation;
            boolean isPortrait = rotation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
            int previewWidth = isPortrait ? RESOLUTION_WIDTHS_PORTRAIT[currentResolutionLevel] : RESOLUTION_WIDTHS[currentResolutionLevel];
            focalLengthPixels = previewWidth * 1.2f;
            Log.w(TAG, "Fallback focal length due to exception: " + focalLengthPixels);
        }
    }
    
    /**
     * 设置帧可用监听器
     */
    public void setOnFrameAvailableListener(OnFrameAvailableListener listener) {
        this.frameAvailableListener = listener;
    }
    
    /**
     * 获取焦距像素值
     */
    public float getFocalLengthPixels() {
        return focalLengthPixels;
    }
    
    /**
     * 获取相机对象
     */
    public Camera getCamera() {
        return camera;
    }
    
    /**
     * 释放资源
     */
    public void release() {
        try {
            if (cameraExecutor != null) {
                cameraExecutor.shutdown();
                Log.d(TAG, "Camera executor shut down");
            }
            if (imageAnalysisThread != null) {
                imageAnalysisThread.quitSafely();
                Log.d(TAG, "Image analysis thread quit safely");
            }
            if (cameraProviderFuture != null && !cameraProviderFuture.isDone()) {
                cameraProviderFuture.cancel(true);
                Log.d(TAG, "Camera provider future cancelled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error releasing camera resources: " + e.getMessage());
        }
    }
    
    /**
     * 处理配置变更
     */
    public void onConfigurationChanged() {
        try {
            if (cameraProviderFuture != null && cameraProviderFuture.isDone()) {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
                bindCamera(cameraProvider);
                Log.d(TAG, "Camera reconfigured on orientation change");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reconfiguring camera: " + e.getMessage());
        }
    }
    
    /**
     * 设置分辨率级别
     * @param level 分辨率级别：0-低，1-中，2-高
     */
    public void setResolutionLevel(int level) {
        if (level >= 0 && level < RESOLUTION_WIDTHS.length) {
            currentResolutionLevel = level;
            Log.d(TAG, "Resolution level set to: " + level);
            // 重新绑定相机以应用新分辨率
            try {
                if (cameraProviderFuture != null && cameraProviderFuture.isDone()) {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    cameraProvider.unbindAll();
                    bindCamera(cameraProvider);
                    Log.d(TAG, "Camera rebind with new resolution");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error rebinding camera with new resolution: " + e.getMessage());
            }
        } else {
            Log.e(TAG, "Invalid resolution level: " + level);
        }
    }
    
    /**
     * 获取当前分辨率级别
     * @return 分辨率级别
     */
    public int getResolutionLevel() {
        return currentResolutionLevel;
    }
    
    /**
     * 根据设备性能自动选择合适的分辨率
     */
    public void autoSelectResolution() {
        // 检查设备内存
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        
        // 检查设备处理器核心数
        int cpuCores = Runtime.getRuntime().availableProcessors();
        
        // 根据设备性能选择分辨率
        if (memoryInfo.totalMem < 4 * 1024 * 1024 * 1024) { // 小于4GB内存
            currentResolutionLevel = 0; // 低分辨率
        } else if (cpuCores < 4) { // 小于4核心
            currentResolutionLevel = 1; // 中分辨率
        } else {
            currentResolutionLevel = 2; // 高分辨率
        }
        
        Log.d(TAG, "Auto selected resolution level: " + currentResolutionLevel + ", Memory: " + (memoryInfo.totalMem / (1024 * 1024 * 1024)) + "GB, CPU cores: " + cpuCores);
    }
    
    /**
     * 帧可用监听器
     */
    public interface OnFrameAvailableListener {
        void onFrameAvailable(Bitmap bitmap, int rotationDegrees);
        void onFrameError(Exception e);
    }
    
    /**
     * 初始化回调
     */
    public interface InitializeCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
}