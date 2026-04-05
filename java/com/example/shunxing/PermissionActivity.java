package com.example.shunxing;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

/**
 * 权限申请页 Activity
 * 功能：申请相机、存储等必要权限，引导用户开启权限
 * 详细功能：
 * 1. 检查应用所需的所有权限
 * 2. 显示权限状态（已授予/未授予）
 * 3. 引导用户开启未授予的权限
 * 4. 提供前往系统设置的入口
 * 5. 权限全部授予后跳转到相机页面
 */
public class PermissionActivity extends AppCompatActivity {

    /**
     * 常量定义
     */
    private static final int REQUEST_CODE_PERMISSIONS = 100; // 权限请求码
    private static final int REQUEST_CODE_SETTINGS = 101; // 设置页面请求码

    /**
     * 所需权限列表
     */
    private static final String[] REQUIRED_PERMISSIONS = {
            android.Manifest.permission.CAMERA, // 相机权限
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE, // 写入存储权限
            android.Manifest.permission.READ_EXTERNAL_STORAGE // 读取存储权限
    };

    /**
     * UI 组件
     */
    private ImageView[] statusIcons; // 权限状态图标数组
    private TextView[] statusTexts; // 权限状态文本数组
    private MaterialButton btnContinue; // 继续按钮
    private MaterialButton btnSettings; // 设置按钮
    private View btnBack; // 返回按钮

    /**
     * 生命周期方法 - 创建Activity
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);

        // 初始化 UI 组件
        initViews();
        // 检查权限
        checkPermissions();
        // 设置点击监听器
        setupListeners();
        
        // 检查是否从设置页面进入
        boolean fromSettings = getIntent().getBooleanExtra("fromSettings", false);
        if (fromSettings) {
            // 从设置页面进入，隐藏继续按钮
            btnContinue.setVisibility(View.GONE);
        }
    }

    /**
     * 初始化 UI 组件
     */
    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnContinue = findViewById(R.id.btnContinue);
        btnSettings = findViewById(R.id.btnSettings);

        // 权限状态图标和文本
        statusIcons = new ImageView[]{
                findViewById(R.id.ivCameraStatus),
                findViewById(R.id.ivStorageStatus),
                findViewById(R.id.ivLocationStatus)
        };

        statusTexts = new TextView[]{
                findViewById(R.id.tvCameraStatus),
                findViewById(R.id.tvStorageStatus),
                findViewById(R.id.tvLocationStatus)
        };
    }

    /**
     * 检查权限
     */
    private void checkPermissions() {
        boolean allGranted = true;

        for (int i = 0; i < REQUIRED_PERMISSIONS.length; i++) {
            if (ContextCompat.checkSelfPermission(this, REQUIRED_PERMISSIONS[i])
                    != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            // 所有权限已授予，进入扫描页
            updatePermissionStatus(true);
            btnContinue.setEnabled(true);
        } else {
            // 请求权限
            requestPermissions();
        }
    }

    /**
     * 请求权限
     */
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
    }

    /**
     * 更新权限状态显示
     * @param allGranted 是否所有权限都已授予
     */
    private void updatePermissionStatus(boolean allGranted) {
        // 更新权限状态显示
        for (int i = 0; i < REQUIRED_PERMISSIONS.length; i++) {
            if (ContextCompat.checkSelfPermission(this, REQUIRED_PERMISSIONS[i])
                    == PackageManager.PERMISSION_GRANTED) {
                statusIcons[i].setColorFilter(getResources().getColor(R.color.success));
                statusTexts[i].setText(R.string.permission_granted);
            } else {
                statusIcons[i].setColorFilter(getResources().getColor(R.color.error));
                statusTexts[i].setText(R.string.permission_denied);
            }
        }
    }

    /**
     * 设置点击监听器
     */
    private void setupListeners() {
        // 返回按钮
        btnBack.setOnClickListener(v -> finish());

        // 继续按钮
        btnContinue.setOnClickListener(v -> {
            // 检查所有权限是否已授予
            boolean allGranted = true;
            for (String permission : REQUIRED_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                // 进入引导页
                Intent intent = new Intent(this, MainCameraActivity.class);
                startActivity(intent);
                finish();
            } else {
                // 提示用户开启权限
                showPermissionDialog();
            }
        });

        // 设置按钮
        btnSettings.setOnClickListener(v -> {
            // 打开系统设置页面
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivityForResult(intent, REQUEST_CODE_SETTINGS);
        });
    }

    /**
     * 显示权限对话框
     */
    private void showPermissionDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.permission_required)
                .setMessage(R.string.permission_message)
                .setPositiveButton(R.string.go_to_settings, (dialog, which) -> {
                    // 打开系统设置页面
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivityForResult(intent, REQUEST_CODE_SETTINGS);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * 处理权限请求结果
     * @param requestCode 请求码
     * @param permissions 权限列表
     * @param grantResults 授权结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            updatePermissionStatus(allGranted);
            btnContinue.setEnabled(allGranted);

            if (allGranted) {
                // 所有权限已授予，进入引导页
                Intent intent = new Intent(this, MainCameraActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }

    /**
     * 处理活动结果
     * @param requestCode 请求码
     * @param resultCode 结果码
     * @param data 返回数据
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SETTINGS) {
            // 从设置页面返回，重新检查权限
            checkPermissions();
        }
    }


}
