package io.agora.education.impl.manager

import android.os.Build
import android.util.Base64
import io.agora.Constants.Companion.API_BASE_URL
import io.agora.Constants.Companion.APPID
import io.agora.Constants.Companion.AgoraLog
import io.agora.Constants.Companion.LOGS_DIR_NAME
import io.agora.Constants.Companion.LOG_APPSECRET
import io.agora.Convert
import io.agora.base.callback.ThrowableCallback
import io.agora.base.network.BusinessException
import io.agora.base.network.RetrofitManager
import io.agora.education.BuildConfig
import io.agora.education.api.EduCallback
import io.agora.education.api.logger.DebugItem
import io.agora.education.api.logger.LogLevel
import io.agora.education.api.manager.EduManager
import io.agora.education.api.manager.EduManagerOptions
import io.agora.education.api.room.data.EduLoginOptions
import io.agora.education.api.room.data.RoomCreateOptions
import io.agora.education.api.statistics.AgoraError
import io.agora.education.api.util.CryptoUtil
import io.agora.education.impl.ResponseBody
import io.agora.education.impl.room.data.response.EduLoginRes
import io.agora.education.impl.room.network.RoomService
import io.agora.log.LogManager
import io.agora.log.UploadManager
import io.agora.rte.RteEngineImpl
import java.io.File

internal class EduManagerImpl(
        options: EduManagerOptions
) : EduManager(options) {

    init {
        options.logFileDir?.let {
            options.logFileDir = options.context.cacheDir.toString().plus(File.separatorChar).plus(LOGS_DIR_NAME)
        }
        LogManager.init(options.logFileDir!!, "AgoraEducation")
        AgoraLog = LogManager("SDK")
        RteEngineImpl.init(options.context, options.appId, options.logFileDir!!)
        APPID = options.appId
        val auth = Base64.encodeToString("${options.customerId}:${options.customerCertificate}"
                .toByteArray(Charsets.UTF_8), Base64.DEFAULT).replace("\n", "").trim()
        RetrofitManager.instance().addHeader("Authorization", CryptoUtil.getAuth(auth))
    }

//    override fun createClassroom(config: RoomCreateOptions, callback: EduCallback<EduRoom>) {
//        if(config.createRemoteClassroom) {
//            RetrofitManager.instance().getService(API_BASE_URL, RoomService::class.java)
//                    .createClassroom(APPID, config.roomUuid, Convert.convertRoomCreateOptions(config))
//                    .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<io.agora.base.network.ResponseBody<String>> {
//                        /**接口返回Int类型的roomId*/
//                        override fun onSuccess(res: io.agora.base.network.ResponseBody<String>?) {
//                            createSuccess(config, callback)
//                        }
//
//                        override fun onFailure(throwable: Throwable?) {
//                            var error = throwable as? BusinessException
//                            error?.code?.let {
//                                if(error?.code == AgoraError.ROOM_ALREADY_EXISTS.value)
//                                {
//                                    createSuccess(config, callback)
//                                }else
//                                {
//                                    callback.onFailure(error?.code, error?.message ?: throwable?.message)
//                                }
//                            }
//                        }
//                    }))
//        }
//        else {
//            createSuccess(config, callback)
//        }
//    }

//    private fun createSuccess(config: RoomCreateOptions, callback: EduCallback<EduRoom>) {
//        var eduRoomInfo = EduRoomInfoImpl(Convert.convertRoomType(config.roomType), config.roomUuid, config.roomName)
//        var eduRoomStatus = EduRoomStatus(EduRoomState.INIT, System.currentTimeMillis(), true, 0)
//        var eduRoomImpl = EduRoomImpl(eduRoomInfo, eduRoomStatus)
//        /**转换为抽象对象并回调出去*/
//        eduRooms[eduRoomInfo.roomUuid] = eduRoomImpl
//        callback.onSuccess(eduRooms[eduRoomInfo.roomUuid])
//    }

    override fun scheduleClass(config: RoomCreateOptions, callback: EduCallback<Unit>) {
        RetrofitManager.instance().getService(API_BASE_URL, RoomService::class.java)
                .createClassroom(APPID, config.roomUuid, Convert.convertRoomCreateOptions(config))
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<io.agora.base.network.ResponseBody<String>> {
                    /**接口返回Int类型的roomId*/
                    override fun onSuccess(res: io.agora.base.network.ResponseBody<String>?) {
//                        createSuccess(config, callback)
                        callback.onSuccess(Unit)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        error = error ?: BusinessException(throwable?.message)
                        error?.code?.let {
                            if (error?.code == AgoraError.ROOM_ALREADY_EXISTS.value) {
//                                createSuccess(config, callback)
                                callback.onSuccess(Unit)
                            } else {
                                callback.onFailure(error?.code, error?.message
                                        ?: throwable?.message)
                            }
                        }
                    }
                }))
    }

    override fun login(loginOptions: EduLoginOptions, callback: EduCallback<Unit>) {
        RetrofitManager.instance().getService(API_BASE_URL, RoomService::class.java)
                .login(APPID, loginOptions.userUuid)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<EduLoginRes>> {
                    override fun onSuccess(res: ResponseBody<EduLoginRes>?) {
                        val loginRes = res?.data
                        loginRes?.let {
                            RteEngineImpl.loginRtm(loginRes.userUuid, loginRes.rtmToken,
                                    object : EduCallback<Unit> {
                                        override fun onSuccess(res: Unit?) {
                                            callback.onSuccess(res)
                                        }

                                        override fun onFailure(code: Int, reason: String?) {
                                            callback.onFailure(code, reason)
                                        }
                                    })
                        }
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        error = error ?: BusinessException(throwable?.message)
                        error?.code?.let {
                            callback.onFailure(error?.code, error?.message ?: throwable?.message)
                        }
                    }
                }))
    }

    override fun release() {
        RteEngineImpl.logoutRtm()
    }

    override fun logMessage(message: String, level: LogLevel): AgoraError {
        when (level) {
            LogLevel.NONE -> {
                AgoraLog.d(message)
            }
            LogLevel.INFO -> {
                AgoraLog.i(message)
            }
            LogLevel.WARN -> {
                AgoraLog.w(message)
            }
            LogLevel.ERROR -> {
                AgoraLog.e(message)
            }
        }
        return AgoraError.NONE
    }

    override fun uploadDebugItem(item: DebugItem, callback: EduCallback<String>): AgoraError {
        val uploadParam = UploadManager.UploadParam(APPID, BuildConfig.VERSION_NAME, Build.DEVICE,
                Build.VERSION.SDK, "ZIP", "Android", null)
        UploadManager.upload(options.context, LOG_APPSECRET, API_BASE_URL, options.logFileDir!!, uploadParam,
                object : ThrowableCallback<String> {
                    override fun onSuccess(res: String?) {
                        res?.let {
                            callback.onSuccess(res)
                        }
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        error = error ?: BusinessException(throwable?.message)
                        error?.code?.let {
                            callback.onFailure(error?.code, error?.message ?: throwable?.message)
                        }
                    }
                })
        return AgoraError.NONE
    }
}
