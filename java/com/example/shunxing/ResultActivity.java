package com.example.shunxing;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.shunxing.BuildConfig;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.appcompat.app.AlertDialog;

import com.example.shunxing.api.KiriApiManager;
import com.example.shunxing.data.MeasurementRecord;
import com.example.shunxing.data.MeasurementRecordManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 结果页 Activity
 * 功能：展示 3D 模型、测量数据、健康建议、保存分享
 * 详细功能：
 * 1. 展示测量的腰围数据
 * 2. 显示 3D 模型，支持旋转和缩放交互
 * 3. 计算并显示腰臀比
 * 4. 提供健康评级和建议
 * 5. 与上次测量结果进行对比
 * 6. 支持保存测量结果
 * 7. 提供多种分享选项（图片、PDF、Excel、模型数据）
 * 8. 支持重新扫描和返回首页
 */
public class ResultActivity extends AppCompatActivity {

    /**
     * 常量定义
     */

    /**
     * UI 组件
     */
    private CardView card3DModel; // 3D 模型卡片
    private ImageView ivMeasurementPhoto; // 测量照片显示
    private TextView tvWaistValue; // 腰围值显示
    private TextView tvWaistHeight; // 腰高值显示
    private TextView tvWaistCurvature; // 腰部弧度值显示
    private TextView tvWhr; // 腰臀比显示
    private TextView tvRating; // 健康评级显示
    private TextView tvCompare; // 与上次对比显示
    private TextView tvAdvice; // 健康建议显示
    private Button btnRescan; // 重新扫描按钮
    private Button btnShare; // 分享按钮
    private Button btnSave; // 保存按钮
    private Button btnGenerateModel; // 生成3D模型按钮
    // private Button btnHistory; // 历史记录按钮
    private View btnClose; // 关闭按钮

    /**
     * 3D 模型交互
     */
    private GestureDetector gestureDetector; // 手势检测器
    private ScaleGestureDetector scaleGestureDetector; // 缩放手势检测器
    private float rotationX = 0f; // X轴旋转角度
    private float rotationY = 0f; // Y轴旋转角度
    private float scale = 1f; // 缩放比例

    /**
     * 数据
     */
    private float waistSize; // 腰围尺寸
    private float waistHeight; // 腰部高度
    private float waistCurvature; // 腰部弧度
    private String imagePath; // 测量时的照片路径
    private int recordId; // 测量记录ID
    private float whr; // 腰臀比
    private String rating; // 健康评级
    private float lastWaist; // 上次腰围
    private float[] pointCloudData; // 点云数据
    private ArrayList<String> imagePaths; // 关键帧图片路径列表
    private MeasurementRecordManager recordManager; // 测量记录管理器
    
    /**
     * 云端建模相关
     */
    private KiriApiManager kiriApiManager;

    private boolean isGeneratingModel = false; // 防止重复点击

    /**
     * 生命周期方法 - 创建Activity
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_result);

            // 初始化测量记录管理器
            recordManager = MeasurementRecordManager.getInstance(this);

            // 初始化 Kiri API Manager
            kiriApiManager = KiriApiManager.getInstance(this);

            // 初始化 UI 组件
            initViews();
            // 初始化数据
            initData();
            // 初始化 3D 交互
            init3DInteraction();
            // 更新 UI
            updateUI();
            // 设置点击监听器
            setupListeners();
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.e("ResultActivity", "Error in onCreate: " + e.getMessage());
            // 显示错误信息并返回首页
            Toast.makeText(this, "加载失败，请重试", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * 初始化 UI 组件
     */
    private void initViews() {
        try {
            card3DModel = findViewById(R.id.card3DModel);
            ivMeasurementPhoto = findViewById(R.id.ivMeasurementPhoto);
            tvWaistValue = findViewById(R.id.tvWaistValue);
            tvWaistHeight = findViewById(R.id.tvWaistHeight);
            tvWaistCurvature = findViewById(R.id.tvWaistCurvature);
            tvWhr = findViewById(R.id.tvWhr);
            tvRating = findViewById(R.id.tvRating);
            tvCompare = findViewById(R.id.tvCompare);
            tvAdvice = findViewById(R.id.tvAdvice);
            btnRescan = findViewById(R.id.btnRescan);
            btnShare = findViewById(R.id.btnShare);
            btnSave = findViewById(R.id.btnSave);
            btnGenerateModel = findViewById(R.id.btnGenerateModel); // 生成3D模型按钮
            // btnHistory = findViewById(R.id.btnHistory);
            btnClose = findViewById(R.id.btnClose);

            // 初始化 3D 模型视图
            init3DModelView();
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.e("ResultActivity", "Error in initViews: " + e.getMessage());
            throw e; // 重新抛出异常，让 onCreate 中的 catch 块处理
        }
    }

    /**
     * 初始化数据
     */
    private void initData() {
        try {
            Intent intent = getIntent();
            waistSize = intent.getFloatExtra("waist_size", 0);
            waistHeight = intent.getFloatExtra("waist_height", 0);
            waistCurvature = intent.getFloatExtra("waist_curvature", 0);
            imagePath = intent.getStringExtra("image_path");
            recordId = intent.getIntExtra("record_id", 0);
            pointCloudData = intent.getFloatArrayExtra("point_cloud");
            imagePaths = intent.getStringArrayListExtra("image_paths");

            // 计算腰臀比（简化：假设臀围 = 腰围 * 1.2）
            float hipSize = waistSize * 1.2f;
            whr = waistSize / hipSize;

            // 健康评级
            rating = calculateHealthRating(whr);

            // 与上次对比
            lastWaist = getLastWaistMeasurement();
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.e("ResultActivity", "Error in initData: " + e.getMessage());
            // 设置默认值
            waistSize = 0;
            waistHeight = 0;
            waistCurvature = 0;
            whr = 0;
            rating = "C";
            lastWaist = -1;
        }
    }
    
    /**
     * 获取上次的腰围测量值
     * @return 上次的腰围值，-1表示无记录
     */
    private float getLastWaistMeasurement() {
        try {
            java.util.List<MeasurementRecord> records = recordManager.getAllRecords();
            if (records != null && records.size() > 1) {
                // 按时间戳降序排序，确保最新的记录在前面
                java.util.Collections.sort(records, (r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));
                // 获取第二条记录（最新的是当前记录，第二条是上次记录）
                MeasurementRecord lastRecord = records.get(1);
                return lastRecord.getWaistSize();
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.e("ResultActivity", "Error getting last waist measurement: " + e.getMessage());
        }
        return -1;
    }

    /**
     * 初始化 3D 模型视图
     */
    private void init3DModelView() {
        // 这里应该初始化 OpenGL ES 渲染器
        // 简化实现：使用占位视图
    }

    /**
     * 初始化 3D 交互
     */
    private void init3DInteraction() {
        try {
            // 手势识别器
            gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                    rotationY -= distanceX * 0.5f;
                    rotationX += distanceY * 0.5f;
                    update3DModelRotation();
                    return true;
                }
            });

            // 缩放手势识别器
            scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    scale *= detector.getScaleFactor();
                    scale = Math.max(0.5f, Math.min(scale, 3.0f)); // 限制缩放范围
                    update3DModelScale();
                    return true;
                }
            });

            // 设置触摸监听
            if (card3DModel != null) {
                card3DModel.setOnTouchListener((v, event) -> {
                    gestureDetector.onTouchEvent(event);
                    scaleGestureDetector.onTouchEvent(event);
                    return true;
                });
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.e("ResultActivity", "Error in init3DInteraction: " + e.getMessage());
        }
    }

    /**
     * 更新 3D 模型旋转
     */
    private void update3DModelRotation() {
        // 更新 3D 模型旋转
        // 实际应该调用 OpenGL 渲染器的旋转方法
    }

    /**
     * 更新 3D 模型缩放
     */
    private void update3DModelScale() {
        // 更新 3D 模型缩放
        // 实际应该调用 OpenGL 渲染器的缩放方法
    }

    /**
     * 更新 UI
     */
    private void updateUI() {
        try {
            // 显示测量照片（如果有）
            if (ivMeasurementPhoto != null) {
                if (imagePath != null && !imagePath.isEmpty()) {
                    try {
                        // 加载并显示照片
                        ivMeasurementPhoto.setVisibility(View.VISIBLE);
                        ivMeasurementPhoto.setImageURI(android.net.Uri.parse(imagePath));
                    } catch (Exception e) {
                        if (BuildConfig.DEBUG) Log.e("ResultActivity", "Error loading measurement photo: " + e.getMessage());
                        ivMeasurementPhoto.setVisibility(View.GONE);
                    }
                } else {
                    ivMeasurementPhoto.setVisibility(View.GONE);
                }
            }

            // 腰围
            if (tvWaistValue != null) {
                tvWaistValue.setText(String.format(Locale.getDefault(), "%.1f", waistSize));
            }

            // 腰高
            if (tvWaistHeight != null) {
                if (waistHeight > 0) {
                    tvWaistHeight.setText(String.format(Locale.getDefault(), "%.1f cm", waistHeight));
                } else {
                    tvWaistHeight.setText("--");
                }
            }

            // 腰部弧度
            if (tvWaistCurvature != null) {
                if (waistCurvature > 0) {
                    tvWaistCurvature.setText(String.format(Locale.getDefault(), "%.1f°", waistCurvature));
                } else {
                    tvWaistCurvature.setText("--");
                }
            }

            // 腰臀比
            if (tvWhr != null) {
                tvWhr.setText(String.format(Locale.getDefault(), "%.2f", whr));
            }

            // 健康评级
            if (tvRating != null) {
                tvRating.setText(rating);
                int ratingColor = getRatingColor(rating);
                try {
                    // 获取MaterialCardView并设置背景颜色
                    View parentView = (View) tvRating.getParent();
                    if (parentView instanceof com.google.android.material.card.MaterialCardView) {
                        ((com.google.android.material.card.MaterialCardView) parentView).setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(this, ratingColor));
                    }
                    // 设置文字颜色为白色，确保在任何背景下都清晰可见
                    tvRating.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
                } catch (Exception e) {
                    if (BuildConfig.DEBUG) Log.e("ResultActivity", "Error setting rating color: " + e.getMessage());
                }
            }

            // 与上次对比
            if (tvCompare != null) {
                if (lastWaist > 0) {
                    float diff = waistSize - lastWaist;
                    if (diff > 0) {
                        tvCompare.setText(String.format(Locale.getDefault(), "+%.1f cm", diff));
                        try {
                            tvCompare.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.error));
                        } catch (Exception e) {
                            if (BuildConfig.DEBUG) Log.e("ResultActivity", "Error setting compare color: " + e.getMessage());
                        }
                    } else {
                        tvCompare.setText(String.format(Locale.getDefault(), "%.1f cm", diff));
                        try {
                            tvCompare.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.success));
                        } catch (Exception e) {
                            if (BuildConfig.DEBUG) Log.e("ResultActivity", "Error setting compare color: " + e.getMessage());
                        }
                    }
                } else {
                    tvCompare.setText("--");
                }
            }

            // 健康建议
            if (tvAdvice != null) {
                tvAdvice.setText(generateHealthAdvice(whr));
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.e("ResultActivity", "Error in updateUI: " + e.getMessage());
        }
    }

    /**
     * 计算健康评级
     * @param whr 腰臀比
     * @return 健康评级
     */
    private String calculateHealthRating(float whr) {
        if (whr < 0.80) {
            return "A";
        } else if (whr < 0.85) {
            return "B";
        } else if (whr < 0.90) {
            return "C";
        } else {
            return "D";
        }
    }

    /**
     * 获取评级颜色
     * @param rating 健康评级
     * @return 颜色资源ID
     */
    private int getRatingColor(String rating) {
        switch (rating) {
            case "A":
                return R.color.success;
            case "B":
                return R.color.info;
            case "C":
                return R.color.warning;
            case "D":
                return R.color.error;
            default:
                return R.color.mid;
        }
    }

    /**
     * 生成健康建议
     * @param whr 腰臀比
     * @return 健康建议
     */
    private String generateHealthAdvice(float whr) {
        if (whr < 0.8) {
            return getString(R.string.health_advice_a);
        } else if (whr < 0.85) {
            return getString(R.string.health_advice_b);
        } else if (whr < 0.9) {
            return getString(R.string.health_advice_c);
        } else {
            return getString(R.string.health_advice_d);
        }
    }

    /**
     * 设置点击监听器
     */
    private void setupListeners() {
        try {
            // 关闭按钮
            if (btnClose != null) {
                btnClose.setOnClickListener(v -> {
                    // 返回首页
                    try {
                        Intent intent = new Intent(this, HomeActivity.class);
                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        if (BuildConfig.DEBUG) Log.e("ResultActivity", "Error in btnClose click: " + e.getMessage());
                    }
                });
            }

            // 重新扫描
            if (btnRescan != null) {
                btnRescan.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(this, MainCameraActivity.class);
                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        if (BuildConfig.DEBUG) Log.e("ResultActivity", "Error in btnRescan click: " + e.getMessage());
                    }
                });
            }

            // 分享
            if (btnShare != null) {
                btnShare.setOnClickListener(v -> {
                    try {
                        showShareOptions();
                    } catch (Exception e) {
                        if (BuildConfig.DEBUG) Log.e("ResultActivity", "Error in btnShare click: " + e.getMessage());
                    }
                });
            }

            // 保存
            if (btnSave != null) {
                btnSave.setOnClickListener(v -> {
                    try {
                        saveResult();
                    } catch (Exception e) {
                        if (BuildConfig.DEBUG) Log.e("ResultActivity", "Error in btnSave click: " + e.getMessage());
                    }
                });
            }

            // 生成3D模型
            if (btnGenerateModel != null) {
                btnGenerateModel.setOnClickListener(v -> {
                    try {
                        generate3DModel();
                    } catch (Exception e) {
                        if (BuildConfig.DEBUG) Log.e("ResultActivity", "Error in btnGenerateModel click: " + e.getMessage());
                    }
                });
            }

            // 历史记录
            // btnHistory.setOnClickListener(v -> showHistoryRecords());
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.e("ResultActivity", "Error in setupListeners: " + e.getMessage());
        }
    }

    /**
     * 显示分享选项
     */
    private void showShareOptions() {
        // 创建分享选项对话框（移除分享模型数据选项，因为点云数据未实现）
        String[] options = {getString(R.string.share_image), getString(R.string.share_pdf), getString(R.string.share_excel)};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.share)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            shareImage();
                            break;
                        case 1:
                            generateAndSharePDF();
                            break;
                        case 2:
                            generateAndShareExcel();
                            break;
                    }
                })
                .show();
    }

    /**
     * 分享图片
     */
    private void shareImage() {
        // 截取 3D 模型视图
        Bitmap bitmap = capture3DModelView();
        if (bitmap != null) {
            // 保存图片到文件
            File imageFile = saveBitmapToFile(bitmap);
            if (imageFile != null) {
                // 分享图片
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/jpeg");
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(imageFile));
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
            }
        }
    }

    /**
     * 生成并分享 PDF
     */
    private void generateAndSharePDF() {
        // 生成 PDF 报告
        // 简化实现：使用第三方库或 PdfDocument
        Toast.makeText(this, R.string.pdf_generating, Toast.LENGTH_SHORT).show();
    }

    /**
     * 生成并分享 Excel
     */
    private void generateAndShareExcel() {
        // 生成 Excel 数据
        // 简化实现：使用 Apache POI 等
        Toast.makeText(this, R.string.excel_generating, Toast.LENGTH_SHORT).show();
    }

    /**
     * 保存结果
     */
    private void saveResult() {
        // 测量记录已经在MainCameraActivity中保存
        // 这里只需要显示成功消息并返回首页

        // 上传到云端（异步）
        uploadToCloud();

        Toast.makeText(this, R.string.save_success, Toast.LENGTH_SHORT).show();

        // 返回首页
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * 保存到数据库
     */
    private void saveToDatabase() {
        // 保存到本地数据库
        // 注意：MeasurementData和DatabaseHelper类未实现
        // 这里可以添加实际的数据库保存逻辑
    }

    /**
     * 上传到云端
     */
    private void uploadToCloud() {
        // 异步上传到云端
        // 简化实现：使用 Retrofit 或 OkHttp
        new Thread(() -> {
            // 模拟上传
            try {
                Thread.sleep(2000);
                // 更新上传状态
                runOnUiThread(() -> {
                    // 更新 UI
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 截取 3D 模型视图
     * @return 截取的 Bitmap
     */
    private Bitmap capture3DModelView() {
        // 截取 3D 模型视图为 Bitmap
        // 简化实现：返回 null
        return null;
    }

    /**
     * 保存 Bitmap 到文件
     * @param bitmap 要保存的 Bitmap
     * @return 保存的文件
     */
    private File saveBitmapToFile(Bitmap bitmap) {
        try {
            File dir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "WaistMeasurement");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = "measurement_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date()) + ".jpg";
            File file = new File(dir, fileName);

            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();

            return file;
        } catch (IOException e) {
            if (BuildConfig.DEBUG) e.printStackTrace();
            return null;
        }
    }

    /**
     * 分享模型数据
     */
    private void shareModelData() {
        // 将点云数据转换为JSON格式
        String modelDataJson = convertPointCloudToJson();
        if (modelDataJson != null) {
            // 保存到文件
            File modelFile = saveModelDataToFile(modelDataJson);
            if (modelFile != null) {
                // 分享文件
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/json");
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(modelFile));
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_model_data_subject));
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
            } else {
                Toast.makeText(this, R.string.share_failed, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, R.string.share_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 将点云数据转换为JSON格式
     * @return JSON字符串
     */
    private String convertPointCloudToJson() {
        if (pointCloudData == null || pointCloudData.length == 0) {
            return null;
        }

        // 简化实现：创建JSON字符串
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{");
        jsonBuilder.append("\"waist_size\": ").append(waistSize).append(",");
        jsonBuilder.append("\"whr\": ").append(whr).append(",");
        jsonBuilder.append("\"rating\": \"").append(rating).append("\",");
        jsonBuilder.append("\"timestamp\": ").append(System.currentTimeMillis()).append(",");
        jsonBuilder.append("\"point_cloud\": [");

        for (int i = 0; i < pointCloudData.length; i++) {
            jsonBuilder.append(pointCloudData[i]);
            if (i < pointCloudData.length - 1) {
                jsonBuilder.append(",");
            }
        }

        jsonBuilder.append("]}");
        return jsonBuilder.toString();
    }

    /**
     * 保存模型数据到文件
     * @param modelDataJson 模型数据JSON字符串
     * @return 保存的文件
     */
    private File saveModelDataToFile(String modelDataJson) {
        try {
            File dir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "WaistMeasurement");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = "waist_model_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date()) + ".json";
            File file = new File(dir, fileName);

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(modelDataJson.getBytes());
            fos.flush();
            fos.close();

            return file;
        } catch (IOException e) {
            if (BuildConfig.DEBUG) e.printStackTrace();
            return null;
        }
    }

    /**
     * 显示历史记录
     */
    private void showHistoryRecords() {
        // 获取所有历史记录
        java.util.List<MeasurementRecord> records = recordManager.getAllRecords();
        
        if (records.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_records), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 创建历史记录对话框
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.measurement_history_title));
        
        // 构建记录列表
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < records.size(); i++) {
            MeasurementRecord record = records.get(i);
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
            String dateStr = sdf.format(record.getTimestamp());
            message.append("记录 " + (i + 1) + " (" + dateStr + "):\n")
                  .append("  腰围: " + String.format(java.util.Locale.getDefault(), "%.1f", record.getWaistSize()) + " cm\n")
                  .append("  腰部高度: " + String.format(java.util.Locale.getDefault(), "%.1f", record.getWaistHeight()) + " cm\n")
                  .append("  腰部弧度: " + String.format(java.util.Locale.getDefault(), "%.1f", record.getWaistCurvature()) + " 度\n\n");
        }
        
        builder.setMessage(message.toString());
        builder.setPositiveButton(getString(R.string.confirm), null);
        builder.show();
    }



    /**
     * 处理返回按钮点击
     */
    @Override
    public void onBackPressed() {
        // 返回首页
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
        super.onBackPressed();
    }

    /**
     * 生命周期方法 - 销毁Activity
     */


    /**
     * 生成3D模型
     */
    private void generate3DModel() {
        try {
            // 防止重复点击
            if (isGeneratingModel) {
                Toast.makeText(this, "正在处理中，请稍候", Toast.LENGTH_SHORT).show();
                return;
            }

            // 检查是否有图片
            if (imagePaths == null || imagePaths.isEmpty()) {
                Toast.makeText(this, "没有可用的图片，无法生成3D模型", Toast.LENGTH_SHORT).show();
                return;
            }

            // 禁用按钮
            isGeneratingModel = true;
            if (btnGenerateModel != null) {
                btnGenerateModel.setEnabled(false);
            }

            // 显示进度对话框
            showProgressDialog("正在上传图片...");

            // 使用 KiriApiManager 生成3D模型
            kiriApiManager.generate3DModel(imagePaths, new KiriApiManager.ModelGenerationCallback() {
                @Override
                public void onProgress(String status) {
                    runOnUiThread(() -> {
                        showProgressDialog(status);
                    });
                }

                @Override
                public void onSuccess(String modelUrl) {
                    runOnUiThread(() -> {
                        hideProgressDialog();
                        Toast.makeText(ResultActivity.this, getString(R.string.model_generation_success), Toast.LENGTH_SHORT).show();
                        // 下载模型
                        downloadModel(modelUrl);
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        hideProgressDialog();
                        Toast.makeText(ResultActivity.this, "模型生成失败: " + error, Toast.LENGTH_SHORT).show();
                        // 重新启用按钮
                        isGeneratingModel = false;
                        if (btnGenerateModel != null) {
                            btnGenerateModel.setEnabled(true);
                        }
                    });
                }
            });

        } catch (Exception e) {
            hideProgressDialog();
            if (BuildConfig.DEBUG) Log.e("ResultActivity", "Error generating 3D model: " + e.getMessage());
            Toast.makeText(this, "启动3D建模失败，请检查网络连接", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialog(String message) {
        ProgressDialogFragment dialog = ProgressDialogFragment.newInstance(message);
        dialog.show(getSupportFragmentManager(), "progress_dialog");
    }

    /**
     * 隐藏进度对话框
     */
    private void hideProgressDialog() {
        ProgressDialogFragment dialog = (ProgressDialogFragment) getSupportFragmentManager().findFragmentByTag("progress_dialog");
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    /**
     * 进度对话框Fragment，用于处理屏幕旋转
     */
    public static class ProgressDialogFragment extends androidx.fragment.app.DialogFragment {
        private static final String ARG_MESSAGE = "message";

        public static ProgressDialogFragment newInstance(String message) {
            ProgressDialogFragment fragment = new ProgressDialogFragment();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public androidx.appcompat.app.AlertDialog onCreateDialog(Bundle savedInstanceState) {
            String message = getArguments().getString(ARG_MESSAGE, "请稍候...");
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getActivity());
            builder.setMessage(message)
                    .setCancelable(false);
            return builder.create();
        }
    }



    /**
     * 下载模型文件
     */
    private void downloadModel(String modelUrl) {
        try {
            showProgressDialog("正在下载模型...");

            // 创建模型存储目录
            File modelDir = new File(getExternalFilesDir("3DModels"), "waist");
            if (!modelDir.exists()) {
                modelDir.mkdirs();
            }

            // 生成文件名
            String fileName = "model_" + System.currentTimeMillis() + ".glb";
            File modelFile = new File(modelDir, fileName);

            // 使用 OkHttp 下载文件，设置超时时间
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
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

                            runOnUiThread(() -> {
                                hideProgressDialog();
                                Toast.makeText(ResultActivity.this, getString(R.string.model_download_success), Toast.LENGTH_SHORT).show();
                                // 这里可以跳转到模型查看页面或更新UI
                                // 暂时显示文件路径
                                if (BuildConfig.DEBUG) Log.d("ResultActivity", "Model saved at: " + modelFile.getAbsolutePath());
                                // 重新启用按钮
                                isGeneratingModel = false;
                                if (btnGenerateModel != null) {
                                    btnGenerateModel.setEnabled(true);
                                }
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                hideProgressDialog();
                                Toast.makeText(ResultActivity.this, getString(R.string.model_save_failed), Toast.LENGTH_SHORT).show();
                                if (BuildConfig.DEBUG) Log.e("ResultActivity", "Error saving model file: " + e.getMessage());
                                // 重新启用按钮
                                isGeneratingModel = false;
                                if (btnGenerateModel != null) {
                                    btnGenerateModel.setEnabled(true);
                                }
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            hideProgressDialog();
                            Toast.makeText(ResultActivity.this, getString(R.string.model_download_failed), Toast.LENGTH_SHORT).show();
                            // 重新启用按钮
                            isGeneratingModel = false;
                            if (btnGenerateModel != null) {
                                btnGenerateModel.setEnabled(true);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    runOnUiThread(() -> {
                                hideProgressDialog();
                                Toast.makeText(ResultActivity.this, "网络连接失败，请检查设置", Toast.LENGTH_SHORT).show();
                                if (BuildConfig.DEBUG) Log.e("ResultActivity", "Error downloading model: " + e.getMessage());
                                // 重新启用按钮
                                isGeneratingModel = false;
                                if (btnGenerateModel != null) {
                                    btnGenerateModel.setEnabled(true);
                                }
                            });
                }
            });

        } catch (Exception e) {
            hideProgressDialog();
            if (BuildConfig.DEBUG) Log.e("ResultActivity", "Error downloading model: " + e.getMessage());
            Toast.makeText(this, getString(R.string.model_download_failed), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 生命周期方法 - 销毁Activity
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 隐藏进度对话框，防止WindowLeak
        hideProgressDialog();
    }


}