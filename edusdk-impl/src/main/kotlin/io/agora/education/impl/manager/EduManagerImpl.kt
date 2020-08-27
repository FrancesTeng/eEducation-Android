package io.agora.education.impl.manager

import android.util.Base64
import io.agora.Constants.Companion.API_BASE_URL
import io.agora.Constants.Companion.APPID
import io.agora.Convert
import io.agora.base.callback.Callback
import io.agora.base.callback.ThrowableCallback
import io.agora.base.network.BusinessException
import io.agora.base.network.RetrofitManager
import io.agora.education.R
import io.agora.education.api.EduCallback
import io.agora.education.api.logger.DebugItem
import io.agora.education.api.logger.LogLevel
import io.agora.education.api.manager.EduManager
import io.agora.education.api.manager.EduManagerOptions
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.*
import io.agora.education.api.statistics.AgoraError
import io.agora.education.api.user.data.EduUserRole
import io.agora.education.api.util.CryptoUtil
import io.agora.education.impl.ResponseBody
import io.agora.education.impl.room.EduRoomImpl
import io.agora.education.impl.room.data.EduRoomInfoImpl
import io.agora.education.impl.room.data.request.RoomCreateOptionsReq
import io.agora.education.impl.room.data.response.EduLoginRes
import io.agora.education.impl.room.network.RoomService
import io.agora.rte.RteEngineImpl
import io.agora.rtm.ErrorInfo
import io.agora.rtm.ResultCallback

internal class EduManagerImpl(
        options: EduManagerOptions
) : EduManager(options) {
    init {
        RteEngineImpl.init(options.context, options.appId)
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
                        error?.code?.let {
                            if (error?.code == AgoraError.ROOM_ALREADY_EXISTS.value) {
//                                createSuccess(config, callback)
                                callback.onSuccess(Unit)
                            } else {
                                callback.onFailure(error?.code ?: AgoraError.INTERNAL_ERROR.value,
                                        error?.message ?: throwable?.message)
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
        return AgoraError.NONE
    }

    override fun uploadDebugItem(item: DebugItem, callback: EduCallback<String>): AgoraError {
        return AgoraError.NONE
    }
}
