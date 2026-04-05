package com.example.shunxing;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 登录页 Activity
 * 功能：用户登录、跳转到注册界面
 */
public class LoginActivity extends AppCompatActivity {

    /**
     * 常量定义
     */
    private static final String PREFS_NAME = "WaistMeasurementPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ACCOUNT = "user_account";

    /**
     * UI 组件
     */
    private EditText etUsername;
    private EditText etPassword;
    private TextView tvRegister;

    /**
     * 数据存储
     */
    private SharedPreferences prefs;

    /**
     * 生命周期方法
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 初始化 SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 初始化 UI 组件
        initViews();
        // 设置点击监听器
        setupListeners();
    }

    /**
     * 初始化 UI 组件
     */
    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        tvRegister = findViewById(R.id.tvRegister);
    }

    /**
     * 设置点击监听器
     */
    private void setupListeners() {
        // 登录按钮点击事件
        findViewById(R.id.btnLogin).setOnClickListener(v -> handleLogin());

        // 注册链接点击事件
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    /**
     * 处理登录逻辑
     */
    private void handleLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 表单验证
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "请输入账号和密码", Toast.LENGTH_SHORT).show();
            return;
        }

        // 简单的登录逻辑（实际项目中应该连接服务器验证）
        // 这里我们假设任何非空的账号密码都可以登录
        if (!username.isEmpty() && !password.isEmpty()) {
            // 保存登录状态
            prefs.edit()
                    .putBoolean(KEY_IS_LOGGED_IN, true)
                    .putString(KEY_USER_ACCOUNT, username)
                    .apply();

            // 显示登录成功提示
            Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();

            // 跳转到 home 界面
            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "登录失败，请检查账号和密码", Toast.LENGTH_SHORT).show();
        }
    }
}
