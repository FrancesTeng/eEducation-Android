package io.agora.education;

import android.app.Application;
import android.os.Handler;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Map;

import io.agora.base.PreferenceManager;
import io.agora.base.ToastManager;
import io.agora.base.network.ResponseBody;
import io.agora.education.api.manager.EduManager;
import io.agora.education.api.manager.EduManagerOptions;
import io.agora.education.service.bean.request.UserReq;
import io.agora.education.service.bean.response.AppConfigRes;
import io.agora.log.LogManager;
import kotlin.jvm.internal.TypeReference;

public class EduApplication extends Application {

    public static EduApplication instance;

    private AppConfigRes config;

    private EduManager eduManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        LogManager.init(this, BuildConfig.EXTRA);
        PreferenceManager.init(this);
        ToastManager.init(this);

        String appId = getString(R.string.agora_app_id);
        EduApplication.setAppId(appId);
        String customerId = getString(R.string.agora_app_id);
        String customerCertificate = getString(R.string.agora_app_id);
        EduManagerOptions options = new EduManagerOptions(this, appId);
        options.setCustomerId(customerId);
        options.setCustomerCertificate(customerCertificate);
        eduManager = EduManager.init(options);
    }

    public static EduManager getEduManager()
    {
        if(instance.eduManager == null) {
            return null;
        }
        return instance.eduManager;
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

    public static void setMultiLanguage(Map<String, Map<Integer, String>> multiLanguage) {
        if (instance.config == null) {
            instance.config = new AppConfigRes();
        }
        instance.config.multiLanguage = multiLanguage;
    }

}
