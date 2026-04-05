package com.example.shunxing;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目列表页 Activity
 * 功能：显示所有项目列表
 */
public class ProjectListActivity extends AppCompatActivity {

    /**
     * UI 组件
     */
    private RecyclerView rvProjects;
    private TextView tvEmptyState;

    /**
     * 数据
     */
    private List<Project> projects;
    private ProjectAdapter projectAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_list);

        // 初始化 UI 组件
        initViews();
        // 设置工具栏
        setupToolbar();
        // 加载项目数据
        loadProjects();
        // 设置 RecyclerView
        setupRecyclerView();
        // 检查空状态
        checkEmptyState();
    }

    /**
     * 初始化 UI 组件
     */
    private void initViews() {
        rvProjects = findViewById(R.id.rvProjects);
        tvEmptyState = findViewById(R.id.tvEmptyState);
    }

    /**
     * 设置工具栏
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        // 返回按钮点击事件
        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
    }

    /**
     * 加载项目数据
     */
    private void loadProjects() {
        // 从 ProjectManager 获取项目数据
        projects = ProjectManager.getInstance().getAllProjects();
    }

    /**
     * 设置 RecyclerView
     */
    private void setupRecyclerView() {
        // 设置布局管理器
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvProjects.setLayoutManager(layoutManager);

        // 创建适配器
        projectAdapter = new ProjectAdapter(projects, project -> {
            // 项目点击事件，预留接口
        });
        rvProjects.setAdapter(projectAdapter);
    }

    /**
     * 检查空状态
     */
    private void checkEmptyState() {
        if (projects == null || projects.isEmpty()) {
            rvProjects.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvProjects.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
    }
}
