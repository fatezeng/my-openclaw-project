package com.example.shunxing;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目数据管理类
 * 功能：提供统一的项目数据管理
 */
public class ProjectManager {

    private static ProjectManager instance;
    private List<Project> projects;

    /**
     * 私有构造方法
     */
    private ProjectManager() {
        // 初始化项目数据
        initProjects();
    }

    /**
     * 获取单例实例
     * @return ProjectManager 实例
     */
    public static synchronized ProjectManager getInstance() {
        if (instance == null) {
            instance = new ProjectManager();
        }
        return instance;
    }

    /**
     * 初始化项目数据
     */
    private void initProjects() {
        projects = new ArrayList<>();
        // 初始化空列表，只添加真实的测量记录
    }

    /**
     * 获取所有项目
     * @return 项目列表
     */
    public List<Project> getAllProjects() {
        return new ArrayList<>(projects);
    }

    /**
     * 获取项目数量
     * @return 项目数量
     */
    public int getProjectCount() {
        return projects.size();
    }

    /**
     * 根据索引获取项目
     * @param index 项目索引
     * @return 项目对象
     */
    public Project getProject(int index) {
        if (index >= 0 && index < projects.size()) {
            return projects.get(index);
        }
        return null;
    }

    /**
     * 添加项目
     * @param project 项目对象
     */
    public void addProject(Project project) {
        projects.add(project); // 添加到列表末尾
    }

    /**
     * 删除项目
     * @param project 项目对象
     */
    public void removeProject(Project project) {
        projects.remove(project);
    }

    /**
     * 清空项目
     */
    public void clearProjects() {
        projects.clear();
    }
}
