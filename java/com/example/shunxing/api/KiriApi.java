package com.example.shunxing.api;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

import java.util.List;

/**
 * Kiri Engine API接口定义
 */
public interface KiriApi {
    // 上传图片创建扫描任务
    @Multipart
    @POST("scans")
    Call<CreateScanResponse> createScan(
            @Part List<MultipartBody.Part> images   // 照片文件列表
    );

    // 查询任务状态
    @GET("scans/{taskId}")
    Call<ScanStatusResponse> getScanStatus(@Path("taskId") String taskId);
}
