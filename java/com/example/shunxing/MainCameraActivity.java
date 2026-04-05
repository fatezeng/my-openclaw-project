package com.example.shunxing;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Log;

import com.example.shunxing.BuildConfig;

import com.example.shunxing.camera.CameraManager;
import com.example.shunxing.pose.PoseDetector;
import com.example.shunxing.data.MeasurementRecord;
import com.example.shunxing.data.MeasurementRecordManager;
import com.example.shunxing.api.KiriApiManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Locale;

import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 主界面 Activity
 * 功能：实现三层叠加结构
 * 1. 底层：全屏摄像头预览
 * 2. 中层：半透明参考线网格
 * 3. 上层：动态球体指示器
 */
public class MainCameraActivity extends AppCompatActivity {

    private static final String TAG = "MainCameraActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    // UI 组件
    private PreviewView previewView;
    private TextView statusText;
    private TextView distanceText;
    private View actionButton;
    private View scanCompleteIndicator;
    private View topLayer;
    private View bottomLayer;
    private TextView tvShunXing;
    private RelativeLayout guideDialog;
    private Button btnCloseGuide;
    private ImageButton btnGuide;



    // 摄像头相关
    private boolean isScanning = false;
    private boolean isPaused = false;
    private Handler mainHandler;
    private ExecutorService processingExecutor;
    
    // 分析相关
    private boolean isMeasuring = false;
    private float waistSize = 0.0f;
    private float waistHeight = 0.0f;
    private float waistCurvature = 0.0f;
    
    // 距离测量相关
    private float focalLengthPixels = 0.0f; // 焦距像素值
    private static final float DEFAULT_PELVIS_WIDTH_RATIO = 0.18f; // 骨盆宽度与身高的比例
    private float userHeight = 170.0f; // 默认身高 170cm
    private java.util.List<Float> distanceHistory = new java.util.ArrayList<>(); // 距离历史记录
    
    // 测量记录相关
    private MeasurementRecordManager recordManager;
    
    // 模块
    private CameraManager cameraManager;
    private PoseDetector poseDetector;
    private Camera camera;
    
    // 关键帧存储
    private List<Bitmap> keyFrames = new CopyOnWriteArrayList<>();
    private static final int MAX_KEY_FRAMES = 3;
    
    // 拍照状态管理
    private int capturedPhotoCount = 0;
    private int targetPhotoCount = 30; // 目标照片数
    private List<Bitmap> capturedPhotos = new CopyOnWriteArrayList<>();
    private boolean isPhotoPending = false;
    private Bitmap currentFrameBitmap = null; // 当前帧的Bitmap
    
    // 传感器相关
    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    private Sensor accelerometerSensor;
    private SensorEventListener sensorEventListener;
    private boolean isBaseAngleSet = false;
    private float[] baseAngle = new float[3]; // 方位角、俯仰角、横滚角
    private float[] currentAngle = new float[3]; // currentAngle
    private float[] angleDifference = new float[3]; // angleDifference
    
    // 稳定性检测相关
    private static final float SHAKE_THRESHOLD = 15.0f; // 晃动阈值
    private long lastShakeTime = 0; // 上次晃动时间
    private float[] lastAccelerometerValues = new float[3]; // 上次加速度值
    
    // 角度阈值相关
    private static final float ANGLE_THRESHOLD = 30.0f; // 角度阈值
    private boolean isAngleThresholdReached = false;
    
    // 拍照逻辑相关
    private static final float ANGLE_CAPTURE_THRESHOLD = 12f; // 每12度拍一张
    private float lastCaptureAngle = 0f;
    private boolean hasStartedCapturing = false;
    
    // 距离测量相关
    private float estimatedDistance = 0.0f; // 估计距离
    
    // 角度历史记录相关
    private java.util.List<float[]> angleHistory = new java.util.ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 100; // 最大历史记录数量
    
    // 错误类型枚举
    public enum ErrorType {
        DISTANCE_TOO_CLOSE,
        DISTANCE_TOO_FAR,
        POSE_INCORRECT,
        LIGHTING_POOR,
        SHAKING_TOO_MUCH
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 使用现有的布局文件
        setContentView(R.layout.activity_main_camera);

        mainHandler = new Handler(Looper.getMainLooper());
        processingExecutor = Executors.newSingleThreadExecutor();
        
        initViews();
        setupListeners();
        
        // 显示操作指南对话框
        if (guideDialog != null) {
            guideDialog.setVisibility(View.VISIBLE);
        }
        
        // 初始化测量记录管理器
        recordManager = MeasurementRecordManager.getInstance(this);
        
        // 从 SharedPreferences 读取用户身高
        android.content.SharedPreferences prefs = getSharedPreferences("WaistMeasurementPrefs", MODE_PRIVATE);
        userHeight = prefs.getFloat("user_height", 170.0f); // 默认170cm
        
        // 初始化姿势检测器
        poseDetector = new PoseDetector(this);
        poseDetector.initialize(new PoseDetector.InitializeCallback() {
            @Override
            public void onSuccess() {
                if (BuildConfig.DEBUG) Log.i(TAG, "Pose detector initialized successfully");
                runOnUiThread(() -> {
                    statusText.setText("姿势检测器初始化成功，点击开始测量");
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to initialize pose detector: " + e.getMessage());
                runOnUiThread(() -> {
                    statusText.setText("姿势检测器初始化失败: " + e.getMessage());
                });
            }
        });
        
        // 隐藏状态栏，实现全屏效果
        hideStatusBar();
        
        // 初始化准星管理器
        ViewGroup contentView = (ViewGroup) findViewById(android.R.id.content);
        if (contentView != null && contentView.getChildCount() > 0) {
            View rootView = contentView.getChildAt(0);
            if (rootView instanceof FrameLayout) {
                FocusIndicatorManager.getInstance().init((FrameLayout) rootView);
            } else {
                // 如果根视图不是FrameLayout，创建一个FrameLayout作为容器
                FrameLayout frameLayout = new FrameLayout(this);
                contentView.addView(frameLayout);
                FocusIndicatorManager.getInstance().init(frameLayout);
            }
        }
        
        // 设置手势检测器处理单击聚焦和双击切换状态栏
        setupPreviewGestureDetector();



        // 初始化传感器管理器
        initSensorManager();

        // 初始化完成，准备启动相机
        if (BuildConfig.DEBUG) Log.i(TAG, "Initialization completed");

        // 检查摄像头权限
        if (checkCameraPermission()) {
            startCameraPreview();
        } else {
            requestCameraPermission();
        }
    }
    


    private static final int REQUEST_GALLERY = 101;

    private ProgressBar captureProgressBar;
    private TextView tvProgressPercent;
    
    private View centerIndicator;
    private ImageView statusIcon;
    private TextView guideText;
    
    private void initViews() {
        previewView = findViewById(R.id.previewView);
        statusText = findViewById(R.id.statusText);
        distanceText = findViewById(R.id.distanceText);
        actionButton = findViewById(R.id.actionButton);
        scanCompleteIndicator = findViewById(R.id.scanCompleteIndicator);
        topLayer = findViewById(R.id.topLayer);
        bottomLayer = findViewById(R.id.bottomLayer);
        tvShunXing = findViewById(R.id.tvShunXing);
        guideDialog = findViewById(R.id.guideDialog);
        btnCloseGuide = findViewById(R.id.btnCloseGuide);
        btnGuide = findViewById(R.id.btnGuide);
        captureProgressBar = findViewById(R.id.captureProgressBar);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        centerIndicator = findViewById(R.id.centerIndicator);
        statusIcon = findViewById(R.id.statusIcon);
        guideText = findViewById(R.id.guideText);
    }

    private void setupListeners() {
        // 拍摄按钮点击事件
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 确保点击时获得焦点
                v.requestFocus();
                
                if (isMeasuring) {
                    // 停止测量
                    stopMeasuring();
                } else {
                    // 开始测量
                    startMeasuring();
                }
            }
        });
        
        // 瞬形按钮点击事件
        if (tvShunXing != null) {
            tvShunXing.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 返回主界面
                    Intent intent = new Intent(MainCameraActivity.this, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
            });
        }
        
        // 关闭操作指南按钮点击事件
        if (btnCloseGuide != null) {
            btnCloseGuide.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (guideDialog != null) {
                        guideDialog.setVisibility(View.GONE);
                    }
                }
            });
        }
        
        // 操作指南按钮点击事件
        if (btnGuide != null) {
            btnGuide.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (guideDialog != null) {
                        guideDialog.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
        




        // 添加聚焦状态监听器
        actionButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    // 获得聚焦
                    if (!isScanning) {
                        v.setBackgroundResource(R.drawable.circle_background_focused);
                    }
                } else {
                    // 失去聚焦
                    if (!isScanning) {
                        v.setBackgroundResource(R.drawable.circle_background);
                    }
                }
            }
        });
        
        // 添加触摸监听器以确保点击时获得焦点
        actionButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.requestFocus();
                }
                return false; // 不消费事件，让onClick处理
            }
        });
        
        // 添加键盘导航支持
        actionButton.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_ENTER:
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                            // 模拟点击
                            v.performClick();
                            return true;
                        case KeyEvent.KEYCODE_DPAD_UP:
                        case KeyEvent.KEYCODE_DPAD_DOWN:
                        case KeyEvent.KEYCODE_DPAD_LEFT:
                        case KeyEvent.KEYCODE_DPAD_RIGHT:
                            // 处理方向键导航
                            return handleDirectionKey(keyCode);
                    }
                }
                return false;
            }
        });
    }
    
    /**
     * 处理方向键导航
     */
    private boolean handleDirectionKey(int keyCode) {
        // 这里可以实现更复杂的键盘导航逻辑
        // 目前仅支持基本的焦点管理
        return false;
    }
    
    /**
     * 暂停测量
     */
    private void pauseMeasuring() {
        isPaused = true;
        statusText.setText(getString(R.string.measuring_paused));
        actionButton.setBackgroundResource(R.drawable.circle_background);
        

    }
    
    /**
     * 恢复测量
     */
    private void resumeMeasuring() {
        isPaused = false;
        statusText.setText("测量中...");
        actionButton.setBackgroundResource(R.drawable.circle_background_error);
        

    }
    
    /**
     * 设置actionButton的背景，考虑聚焦状态
     */
    private void updateActionButtonBackground() {
        if (isMeasuring) {
            if (isPaused) {
                actionButton.setBackgroundResource(R.drawable.circle_background);
            } else {
                actionButton.setBackgroundResource(R.drawable.circle_background_error);
            }
        } else {
            if (actionButton.isFocused()) {
                actionButton.setBackgroundResource(R.drawable.circle_background_focused);
            } else {
                actionButton.setBackgroundResource(R.drawable.circle_background);
            }
        }
    }
    
    /**
     * 执行测量完成视觉反馈动画
     */
    private void performScanCompleteAnimation() {
        if (scanCompleteIndicator != null) {
            // 显示红色正方形
            scanCompleteIndicator.setVisibility(View.VISIBLE);
            
            // 创建旋转动画
            android.view.animation.RotateAnimation rotateAnimation = new android.view.animation.RotateAnimation(
                    0,
                    360,
                    android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                    android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
            );
            
            // 设置动画参数
            rotateAnimation.setDuration(800); // 0.8秒完成旋转
            rotateAnimation.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
            rotateAnimation.setFillAfter(false);
            
            // 设置动画监听器
            rotateAnimation.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
                @Override
                public void onAnimationStart(android.view.animation.Animation animation) {
                }
                
                @Override
                public void onAnimationEnd(android.view.animation.Animation animation) {
                    // 动画结束后隐藏红色正方形
                    scanCompleteIndicator.setVisibility(View.GONE);
                    
                    // 动画结束后显示白色圆形按钮
                    actionButton.setVisibility(View.VISIBLE);
                    
                    // 重置测量状态
                    isMeasuring = false;
                    isPaused = false;
                    
                    // 更新按钮背景，考虑聚焦状态
                    updateActionButtonBackground();
                    
                    // 动画结束后重新请求焦点
                    actionButton.requestFocus();
                    
                    // 1秒后恢复到初始状态
                    mainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            statusText.setText(getString(R.string.camera_ready));
                            // 保持距离显示
                            updateDistanceUI();
                        }
                    }, 1000);
                }
                
                @Override
                public void onAnimationRepeat(android.view.animation.Animation animation) {
                }
            });
            
            // 启动动画
            scanCompleteIndicator.startAnimation(rotateAnimation);
        }
    }
    
    /**
     * 隐藏状态栏
     */
    private void hideStatusBar() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            
            getWindow().getDecorView().setSystemUiVisibility(flags);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        } else {
            // 兼容旧版本
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
        }
    }
    
    /**
     * 显示状态栏
     */
    private void showStatusBar() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            
            getWindow().getDecorView().setSystemUiVisibility(flags);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        } else {
            // 兼容旧版本
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }
    

    
    /**
     * 为PreviewView设置手势检测器
     */
    private void setupPreviewGestureDetector() {
        final GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                // 处理单击聚焦
                // 移动准星到新位置并执行聚焦
                FocusIndicatorManager manager = FocusIndicatorManager.getInstance();
                if (manager.hasIndicator()) {
                    manager.moveTo(e.getX(), e.getY());
                } else {
                    manager.showAt(e.getX(), e.getY(), android.graphics.Color.parseColor("#9C27B0"));
                }
                
                performFocus(e.getX(), e.getY());
                return true;
            }
            
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // 处理双击切换状态栏
                toggleStatusBar();
                return true;
            }
        });
        
        // 为PreviewView设置触摸监听器
        previewView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 始终将事件传递给GestureDetector
                gestureDetector.onTouchEvent(event);
                
                // 返回true表示事件已被消费，这样后续事件（MOVE、UP）才会继续传递到这里
                return true;
            }
        });
        

    }
    
    /**
     * 切换状态栏显示/隐藏
     */
    private void toggleStatusBar() {
        int currentFlags = getWindow().getDecorView().getSystemUiVisibility();
        if ((currentFlags & View.SYSTEM_UI_FLAG_FULLSCREEN) == View.SYSTEM_UI_FLAG_FULLSCREEN) {
            // 当前是全屏模式，显示状态栏
            showStatusBar();
        } else {
            // 当前不是全屏模式，隐藏状态栏
            hideStatusBar();
        }
    }
    
    /**
     * 执行相机聚焦操作
     */
    private void performFocus(float x, float y) {
        // 确保准星显示在点击位置
        FocusIndicatorManager indicatorManager = FocusIndicatorManager.getInstance();
        if (!indicatorManager.hasIndicator()) {
            indicatorManager.showAt(x, y, android.graphics.Color.parseColor("#9C27B0"));
        }
        
        if (camera != null) {
            try {
                // 立即显示屏幕点击指示器，提供即时反馈
                showScreenClickIndicator(x, y);
                

                
                // 显示对焦框动画，模拟相机聚焦过程
                showFocusAnimation(x, y);
                
                // 使用PreviewView的MeteringPointFactory将屏幕坐标转换为相机坐标
                MeteringPointFactory factory = previewView.getMeteringPointFactory();
                MeteringPoint point = factory.createPoint(x, y);
                
                // 创建聚焦操作，使用AF模式
                FocusMeteringAction action = new FocusMeteringAction.Builder(
                        point, FocusMeteringAction.FLAG_AF)
                        .setAutoCancelDuration(3000, TimeUnit.MILLISECONDS) // 3秒后自动取消
                        .build();
                
                // 执行聚焦，并添加回调监听聚焦结果
                try {
                    camera.getCameraControl().startFocusAndMetering(action);
                    
                    // 延迟显示聚焦成功提示，模拟真实聚焦过程
                    // 实际应用中应该使用FocusMeteringAction的回调
                    mainHandler.postDelayed(() -> {
                        showFocusSuccessIndicator(x, y);
                        // 更新准星颜色为白色，表示聚焦成功
                        indicatorManager.updateColor(Color.WHITE);
                    }, 200);
                    
                } catch (UnsupportedOperationException e) {
                    // 相机不支持手动聚焦
                    showFocusFailedIndicator(x, y);
                    // 更新准星颜色为白色，表示聚焦失败
                    indicatorManager.updateColor(Color.WHITE);
                } catch (Exception e) {
                    // 其他聚焦异常
                    showFocusFailedIndicator(x, y);
                    // 更新准星颜色为白色，表示聚焦失败
                    indicatorManager.updateColor(Color.WHITE);
                }
                
            } catch (Exception e) {
                showFocusFailedIndicator(x, y);
                // 更新准星颜色为白色，表示聚焦失败
                indicatorManager.updateColor(Color.WHITE);
            }
        } else {
            // 相机对象为null，无法执行聚焦
            indicatorManager.updateColor(Color.WHITE);
        }
    }
    
    /**
     * 显示聚焦成功指示器
     */
    private void showFocusSuccessIndicator(float x, float y) {
        // 创建聚焦成功指示器视图
        View successIndicator = getLayoutInflater().inflate(R.layout.focus_success_indicator, null);
        
        // 获取父布局
        ViewGroup rootLayout = findViewById(android.R.id.content);
        RelativeLayout parent;
        if (rootLayout.getChildAt(0) instanceof RelativeLayout) {
            parent = (RelativeLayout) rootLayout.getChildAt(0);
        } else {
            parent = new RelativeLayout(this);
            rootLayout.addView(parent);
        }
        
        // 设置指示器位置
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                80, 80);
        params.leftMargin = (int) (x - 40);
        params.topMargin = (int) (y - 40);
        
        // 添加到布局
        parent.addView(successIndicator, params);
        
        // 加载并启动成功动画
        android.view.animation.Animation animation = android.view.animation.AnimationUtils.loadAnimation(
                this, R.anim.focus_success_animation);
        
        // 设置动画监听器，动画结束后移除视图
        animation.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
            @Override
            public void onAnimationStart(android.view.animation.Animation animation) {}
            
            @Override
            public void onAnimationEnd(android.view.animation.Animation animation) {
                parent.removeView(successIndicator);
            }
            
            @Override
            public void onAnimationRepeat(android.view.animation.Animation animation) {}
        });
        
        // 启动动画
        successIndicator.startAnimation(animation);
    }
    
    /**
     * 显示聚焦失败指示器
     */
    private void showFocusFailedIndicator(float x, float y) {
        // 这里可以实现聚焦失败的视觉提示
        // 例如显示红色指示器或错误动画
    }
    
    /**
     * 显示屏幕点击指示器
     */
    private void showScreenClickIndicator(float x, float y) {
        // 创建屏幕点击指示器视图
        View clickIndicator = getLayoutInflater().inflate(R.layout.screen_click_indicator, null);
        
        // 计算指示器位置
        RelativeLayout parent;
        ViewGroup rootLayout = findViewById(android.R.id.content);
        if (rootLayout.getChildAt(0) instanceof RelativeLayout) {
            parent = (RelativeLayout) rootLayout.getChildAt(0);
        } else {
            parent = new RelativeLayout(this);
            rootLayout.addView(parent);
        }
        
        // 设置指示器位置
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                40, 40);
        params.leftMargin = (int) (x - 20);
        params.topMargin = (int) (y - 20);
        
        // 添加到布局
        parent.addView(clickIndicator, params);
        
        // 加载并启动动画
        android.view.animation.Animation animation = android.view.animation.AnimationUtils.loadAnimation(
                this, R.anim.screen_click_animation);
        
        // 设置动画监听器，动画结束后移除视图
        animation.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
            @Override
            public void onAnimationStart(android.view.animation.Animation animation) {}
            
            @Override
            public void onAnimationEnd(android.view.animation.Animation animation) {
                parent.removeView(clickIndicator);
            }
            
            @Override
            public void onAnimationRepeat(android.view.animation.Animation animation) {}
        });
        
        // 启动动画
        clickIndicator.startAnimation(animation);
    }
    
    /**
     * 显示对焦框动画
     */
    private void showFocusAnimation(float x, float y) {
        // 创建对焦框视图
        View focusFrame = getLayoutInflater().inflate(R.layout.focus_frame, null);
        
        // 计算对焦框位置
        RelativeLayout parent;
        ViewGroup rootLayout = findViewById(android.R.id.content);
        if (rootLayout.getChildAt(0) instanceof RelativeLayout) {
            parent = (RelativeLayout) rootLayout.getChildAt(0);
        } else {
            parent = new RelativeLayout(this);
            rootLayout.addView(parent);
        }
        
        // 设置对焦框位置
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                80, 80);
        params.leftMargin = (int) (x - 40);
        params.topMargin = (int) (y - 40);
        
        // 添加到布局
        parent.addView(focusFrame, params);
        
        // 加载并启动动画
        android.view.animation.Animation animation = android.view.animation.AnimationUtils.loadAnimation(
                this, R.anim.focus_animation);
        
        // 设置动画监听器，动画结束后移除视图
        animation.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
            @Override
            public void onAnimationStart(android.view.animation.Animation animation) {}
            
            @Override
            public void onAnimationEnd(android.view.animation.Animation animation) {
                parent.removeView(focusFrame);
            }
            
            @Override
            public void onAnimationRepeat(android.view.animation.Animation animation) {}
        });
        
        // 启动动画
        focusFrame.startAnimation(animation);
    }

    /**
     * 检查相机权限
     * @return 是否有权限
     */
    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 请求相机权限
     */
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    }

    /**
     * 处理权限请求结果
     * @param requestCode 请求码
     * @param permissions 权限列表
     * @param grantResults 授权结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraPreview();
            } else {
                // 权限被拒绝，显示错误信息
                statusText.setText("摄像头权限被拒绝，请在设置中开启");
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予，打开相册
                openGallery();
            } else {
                // 权限被拒绝，显示错误信息
                statusText.setText("存储权限被拒绝，无法访问相册");
            }
        }
    }
    


    /**
     * 启动相机预览
     */
    private void startCameraPreview() {
        // 初始化 CameraManager
        cameraManager = new CameraManager(this, this, previewView);
        
        // 设置帧可用监听器
        cameraManager.setOnFrameAvailableListener(new com.example.shunxing.camera.CameraManager.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(Bitmap bitmap, int rotationDegrees) {
                // 保存当前帧的Bitmap
                currentFrameBitmap = bitmap;
                
                // 在后台线程处理帧
                processingExecutor.execute(() -> {
                    try {
                        // 检测姿势
                        com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult result = poseDetector.detectPose(bitmap, rotationDegrees);
                        
                        // 计算距离
                        calculateDistance(result, bitmap.getWidth(), bitmap.getHeight());
                        
                        // 实时更新距离信息
                        runOnUiThread(() -> {
                            if (isMeasuring) {
                                // 测量状态下显示完整信息
                                String distanceText = estimatedDistance > 0 ? String.format("%.2f m", estimatedDistance) : getString(R.string.detecting);
                                String angleText = String.format("方位角: %.1f°\n俯仰角: %.1f°\n横滚角: %.1f°\n距离: %s", 
                                        angleDifference[0], angleDifference[1], angleDifference[2], distanceText);
                                
                                // 检查是否检测到腰部
                                boolean waistDetected = isWaistDetected(result);
                                // 检查腰部是否对准中心
                                boolean isAligned = isWaistAligned(result, bitmap.getWidth(), bitmap.getHeight());
                                
                                // 更新中心指示器
                                updateCenterIndicator(waistDetected, isAligned);
                                
                                if (!waistDetected) {
                                    statusText.setText("识别失败，请正确拍摄腰部！\n" + angleText);
                                } else if (!isAligned) {
                                    statusText.setText("请将腰部对准中心！\n" + angleText);
                                } else {
                                    // 处理姿势结果
                                    processPoseResult(result, bitmap.getWidth(), bitmap.getHeight());
                                    
                                    // 保存关键帧
                                    if (isMeasuring && waistSize > 0 && keyFrames.size() < MAX_KEY_FRAMES) {
                                        keyFrames.add(bitmap.copy(Bitmap.Config.ARGB_8888, false));
                                    }
                                    
                                    // 检测角度变化，触发拍照
                                    checkAngleAndCapture();
                                }
                            } else {
                                // 非测量状态下只显示距离信息
                                updateDistanceUI();
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing frame: " + e.getMessage());
                    }
                });
            }
            
            @Override
            public void onFrameError(Exception e) {
                Log.e(TAG, "Frame error from camera analyzer: " + e.getMessage());
                // 在这里处理分析器内部的异常，例如显示错误信息给用户
                runOnUiThread(() -> {
                    statusText.setText("相机分析器错误: " + e.getMessage());
                });
            }
        });
        
        // 初始化相机
        cameraManager.initializeCamera(new com.example.shunxing.camera.CameraManager.InitializeCallback() {
            @Override
            public void onSuccess() {
                if (BuildConfig.DEBUG) Log.d(TAG, "Camera initialized successfully");
                // 根据设备性能自动选择合适的分辨率
                cameraManager.autoSelectResolution();
                // 获取焦距像素值
                focalLengthPixels = cameraManager.getFocalLengthPixels();
                // 获取相机对象
                camera = cameraManager.getCamera();
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error initializing camera: " + e.getMessage());
                runOnUiThread(() -> {
                    statusText.setText("相机初始化失败: " + e.getMessage());
                });
            }
        });
    }



    /**
     * 更新距离UI显示
     */
    private void updateDistanceUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String distanceStr = estimatedDistance > 0 ? String.format("%.2f m", estimatedDistance) : getString(R.string.detecting);
                // 在界面正中间显示距离
                if (distanceText != null) {
                    distanceText.setText("距离: " + distanceStr);
                    // 根据距离设置颜色反馈
                    if (estimatedDistance > 0) {
                        if (estimatedDistance >= 1.0f && estimatedDistance <= 2.0f) {
                            // 距离合适
                            distanceText.setTextColor(Color.GREEN);
                            updateStatusSystem(StatusType.SUCCESS, "距离合适，请保持姿势", "请将腰部对准中心，点击开始测量");
                            clearError();
                        } else if (estimatedDistance >= 0.5f && estimatedDistance < 1.0f) {
                            // 距离接近
                            distanceText.setTextColor(Color.YELLOW);
                            updateStatusSystem(StatusType.WARNING, "距离接近，请后退一点", "保持身体稳定，调整到合适距离");
                        } else {
                            // 距离不合适
                            distanceText.setTextColor(Color.RED);
                            if (estimatedDistance < 0.5f) {
                                showError(ErrorType.DISTANCE_TOO_CLOSE);
                            } else {
                                showError(ErrorType.DISTANCE_TOO_FAR);
                            }
                        }
                    } else {
                        // 距离检测中
                        distanceText.setTextColor(Color.WHITE);
                        updateStatusSystem(StatusType.INFO, getString(R.string.camera_ready), "请站在1-2米处，将腰部对准中心");
                    }
                }
            }
        });
    }
    
    /**
     * 处理姿势检测结果
     * @param result 姿势检测结果
     * @param width 图像宽度
     * @param height 图像高度
     */
    private void processPoseResult(com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult result, int width, int height) {
        try {
            // 检查结果是否有效
            if (result == null || result.landmarks().isEmpty()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isMeasuring) {
                            statusText.setText("测量中...\n未检测到人体，请确保身体在画面中");
                        } else {
                            statusText.setText("请站在摄像头前，确保全身在画面中");
                        }
                    }
                });
                return;
            }
            
            // 检测姿势
            if (!isMeasuring) {
                detectPosture(result);
            }
            
            // 计算距离
            try {
                calculateDistance(result, width, height);
                if (estimatedDistance <= 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isMeasuring) {
                                statusText.setText("测量中...\n距离检测失败，请确保全身在画面中");
                            }
                        }
                    });
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calculating distance: " + e.getMessage());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isMeasuring) {
                            statusText.setText("测量中...\n距离计算失败，请重试");
                        }
                    }
                });
                return;
            }
            
            // 使用PoseDetector计算腰部尺寸
            float calculatedWaistSize = 0.0f;
            float calculatedWaistHeight = 0.0f;
            float calculatedWaistCurvature = 0.0f;
            
            try {
                calculatedWaistSize = poseDetector.calculateWaistSize(result, width, height, focalLengthPixels, estimatedDistance);
                calculatedWaistHeight = poseDetector.calculateWaistHeight(result, width, height, focalLengthPixels, estimatedDistance);
                calculatedWaistCurvature = poseDetector.calculateWaistCurvature(result, width, height);
                
                if (calculatedWaistSize <= 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isMeasuring) {
                                statusText.setText("测量中...\n无法检测到腰部，请调整姿势");
                            }
                        }
                    });
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calculating waist measurements: " + e.getMessage());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isMeasuring) {
                            statusText.setText("测量中...\n计算腰围失败，请重试");
                        }
                    }
                });
                return;
            }
            
            // 更新腰部尺寸、高度和弧度
            if (calculatedWaistSize > 0) {
                waistSize = calculatedWaistSize;
            }
            if (calculatedWaistHeight > 0) {
                waistHeight = calculatedWaistHeight;
            }
            if (calculatedWaistCurvature > 0) {
                waistCurvature = calculatedWaistCurvature;
            }
            
            // 在主线程更新UI
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isMeasuring) {
                        // 更新状态文本，显示腰围数据和角度信息
                        String distanceText = estimatedDistance > 0 ? String.format("%.2f m", estimatedDistance) : "检测中";
                        String angleText = String.format("方位角: %.1f°\n俯仰角: %.1f°\n横滚角: %.1f°\n距离: %s", 
                                angleDifference[0], angleDifference[1], angleDifference[2], distanceText);
                        statusText.setText("测量中...\n腰围: " + String.format("%.1f", waistSize) + " cm\n" + angleText);
                        if (BuildConfig.DEBUG) Log.d(TAG, "UI updated with waist: " + waistSize + " cm, distance: " + distanceText);
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing pose result: " + e.getMessage());
            
            // 在主线程更新UI
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isMeasuring) {
                        statusText.setText("测量中...\n系统错误，请重试");
                    }
                }
            });
        }
    }
    
    /**
     * 计算距离
     * @param result 姿势检测结果
     * @param width 图像宽度
     * @param height 图像高度
     */
    private void calculateDistance(com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult result, int width, int height) {
        try {
            if (BuildConfig.DEBUG) Log.d(TAG, "calculateDistance called with width: " + width + ", height: " + height);
            if (BuildConfig.DEBUG) Log.d(TAG, "focalLengthPixels: " + focalLengthPixels);
            
            if (result == null || result.landmarks().isEmpty()) {
                if (BuildConfig.DEBUG) Log.d(TAG, "No pose landmarks detected");
                estimatedDistance = 0.0f;
                return;
            }
            
            // 获取第一个人的关键点
            java.util.List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark> landmarks = result.landmarks().get(0);
            
            if (BuildConfig.DEBUG) Log.d(TAG, "Number of landmarks: " + landmarks.size());
            
            // 检查是否有足够的关键点
            if (landmarks.size() < 25) {
                if (BuildConfig.DEBUG) Log.d(TAG, "Not enough landmarks: " + landmarks.size());
                estimatedDistance = 0.0f;
                return;
            }
            
            // 提取左右髋关键点（索引23、24）
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark leftHip = landmarks.get(23);
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark rightHip = landmarks.get(24);
            
            // 检查关键点是否有效
            if (leftHip == null || rightHip == null) {
                if (BuildConfig.DEBUG) Log.d(TAG, "Invalid hip landmarks: leftHip=" + leftHip + ", rightHip=" + rightHip);
                estimatedDistance = 0.0f;
                return;
            }
            
            // 计算髋部像素宽度
            float leftHipX = leftHip.x() * width;
            float rightHipX = rightHip.x() * width;
            float hipPixelWidth = Math.abs(rightHipX - leftHipX);
            
            if (BuildConfig.DEBUG) Log.d(TAG, "Hip pixel width: " + hipPixelWidth + ", leftHipX: " + leftHipX + ", rightHipX: " + rightHipX);
            if (BuildConfig.DEBUG) Log.d(TAG, "Left hip normalized coordinates: x=" + leftHip.x() + ", y=" + leftHip.y());
            if (BuildConfig.DEBUG) Log.d(TAG, "Right hip normalized coordinates: x=" + rightHip.x() + ", y=" + rightHip.y());
            
            // 计算距离
            if (hipPixelWidth > 0 && focalLengthPixels > 0) {
                // 使用用户身高计算骨盆宽度
                float pelvisActualWidth = (userHeight * DEFAULT_PELVIS_WIDTH_RATIO) / 100.0f; // 转为米
                
                float newDistance = (pelvisActualWidth * focalLengthPixels) / hipPixelWidth;
                // 限制距离范围（0.1米到5米）
                newDistance = Math.max(0.1f, Math.min(5.0f, newDistance));
                
                // 应用距离平滑滤波
                estimatedDistance = smoothDistance(newDistance);
                
                if (BuildConfig.DEBUG) Log.d(TAG, "Distance calculated: " + estimatedDistance + " m");
                if (BuildConfig.DEBUG) Log.d(TAG, "Calculation details: pelvisActualWidth=" + pelvisActualWidth + " m, focalLengthPixels=" + focalLengthPixels + ", hipPixelWidth=" + hipPixelWidth);
            } else {
                if (BuildConfig.DEBUG) Log.d(TAG, "Cannot calculate distance: hipPixelWidth=" + hipPixelWidth + ", focalLengthPixels=" + focalLengthPixels);
                estimatedDistance = 0.0f;
            }
            
            // 更新距离显示
            updateDistanceUI();
        } catch (Exception e) {
            Log.e(TAG, "Error calculating distance: " + e.getMessage());
            e.printStackTrace();
            estimatedDistance = 0.0f;
        }
    }
    
    /**
     * 平滑距离值，减少波动
     * @param newDistance 新计算的距离
     * @return 平滑后的距离
     */
    private float smoothDistance(float newDistance) {
        if (distanceHistory.size() >= 5) {
            distanceHistory.remove(0);
        }
        distanceHistory.add(newDistance);
        
        float sum = 0;
        for (float d : distanceHistory) {
            sum += d;
        }
        return sum / distanceHistory.size();
    }
    
    /**
     * 检查角度变化并触发拍照
     */
    private void checkAngleAndCapture() {
        if (!isMeasuring || isPhotoPending || estimatedDistance <= 0.5f) {
            return;
        }
        
        // 使用方位角（水平旋转）来判断
        float currentAzimuth = angleDifference[0];
        
        // 首次开始拍照时记录初始角度
        if (!hasStartedCapturing) {
            lastCaptureAngle = currentAzimuth;
            hasStartedCapturing = true;
            return;
        }
        
        // 计算与上次拍照的角度差
        float angleDelta = Math.abs(currentAzimuth - lastCaptureAngle);
        // 处理角度环绕（例如从 350° 到 10°）
        if (angleDelta > 180f) {
            angleDelta = 360f - angleDelta;
        }
        
        // 当角度变化超过阈值时触发拍照
        if (angleDelta >= ANGLE_CAPTURE_THRESHOLD) {
            capturePhoto();
            lastCaptureAngle = currentAzimuth;
            updateCaptureProgressUI();
        }
    }
    
    /**
     * 更新拍照进度UI
     */
    private void updateCaptureProgressUI() {
        runOnUiThread(() -> {
            float progress = (float) capturedPhotoCount / targetPhotoCount * 100;
            float currentAngle = Math.abs(angleDifference[0]);
            float remainingAngle = Math.max(0, 360f - currentAngle);
            
            String status = String.format("已拍摄 %d/%d 张 (%.0f%%)\n", 
                capturedPhotoCount, targetPhotoCount, progress);
            
            if (capturedPhotoCount < targetPhotoCount) {
                status += String.format("当前角度: %.0f°\n还需旋转: %.0f°\n请继续缓慢旋转手机", 
                    currentAngle, remainingAngle);
            } else {
                status += "拍照完成！正在处理...";
                // 隐藏进度条
                if (findViewById(R.id.progressContainer) != null) {
                    findViewById(R.id.progressContainer).setVisibility(View.GONE);
                }
            }
            
            // 更新状态系统
            updateStatusSystem(StatusType.INFO, status, "保持手机稳定，继续旋转");
            
            // 更新进度环
            if (captureProgressBar != null && tvProgressPercent != null) {
                captureProgressBar.setProgress((int) progress);
                tvProgressPercent.setText(String.format("%.0f%%", progress));
            }
        });
    }
    
    /**
     * 拍照方法
     */
    private void capturePhoto() {
        if (currentFrameBitmap == null) {
            return;
        }
        
        isPhotoPending = true;
        
        // 从当前帧获取Bitmap
        Bitmap photo = currentFrameBitmap.copy(Bitmap.Config.ARGB_8888, true);
        capturedPhotos.add(photo);
        capturedPhotoCount++;
        
        // 更新拍照进度
        updatePhotoProgress();



        // 检查是否达到目标照片数
        if (capturedPhotoCount >= targetPhotoCount) {
            finishPhotoCapture();
        }

        // 重置标志，允许下一张拍照
        isPhotoPending = false;
    }
    
    /**
     * 更新拍照进度
     */
    private void updatePhotoProgress() {
        runOnUiThread(() -> {
            String progress = String.format("已拍 %d/%d 张", capturedPhotoCount, targetPhotoCount);
            statusText.setText(progress);
        });
    }
    
    /**
     * 结束拍照并开始建模
     */
    private void finishPhotoCapture() {
        isMeasuring = false;
        statusText.setText("正在准备建模...");
        
        // 保存测量记录
        saveMeasurementRecord();
        
        // 保存照片并开始建模
        savePhotosAndStartModeling();
    }
    
    /**
     * 保存测量记录
     */
    private void saveMeasurementRecord() {
        if (waistSize > 0) {
            try {
                // 检查recordManager是否初始化
                if (recordManager != null) {
                    // 异步获取记录列表以生成新记录ID
                    recordManager.getAllRecordsAsync(new MeasurementRecordManager.RecordsCallback() {
                        @Override
                        public void onResult(java.util.List<MeasurementRecord> records) {
                            try {
                                final int recordId = (records != null ? records.size() : 0) + 1;
                                
                                // 保存关键帧
                                final ArrayList<String> imagePaths = saveKeyFrames();
                                
                                // 创建并保存测量记录
                                MeasurementRecord record = new MeasurementRecord(recordId, waistSize, waistHeight, waistCurvature);
                                // 设置imagePath
                                if (!imagePaths.isEmpty()) {
                                    record.setImagePath(imagePaths.get(0));
                                }
                                recordManager.saveRecord(record);
                                
                                // 添加到主界面的项目列表中
                                try {
                                    final java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                                    String dateStr = sdf.format(record.getTimestamp());
                                    // 检查资源ID是否存在
                                    int logoResId = R.drawable.logo;
                                    Project project = new Project(getString(R.string.measurement_record_prefix) + recordId, dateStr, logoResId);
                                    // 检查ProjectManager是否初始化
                                    ProjectManager projectManager = ProjectManager.getInstance();
                                    if (projectManager != null) {
                                        // 清空旧的模拟数据，只保留真实记录
                                        projectManager.clearProjects();
                                        // 添加新的测量记录
                                        projectManager.addProject(project);
                                        // 重新加载所有真实记录，确保日期正确
                                        recordManager.getAllRecordsAsync(new MeasurementRecordManager.RecordsCallback() {
                                            @Override
                                            public void onResult(java.util.List<MeasurementRecord> allRecords) {
                                                try {
                                                    for (int i = 0; i < allRecords.size(); i++) {
                                                        MeasurementRecord existingRecord = allRecords.get(i);
                                                        if (existingRecord.getId() != recordId) {
                                                            String existingDateStr = sdf.format(existingRecord.getTimestamp());
                                                            Project existingProject = new Project(getString(R.string.measurement_record_prefix) + existingRecord.getId(), existingDateStr, R.drawable.logo);
                                                            ProjectManager.getInstance().addProject(existingProject);
                                                        }
                                                    }
                                                } catch (Exception e) {
                                                    Log.e(TAG, "Error processing all records: " + e.getMessage());
                                                }
                                            }
                                        });
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error adding project to list: " + e.getMessage());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing records: " + e.getMessage());
                            }
                        }
                    });
                } else {
                    Log.e(TAG, "recordManager is not initialized");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving measurement record: " + e.getMessage());
            }
        }
    }
    
    /**
     * 保存照片到文件并开始建模
     */
    private void savePhotosAndStartModeling() {
        processingExecutor.execute(() -> {
            try {
                // 保存照片到文件
                java.util.List<String> imagePaths = new java.util.ArrayList<>();
                for (Bitmap photo : capturedPhotos) {
                    String path = savePhotoToFile(photo);
                    if (path != null) {
                        imagePaths.add(path);
                    }
                }
                
                // 清空照片列表，释放内存
                for (Bitmap photo : capturedPhotos) {
                    photo.recycle();
                }
                capturedPhotos.clear();
                
                // 检查是否有足够的照片
                if (imagePaths.isEmpty()) {
                    runOnUiThread(() -> {
                        statusText.setText("未拍摄到足够的照片，请重试");
                        actionButton.setVisibility(View.VISIBLE);
                        actionButton.setBackgroundResource(R.drawable.circle_background);
                    });
                    return;
                }
                
                // 开始建模
                startModelGeneration(imagePaths);
                
            } catch (Exception e) {
                Log.e(TAG, "Error saving photos: " + e.getMessage());
                runOnUiThread(() -> {
                    statusText.setText("保存照片失败，请重试");
                    actionButton.setVisibility(View.VISIBLE);
                    actionButton.setBackgroundResource(R.drawable.circle_background);
                });
            }
        });
    }
    
    /**
     * 保存照片到文件
     * @param bitmap 照片Bitmap
     * @return 保存的文件路径
     */
    private String savePhotoToFile(Bitmap bitmap) {
        File dir = new File(getExternalFilesDir("captured_photos"), "waist");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fileName = "photo_" + System.currentTimeMillis() + ".jpg";
        File file = new File(dir, fileName);
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Error saving photo: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 【修复】真正调用 KiriApiManager 进行3D建模
     */
    private void startModelGeneration(java.util.List<String> imagePaths) {
        runOnUiThread(() -> {
            statusText.setText("正在上传图片到云端...");
            actionButton.setVisibility(View.GONE);
        });
        
        // 获取 KiriApiManager 实例并调用
        KiriApiManager kiriApiManager = KiriApiManager.getInstance(this);
        
        kiriApiManager.generate3DModel(imagePaths, new KiriApiManager.ModelGenerationCallback() {
            @Override
            public void onProgress(String status) {
                runOnUiThread(() -> statusText.setText(status));
            }
            
            @Override
            public void onSuccess(String modelPath) {
                runOnUiThread(() -> {
                    statusText.setText("模型生成完成！");
                    android.widget.Toast.makeText(MainCameraActivity.this, "3D模型生成成功！", android.widget.Toast.LENGTH_SHORT).show();
                    
                    // 跳转到结果页面
                    android.content.Intent intent = new android.content.Intent(MainCameraActivity.this, ResultActivity.class);
                    intent.putExtra("waist_size", waistSize);
                    intent.putExtra("waist_height", waistHeight);
                    intent.putExtra("waist_curvature", waistCurvature);
                    intent.putExtra("model_path", modelPath);
                    intent.putStringArrayListExtra("image_paths", new java.util.ArrayList<>(imagePaths));
                    startActivity(intent);
                });
            }
            
            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    statusText.setText("模型生成失败: " + error);
                    actionButton.setVisibility(View.VISIBLE);
                    actionButton.setBackgroundResource(R.drawable.circle_background);
                    android.widget.Toast.makeText(MainCameraActivity.this, 
                        "模型生成失败: " + error, android.widget.Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * 开始测量
     */
    private void startMeasuring() {
        // 开始测量
        isMeasuring = true;
        isPaused = false;
        capturedPhotoCount = 0;
        capturedPhotos.clear();
        statusText.setText(getString(R.string.angle_aligned));
        actionButton.setBackgroundResource(R.drawable.circle_background_error);
        
        // 保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // 显示进度条
        if (findViewById(R.id.progressContainer) != null) {
            findViewById(R.id.progressContainer).setVisibility(View.VISIBLE);
            if (captureProgressBar != null && tvProgressPercent != null) {
                captureProgressBar.setProgress(0);
                tvProgressPercent.setText("0%");
            }
        }
        
        // 测量过程 - 提示用户开始旋转手机
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isMeasuring) {
                    statusText.setText("请缓慢旋转手机，环绕腰部拍摄\n系统将自动拍照");
                }
            }
        }, 1000);
    }
    

    
    /**
     * 开始测量倒计时
     */
    private void startMeasurementCountdown() {
        // 倒计时3秒
        final int[] countdown = {3};
        statusText.setText("准备测量... " + countdown[0]);
        
        final android.os.CountDownTimer countDownTimer = new android.os.CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                countdown[0]--;
                statusText.setText("准备测量... " + countdown[0]);
                

            }
            
            @Override
            public void onFinish() {
                statusText.setText("正在测量...");
                // 倒计时结束，开始实际测量
            }
        };
        
        countDownTimer.start();
    }

    /**
     * 停止测量
     */
    private void stopMeasuring() {
        isMeasuring = false;
        isPaused = false;
        statusText.setText(getString(R.string.camera_ready));
        actionButton.setBackgroundResource(R.drawable.circle_background);
        
        // 隐藏进度条
        if (findViewById(R.id.progressContainer) != null) {
            findViewById(R.id.progressContainer).setVisibility(View.GONE);
        }
        
        // 移除屏幕常亮标志
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * 初始化传感器管理器
     */
    private void initSensorManager() {
        // 获取传感器管理器
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        
        // 获取旋转矢量传感器
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        // 获取加速度传感器
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        
        if (rotationVectorSensor != null && accelerometerSensor != null) {
            // 创建传感器监听器
            sensorEventListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                        // 将旋转矢量转换为四元数
                        float[] rotationMatrix = new float[9];
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                        
                        // 将旋转矩阵转换为欧拉角
                        float[] orientation = new float[3];
                        SensorManager.getOrientation(rotationMatrix, orientation);
                        
                        // 将弧度转换为角度
                        currentAngle[0] = (float) Math.toDegrees(orientation[0]); // 方位角
                        currentAngle[1] = (float) Math.toDegrees(orientation[1]); // 俯仰角
                        currentAngle[2] = (float) Math.toDegrees(orientation[2]); // 横滚角
                        
                        // 如果baseAngle未设置，则设置为currentAngle
                        if (!isBaseAngleSet) {
                            System.arraycopy(currentAngle, 0, baseAngle, 0, 3);
                            isBaseAngleSet = true;
                        }
                        
                        // 计算angleDifference
                        angleDifference[0] = currentAngle[0] - baseAngle[0];
                        angleDifference[1] = currentAngle[1] - baseAngle[1];
                        angleDifference[2] = currentAngle[2] - baseAngle[2];
                        
                        // 将angleDifference归一化到 -180° 到 180° 范围
                        for (int i = 0; i < 3; i++) {
                            while (angleDifference[i] > 180) {
                                angleDifference[i] -= 360;
                            }
                            while (angleDifference[i] < -180) {
                                angleDifference[i] += 360;
                            }
                        }
                        
                        // 检查角度阈值
                        checkAngleThreshold();
                        
                        // 添加到角度历史记录
                        addToAngleHistory();
                        
                        // 在UI上显示angleDifference
                        updateAngleDisplay();
                    } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                        // 检测手机晃动
                        detectShake(event);
                    }
                }
                
                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                    // 精度变化时的处理
                }
            };
            
            // 注册传感器监听器
            sensorManager.registerListener(sensorEventListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Log.e(TAG, "Sensors not available");
        }
    }
    
    /**
     * 检查角度阈值
     */
    private void checkAngleThreshold() {
        // 检查任何一个angleDifference是否超过阈值
        boolean thresholdReached = false;
        for (float angle : angleDifference) {
            if (Math.abs(angle) >= ANGLE_THRESHOLD) {
                thresholdReached = true;
                break;
            }
        }
        
        // 如果阈值被达到且之前未达到，则触发提示
        if (thresholdReached && !isAngleThresholdReached) {
            isAngleThresholdReached = true;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // 显示角度阈值达到提示
                    statusText.setText("角度阈值达到！请保持此角度拍摄\n" + 
                            String.format("方位角: %.1f°\n俯仰角: %.1f°\n横滚角: %.1f°", 
                                    angleDifference[0], angleDifference[1], angleDifference[2]));
                    

                }
            });
        } else if (!thresholdReached) {
            isAngleThresholdReached = false;
        }
    }
    

    
    /**
     * 添加到角度历史记录
     */
    private void addToAngleHistory() {
        // 创建currentAngle的副本
        float[] angleCopy = new float[3];
        System.arraycopy(angleDifference, 0, angleCopy, 0, 3);
        
        // 添加到历史记录
        angleHistory.add(angleCopy);
        
        // 限制历史记录数量
        if (angleHistory.size() > MAX_HISTORY_SIZE) {
            angleHistory.remove(0);
        }
    }
    
    /**
     * 更新角度显示
     */
    private void updateAngleDisplay() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (statusText != null) {
                    // 在状态文本中显示angleDifference和距离
                    String distanceText = estimatedDistance > 0 ? String.format("%.2f m", estimatedDistance) : "检测中";
                    String angleText = String.format("方位角: %.1f°\n俯仰角: %.1f°\n横滚角: %.1f°\n距离: %s", 
                            angleDifference[0], angleDifference[1], angleDifference[2], distanceText);
                    
                    if (BuildConfig.DEBUG) Log.d(TAG, "updateAngleDisplay called, estimatedDistance: " + estimatedDistance);
                    if (BuildConfig.DEBUG) Log.d(TAG, "Angle text: " + angleText);
                    
                    // 如果正在测量，则显示测量提示和腰围数据，否则显示角度信息
                    if (isMeasuring) {
                        if (waistSize > 0) {
                            // 显示腰围测量数据和角度信息
                            statusText.setText("测量中...\n腰围: " + String.format("%.1f", waistSize) + " cm\n" + angleText);
                            if (BuildConfig.DEBUG) Log.d(TAG, "Displaying waist size and angle info: " + waistSize + " cm, " + angleText);
                        } else {
                            statusText.setText("请将腰部对准摄像头中心...\n" + angleText);
                            if (BuildConfig.DEBUG) Log.d(TAG, "Displaying alignment prompt and angle info: " + angleText);
                        }
                    } else {
                        statusText.setText(getString(R.string.camera_ready) + "\n" + angleText);
                        if (BuildConfig.DEBUG) Log.d(TAG, "Displaying ready prompt and angle info: " + angleText);
                    }
                }
            }
        });
    }
    
    /**
     * 显示角度历史记录
     */
    private void showAngleHistory() {
        if (angleHistory.isEmpty()) {
            return;
        }
        
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 创建历史记录对话框
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainCameraActivity.this);
                builder.setTitle(getString(R.string.angle_history_title));
                
                // 构建历史记录文本
                StringBuilder historyText = new StringBuilder();
                int count = 0;
                for (int i = angleHistory.size() - 1; i >= 0 && count < 10; i--) {
                    float[] angles = angleHistory.get(i);
                    historyText.append(String.format("#%d: 方位角: %.1f°, 俯仰角: %.1f°, 横滚角: %.1f°\n", 
                            count + 1, angles[0], angles[1], angles[2]));
                    count++;
                }
                
                builder.setMessage(historyText.toString());
                builder.setPositiveButton(getString(R.string.confirm), null);
                builder.show();
            }
        });
    }
    
    /**
     * 检测错误并提供纠正建议
     */
    private void detectErrors() {
        // 检测距离错误
        if (estimatedDistance > 0) {
            if (estimatedDistance < 0.5f) {
                showError(ErrorType.DISTANCE_TOO_CLOSE);
                return;
            } else if (estimatedDistance > 2.5f) {
                showError(ErrorType.DISTANCE_TOO_FAR);
                return;
            }
        }
        
        // 检测姿势错误
        // 这里可以添加姿势检测逻辑
        
        // 检测光线错误
        // 这里可以添加光线检测逻辑
        
        // 检测晃动错误
        // 这里可以添加晃动检测逻辑
    }
    
    /**
     * 显示错误提示
     * @param errorType 错误类型
     */
    private void showError(ErrorType errorType) {
        runOnUiThread(() -> {
            String errorMessage = "";
            String correctionAdvice = "";
            int iconResId = R.drawable.ic_info;
            
            switch (errorType) {
                case DISTANCE_TOO_CLOSE:
                    errorMessage = "距离过近"; 
                    correctionAdvice = "请后退到1-2米的距离";
                    iconResId = R.drawable.ic_info;
                    break;
                case DISTANCE_TOO_FAR:
                    errorMessage = "距离过远";
                    correctionAdvice = "请靠近到1-2米的距离";
                    iconResId = R.drawable.ic_info;
                    break;
                case POSE_INCORRECT:
                    errorMessage = "姿势不正确";
                    correctionAdvice = "请保持身体直立，放松腹部";
                    iconResId = R.drawable.ic_info;
                    break;
                case LIGHTING_POOR:
                    errorMessage = "光线不足";
                    correctionAdvice = "请移到光线充足的地方";
                    iconResId = R.drawable.ic_info;
                    break;
                case SHAKING_TOO_MUCH:
                    errorMessage = "手机晃动";
                    correctionAdvice = "请保持手机稳定";
                    iconResId = R.drawable.ic_info;
                    break;
            }
            
            // 更新状态系统
            updateStatusSystem(StatusType.ERROR, errorMessage, correctionAdvice);
            
            // 更新状态图标
            if (statusIcon != null) {
                statusIcon.setImageResource(iconResId);
                statusIcon.setVisibility(View.VISIBLE);
            }
        });
    }
    
    /**
     * 清除错误提示
     */
    private void clearError() {
        runOnUiThread(() -> {
            if (statusIcon != null) {
                statusIcon.setVisibility(View.GONE);
            }
        });
    }
    
    /**
     * 检测用户姿势
     * @param result 姿势检测结果
     */
    private void detectPosture(com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult result) {
        try {
            // 获取关键点
            java.util.List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark> landmarks = result.landmarks().get(0);
            
            // 检查关键点数量
            if (landmarks.size() < 33) {
                return;
            }
            
            // 获取关键节点
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark leftShoulder = landmarks.get(11);
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark rightShoulder = landmarks.get(12);
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark leftHip = landmarks.get(23);
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark rightHip = landmarks.get(24);
            
            // 检测站姿
            boolean isStanding = isUserStanding(leftShoulder, rightShoulder, leftHip, rightHip);
            
            // 检测光线
            boolean isGoodLighting = isGoodLighting();
            
            // 根据检测结果更新UI
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isStanding) {
                        statusText.setText("请保持站立姿势，身体直立");
                    } else if (!isGoodLighting) {
                        statusText.setText("光线不足，请调整环境光线");
                    } else {
                        // 姿势和光线都合适
                        if (estimatedDistance >= 1.0f && estimatedDistance <= 2.0f) {
                            statusText.setText("姿势良好，距离合适，可以开始测量");
                        }
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error detecting posture: " + e.getMessage());
        }
    }
    
    /**
     * 检测用户是否站立
     */
    private boolean isUserStanding(
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark leftShoulder,
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark rightShoulder,
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark leftHip,
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark rightHip) {
        // 计算肩宽
        float shoulderWidth = Math.abs(leftShoulder.x() - rightShoulder.x());
        // 计算肩到髋的距离
        float shoulderToHip = Math.abs(leftShoulder.y() - leftHip.y());
        
        // 如果肩到髋的距离大于肩宽的1.5倍，认为是站立姿势
        return shoulderToHip > shoulderWidth * 1.5f;
    }
    
    /**
     * 检测光线是否充足
     */
    private boolean isGoodLighting() {
        // 这里可以通过相机的曝光值或图像亮度来判断
        // 简化实现，返回true
        return true;
    }
    
    /**
     * 更新中心对准指示器
     * @param waistDetected 是否检测到腰部
     * @param isAligned 是否对准中心
     */
    private void updateCenterIndicator(boolean waistDetected, boolean isAligned) {
        if (centerIndicator == null) {
            return;
        }
        
        runOnUiThread(() -> {
            if (!waistDetected) {
                // 未检测到腰部，显示红色
                centerIndicator.setBackgroundResource(R.drawable.center_indicator_red);
            } else if (!isAligned) {
                // 检测到腰部但未对准，显示黄色
                centerIndicator.setBackgroundResource(R.drawable.center_indicator_yellow);
            } else {
                // 腰部已对准，显示绿色并播放成功动画
                centerIndicator.setBackgroundResource(R.drawable.center_indicator_green);
                playSuccessAnimation();
            }
        });
    }
    
    /**
     * 播放成功动画
     */
    private void playSuccessAnimation() {
        if (centerIndicator == null) {
            return;
        }
        
        // 创建缩放动画
        android.view.animation.ScaleAnimation scaleAnimation = new android.view.animation.ScaleAnimation(
                1.0f, 1.2f, 1.0f, 1.2f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(300);
        scaleAnimation.setRepeatCount(1);
        scaleAnimation.setRepeatMode(android.view.animation.Animation.REVERSE);
        
        centerIndicator.startAnimation(scaleAnimation);
    }
    
    /**
     * 检测腰部是否对准中心
     * @param result 姿势检测结果
     * @param width 图像宽度
     * @param height 图像高度
     * @return 是否对准中心
     */
    private boolean isWaistAligned(com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult result, int width, int height) {
        try {
            if (result == null || result.landmarks().isEmpty()) {
                return false;
            }
            
            // 获取腰部关键点（髂嵴）
            java.util.List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark> landmarks = result.landmarks().get(0);
            if (landmarks.size() < 25) {
                return false;
            }
            
            // 左右髂嵴点
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark leftHip = landmarks.get(23);
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark rightHip = landmarks.get(24);
            
            // 计算腰部中心点
            float waistX = (leftHip.x() + rightHip.x()) / 2;
            float waistY = (leftHip.y() + rightHip.y()) / 2;
            
            // 计算中心点偏移
            float centerX = 0.5f; // 画面中心X坐标
            float centerY = 0.5f; // 画面中心Y坐标
            float offsetX = Math.abs(waistX - centerX);
            float offsetY = Math.abs(waistY - centerY);
            
            // 定义对准阈值（画面宽度/高度的10%）
            float alignmentThreshold = 0.1f;
            
            // 如果偏移小于阈值，则认为对准
            return offsetX < alignmentThreshold && offsetY < alignmentThreshold;
            
        } catch (Exception e) {
            Log.e(TAG, "Error detecting waist alignment: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 更新状态提示系统
     * @param statusType 状态类型
     * @param message 状态消息
     * @param guideMessage 操作指南消息
     */
    private void updateStatusSystem(StatusType statusType, String message, String guideMessage) {
        runOnUiThread(() -> {
            // 更新状态图标
            if (statusIcon != null) {
                switch (statusType) {
                    case SUCCESS:
                        statusIcon.setImageResource(R.drawable.ic_check_circle);
                        statusIcon.setColorFilter(getResources().getColor(R.color.success));
                        statusIcon.setVisibility(View.VISIBLE);
                        break;
                    case WARNING:
                        statusIcon.setImageResource(R.drawable.ic_info);
                        statusIcon.setColorFilter(getResources().getColor(R.color.warning));
                        statusIcon.setVisibility(View.VISIBLE);
                        break;
                    case ERROR:
                        statusIcon.setImageResource(R.drawable.ic_close);
                        statusIcon.setColorFilter(getResources().getColor(R.color.error));
                        statusIcon.setVisibility(View.VISIBLE);
                        break;
                    default:
                        statusIcon.setVisibility(View.GONE);
                        break;
                }
            }
            
            // 更新状态文本
            if (statusText != null) {
                statusText.setText(message);
            }
            
            // 更新操作指南文本
            if (guideText != null) {
                guideText.setText(guideMessage);
            }
        });
    }
    
    /**
     * 状态类型枚举
     */
    private enum StatusType {
        SUCCESS, WARNING, ERROR, INFO
    }
    
    /**
     * 检测手机晃动
     */
    private void detectShake(SensorEvent event) {
        long currentTime = System.currentTimeMillis();
        
        // 只在测量过程中检测晃动
        if (!isMeasuring) {
            return;
        }
        
        // 防止过于频繁的检测
        if ((currentTime - lastShakeTime) > 100) {
            long diffTime = currentTime - lastShakeTime;
            lastShakeTime = currentTime;
            
            float[] values = event.values;
            float x = values[0];
            float y = values[1];
            float z = values[2];
            
            // 计算加速度变化
            float acceleration = Math.abs(x + y + z - lastAccelerometerValues[0] - lastAccelerometerValues[1] - lastAccelerometerValues[2]) / diffTime * 10000;
            
            // 保存当前加速度值
            System.arraycopy(values, 0, lastAccelerometerValues, 0, 3);
            
            // 检测是否发生晃动
            if (acceleration > SHAKE_THRESHOLD) {
                // 触发晃动提示
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusText.setText("检测到手机晃动，请保持稳定");
                    }
                });
                

            }
        }
    }
    
    /**
     * 生命周期方法 - 暂停Activity
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (isMeasuring) {
            stopMeasuring();
        }
        // 清理准星，避免内存泄漏
        FocusIndicatorManager.getInstance().clear();
        
        // 注销传感器监听器
        if (sensorManager != null && sensorEventListener != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
    }
    
    /**
     * 生命周期方法 - 恢复Activity
     */
    @Override
    protected void onResume() {
        super.onResume();
        
        // 重新注册传感器监听器
        if (sensorManager != null && rotationVectorSensor != null && sensorEventListener != null) {
            sensorManager.registerListener(sensorEventListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
    
    /**
     * 生命周期方法 - 配置变化时调用
     */

    

    
    /**
     * 打开相册选择照片
     */
    private static final int REQUEST_STORAGE_PERMISSION = 102;
    
    private void openGallery() {
        // 检查存储权限
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // 请求存储权限
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
                return;
            }
        }
        
        // 权限已授予，打开相册
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY);
    }
    
    /**
     * 处理图片旋转
     * @param bitmap 原始Bitmap
     * @param imageUri 图片URI
     * @return 旋转后的Bitmap
     */
    private android.graphics.Bitmap rotateBitmapIfNeeded(android.graphics.Bitmap bitmap, android.net.Uri imageUri) {
        try {
            // 获取图片的Exif信息
            android.media.ExifInterface exif = new android.media.ExifInterface(getContentResolver().openInputStream(imageUri));
            int orientation = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL);
            
            // 根据方向旋转图片
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            switch (orientation) {
                case android.media.ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case android.media.ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case android.media.ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    return bitmap;
            }
            
            return android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (Exception e) {
            Log.e(TAG, "Error rotating bitmap: " + e.getMessage());
            return bitmap;
        }
    }

    /**
     * 处理相册选择结果
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_GALLERY && resultCode == RESULT_OK && data != null) {
            try {
                // 获取选中的图片URI
                android.net.Uri imageUri = data.getData();
                if (imageUri != null) {
                    // 从URI加载Bitmap
                    android.graphics.Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    if (bitmap != null) {
                        // 处理图片旋转
                        bitmap = rotateBitmapIfNeeded(bitmap, imageUri);
                        // 处理选中的图片
                        processGalleryImage(bitmap);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing gallery image: " + e.getMessage());
                statusText.setText("加载图片失败，请重试");
            }
        }
    }

    /**
     * 处理从相册选择的图片
     * @param bitmap 选中的图片
     */
    private void processGalleryImage(final android.graphics.Bitmap bitmap) {
        statusText.setText("正在分析图片...");
        
        // 在后台线程处理图片
        processingExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // 使用PoseDetector检测姿势
                    com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult result = poseDetector.detectPose(bitmap, 0);
                    
                    // 检查是否检测到腰部
                    if (!isWaistDetected(result)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                statusText.setText("识别失败，请正确拍摄腰部！");
                            }
                        });
                        return;
                    }
                    
                    // 相册图片无法实时计算距离，使用默认值1.5米
                    float defaultDistance = 1.5f;
                    // 计算腰部尺寸
                    float calculatedWaistSize = poseDetector.calculateWaistSize(result, bitmap.getWidth(), bitmap.getHeight(), focalLengthPixels, defaultDistance);
                    float calculatedWaistHeight = poseDetector.calculateWaistHeight(result, bitmap.getWidth(), bitmap.getHeight(), focalLengthPixels, defaultDistance);
                    float calculatedWaistCurvature = poseDetector.calculateWaistCurvature(result, bitmap.getWidth(), bitmap.getHeight());
                    
                    // 更新测量结果
                    if (calculatedWaistSize > 0) {
                        waistSize = calculatedWaistSize;
                    }
                    if (calculatedWaistHeight > 0) {
                        waistHeight = calculatedWaistHeight;
                    }
                    if (calculatedWaistCurvature > 0) {
                        waistCurvature = calculatedWaistCurvature;
                    }
                    
                    // 在主线程更新UI并保存结果
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (waistSize > 0) {
                                statusText.setText(getString(R.string.measurement_completed) + "\n腰围: " + String.format("%.1f", waistSize) + " cm\n" + getString(R.string.waist_height) + ": " + String.format("%.1f", waistHeight) + " cm\n" + getString(R.string.waist_curvature) + ": " + String.format("%.1f", waistCurvature) + " " + getString(R.string.degree_symbol));
                                
                                // 保存测量记录
                                try {
                                    if (recordManager != null) {
                                        // 异步获取记录列表以生成新记录ID
                                        recordManager.getAllRecordsAsync(new MeasurementRecordManager.RecordsCallback() {
                                            @Override
                                            public void onResult(java.util.List<MeasurementRecord> records) {
                                                try {
                                                    final int recordId = (records != null ? records.size() : 0) + 1;
                                                    MeasurementRecord record = new MeasurementRecord(recordId, waistSize, waistHeight, waistCurvature);
                                                    // 保存照片路径（如果有）
                                                    // 注意：这里需要实现保存测量时照片的功能
                                                    boolean saved = recordManager.saveRecord(record);
                                                    if (!saved) {
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                statusText.setText(getString(R.string.record_limit_reached));
                                                            }
                                                        });
                                                        return;
                                                    }
                                                    
                                                    // 添加到主界面的项目列表中
                                                    try {
                                                        final java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                                                        String dateStr = sdf.format(record.getTimestamp());
                                                        int logoResId = R.drawable.logo;
                                                        Project project = new Project("测量记录" + recordId, dateStr, logoResId);
                                                        ProjectManager projectManager = ProjectManager.getInstance();
                                                        if (projectManager != null) {
                                                            projectManager.clearProjects();
                                                            projectManager.addProject(project);
                                                            // 重新加载所有真实记录
                                                            recordManager.getAllRecordsAsync(new MeasurementRecordManager.RecordsCallback() {
                                                                @Override
                                                                public void onResult(java.util.List<MeasurementRecord> allRecords) {
                                                                    try {
                                                                        for (int i = 0; i < allRecords.size(); i++) {
                                                                            MeasurementRecord existingRecord = allRecords.get(i);
                                                                            if (existingRecord.getId() != recordId) {
                                                                                String existingDateStr = sdf.format(existingRecord.getTimestamp());
                                                                                Project existingProject = new Project("测量记录" + existingRecord.getId(), existingDateStr, R.drawable.logo);
                                                                                ProjectManager.getInstance().addProject(existingProject);
                                                                            }
                                                                        }
                                                                    } catch (Exception e) {
                                                                        Log.e(TAG, "Error processing all records: " + e.getMessage());
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    } catch (Exception e) {
                                                        Log.e(TAG, "Error adding project to list: " + e.getMessage());
                                                    }
                                                    
                                                    // 跳转到结果页面
                                                    mainHandler.postDelayed(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            try {
                                                                Intent intent = new Intent(MainCameraActivity.this, ResultActivity.class);
                                                                if (intent != null) {
                                                                    intent.putExtra("waist_size", waistSize);
                                                                    intent.putExtra("waist_height", waistHeight);
                                                                    intent.putExtra("waist_curvature", waistCurvature);
                                                                    intent.putExtra("record_id", recordId);
                                                                    intent.putStringArrayListExtra("image_paths", new ArrayList<>());
                                                                    startActivity(intent);
                                                                }
                                                            } catch (Exception e) {
                                                                Log.e(TAG, "Error starting ResultActivity: " + e.getMessage());
                                                                Intent intent = new Intent(MainCameraActivity.this, HomeActivity.class);
                                                                if (intent != null) {
                                                                    startActivity(intent);
                                                                    finish();
                                                                }
                                                            }
                                                        }
                                                    }, 1000);
                                                } catch (Exception e) {
                                                    Log.e(TAG, "Error processing records: " + e.getMessage());
                                                }
                                            }
                                        });
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error saving measurement record: " + e.getMessage());
                                }
                            } else {
                                statusText.setText(getString(R.string.measurement_failed));
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error processing gallery image: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusText.setText("分析图片失败，请重试");
                        }
                    });
                } finally {
                    bitmap.recycle();
                }
            }
        });
    }

    /**
     * 保存关键帧
     * @return 保存的图像路径列表
     */
    private ArrayList<String> saveKeyFrames() {
        ArrayList<String> imagePaths = new ArrayList<>();
        
        try {
            // 创建存储目录
            File storageDir = new File(getExternalFilesDir(null), "keyframes");
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }
            
            // 保存每个关键帧
            for (int i = 0; i < keyFrames.size(); i++) {
                Bitmap bitmap = keyFrames.get(i);
                if (bitmap != null) {
                    // 创建文件名 - 在循环内生成时间戳，确保唯一性
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.getDefault()).format(new Date());
                    String imageFileName = "KEYFRAME_" + timeStamp + "_" + i + ".jpg";
                    File imageFile = new File(storageDir, imageFileName);
                    
                    // 保存Bitmap
                    try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                        imagePaths.add(imageFile.getAbsolutePath());
                    } finally {
                        bitmap.recycle(); // 立即回收Bitmap，释放内存
                    }
                }
            }
            
            // 清空关键帧列表
            keyFrames.clear();
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving key frames: " + e.getMessage());
        }
        
        return imagePaths;
    }
    
    /**
     * 生命周期方法 - 销毁Activity
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            // 释放传感器资源
            if (sensorManager != null && sensorEventListener != null) {
                sensorManager.unregisterListener(sensorEventListener);
            }
            
            // 释放相机资源
            if (cameraManager != null) {
                cameraManager.release();
            }
            
            // 释放姿势检测器资源
            if (poseDetector != null) {
                poseDetector.close();
            }
            
            if (processingExecutor != null) {
                processingExecutor.shutdown();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage());
        }
    }
    
    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        // 当设备方向变化时，重新配置相机预览
        if (cameraManager != null) {
            cameraManager.onConfigurationChanged();
            // 重新设置相机分析器监听器
            cameraManager.setOnFrameAvailableListener(new com.example.shunxing.camera.CameraManager.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(Bitmap bitmap, int rotationDegrees) {
                    // 保存当前帧的Bitmap
                    currentFrameBitmap = bitmap;
                    
                    // 原有处理逻辑（将 lambda 中的代码复制到这里）
                    processingExecutor.execute(() -> {
                        try {
                            PoseLandmarkerResult result = poseDetector.detectPose(bitmap, rotationDegrees);
                            calculateDistance(result, bitmap.getWidth(), bitmap.getHeight());
                            runOnUiThread(() -> {
                                if (isMeasuring) {
                                    // 测量状态下显示完整信息
                                    String distanceText = estimatedDistance > 0 ? String.format("%.2f m", estimatedDistance) : getString(R.string.detecting);
                                    String angleText = String.format("方位角: %.1f°\n俯仰角: %.1f°\n横滚角: %.1f°\n距离: %s", 
                                            angleDifference[0], angleDifference[1], angleDifference[2], distanceText);
                                    
                                    // 检查是否检测到腰部
                                    boolean waistDetected = isWaistDetected(result);
                                    
                                    if (!waistDetected) {
                                        statusText.setText("识别失败，请正确拍摄腰部！\n" + angleText);
                                    } else {
                                        // 处理姿势结果
                                        processPoseResult(result, bitmap.getWidth(), bitmap.getHeight());
                                        
                                        // 保存关键帧
                                        if (isMeasuring && waistSize > 0 && keyFrames.size() < MAX_KEY_FRAMES) {
                                            keyFrames.add(bitmap.copy(Bitmap.Config.ARGB_8888, false));
                                        }
                                        
                                        // 检测角度变化，触发拍照
                                        checkAngleAndCapture();
                                    }
                                } else {
                                    updateDistanceUI();
                                }
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing frame: " + e.getMessage());
                        }
                    });
                }

                @Override
                public void onFrameError(Exception e) {
                    Log.e(TAG, "Frame error from camera analyzer: " + e.getMessage());
                    runOnUiThread(() -> statusText.setText("相机分析器错误: " + e.getMessage()));
                }
            });
        }
        
        // 恢复测量状态的UI
        if (isMeasuring) {
            if (isPaused) {
                statusText.setText(getString(R.string.measuring_paused));
                actionButton.setBackgroundResource(R.drawable.circle_background);
            } else {
                statusText.setText(getString(R.string.measuring));
                actionButton.setBackgroundResource(R.drawable.circle_background_error);
            }
        } else {
            statusText.setText(getString(R.string.camera_ready));
            actionButton.setBackgroundResource(R.drawable.circle_background);
        }
    }


    
    /**
     * 检查是否检测到了腰部
     * @param result 姿势检测结果
     * @return 是否检测到了腰部
     */
    private boolean isWaistDetected(com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult result) {
        if (result == null || result.landmarks().isEmpty()) {
            if (BuildConfig.DEBUG) Log.d(TAG, "No pose landmarks detected");
            return false;
        }
        
        try {
            // 获取第一个人的关键点
            java.util.List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark> landmarks = result.landmarks().get(0);
            
            if (BuildConfig.DEBUG) Log.d(TAG, "Number of landmarks detected: " + landmarks.size());
            
            // 检查是否有足够的关键点（至少需要 25 个关键点，确保检测到完整的人体）
            if (landmarks.size() < 25) { // MediaPipe Pose 有 33 个关键点，至少需要 25 个
                if (BuildConfig.DEBUG) Log.d(TAG, "Not enough landmarks detected: " + landmarks.size());
                return false;
            }
            
            // 检查是否有头部、肩膀、髋关节等关键部位的关键点
            boolean hasHead = landmarks.size() >= 5; // 鼻子是索引0
            boolean hasShoulders = landmarks.size() >= 13; // 肩膀是索引11和12
            boolean hasHips = landmarks.size() >= 25; // 髋关节是索引23和24
            
            if (!hasHead || !hasShoulders || !hasHips) {
                if (BuildConfig.DEBUG) Log.d(TAG, "Missing key body parts: hasHead=" + hasHead + ", hasShoulders=" + hasShoulders + ", hasHips=" + hasHips);
                return false;
            }
            
            // 获取关键部位的关键点
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark nose = landmarks.get(0);
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark leftShoulder = landmarks.get(11);
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark rightShoulder = landmarks.get(12);
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark leftHip = landmarks.get(23);
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark rightHip = landmarks.get(24);
            
            // 检查关键点是否有效
            if (nose == null || leftShoulder == null || rightShoulder == null || leftHip == null || rightHip == null) {
                if (BuildConfig.DEBUG) Log.d(TAG, "Invalid landmarks detected");
                return false;
            }
            
            // 检查关键点的置信度（如果API支持）
            try {
                // 尝试获取置信度值
                float leftHipConfidence = 1.0f;
                float rightHipConfidence = 1.0f;
                float leftShoulderConfidence = 1.0f;
                float rightShoulderConfidence = 1.0f;
                
                // 检查是否有visibility方法
                java.lang.reflect.Method visibilityMethod = leftHip.getClass().getMethod("visibility");
                if (visibilityMethod != null) {
                    leftHipConfidence = (float) visibilityMethod.invoke(leftHip);
                    rightHipConfidence = (float) visibilityMethod.invoke(rightHip);
                    leftShoulderConfidence = (float) visibilityMethod.invoke(leftShoulder);
                    rightShoulderConfidence = (float) visibilityMethod.invoke(rightShoulder);
                }
                
                // 检查是否有presence方法
                java.lang.reflect.Method presenceMethod = leftHip.getClass().getMethod("presence");
                if (presenceMethod != null) {
                    leftHipConfidence = (float) presenceMethod.invoke(leftHip);
                    rightHipConfidence = (float) presenceMethod.invoke(rightHip);
                    leftShoulderConfidence = (float) presenceMethod.invoke(leftShoulder);
                    rightShoulderConfidence = (float) presenceMethod.invoke(rightShoulder);
                }
                
                // 要求置信度至少为0.6（更严格的条件）
                if (leftHipConfidence < 0.6 || rightHipConfidence < 0.6 || 
                    leftShoulderConfidence < 0.6 || rightShoulderConfidence < 0.6) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Low confidence in landmarks: " + 
                          "leftHip=" + leftHipConfidence + ", rightHip=" + rightHipConfidence + ", " +
                          "leftShoulder=" + leftShoulderConfidence + ", rightShoulder=" + rightShoulderConfidence);
                    return false;
                }
            } catch (Exception e) {
                // 如果没有置信度方法，继续执行
                if (BuildConfig.DEBUG) Log.d(TAG, "No confidence method available, proceeding without confidence check");
            }
            
            // 检查关键点的分布是否符合人体结构
            // 确保从上到下的顺序是：头部 -> 肩膀 -> 髋关节
            float noseY = nose.y();
            float shoulderCenterY = (leftShoulder.y() + rightShoulder.y()) / 2.0f;
            float hipCenterY = (leftHip.y() + rightHip.y()) / 2.0f;
            
            if (!(noseY < shoulderCenterY && shoulderCenterY < hipCenterY)) {
                if (BuildConfig.DEBUG) Log.d(TAG, "Invalid body part order: noseY=" + noseY + ", shoulderCenterY=" + shoulderCenterY + ", hipCenterY=" + hipCenterY);
                return false;
            }
            
            // 检查肩膀宽度是否合理
            float shoulderWidth = Math.abs(leftShoulder.x() - rightShoulder.x());
            float hipWidth = Math.abs(leftHip.x() - rightHip.x());
            
            // 肩膀宽度应该大于或等于髋关节宽度
            if (shoulderWidth < hipWidth * 0.7) {
                if (BuildConfig.DEBUG) Log.d(TAG, "Invalid body proportions: shoulderWidth=" + shoulderWidth + ", hipWidth=" + hipWidth);
                return false;
            }
            
            // 检查髋关节是否在图像中心的 70% 区域内（更严格的条件）
            float hipCenterX = (leftHip.x() + rightHip.x()) / 2.0f;
            if (!(hipCenterX > 0.15f && hipCenterX < 0.85f && hipCenterY > 0.15f && hipCenterY < 0.85f)) {
                Log.d(TAG, "Hip center outside central region: x=" + hipCenterX + ", y=" + hipCenterY);
                return false;
            }
            
            // 检查人体比例是否合理
            float bodyHeight = hipCenterY - noseY;
            if (bodyHeight < 0.2) { // 人体高度至少应该是图像高度的20%
                Log.d(TAG, "Body height too small: " + bodyHeight);
                return false;
            }
            
            Log.d(TAG, "Waist detected using full body landmark distribution with valid proportions");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking waist detection: " + e.getMessage());
            return false;
        }
    }



}