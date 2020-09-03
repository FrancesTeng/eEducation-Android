package io.agora.log.service;

import androidx.annotation.NonNull;

import io.agora.log.service.bean.ResponseBody;
import io.agora.log.service.bean.response.LogParamsRes;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface LogService {
    @GET("/monitor/v1/log/oss/policy")
    Call<ResponseBody<LogParamsRes>> logParams(
            @Header("sign") @NonNull String sign,
            @Header("timestamp") @NonNull String timestamp,
            @Query("appId") @NonNull String appId,
            @Query("roomId") String roomId,
            @Query("fileExt") String fileExt,
            @Query("appCode") @NonNull String appCode,
            @Query("osType") @NonNull String osType,
            @Query("terminalType") @NonNull String terminalType,
            @Query("appVersion") @NonNull String appVersion
    );

    @POST
    Call<ResponseBody<String>> logStsCallback(@Url String url);
}
