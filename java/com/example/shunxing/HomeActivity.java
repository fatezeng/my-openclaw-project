package com.example.shunxing;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shunxing.data.MeasurementRecord;
import com.example.shunxing.data.MeasurementRecordManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页 Activity
 * 功能：展示可滑动项目卡片栏、圆形拍摄入口按钮、"我"字按钮
 */
public class HomeActivity extends AppCompatActivity {

    /**
     * 常量定义
     */
    private static final String PREFS_NAME = "WaistMeasurementPrefs"; // SharedPreferences 存储名称
    private static final String KEY_HEIGHT = "user_height"; // 用户身高存储键

    /**
     * UI 组件
     */
    private ImageView tvLogin; // 右上角"我"按钮
    private TextView tvViewAll; // "查看全部"按钮
    private TextView tvClear; // "清除"按钮
    private ImageView fabScan; // 右下角圆形拍摄按钮
    private RecyclerView rvProjects; // 项目卡片横向滚动列表
    private LinearLayout llEmptyState; // 空状态提示布局

    /**
     * 数据存储
     */
    private SharedPreferences prefs; // SharedPreferences 实例
    private List<Project> projects; // 项目列表数据
    private ProjectAdapter projectAdapter; // 项目卡片适配器

    /**
     * 生命周期方法
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置布局文件
        setContentView(R.layout.activity_home);

        // 初始化 SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 初始化 UI 组件
        initViews();
        // 设置点击监听器
        setupListeners();
        // 设置欢迎语
        setupWelcomeMessage();
        // 设置 RecyclerView
        setupRecyclerView();
        // 检查空状态
        checkEmptyState();
    }

    /**
     * 设置欢迎语
     * 根据系统当前时间显示不同的问候语，登录后显示问候语+账户名
     */
    private void setupWelcomeMessage() {
        TextView welcomeText = findViewById(R.id.tvWelcome);
        if (welcomeText != null) {
            boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
            if (isLoggedIn) {
                String account = prefs.getString("user_account", "");
                welcomeText.setText(getGreetingMessage() + "，" + account);
            } else {
                welcomeText.setText(getGreetingMessage());
            }
        }
    }

    /**
     * 根据当前时间获取对应问候语
     * @return 问候语
     */
    private String getGreetingMessage() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        
        if (hour >= 5 && hour < 9) {
            return getString(R.string.morning_greeting);
        } else if (hour >= 9 && hour < 12) {
            return getString(R.string.midmorning_greeting);
        } else if (hour >= 12 && hour < 14) {
            return getString(R.string.noon_greeting);
        } else if (hour >= 14 && hour < 17) {
            return getString(R.string.afternoon_greeting);
        } else if (hour >= 17 && hour < 19) {
            return getString(R.string.evening_greeting);
        } else {
            return getString(R.string.night_greeting);
        }
    }

    /**
     * 初始化 UI 组件
     */
    private void initViews() {
        tvLogin = findViewById(R.id.tvLogin);
        tvViewAll = findViewById(R.id.tvViewAll);
        tvClear = findViewById(R.id.tvClear);
        fabScan = findViewById(R.id.fabScan);
        rvProjects = findViewById(R.id.rvProjects);
        llEmptyState = findViewById(R.id.llEmptyState);
        
        // 设置 Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            // 隐藏返回按钮
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            // 隐藏标题
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    /**
     * 设置 RecyclerView
     */
    private void setupRecyclerView() {
        // 从 ProjectManager 获取项目数据
        try {
            ProjectManager projectManager = ProjectManager.getInstance();
            if (projectManager != null) {
                // 从MeasurementRecordManager异步获取记录并同步到ProjectManager
                syncRecordsToProjects(projectManager);
            } else {
                // 确保projects不为null
                projects = new ArrayList<>();
                // 设置 RecyclerView 布局和适配器
                setupRecyclerViewWithData(projects);
            }
        } catch (Exception e) {
            // 异常处理，防止闪退
            android.widget.Toast.makeText(this, getString(R.string.loading_projects_failed), android.widget.Toast.LENGTH_SHORT).show();
            // 确保显示空状态
            rvProjects.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 从MeasurementRecordManager同步记录到ProjectManager（异步）
     * @param projectManager ProjectManager实例
     */
    private void syncRecordsToProjects(ProjectManager projectManager) {
        // 清空现有的项目列表
        projectManager.clearProjects();
        
        // 创建日期格式化器
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        int logoResId = R.drawable.logo;
        
        // 获取MeasurementRecordManager实例
        MeasurementRecordManager recordManager = MeasurementRecordManager.getInstance(this);
        if (recordManager != null) {
            // 异步获取所有测量记录
            recordManager.getAllRecordsAsync(new MeasurementRecordManager.RecordsCallback() {
                @Override
                public void onResult(java.util.List<MeasurementRecord> records) {
                    // 基于实际的测量记录动态生成项目列表
                    if (records != null && !records.isEmpty()) {
                        // 按时间戳降序排序，确保最新的记录在前面
                        java.util.Collections.sort(records, (r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));
                        
                        // 为每个测量记录创建项目卡片
                        for (MeasurementRecord record : records) {
                            if (record != null) {
                                String dateStr = sdf.format(record.getTimestamp());
                                // 创建项目卡片
                                Project project = new Project(getString(R.string.measurement_record_prefix) + record.getId(), dateStr, logoResId);
                                projectManager.addProject(project);
                            }
                        }
                    }
                    
                    // 获取更新后的项目列表
                    projects = projectManager.getAllProjects();
                    // 确保projects不为null
                    if (projects == null) {
                        projects = new ArrayList<>();
                    }
                    // 设置 RecyclerView 布局和适配器
                    setupRecyclerViewWithData(projects);
                }
            });
        } else {
            // 确保projects不为null
            projects = new ArrayList<>();
            // 设置 RecyclerView 布局和适配器
            setupRecyclerViewWithData(projects);
        }
    }
    
    /**
     * 设置 RecyclerView 布局和适配器
     * @param projectList 项目列表
     */
    private void setupRecyclerViewWithData(List<Project> projectList) {
        // 设置 RecyclerView 布局管理器为横向线性布局
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvProjects.setLayoutManager(layoutManager);

        // 添加水平间距装饰器
        int spacing = getResources().getDimensionPixelSize(R.dimen.card_spacing);
        rvProjects.addItemDecoration(new HorizontalSpacingItemDecoration(spacing));

        // 创建并设置项目卡片适配器
        projectAdapter = new ProjectAdapter(projectList, project -> {
            // 项目卡片点击事件处理
            // 跳转到测量记录详情页面
            try {
                if (project != null) {
                    String projectName = project.getName();
                    if (projectName != null && projectName.startsWith(getString(R.string.measurement_record_prefix))) {
                            try {
                                // 提取记录ID
                                String idStr = projectName.replace(getString(R.string.measurement_record_prefix), "");
                            if (idStr != null && !idStr.isEmpty()) {
                                int recordId = Integer.parseInt(idStr);
                                // 获取对应的测量记录
                                try {
                                    MeasurementRecordManager recordManager = MeasurementRecordManager.getInstance(HomeActivity.this);
                                    if (recordManager != null) {
                                        // 异步获取记录
                                        recordManager.getRecordByIdAsync(recordId, new MeasurementRecordManager.RecordCallback() {
                                            @Override
                                            public void onResult(MeasurementRecord record) {
                                                // 检查Activity是否存活
                                                if (isFinishing() || isDestroyed()) {
                                                    return;
                                                }
                                                
                                                if (record != null) {
                                                    // 跳转到结果页面，显示测量详情
                                                    try {
                                                        Intent intent = new Intent(HomeActivity.this, ResultActivity.class);
                                                        if (intent != null) {
                                                            intent.putExtra("waist_size", record.getWaistSize());
                                                            intent.putExtra("waist_height", record.getWaistHeight());
                                                            intent.putExtra("waist_curvature", record.getWaistCurvature());
                                                            intent.putExtra("image_path", record.getImagePath());
                                                            intent.putExtra("record_id", record.getId());
                                                            startActivity(intent);
                                                        } else {
                                                            android.widget.Toast.makeText(HomeActivity.this, getString(R.string.cannot_create_intent), android.widget.Toast.LENGTH_SHORT).show();
                                                        }
                                                    } catch (Exception e) {
                                                        // 跳转异常，显示提示
                                                        android.widget.Toast.makeText(HomeActivity.this, getString(R.string.cannot_open_detail), android.widget.Toast.LENGTH_SHORT).show();
                                                    }
                                                } else {
                                                    // 记录不存在，显示提示
                                                    android.widget.Toast.makeText(HomeActivity.this, getString(R.string.record_not_exist), android.widget.Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    } else {
                                        // 记录管理器未初始化，显示提示
                                        android.widget.Toast.makeText(HomeActivity.this, getString(R.string.record_manager_not_initialized), android.widget.Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    // 获取记录异常，显示提示
                                    android.widget.Toast.makeText(HomeActivity.this, getString(R.string.failed_to_get_records), android.widget.Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                // 记录ID为空，显示提示
                                android.widget.Toast.makeText(HomeActivity.this, getString(R.string.invalid_record_id), android.widget.Toast.LENGTH_SHORT).show();
                            }
                        } catch (NumberFormatException e) {
                            // 无法提取记录ID，显示提示
                            android.widget.Toast.makeText(HomeActivity.this, getString(R.string.cannot_recognize_record), android.widget.Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            // 其他异常，显示提示
                            android.widget.Toast.makeText(HomeActivity.this, getString(R.string.failed_to_process_record_id), android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            } catch (Exception e) {
                // 全局异常捕获，防止应用闪退
                android.widget.Toast.makeText(HomeActivity.this, "操作失败，请重试", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        rvProjects.setAdapter(projectAdapter);

        // 显示 RecyclerView，隐藏空状态提示
        rvProjects.setVisibility(View.VISIBLE);
        llEmptyState.setVisibility(View.GONE);
        
        // 检查空状态
        checkEmptyState();
    }

    /**
     * 设置点击监听器
     */
    private void setupListeners() {
        // "我"字按钮点击事件
        tvLogin.setOnClickListener(v -> {
            // 检查登录状态
            boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
            if (isLoggedIn) {
                // 已登录，跳转到我的页面
                Intent intent = new Intent(HomeActivity.this, MyActivity.class);
                startActivity(intent);
            } else {
                // 未登录，跳转到登录页面
                Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        // "清除"按钮点击事件
        tvClear.setOnClickListener(v -> {
            // 显示确认对话框
            new androidx.appcompat.app.AlertDialog.Builder(HomeActivity.this)
                .setTitle(getString(R.string.clear_records_title))
                .setMessage(getString(R.string.clear_records_message))
                .setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
                    // 清除所有测量记录
                    MeasurementRecordManager recordManager = MeasurementRecordManager.getInstance(HomeActivity.this);
                    if (recordManager != null) {
                        recordManager.clearAllRecords();
                    }
                    // 重新加载项目列表
                    setupRecyclerView();
                    // 显示提示信息
                    android.widget.Toast.makeText(HomeActivity.this, getString(R.string.records_cleared), android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
        });

        // "查看全部"按钮点击事件
        tvViewAll.setOnClickListener(v -> {
            // 跳转到项目列表页
            Intent intent = new Intent(HomeActivity.this, ProjectListActivity.class);
            startActivity(intent);
        });

        // 圆形拍摄入口按钮点击事件
        fabScan.setOnClickListener(v -> startScanFlow());
    }

    /**
     * 开始扫描流程
     */
    private void startScanFlow() {
        // 检查是否已输入身高
        float height = prefs.getFloat(KEY_HEIGHT, -1);
        if (height <= 0) {
            // 首次使用，显示身高输入对话框
            HeightInputDialog dialog = new HeightInputDialog(this);
            dialog.setOnHeightConfirmedListener(heightValue -> {
                // 保存身高
                prefs.edit().putFloat(KEY_HEIGHT, heightValue).apply();
                // 检查权限
                if (checkAllPermissionsGranted()) {
                    // 权限已全部授予，直接跳转到相机页面
                    Intent intent = new Intent(HomeActivity.this, MainCameraActivity.class);
                    startActivity(intent);
                } else {
                    // 权限未全部授予，跳转到权限检查
                    Intent intent = new Intent(HomeActivity.this, PermissionActivity.class);
                    startActivity(intent);
                }
            });
            dialog.show();
        } else {
            // 检查权限
            if (checkAllPermissionsGranted()) {
                // 权限已全部授予，直接跳转到相机页面
                Intent intent = new Intent(HomeActivity.this, MainCameraActivity.class);
                startActivity(intent);
            } else {
                // 权限未全部授予，跳转到权限检查
                Intent intent = new Intent(HomeActivity.this, PermissionActivity.class);
                startActivity(intent);
            }
        }
    }

    /**
     * 检查空状态
     */
    private void checkEmptyState() {
        // 根据项目列表是否为空来显示相应的视图
        if (projects == null || projects.isEmpty()) {
            rvProjects.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvProjects.setVisibility(View.VISIBLE);
            llEmptyState.setVisibility(View.GONE);
        }
    }
    
    /**
     * 检查所有权限是否已授予
     * @return 是否所有权限都已授予
     */
    private boolean checkAllPermissionsGranted() {
        // 需要检查的权限列表
        String[] requiredPermissions = {
            android.Manifest.permission.CAMERA, // 相机权限
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE, // 写入存储权限
            android.Manifest.permission.READ_EXTERNAL_STORAGE // 读取存储权限
        };
        
        // 遍历检查每个权限
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


}