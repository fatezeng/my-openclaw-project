package com.example.shunxing;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class ModelViewerActivity extends AppCompatActivity {

    private static final String TAG = "ModelViewerActivity";
    private static final String EXTRA_MODEL_PATH = "model_path";

    private String modelPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_viewer);

        // 获取模型路径
        Intent intent = getIntent();
        modelPath = intent.getStringExtra(EXTRA_MODEL_PATH);

        if (modelPath == null || !new File(modelPath).exists()) {
            Toast.makeText(this, "模型文件不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 显示网络连接提示
        Toast.makeText(this, "3D模型预览功能暂时不可用，请检查网络连接", Toast.LENGTH_LONG).show();

        // 设置按钮监听器
        setupButtons();
    }

    private void setupButtons() {
        // 返回按钮
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // 导出模型按钮
        Button btnExport = findViewById(R.id.btnExport);
        btnExport.setOnClickListener(v -> exportModel());
    }

    private void exportModel() {
        // 模型格式选择
        String[] formats = {"GLB", "OBJ", "STL"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("选择导出格式")
                .setItems(formats, (dialog, which) -> {
                    String selectedFormat = formats[which];
                    exportModelToFormat(selectedFormat);
                })
                .show();
    }

    private void exportModelToFormat(String format) {
        try {
            // 检查模型文件是否存在
            File modelFile = new File(modelPath);
            if (!modelFile.exists()) {
                Toast.makeText(this, "模型文件不存在", Toast.LENGTH_SHORT).show();
                return;
            }

            // 根据选择的格式处理
            switch (format) {
                case "GLB":
                    // GLB格式直接分享
                    shareModelFile(modelFile, "model/gltf-binary", "模型.glb");
                    break;
                case "OBJ":
                    // OBJ格式（需要转换，这里简化处理）
                    Toast.makeText(this, "OBJ格式导出功能开发中", Toast.LENGTH_SHORT).show();
                    break;
                case "STL":
                    // STL格式（需要转换，这里简化处理）
                    Toast.makeText(this, "STL格式导出功能开发中", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(this, "不支持的格式", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "导出模型失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareModelFile(File file, String mimeType, String fileName) {
        try {
            // 创建分享意图
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(mimeType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, android.net.Uri.fromFile(file));
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "3D模型");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "这是一个3D模型文件");
            startActivity(Intent.createChooser(shareIntent, "分享模型文件"));
        } catch (Exception e) {
            Toast.makeText(this, "分享模型失败", Toast.LENGTH_SHORT).show();
        }
    }

    // 启动模型查看器的静态方法
    public static void start(android.content.Context context, String modelPath) {
        Intent intent = new Intent(context, ModelViewerActivity.class);
        intent.putExtra(EXTRA_MODEL_PATH, modelPath);
        context.startActivity(intent);
    }
}