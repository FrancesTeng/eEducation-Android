package io.agora.education.impl.manager

import android.os.Build
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.EduLoginOptions
import io.agora.education.api.room.data.EduRoomInfo
import io.agora.education.api.room.data.RoomCreateOptions
import io.agora.education.api.statistics.AgoraError
import io.agora.education.api.util.CryptoUtil
import io.agora.education.impl.ResponseBody
import io.agora.education.impl.cmd.bean.CMDId
import io.agora.education.impl.cmd.bean.CMDResponseBody
import io.agora.education.impl.cmd.bean.RtmMsg
import io.agora.education.impl.room.EduRoomImpl
import io.agora.education.impl.room.data.EduRoomInfoImpl
import io.agora.education.impl.room.data.RtmConnectState
import io.agora.education.impl.room.data.response.EduLoginRes
import io.agora.education.impl.room.network.RoomService
import io.agora.log.LogManager
import io.agora.log.UploadManager
import io.agora.rte.RteEngineEventListener
import io.agora.rte.RteEngineImpl
import io.agora.rtm.RtmMessage
import io.agora.rtm.RtmStatusCode
import java.io.File

internal class EduManagerImpl(
        options: EduManagerOptions
) : EduManager(options), RteEngineEventListener {

    companion object {
        /**管理所有EduRoom示例的集合*/
        private val eduRooms = mutableListOf<EduRoom>()

        fun addRoom(eduRoom: EduRoom): Boolean {
            return eduRooms.add(eduRoom)
        }

        fun removeRoom(eduRoom: EduRoom): Boolean {
            return eduRooms.remove(eduRoom)
        }
    }

    /**全局的rtm连接状态*/
    private val rtmConnectState = RtmConnectState()

    init {
        options.logFileDir?.let {
            options.logFileDir = options.context.cacheDir.toString().plus(File.separatorChar).plus(LOGS_DIR_NAME)
        }
        LogManager.init(options.logFileDir!!, "AgoraEducation")
        AgoraLog = LogManager("SDK")
        RteEngineImpl.init(options.context, options.appId, options.logFileDir!!)
        /**为RteEngine设置eventListener*/
        RteEngineImpl.eventListener = this
        APPID = options.appId
        val auth = Base64.encodeToString("${options.customerId}:${options.customerCertificate}"
                .toByteArray(Charsets.UTF_8), Base64.DEFAULT).replace("\n", "").trim()
        RetrofitManager.instance().addHeader("Authorization", CryptoUtil.getAuth(auth))
    }

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
        eduRooms.clear()
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


    /***/
    override fun onConnectionStateChanged(p0: Int, p1: Int) {
        /**断线重连之后，同步每一个教室的信息*/
        eduRooms?.forEach {
            if (rtmConnectState.isReconnecting() &&
                    p0 == RtmStatusCode.ConnectionState.CONNECTION_STATE_CONNECTED) {
                (it as EduRoomImpl).roomSyncSession.fetchLostSequence(object : EduCallback<Unit> {
                    override fun onSuccess(res: Unit?) {
                    }

                    override fun onFailure(code: Int, reason: String?) {
                        /**无限重试，保证数据同步成功*/
                        it.roomSyncSession.fetchLostSequence(this)
                    }
                })
            } else {
                eduManagerEventListener?.onConnectionStateChanged(Convert.convertConnectionState(p0),
                        Convert.convertConnectionStateChangeReason(p1), it)
            }
        }
        rtmConnectState.lastConnectionState = p0
    }

    override fun onPeerMsgReceived(p0: RtmMessage?, p1: String?) {
        /**RTM保证peerMsg能到达,不用走同步检查(seq衔接性检查)*/
        p0?.text?.let {
            val cmdResponseBody = Gson().fromJson<CMDResponseBody<RtmMsg>>(p0.text, object :
                    TypeToken<CMDResponseBody<RtmMsg>>() {}.type)
            findRoom(cmdResponseBody.data.fromRoom)?.let {
                (it as EduRoomImpl).cmdDispatch.dispatchPeerMsg(Gson().toJson(cmdResponseBody), eduManagerEventListener)
            }
        }
    }

    private fun findRoom(roomInfo: EduRoomInfo): EduRoom? {
        eduRooms?.forEach {
            if (roomInfo == it.roomInfo) {
                return it
            }
        }
        return null
    }
}
