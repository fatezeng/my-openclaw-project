package com.example.shunxing;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 启动页 Activity
 * 功能：显示加载界面，然后跳转到相机界面
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 2000; // 加载延迟时间（毫秒）

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置布局文件，显示load.png图片
        setContentView(R.layout.activity_splash);

        // 延迟跳转到相应界面
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // 检查是否需要显示引导页
                if (TutorialActivity.isTutorialCompleted(SplashActivity.this)) {
                    // 已完成引导，跳转到主界面
                    Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
                    startActivity(intent);
                } else {
                    // 未完成引导，跳转到引导页
                    Intent intent = new Intent(SplashActivity.this, TutorialActivity.class);
                    startActivity(intent);
                }
                // 结束当前Activity
                finish();
            }
        }, SPLASH_DELAY);
    }
}
