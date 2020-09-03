package io.agora.education;

import android.app.Application;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Map;

import io.agora.base.PreferenceManager;
import io.agora.base.ToastManager;
import io.agora.base.network.ResponseBody;
import io.agora.education.api.EduCallback;
import io.agora.education.api.logger.DebugItem;
import io.agora.education.api.logger.LogLevel;
import io.agora.education.api.manager.EduManager;
import io.agora.education.api.manager.EduManagerOptions;
import io.agora.education.service.bean.request.UserReq;
import io.agora.education.service.bean.response.AppConfigRes;
import io.agora.log.LogManager;
import kotlin.jvm.internal.TypeReference;

public class EduApplication extends Application {
    private static final String TAG = "EduApplication";

    public static EduApplication instance;

    private AppConfigRes config;

    private EduManager eduManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        PreferenceManager.init(this);
        ToastManager.init(this);

        String appId = getString(R.string.agora_app_id);
        EduApplication.setAppId(appId);
        String customerId = getString(R.string.agora_app_id);
        String customerCertificate = getString(R.string.agora_app_id);
        EduManagerOptions options = new EduManagerOptions(this, appId);
        options.setCustomerId(customerId);
        options.setCustomerCertificate(customerCertificate);
        options.setLogFileDir(getCacheDir().getAbsolutePath());
        eduManager = EduManager.init(options);
        /**上传log*/
        eduManager.uploadDebugItem(DebugItem.LOG, new EduCallback<String>() {
            @Override
            public void onSuccess(@Nullable String res) {
                Log.e(TAG, "日志上传成功->" + res);
            }

            @Override
            public void onFailure(int code, @Nullable String reason) {
                Log.e(TAG, "日志上传错误->code:" + code + ", reason:" + reason);
            }
        });
    }

    public static EduManager getManager() {
        if (instance.eduManager == null) {
            return null;
        }
        return instance.eduManager;
    }

    public static void LogNone(String tag, String msg) {
        getManager().logMessage(tag.concat("-").concat(msg), LogLevel.NONE);
    }

    public static void LogInfo(String tag, String msg) {
        getManager().logMessage(tag.concat("-").concat(msg), LogLevel.INFO);
    }

    public static void LogWarn(String tag, String msg) {
        getManager().logMessage(tag.concat("-").concat(msg), LogLevel.WARN);
    }

    public static void LogError(String tag, String msg) {
        getManager().logMessage(tag.concat("-").concat(msg), LogLevel.ERROR);
    }

    @Nullable
    public static String getAppId() {
        if (instance.config == null) {
            return null;
        }
        return instance.config.appId;
    }

    public static void setAppId(String appId) {
        if (instance.config == null) {
            instance.config = new AppConfigRes();
        }
        instance.config.appId = appId;
    }

    @Nullable
    public static Map<String, Map<Integer, String>> getMultiLanguage() {
        if (instance.config == null) {
            return null;
        }
        return instance.config.multiLanguage;
    }

}
