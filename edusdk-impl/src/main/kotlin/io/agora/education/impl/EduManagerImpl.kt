package io.agora.education.impl

import io.agora.base.callback.ThrowableCallback
import io.agora.base.network.BusinessException
import io.agora.base.network.RetrofitManager
import io.agora.education.api.EduCallback
import io.agora.education.api.EduManager
import io.agora.education.api.EduManagerOptions
import io.agora.education.api.logger.DebugItem
import io.agora.education.api.logger.LogLevel
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.*
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.impl.room.EduRoomImpl
import io.agora.education.impl.room.data.EduRoomInfoImpl
import io.agora.education.impl.room.data.request.RoomCreateOptionsReq
import io.agora.education.impl.room.network.RoomService
import io.agora.education.impl.room.data.response.ResponseBody
import io.agora.education.impl.user.EduUserImpl
import io.agora.education.impl.user.data.EduUserInfoImpl
import io.agora.rte.RteEngineImpl

internal class EduManagerImpl(
        options: EduManagerOptions
) : EduManager(options) {
    init {
        RteEngineImpl.init(options.context, options.appId)
    }

    private lateinit var eduRoom: EduRoom

    private var eduUserInfoList = mutableListOf<EduUserInfo>()
    private var eduStreamInfoList = mutableListOf<EduStreamInfo>()

    override fun createClassroom(config: RoomCreateOptions, callback: EduCallback<EduRoom>) {

        RetrofitManager.instance().getService("", RoomService::class.java)
                .createClassroom("", RoomCreateOptionsReq.convertToSelf(config))
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<Int>> {
                    /**接口返回Int类型的roomId*/
                    override fun onSuccess(res: ResponseBody<Int>?) {
                        var eduRoomInfo = EduRoomInfoImpl(res?.data.toString(), config.roomUuid, config.roomName)
                        var eduRoomStatus = EduRoomStatus(EduRoomState.START, System.currentTimeMillis(), true, 0)
                        var eduRoomImpl = EduRoomImpl(eduRoomInfo, eduRoomStatus)
                        /**roomId设置进roomImpl中的localUser里面*/
                        (eduRoomImpl.localUser as EduUserImpl).roomId = res?.data.toString()
                        eduRoom = eduRoomImpl
                        callback.onSuccess(eduRoom)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as BusinessException
                        callback.onFailure(error.code, error.message)
                    }
                }))

        RteEngineImpl.createChannel(config.roomId)

    }

    override fun logMessage(message: String, level: LogLevel) {
        TODO("Not yet implemented")
    }

    override fun uploadDebugItem(item: DebugItem, callback: EduCallback<String>) {
        TODO("Not yet implemented")
    }
}
