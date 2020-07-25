package io.agora.education.impl.manager

import io.agora.Constants
import io.agora.Constants.Companion.API_BASE_URL
import io.agora.Constants.Companion.APPID
import io.agora.base.callback.ThrowableCallback
import io.agora.base.network.BusinessException
import io.agora.base.network.RetrofitManager
import io.agora.education.api.EduCallback
import io.agora.education.api.manager.EduManager
import io.agora.education.api.manager.EduManagerOptions
import io.agora.education.api.logger.DebugItem
import io.agora.education.api.logger.LogLevel
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.*
import io.agora.education.api.statistics.AgoraError
import io.agora.education.impl.ResponseBody
import io.agora.education.impl.room.EduRoomImpl
import io.agora.education.impl.room.data.EduRoomInfoImpl
import io.agora.education.impl.room.data.request.RoomCreateOptionsReq
import io.agora.education.impl.room.network.RoomService
import io.agora.education.impl.user.EduUserImpl
import io.agora.rte.RteEngineEventListener
import io.agora.rte.RteEngineImpl

internal class EduManagerImpl(
        options: EduManagerOptions
) : EduManager(options) {
    init {
        RteEngineImpl.init(options.context, options.appId)
        Constants.APPID = options.appId
    }

    private lateinit var eduRoom: EduRoom

    override fun createClassroom(config: RoomCreateOptions, callback: EduCallback<EduRoom>) {
        RetrofitManager.instance().getService(API_BASE_URL, RoomService::class.java)
                .createClassroom(APPID, config.roomUuid, RoomCreateOptionsReq.convertToSelf(config))
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<io.agora.base.network.ResponseBody<String>> {
                    /**接口返回Int类型的roomId*/
                    override fun onSuccess(res: io.agora.base.network.ResponseBody<String>?) {
                        /**此处room对象的信息可能不可靠(因为此房间有可能已经被创建)，所以需要在entry接口调用成功之后，
                         * 根据返回的room信息进行同步*/
                        var eduRoomInfo = EduRoomInfoImpl(config.roomType, config.roomUuid, config.roomName)
                        var eduRoomStatus = EduRoomStatus(EduRoomState.INIT, System.currentTimeMillis(), true, 0)
                        var eduRoomImpl = EduRoomImpl(eduRoomInfo, eduRoomStatus)
                        /**把roomUuid设置进roomImpl中的localUser里面*/
                        (eduRoomImpl.localUser as EduUserImpl).eduRoom = eduRoomImpl
                        /**为RteEngine设置eventListener*/
                        RteEngineImpl.eventListener = eduRoomImpl
                        /**转换为抽象对象并回调出去*/
                        eduRoom = eduRoomImpl
                        callback.onSuccess(eduRoom)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        callback.onFailure(error?.code ?: AgoraError.INTERNAL_ERROR.value,
                                error?.message ?: throwable?.message)
                    }
                }))

    }

    override fun logMessage(message: String, level: LogLevel) {
        TODO("Not yet implemented")
    }

    override fun uploadDebugItem(item: DebugItem, callback: EduCallback<String>) {
        TODO("Not yet implemented")
    }
}
