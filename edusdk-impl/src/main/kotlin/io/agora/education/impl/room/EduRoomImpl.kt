package io.agora.education.impl.room

import android.util.Log
import androidx.annotation.NonNull
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.agora.Constants.Companion.API_BASE_URL
import io.agora.Constants.Companion.APPID
import io.agora.base.callback.ThrowableCallback
import io.agora.base.network.BusinessException
import io.agora.base.network.RetrofitManager
import io.agora.education.api.EduCallback
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.*
import io.agora.education.api.user.EduStudent
import io.agora.education.api.user.EduTeacher
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.api.user.data.EduUserRole
import io.agora.Convert
import io.agora.education.api.statistics.AgoraError
import io.agora.education.api.stream.data.*
import io.agora.education.api.user.EduUser
import io.agora.education.api.user.data.EduChatState
import io.agora.education.api.user.data.EduUserEvent
import io.agora.education.impl.ResponseBody
import io.agora.education.impl.board.EduBoardImpl
import io.agora.education.impl.cmd.*
import io.agora.education.impl.record.EduRecordImpl
import io.agora.education.impl.role.data.EduUserRoleStr
import io.agora.education.impl.room.data.EduRoomInfoImpl
import io.agora.education.impl.room.data.IncludeOffline
import io.agora.education.impl.room.data.request.EduJoinClassroomReq
import io.agora.education.impl.room.data.response.*
import io.agora.education.impl.room.network.RoomService
import io.agora.education.impl.stream.EduStreamInfoImpl
import io.agora.education.impl.user.EduStudentImpl
import io.agora.education.impl.user.EduUserImpl
import io.agora.education.impl.user.network.UserService
import io.agora.rtc.Constants
import io.agora.rtc.Constants.CLIENT_ROLE_AUDIENCE
import io.agora.rtc.Constants.CLIENT_ROLE_BROADCASTER
import io.agora.rtc.models.ChannelMediaOptions
import io.agora.rte.RteChannelEventListener
import io.agora.rte.RteEngineEventListener
import io.agora.rte.RteEngineImpl
import io.agora.rtm.*
import java.util.*

internal class EduRoomImpl(
        roomInfo: EduRoomInfo,
        roomStatus: EduRoomStatus
) : EduRoom(roomInfo, roomStatus), RteChannelEventListener, RteEngineEventListener {

    init {
        RteEngineImpl.createChannel(roomInfo.roomUuid, this)
        record = EduRecordImpl()
        board = EduBoardImpl()
    }

    lateinit var RTMTOKEN: String
    lateinit var RTCTOKEN: String
    private val COUNT = 1000
    private var eduUserInfoList = Collections.synchronizedList(mutableListOf<EduUserInfo>())
    private var eduStreamInfoList = Collections.synchronizedList(mutableListOf<EduStreamInfo>())
    var joinSuccess: Boolean = false
    private var lastConnectionState: Int = RtmStatusCode.ConnectionState.CONNECTION_STATE_DISCONNECTED

    /**断线重连之后获取的增量数据(需合并到本地缓存的人流数据中)*/
    private var incrementUserList = Collections.synchronizedList(mutableListOf<EduUserRes>())
    private var incrementStreamList = Collections.synchronizedList(mutableListOf<EduStreamRes>())

    internal fun getCurRoomType(): RoomType {
        return (roomInfo as EduRoomInfoImpl).roomType
    }

    internal fun getCurUserList(): MutableList<EduUserInfo> {
        return eduUserInfoList
    }

    internal fun getCurStreamList(): MutableList<EduStreamInfo> {
        return eduStreamInfoList
    }

    /**此处先注释暂不实现*/
    @Deprecated(message = "此处先注释暂不实现,移动端暂无老师")
    override fun joinClassroomAsTeacher(options: RoomJoinOptions, callback: EduCallback<EduTeacher>) {
//        val localUserInfo = EduUserInfo(options.userUuid, options.userName, EduUserRole.TEACHER, null)
//        /**此处需要把localUserInfo设置进localUser中*/
//        localUser = EduUserImpl(localUserInfo)
//        (localUser as EduUserImpl).roomMediaOptions = options.mediaOptions
//        /**根据classroomType和用户传的角色值转化出一个角色字符串来和后端交互*/
//        val roomType = getCurRoomType()
//        val role = Convert.convertUserRole(localUserInfo.role, roomType)
//        val eduJoinClassroomReq = EduJoinClassroomReq(localUserInfo.userName,
//                role, options.mediaOptions.primaryStreamId.toString())
//        RetrofitManager.instance().getService(API_BASE_URL, UserService::class.java)
//                .joinClassroom(APPID, roomInfo.roomUuid, localUserInfo.userUuid, eduJoinClassroomReq)
//                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<EduEntryRes>> {
//                    override fun onSuccess(res: ResponseBody<EduEntryRes>?) {
//                        val classRoomEntryRes = res?.data!!
//                        /**设置全局静态数据*/
//                        USERTOKEN = classRoomEntryRes.user.userToken
//                        RTMTOKEN = classRoomEntryRes.user.rtmToken
//                        RTCTOKEN = classRoomEntryRes.user.rtcToken
//                        localUserInfo.isChatAllowed = classRoomEntryRes.user.muteChat == EduChatState.Allow.value
//                        /**解析返回的room相关数据并同步保存至本地*/
//                        roomStatus.startTime = classRoomEntryRes.room.roomState.startTime
//                        roomStatus.courseState = Convert.convertRoomState(classRoomEntryRes.room.roomState.state)
//                        roomStatus.isStudentChatAllowed = Convert.extractStudentChatAllowState(
//                                classRoomEntryRes.room.roomState, getCurRoomType())
//                        /**加入rte(包括rtm和rtc)*/
//                        val channelMediaOptions = ChannelMediaOptions()
//                        channelMediaOptions.autoSubscribeAudio = options.mediaOptions.autoSubscribeAudio
//                        channelMediaOptions.autoSubscribeVideo = options.mediaOptions.autoSubscribeVideo
//                        joinRte(RTCTOKEN, RTMTOKEN, options.userUuid.toInt(), channelMediaOptions, object : ResultCallback<Void> {
//                            override fun onSuccess(p0: Void?) {
//
//                            }
//
//                            override fun onFailure(p0: ErrorInfo?) {
//
//                            }
//                        })
//                        /**同步用户和流的全量数据*/
//                        syncUserList(null, count, object : EduCallback<Unit> {
//                            override fun onSuccess(res: Unit?) {
//                                eventListener?.onRemoteUsersInitialized(eduUserInfoList, this@EduRoomImpl)
//                            }
//
//                            override fun onFailure(code: Int, reason: String?) {
//
//                            }
//                        })
//                        syncStreamList(null, count, object : EduCallback<Unit> {
//                            override fun onSuccess(res: Unit?) {
//                                eventListener?.onRemoteStreamsInitialized(eduStreamInfoList, this@EduRoomImpl)
//                            }
//
//                            override fun onFailure(code: Int, reason: String?) {
//
//                            }
//                        })
//                        callback.onSuccess(localUser as EduTeacherImpl)
//                    }
//
//                    override fun onFailure(throwable: Throwable?) {
//                        val error = throwable as BusinessException
//                        callback.onFailure(error.code, error.message)
//                    }
//                }))
    }

    /**上课过程中，学生的角色目前不发生改变*/
    override fun joinClassroomAsStudent(options: RoomJoinOptions, callback: EduCallback<EduStudent>) {
        val localUserInfo = EduUserInfo(options.userUuid, options.userName, EduUserRole.STUDENT, null)
        /**此处需要把localUserInfo设置进localUser中*/
        localUser = EduStudentImpl(localUserInfo)
        (localUser as EduUserImpl).eduRoom = this
        val autoPublish = options.mediaOptions.autoPublishCamera ||
                options.mediaOptions.autoPublishMicrophone
        /**1:在一对一和小班课场景下
         * 2:在大班课场景下且autoPublish为true
         * (学生在RTC体系下的角色是broadcaster)*/
        if (getCurRoomType() != RoomType.LARGE_CLASS ||
                (getCurRoomType() == RoomType.LARGE_CLASS && autoPublish)) {
            RteEngineImpl.rtcEngine.setClientRole(CLIENT_ROLE_BROADCASTER)
        }
        else{
            RteEngineImpl.rtcEngine.setClientRole(CLIENT_ROLE_AUDIENCE)
        }
        /**根据classroomType和用户传的角色值转化出一个角色字符串来和后端交互*/
        val role = Convert.convertUserRole(localUserInfo.role, getCurRoomType(), autoPublish)
        val eduJoinClassroomReq = EduJoinClassroomReq(localUserInfo.userName, role,
                options.mediaOptions.primaryStreamId.toString())
        RetrofitManager.instance().getService(API_BASE_URL, UserService::class.java)
                .joinClassroom(APPID, roomInfo.roomUuid, localUserInfo.userUuid, eduJoinClassroomReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<EduEntryRes>> {
                    override fun onSuccess(res: ResponseBody<EduEntryRes>?) {
                        val classRoomEntryRes = res?.data!!
                        /**解析返回的user相关数据*/
                        (localUser as EduUserImpl).USERTOKEN = classRoomEntryRes.user.userToken
                        RTMTOKEN = classRoomEntryRes.user.rtmToken
                        RTCTOKEN = classRoomEntryRes.user.rtcToken
                        RetrofitManager.instance().addHeader("token", classRoomEntryRes.user.userToken)
                        localUserInfo.isChatAllowed = classRoomEntryRes.user.muteChat == EduChatState.Allow.value
                        /**解析返回的room相关数据并同步保存至本地*/
                        roomStatus.startTime = classRoomEntryRes.room.roomState.startTime
                        roomStatus.courseState = Convert.convertRoomState(classRoomEntryRes.room.roomState.state)
                        roomStatus.isStudentChatAllowed = Convert.extractStudentChatAllowState(
                                classRoomEntryRes.room.roomState, getCurRoomType())
                        /**加入rte(包括rtm和rtc)*/
                        val channelMediaOptions = ChannelMediaOptions()
                        channelMediaOptions.autoSubscribeAudio = options.mediaOptions.autoSubscribeAudio
                        channelMediaOptions.autoSubscribeVideo = options.mediaOptions.autoSubscribeVideo
                        joinRte(RTCTOKEN, RTMTOKEN, options.userUuid.toInt(), channelMediaOptions, object : ResultCallback<Void> {
                            override fun onSuccess(p0: Void?) {
                                var syncUserStreamSuccess = false
                                var initUpdateSuccess = false
                                syncUserStreamList(object : EduCallback<Unit> {
                                    override fun onSuccess(res: Unit?) {
                                        syncUserStreamSuccess = true
                                        if (syncUserStreamSuccess && initUpdateSuccess) {
                                            joinSuccess(localUser, callback as EduCallback<EduUser>)
                                        }
                                    }

                                    override fun onFailure(code: Int, reason: String?) {
                                        syncUserStreamSuccess = false
                                        joinFailed(code, reason, callback as EduCallback<EduUser>)
                                    }
                                })
                                /**检查是否自动订阅远端流*/
                                checkAutoSubscribe(options.mediaOptions)
                                /**初始化本地流*/
                                initOrUpdateLocalStream(classRoomEntryRes, options.mediaOptions,
                                        object : EduCallback<Unit> {
                                            override fun onSuccess(res: Unit?) {
                                                initUpdateSuccess = true
                                                if (syncUserStreamSuccess && initUpdateSuccess) {
                                                    joinSuccess(localUser, callback as EduCallback<EduUser>)
                                                }
                                            }

                                            override fun onFailure(code: Int, reason: String?) {
                                                initUpdateSuccess = false
                                                joinFailed(code, reason, callback as EduCallback<EduUser>)
                                            }
                                        })
                            }

                            override fun onFailure(p0: ErrorInfo?) {
                                joinFailed(p0?.errorCode!!, p0?.errorDescription, callback as EduCallback<EduUser>)
                            }
                        })
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        joinFailed(error?.code ?: AgoraError.INTERNAL_ERROR.value,
                                error?.message
                                        ?: throwable?.message, callback as EduCallback<EduUser>)
                    }
                }))
    }

    private fun joinRte(rtcToken: String, rtmToken: String, uid: Int,
                        channelMediaOptions: ChannelMediaOptions, @NonNull callback: ResultCallback<Void>) {
        RteEngineImpl[roomInfo.roomUuid]?.join(rtcToken, rtmToken, uid, channelMediaOptions, callback)
    }

    private fun syncUserStreamList(callback: EduCallback<Unit>) {
        /**两者同时成功才算成功，有一个失败就是失败*/
        var syncUserSuccess = false
        var syncStreamSuccess = false
        /**同步用户和流的全量数据*/
        syncAllUserList(null, COUNT, object : EduCallback<EduUserListRes> {
            override fun onSuccess(res: EduUserListRes?) {
                syncUserSuccess = true
                if (syncUserSuccess && syncStreamSuccess) {
                    callback.onSuccess(Unit)
                }
            }

            override fun onFailure(code: Int, reason: String?) {
                syncUserSuccess = false
                callback.onFailure(code, reason)
            }
        })
        syncAllStreamList(null, COUNT, object : EduCallback<EduStreamListRes> {
            override fun onSuccess(res: EduStreamListRes?) {
                syncStreamSuccess = true
                if (syncUserSuccess && syncStreamSuccess) {
                    callback.onSuccess(Unit)
                }
            }

            override fun onFailure(code: Int, reason: String?) {
                syncStreamSuccess = false
                callback.onFailure(code, reason)
            }
        })
    }

    private fun checkAutoSubscribe(roomMediaOptions: RoomMediaOptions) {
        /**检查是否需要自动订阅远端流*/
        if (roomMediaOptions.autoSubscribeVideo || roomMediaOptions.autoSubscribeAudio) {
            val subscribeOptions = StreamSubscribeOptions(roomMediaOptions.autoSubscribeAudio,
                    roomMediaOptions.autoSubscribeVideo,
                    VideoStreamType.LOW)
            for (element in getCurStreamList()) {
                localUser.subscribeStream(element, subscribeOptions)
            }
        }
    }

    private fun initOrUpdateLocalStream(classRoomEntryRes: EduEntryRes, roomMediaOptions: RoomMediaOptions,
                                        callback: EduCallback<Unit>) {
        val autoPublish = roomMediaOptions.autoPublishCamera || roomMediaOptions.autoPublishMicrophone

        /**初始化或更新本地用户的本地流*/
        val localStreamInitOptions = LocalStreamInitOptions(classRoomEntryRes.user.streamUuid,
                roomMediaOptions.autoPublishCamera, roomMediaOptions.autoPublishMicrophone)
        localUser.initOrUpdateLocalStream(localStreamInitOptions, object : EduCallback<EduStreamInfo> {
            override fun onSuccess(streamInfo: EduStreamInfo?) {
                /**判断是否需要更新本地的流信息(因为当前流信息在本地可能已经存在)*/
                val pos = streamExistsInLocal(streamInfo)
                if (pos > -1) {
                    getCurStreamList()[pos] = streamInfo!!
                }
                /**如果当前用户是观众则调用unPublishStream，否则调用publishStream*/
                if (Convert.convertUserRole(localUser.userInfo.role, getCurRoomType(), autoPublish)
                        == EduUserRoleStr.audience.value) {
                    localUser.unPublishStream(streamInfo!!, object : EduCallback<Boolean> {
                        override fun onSuccess(res: Boolean?) {
                            callback.onSuccess(Unit)
                        }

                        override fun onFailure(code: Int, reason: String?) {
                            callback.onFailure(code, reason)
                        }
                    })
                } else {
                    localUser.publishStream(streamInfo!!, object : EduCallback<Boolean> {
                        override fun onSuccess(res: Boolean?) {
                            callback.onSuccess(Unit)
                        }

                        override fun onFailure(code: Int, reason: String?) {
                            callback.onFailure(code, reason)
                        }
                    })
                }
            }

            override fun onFailure(code: Int, reason: String?) {
                callback.onFailure(code, reason)
            }
        })
    }

    private fun joinSuccess(eduUser: EduUser, callback: EduCallback<EduUser>) {
        Log.e("EduStudentImpl", "加入房间成功")
        joinSuccess = true
        /**维护本地存储的在线人数*/
        roomStatus.onlineUsersCount = getCurUserList().size
        callback.onSuccess(eduUser as EduStudent)
        eventListener?.onRemoteUsersInitialized(getCurUserList(), this@EduRoomImpl)
        eventListener?.onRemoteStreamsInitialized(getCurStreamList(), this@EduRoomImpl)
    }

    /**join失败的情况下，清楚所有本地已存在的缓存数据
     * 并退出rtm和rtc*/
    private fun joinFailed(code: Int, reason: String?, callback: EduCallback<EduUser>) {
        joinSuccess = false
        getCurStreamList().clear()
        getCurUserList().clear()
        RteEngineImpl[roomInfo.roomUuid]?.leave()
        callback.onFailure(code, reason)
    }

    /**获取本地缓存中，人员信息的最新更新时间*/
    fun getLastUserUpdatetime(): Long {
        var lastTime: Long = 0
        for (element in getCurUserList()) {
            if ((element as EduStreamInfoImpl).updateTime!! > lastTime) {
                lastTime = element.updateTime!!
            }
        }
        return lastTime
    }

    /**获取本地缓存中，流信息的最新更新时间*/
    fun getLastStreamUpdatetime(): Long {
        var lastTime: Long = 0
        for (element in getCurStreamList()) {
            if ((element as EduStreamInfoImpl).updateTime!! > lastTime) {
                lastTime = element.updateTime!!
            }
        }
        return lastTime
    }

    /**判断流信息在本地是否存在
     * @param streamInfo 需要判断的流
     * @return >= 0 存在，并返回下标   < 0 不存在*/
    fun streamExistsInLocal(streamInfo: EduStreamInfo?): Int {
        var pos = -1
        streamInfo?.let {
            for ((index, element) in getCurStreamList().withIndex()) {
                if (element == it) {
                    pos = index
                    break
                }
            }
        }
        return pos
    }

    override fun getFullStreamList(): MutableList<EduStreamInfo> {
        return eduStreamInfoList
    }

    /**递归调用，同步全量数据*/
    private fun syncAllStreamList(nextId: String?, count: Int, callback: EduCallback<EduStreamListRes>) {
        Log.e("EduRoomImpl", "开始同步全量流数据")
        RetrofitManager.instance().getService(API_BASE_URL, UserService::class.java)
                .getStreamList(APPID, roomInfo.roomUuid, nextId, count, null,
                        null)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<EduStreamListRes>> {
                    override fun onSuccess(res: ResponseBody<EduStreamListRes>?) {
                        val eduStreamListRes = res?.data

                        synchronized(getCurStreamList()) {
                            /**转换类型*/
                            val streamInfoList = Convert.getStreamInfoList(res?.data, getCurRoomType())
                            getCurStreamList().addAll(streamInfoList)
                            if (getCurStreamList().size < eduStreamListRes?.total!!) {
                                syncAllStreamList(eduStreamListRes.nextId, count, callback)
                            } else {
                                /**拉取全量流数据成功完成*/
                                callback.onSuccess(res?.data)
                            }
                        }
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        callback.onFailure(error?.code ?: AgoraError.INTERNAL_ERROR.value,
                                error?.message ?: throwable?.message)
                    }
                }))
    }

    override fun getFullUserList(): MutableList<EduUserInfo> {
        return eduUserInfoList
    }

    /**递归调用，同步全量数据*/
    private fun syncAllUserList(nextId: String?, count: Int, callback: EduCallback<EduUserListRes>) {
        Log.e("EduRoomImpl", "开始同步全量用户数据")
        RetrofitManager.instance().getService(API_BASE_URL, UserService::class.java)
                .getUserList(APPID, roomInfo.roomUuid, nextId, count, null,
                        null)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<EduUserListRes>> {
                    override fun onSuccess(res: ResponseBody<EduUserListRes>?) {
                        val eduUserListRes = res?.data

                        synchronized(getCurUserList()) {
                            /**转换类型*/
                            val userInfoList = Convert.getUserInfoList(res?.data, getCurRoomType())
                            getCurUserList().addAll(userInfoList)
                            if (getCurUserList().size < eduUserListRes?.total!!) {
                                syncAllUserList(eduUserListRes.nextId, count, callback)
                            } else {
                                /**拉去全量用户数据成功完成*/
                                callback.onSuccess(res?.data)
                            }
                        }
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        callback.onFailure(error?.code ?: AgoraError.INTERNAL_ERROR.value,
                                error?.message ?: throwable?.message)
                    }
                }))
    }

    private fun getClassroomInfo(callback: EduCallback<EduEntryRoomRes>) {
        RetrofitManager.instance().getService(API_BASE_URL, RoomService::class.java)
                .queryClassroomState(APPID, roomInfo.roomUuid)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<EduEntryRoomRes>> {
                    override fun onSuccess(res: ResponseBody<EduEntryRoomRes>?) {
                        val entryRoomRes = res?.data
                        callback.onSuccess(entryRoomRes)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        callback.onFailure(error?.code ?: AgoraError.INTERNAL_ERROR.value,
                                error?.message ?: throwable?.message)
                    }
                }))
    }

    override fun leave(callback: EduCallback<Unit>) {
        RetrofitManager.instance().getService(API_BASE_URL, UserService::class.java)
                .leaveClassroom(APPID, roomInfo.roomUuid, localUser.userInfo.userUuid)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<io.agora.base.network.ResponseBody<String>> {
                    override fun onSuccess(res: io.agora.base.network.ResponseBody<String>?) {
                        getCurUserList().clear()
                        getCurStreamList().clear()
                        RteEngineImpl[roomInfo.roomUuid]?.leave()
                        RteEngineImpl[roomInfo.roomUuid]?.release()
                        /**此处也是会有离开房间的RTM的通知的，但是离开房间只是通知一下后台并不依赖RTM通知，
                         * 所以此处的回调可以作为成功离开房间的依据*/
                        callback.onSuccess(Unit)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as? BusinessException
                        callback.onFailure(error?.code ?: AgoraError.INTERNAL_ERROR.value,
                                error?.message ?: throwable?.message)
                    }
                }))
    }


    override fun onChannelMsgReceived(p0: RtmMessage?, p1: RtmChannelMember?) {
        p0?.text?.let {
            CMDDispatch.dispatchChannelMsg(p0?.text, this, eventListener)
        }
    }

    override fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {

    }

    /**断线重连之后，同步增量的用户数据*/
    private fun syncIncrementUserList(nextId: String?, count: Int, updateTimeOffset: Long?,
                                      callback: EduCallback<Array<Any>>) {
        RetrofitManager.instance().getService(API_BASE_URL, UserService::class.java)
                .getUserList(APPID, roomInfo.roomUuid, nextId, count, updateTimeOffset,
                        IncludeOffline.Include.value)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<EduUserListRes>> {
                    override fun onSuccess(res: ResponseBody<EduUserListRes>?) {
                        val eduUserListRes = res?.data
                        synchronized(getCurUserList()) {
                            eduUserListRes?.list?.let { incrementUserList.addAll(it) }
                            if (incrementUserList.size < eduUserListRes?.total!!) {
                                syncIncrementUserList(eduUserListRes.nextId, count, updateTimeOffset, callback)
                            } else {
                                /**拉去全量用户数据成功完成,合并到本地缓存中*/
                                val streams = RoomUtil.mergeIncrementUserList(incrementUserList, getCurUserList(),
                                        getCurRoomType())
                                /**把解析后的增量数据回调出去*/
                                callback.onSuccess(streams)
                            }
                        }
                    }

                    override fun onFailure(throwable: Throwable?) {
                        /**有一次失败即整个拉取行为失败，从头拉拉取*/
                        incrementUserList.clear()
                        syncIncrementUserList(nextId, count, updateTimeOffset, callback)
                    }
                }))
    }

    /**递归调用，同步增量的的流数据*/
    private fun syncIncrementStreamList(nextId: String?, count: Int, updateTimeOffset: Long?,
                                        callback: EduCallback<Array<MutableList<EduStreamEvent>>>) {
        RetrofitManager.instance().getService(API_BASE_URL, UserService::class.java)
                .getStreamList(APPID, roomInfo.roomUuid, nextId, count, updateTimeOffset,
                        IncludeOffline.Include.value)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<EduStreamListRes>> {
                    override fun onSuccess(res: ResponseBody<EduStreamListRes>?) {
                        val eduStreamListRes = res?.data
                        synchronized(getCurStreamList()) {
                            eduStreamListRes?.list?.let { incrementStreamList.addAll(it) }
                            if (incrementStreamList.size < eduStreamListRes?.total!!) {
                                syncIncrementStreamList(eduStreamListRes.nextId, count, updateTimeOffset, callback)
                            } else {
                                /**拉取全量用户数据成功完成,合并到本地缓存中*/
                                val users = RoomUtil.mergeIncrementStreamList(incrementStreamList, getCurStreamList(),
                                        getCurRoomType())
                                /**把解析后的增量数据回调出去*/
                                callback.onSuccess(users)
                            }
                        }
                    }

                    override fun onFailure(throwable: Throwable?) {
                        /**有一次失败即整个拉取行为失败，从头拉取*/
                        incrementStreamList.clear()
                        syncIncrementStreamList(nextId, count, updateTimeOffset, callback)
                    }
                }))
    }

    override fun onConnectionStateChanged(p0: Int, p1: Int) {
        if (lastConnectionState == RtmStatusCode.ConnectionState.CONNECTION_STATE_RECONNECTING &&
                p0 == RtmStatusCode.ConnectionState.CONNECTION_STATE_CONNECTED) {
            /**断线重连成功
             * 增量更新，通过时间戳增量更新用户（后台会提供一个接口）。
             * 获取房间信息，调用get 房间信息方法，更新sdk教室缓存。*/
            val lastUserUpdatetime = getLastUserUpdatetime()
            val lastStreamUpdatetime = getLastStreamUpdatetime()
            /**同步增量数据至本地缓存并把对应的数据回调出去*/
            syncIncrementStreamList(null, COUNT, lastStreamUpdatetime, object :
                    EduCallback<Array<MutableList<EduStreamEvent>>> {
                override fun onSuccess(res: Array<MutableList<EduStreamEvent>>?) {
                    res?.get(0)?.let {
                        eventListener?.onRemoteStreamsAdded(res[0], this@EduRoomImpl)
                    }
                    res?.get(1)?.let {
                        eventListener?.onRemoteStreamsRemoved(res[1], this@EduRoomImpl)
                    }
                    res?.get(2)?.let {
                        eventListener?.onRemoteStreamsUpdated(res[2], this@EduRoomImpl)
                    }
                }

                override fun onFailure(code: Int, reason: String?) {
                }
            })
            /**同步增量数据至本地缓存并把对应的数据回调出去*/
            syncIncrementUserList(null, COUNT, lastUserUpdatetime, object :
                    EduCallback<Array<Any>> {
                override fun onSuccess(res: Array<Any>?) {
                    res?.get(0)?.let {
                        eventListener?.onRemoteUsersJoined(res[0] as MutableList<EduUserInfo>, this@EduRoomImpl)
                    }
                    res?.get(1)?.let {
                        eventListener?.onRemoteUsersLeft(res[1] as MutableList<EduUserEvent>, this@EduRoomImpl)
                    }
                    res?.get(2)?.let {
                        eventListener?.onRemoteUserUpdated(res[2] as MutableList<EduUserEvent>, this@EduRoomImpl)
                    }
                }

                override fun onFailure(code: Int, reason: String?) {
                }
            })
            /**同步教师信息*/
            getClassroomInfo(object : EduCallback<EduEntryRoomRes> {
                override fun onSuccess(res: EduEntryRoomRes?) {
                    val eduEntryRoomStateRes = res?.roomState
                    /**更新当前room的数据*/
                    roomStatus.courseState = Convert.convertRoomState(eduEntryRoomStateRes?.state!!)
                    roomStatus.startTime = eduEntryRoomStateRes.startTime
                    roomStatus.isStudentChatAllowed = Convert.extractStudentChatAllowState(
                            eduEntryRoomStateRes, getCurRoomType())
                }

                override fun onFailure(code: Int, reason: String?) {
                    /**失败则一直重试*/
                    getClassroomInfo(this)
                }
            })
        } else {
            eventListener?.onConnectionStateChanged(Convert.convertConnectionState(p0),
                    Convert.convertConnectionStateChangeReason(p1), this)
        }
        lastConnectionState = p0
    }

    override fun onPeerMsgReceived(p0: RtmMessage?, p1: String?) {
        p0?.text?.let {
            CMDDispatch.dispatchPeerMsg(p0?.text, this, eventListener)
        }
    }
}
