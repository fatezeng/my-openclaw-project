package com.example.shunxing;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * 设置页 Activity
 * 功能：身高设置、单位切换、通知管理、隐私政策、关于我们
 * 详细功能：
 * 1. 身高设置：允许用户输入和修改身高信息
 * 2. 单位切换：支持厘米(cm)和英寸(inch)之间的切换
 * 3. 通知管理：控制应用通知的开启和关闭
 * 4. 隐私政策：跳转查看应用隐私政策
 * 5. 关于我们：显示应用版本和相关信息
 * 6. 反馈功能：允许用户通过邮件提交反馈
 * 7. 客服功能：提供客服联系方式
 * 8. 帮助中心：跳转查看应用使用帮助
 * 9. 权限设置：管理应用所需的权限
 */
public class MyActivity extends AppCompatActivity {

    /**
     * 常量定义
     */
    private static final String PREFS_NAME = "WaistMeasurementPrefs"; // SharedPreferences 存储名称
    private static final String KEY_HEIGHT = "user_height"; // 用户身高存储键
    private static final String KEY_UNIT = "unit_type"; // 单位类型存储键
    private static final String KEY_NOTIFICATIONS = "notifications_enabled"; // 通知开关存储键

    /**
     * UI 组件
     */
    private TextView tvHeightValue; // 身高值显示
    private SwitchMaterial switchUnit; // 单位切换开关
    private SwitchMaterial switchNotifications; // 通知开关
    private Toolbar toolbar; // 工具栏
    private TextView tvAccount; // 个人账号显示

    /**
     * 数据存储
     */
    private SharedPreferences prefs; // SharedPreferences 实例

    /**
     * 生命周期方法 - 创建Activity
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        // 初始化 SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 初始化 UI 组件
        initViews();
        // 设置工具栏
        setupToolbar();
        // 加载设置
        loadSettings();
        // 设置点击监听器
        setupListeners();
    }

    /**
     * 初始化 UI 组件
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvHeightValue = findViewById(R.id.tvHeightValue);
        switchUnit = findViewById(R.id.switchUnit);
        switchNotifications = findViewById(R.id.switchNotifications);
        tvAccount = findViewById(R.id.tvAccount);
        // 初始化返回按钮
        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
    }

    /**
     * 设置工具栏
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            // 隐藏返回按钮
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            // 隐藏标题
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    /**
     * 加载设置
     */
    private void loadSettings() {
        // 加载身高设置
        float height = prefs.getFloat(KEY_HEIGHT, -1);
        if (height > 0) {
            boolean isCm = prefs.getBoolean(KEY_UNIT, true);
            updateHeightDisplay(height, isCm);
        } else {
            tvHeightValue.setText(R.string.not_set);
        }

        // 加载单位设置
        boolean isCm = prefs.getBoolean(KEY_UNIT, true);
        switchUnit.setChecked(isCm);

        // 加载通知设置
        boolean notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, true);
        switchNotifications.setChecked(notificationsEnabled);

        // 加载个人账号
        String account = prefs.getString("user_account", "未登录");
        tvAccount.setText(account);
    }

    /**
     * 设置点击监听器
     */
    private void setupListeners() {
        // 身高设置
        findViewById(R.id.btnHeightSetting).setOnClickListener(v -> {
            HeightInputDialog dialog = new HeightInputDialog(this);
            dialog.setOnHeightConfirmedListener((height, isCm) -> {
                prefs.edit().putFloat(KEY_HEIGHT, height).apply();
                updateHeightDisplay(height, isCm);
            });
            dialog.show();
        });

        // 单位切换
        switchUnit.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_UNIT, isChecked).apply();
            // 更新身高显示
            float height = prefs.getFloat(KEY_HEIGHT, -1);
            if (height > 0) {
                updateHeightDisplay(height, isChecked);
            }
        });

        // 通知开关
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_NOTIFICATIONS, isChecked).apply();
            if (isChecked) {
                // 检查通知权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // 创建通知渠道
                    createNotificationChannel();
                }
            }
        });

        // 隐私政策
        findViewById(R.id.btnPrivacy).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://shapesnap.com/privacy-policy"));
            startActivity(intent);
        });

        // 关于我们
        findViewById(R.id.btnAbout).setOnClickListener(v -> {
            showAboutDialog();
        });

        // 反馈
        findViewById(R.id.btnFeedback).setOnClickListener(v -> {
            showFeedbackDialog();
        });

        // 客服
        findViewById(R.id.btnCustomerService).setOnClickListener(v -> {
            showCustomerServiceDialog();
        });



        // 权限设置
        findViewById(R.id.btnPermissionSetting).setOnClickListener(v -> {
            Intent intent = new Intent(MyActivity.this, PermissionActivity.class);
            intent.putExtra("fromSettings", true);
            startActivity(intent);
        });
    }

    /**
     * 更新身高显示
     * @param height 身高值
     * @param isCm 是否使用厘米单位
     */
    private void updateHeightDisplay(float height, boolean isCm) {
        if (isCm) {
            tvHeightValue.setText(String.format("%.1f cm", height));
        } else {
            // 转换为英寸
            float inches = height / 2.54f;
            int feet = (int) (inches / 12);
            int remainingInches = (int) (inches % 12);
            tvHeightValue.setText(String.format("%d' %d\"", feet, remainingInches));
        }
    }

    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "measurement_notifications";
            String channelName = "测量提醒";
            String channelDescription = "测量完成、数据上传等通知";
            int importance = android.app.NotificationManager.IMPORTANCE_DEFAULT;

            android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId, channelName, importance);
            channel.setDescription(channelDescription);

            android.app.NotificationManager notificationManager = getSystemService(android.app.NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * 显示关于对话框
     */
    private void showAboutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.about)
                .setMessage(getString(R.string.app_name) + " v1.0.0\n\n" + getString(R.string.about_description))
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    /**
     * 显示反馈对话框
     */
    private void showFeedbackDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.feedback)
                .setMessage(R.string.feedback_message)
                .setPositiveButton(R.string.submit, (dialog, which) -> {
                    // 打开邮件应用
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:feedback@shapesnap.com"));
                    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject));
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, R.string.no_email_app, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * 显示客服对话框
     */
    private void showCustomerServiceDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.customer_service)
                .setMessage(R.string.customer_service_message)
                .setPositiveButton(R.string.contact, (dialog, which) -> {
                    // 拨打电话
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:4001234567"));
                    startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * 创建选项菜单
     * @param menu 菜单对象
     * @return 是否创建成功
     */
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    /**
     * 处理选项菜单选择
     * @param item 选中的菜单项
     * @return 是否处理成功
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 生命周期方法 - 恢复Activity
     */
    @Override
    protected void onResume() {
        super.onResume();
    }
}