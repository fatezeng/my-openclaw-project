package com.example.shunxing;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 注册页 Activity
 * 功能：用户注册、跳转到登录界面
 */
public class RegisterActivity extends AppCompatActivity {

    /**
     * UI 组件
     */
    private EditText etUsername;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private TextView tvLogin;

    /**
     * 生命周期方法
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

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
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        tvLogin = findViewById(R.id.tvLogin);
    }

    /**
     * 设置点击监听器
     */
    private void setupListeners() {
        // 注册按钮点击事件
        findViewById(R.id.btnRegister).setOnClickListener(v -> handleRegister());

        // 登录链接点击事件
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    /**
     * 处理注册逻辑
     */
    private void handleRegister() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // 表单验证
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "请填写所有字段", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        // 简单的注册逻辑（实际项目中应该连接服务器注册）
        // 这里我们假设任何非空的账号密码都可以注册
        if (!username.isEmpty() && !password.isEmpty() && password.equals(confirmPassword)) {
            // 显示注册成功提示
            Toast.makeText(this, "注册成功", Toast.LENGTH_SHORT).show();

            // 跳转到登录界面
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "注册失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }
}
