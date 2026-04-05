package com.example.shunxing.api;

/**
 * 查询任务状态响应
 */
public class ScanStatusResponse {
    private String status;           // "processing", "completed", "failed"
    private String modelUrl;         // 模型下载地址（当status为completed时）
    private String error;            // 错误信息（当status为failed时）

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getModelUrl() {
        return modelUrl;
    }

    public void setModelUrl(String modelUrl) {
        this.modelUrl = modelUrl;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
