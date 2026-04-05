package com.example.shunxing.api;

/**
 * 创建扫描任务响应
 */
public class CreateScanResponse {
    private String taskId;          // 任务ID
    private String status;           // 初始状态，如"pending"

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
