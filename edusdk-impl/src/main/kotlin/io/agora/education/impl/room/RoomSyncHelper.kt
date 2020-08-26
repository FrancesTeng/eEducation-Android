package io.agora.education.impl.room

import android.os.Handler
import android.util.Log
import io.agora.Constants
import io.agora.base.callback.ThrowableCallback
import io.agora.base.network.BusinessException
import io.agora.base.network.ResponseBody
import io.agora.base.network.RetrofitManager
import io.agora.education.api.EduCallback
import io.agora.education.api.room.EduRoom
import io.agora.education.api.statistics.AgoraError
import io.agora.education.impl.room.data.request.EduSyncRoomReq
import io.agora.education.impl.room.data.request.EduSyncStep
import io.agora.education.impl.room.network.RoomService

internal class RoomSyncHelper(private val eduRoom: EduRoom) {

    /**join过程中，请求数据同步接口的body；初始化时，默认为第一阶段*/
    private var eduSyncRoomReq = EduSyncRoomReq(EduSyncStep.FIRST.value)

    private var handler: Handler? = Handler()

    /**rtm超时,说明服务端挂掉,我们要从当前节点重新开始同步数据(重新发起数据同步请求)(step、nextId、nextTs均已当前值为准);
     * 注意：我们只处理RTM正常连接状态下的超时，如果因为RTM断开而超时则另行处理*/
    private val rtmTimeout = Runnable {
        if ((eduRoom as EduRoomImpl).rtmConnectState.isConnected()) {
            launchSyncRoomReq(object : EduCallback<Unit> {
                override fun onSuccess(res: Unit?) {
                }

                override fun onFailure(code: Int, reason: String?) {
                    /**请求发送失败就一直尝试*/
                    launchSyncRoomReq(this)
                }
            })
        }
    }

    /**开始超时计时*/
    private fun startRtmTimeout() {
        handler?.postDelayed(rtmTimeout, 30 * 1000)
    }

    /**打断超时计时(在同步RoomData和同步增量数据的过程中，每接收到一次数据就打断一次；
     * 是否重新开始取决于数据是否成功同步完成)
     * @param restart 是否重新开始*/
    fun interruptRtmTimeout(restart: Boolean) {
        handler?.removeCallbacks(rtmTimeout)
        if (restart) {
            startRtmTimeout()
        }
    }

    fun removeTimeoutTask() {
        handler?.removeCallbacksAndMessages(null)
    }

    fun destroy() {
        removeTimeoutTask()
        handler = null
    }

    /**
     * nextTs:同步数据的第二阶段(增量阶段)以时间戳来排序
     * 更新全局的nextTs,方便在后续出现异常的时候可以以当前节点为起始步骤继续同步*/
    fun updateNextTs(nextTs: Long) {
        eduSyncRoomReq.nextTs = nextTs
    }

    /**
     * nextId:同步数据的第一阶段(全量阶段)以id来排序
     * 更新全局的nextId,方便在后续出现异常的时候可以以当前节点为起始步骤继续同步*/
    fun updateNextId(nextId: String) {
        eduSyncRoomReq.nextId = nextId
    }

    fun updateStep(step: Int) {
        this.eduSyncRoomReq.step = step
    }

    fun getCurRequestId(): String {
        return this.eduSyncRoomReq.requestId
    }

    /**发起同步教室数据的请求，对应的数据通过RTM通知到本地*/
    fun launchSyncRoomReq(callback: EduCallback<Unit>) {
        Log.e("EduRoomImpl", "发起数据同步请求")
        RetrofitManager.instance().getService(Constants.API_BASE_URL, RoomService::class.java)
                .syncRoom(Constants.APPID, eduRoom.roomInfo.roomUuid, eduSyncRoomReq.updateRequestId())
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        /**请求同步RoomData的请求发送成功，数据同步已经开始，
                         * 屏蔽其他RTM消息针对room和userStream的改变*/
                        (eduRoom as EduRoomImpl).cmdDispatch.disableDataChangeEnable()
                        /**开始rtm超时计时*/
                        interruptRtmTimeout(true)
                        callback.onSuccess(Unit)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        callback.onFailure(error?.code ?: AgoraError.INTERNAL_ERROR.value,
                                error?.message ?: throwable?.message)
                    }
                }))
    }
}