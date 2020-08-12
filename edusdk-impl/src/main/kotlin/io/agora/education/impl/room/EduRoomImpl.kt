package io.agora.education.impl.room

import android.os.Handler
import android.util.Log
import androidx.annotation.NonNull
import com.google.gson.Gson
import io.agora.Constants
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
import io.agora.education.impl.ResponseBody
import io.agora.education.impl.board.EduBoardImpl
import io.agora.education.impl.cmd.*
import io.agora.education.impl.manager.EduManagerImpl
import io.agora.education.impl.record.EduRecordImpl
import io.agora.education.impl.role.data.EduUserRoleStr
import io.agora.education.impl.room.data.EduRoomInfoImpl
import io.agora.education.impl.room.data.request.EduJoinClassroomReq
import io.agora.education.impl.room.data.request.EduSyncStep
import io.agora.education.impl.room.data.request.EduSyncRoomReq
import io.agora.education.impl.room.data.response.*
import io.agora.education.impl.room.network.RoomService
import io.agora.education.impl.stream.EduStreamInfoImpl
import io.agora.education.impl.user.EduStudentImpl
import io.agora.education.impl.user.EduUserImpl
import io.agora.education.impl.user.data.EduUserInfoImpl
import io.agora.education.impl.user.network.UserService
import io.agora.rtc.Constants.*
import io.agora.rtc.models.ChannelMediaOptions
import io.agora.rte.RteChannelEventListener
import io.agora.rte.RteChannelImpl
import io.agora.rte.RteEngineEventListener
import io.agora.rte.RteEngineImpl
import io.agora.rte.data.RteChannelMediaOptions
import io.agora.rtm.*
import okhttp3.OkHttp
import java.net.HttpURLConnection
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

    /**用户监听学生join是否成功的回调*/
    private lateinit var studentJoinCallback: EduCallback<EduStudent>
    private lateinit var roomEntryRes: EduEntryRes
    lateinit var mediaOptions: RoomMediaOptions

    /**是否退出房间的标志*/
    private var leaveRoom: Boolean = false

    /**本地缓存的人流数据*/
    private var eduUserInfoList = Collections.synchronizedList(mutableListOf<EduUserInfo>())
    private var eduStreamInfoList = Collections.synchronizedList(mutableListOf<EduStreamInfo>())

    /**entry接口返回的流信息(可能是上次遗留的也可能是本次autoPublish流也可能是在同步过程中添加的远端流)*/
    var addedStreams: MutableList<EduStreamEvent> = mutableListOf()

    /**同步过程中修改的流信息*/
    var modifiedStreams: MutableList<EduStreamEvent> = mutableListOf()

    /**同步过程中移除的流信息*/
    var removedStreams: MutableList<EduStreamEvent> = mutableListOf()

    /**join过程中，标识同步RoomInfo是否完成*/
    var syncRoomInfoSuccess = false

    /**join过程中，请求数据同步接口的body；初始化时，默认为第一阶段*/
    var eduSyncRoomReq = EduSyncRoomReq(EduSyncStep.FIRST.value)

    /**标识join过程中同步全量人流数据是否完成*/
    var syncAllUserStreamSuccess = false

    /**标识join过程中同步增量人流数据是否完成*/
    var syncIncrementUserStreamSuccess = false

    /**join过程中，标识本地流的初始化过程是否完成*/
    var initUpdateSuccess = false

    /**标识join过程是否完全成功*/
    var joinSuccess: Boolean = false

    /**标识join过程是否正在进行中*/
    var joining = false

    /**最近一次的RTM连接状态*/
    var lastConnectionState: Int = RtmStatusCode.ConnectionState.CONNECTION_STATE_DISCONNECTED

    /**当前RTM状态是否是断线重连成功后的状态*/
    var isReconnected: Boolean = false

    /**截止目前，接收到的RTM的消息的序号*/
    var rtmMsgSeq: Long = 0

    /***/
    private var handler: Handler? = Handler()

    /**rtm超时,说明服务端挂掉,我们要从当前节点重新开始同步数据(重新发起数据同步请求)(step、nextId、nextTs均已当前值为准);
     * 注意：我们只处理RTM正常连接状态下的超时，如果因为RTM断开而超时则另行处理*/
    private val rtmTimeout = Runnable {
        if (lastConnectionState != RtmStatusCode.ConnectionState.CONNECTION_STATE_CONNECTED) {
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

    /**上课过程中，学生的角色目前不发生改变;
     * join流程包括请求加入classroom的API接口、加入rte、同步roomInfo、同步、本地流初始化成功，任何一步出错即视为join失败*/
    override fun joinClassroomAsStudent(options: RoomJoinOptions, callback: EduCallback<EduStudent>) {
        joining = true
        this.studentJoinCallback = callback
        val localUserInfo = EduUserInfoImpl(options.userUuid, options.userName, EduUserRole.STUDENT,
                true, System.currentTimeMillis())
        /**此处需要把localUserInfo设置进localUser中*/
        localUser = EduStudentImpl(localUserInfo)
        (localUser as EduUserImpl).eduRoom = this
        /**大班课强制不自动发流*/
        if (getCurRoomType() == RoomType.LARGE_CLASS) {
            options.closeAutoPublish()
        }
        mediaOptions = options.mediaOptions
        /**根据classroomType和用户传的角色值转化出一个角色字符串来和后端交互*/
        val role = Convert.convertUserRole(localUserInfo.role, getCurRoomType())
        val eduJoinClassroomReq = EduJoinClassroomReq(localUserInfo.userName, role,
                mediaOptions.primaryStreamId.toString(), if (mediaOptions.isAutoPublish()) 1 else 0)
        RetrofitManager.instance().getService(API_BASE_URL, UserService::class.java)
                .joinClassroom(APPID, roomInfo.roomUuid, localUserInfo.userUuid, eduJoinClassroomReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<EduEntryRes>> {
                    override fun onSuccess(res: ResponseBody<EduEntryRes>?) {
                        roomEntryRes = res?.data!!
                        /**解析返回的user相关数据*/
                        (localUser as EduUserImpl).USERTOKEN = roomEntryRes.user.userToken
                        RTMTOKEN = roomEntryRes.user.rtmToken
                        RTCTOKEN = roomEntryRes.user.rtcToken
                        RetrofitManager.instance().addHeader("token", roomEntryRes.user.userToken)
                        localUserInfo.isChatAllowed = roomEntryRes.user.muteChat == EduChatState.Allow.value
                        localUserInfo.userProperties = roomEntryRes.user.userProperties
                        localUserInfo.streamUuid = roomEntryRes.user.streamUuid
                        /**获取可能存在的流信息*/
                        roomEntryRes.user.streams?.let {
                            addedStreams.addAll(Convert.convertStreamInfo(it, this@EduRoomImpl))
                        }
                        /**解析返回的room相关数据并同步保存至本地*/
                        roomStatus.startTime = roomEntryRes.room.roomState.startTime
                        roomStatus.courseState = Convert.convertRoomState(roomEntryRes.room.roomState.state)
                        roomStatus.isStudentChatAllowed = Convert.extractStudentChatAllowState(
                                roomEntryRes.room.roomState, getCurRoomType())
                        roomProperties = roomEntryRes.room.roomProperties
                        /**加入rte(包括rtm和rtc)*/
                        joinRte(RTCTOKEN, RTMTOKEN, roomEntryRes.user.streamUuid.toLong(), options.userUuid,
                                RteChannelMediaOptions.build(mediaOptions), object : ResultCallback<Void> {
                            override fun onSuccess(p0: Void?) {
                                /**发起同步数据(房间信息和全量人流数据)的请求，数据从RTM通知到本地*/
                                launchSyncRoomReq(object : EduCallback<Unit> {
                                    override fun onSuccess(res: Unit?) {
                                    }

                                    override fun onFailure(code: Int, reason: String?) {
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

    private fun joinRte(rtcToken: String, rtmToken: String, rtcUid: Long, rtmUid: String,
                        channelMediaOptions: ChannelMediaOptions, @NonNull callback: ResultCallback<Void>) {
        RteEngineImpl.rtcEngine.setChannelProfile(CHANNEL_PROFILE_LIVE_BROADCASTING)
        RteEngineImpl[roomInfo.roomUuid]?.join(rtcToken, rtmToken, rtcUid, rtmUid, channelMediaOptions, callback)
    }

    /**发起同步教室数据的请求，对应的数据通过RTM通知到本地*/
    fun launchSyncRoomReq(callback: EduCallback<Unit>) {
        Log.e("EduRoomImpl", "发起数据同步请求")
        RetrofitManager.instance().getService(API_BASE_URL, RoomService::class.java)
                .syncRoom(APPID, roomInfo.roomUuid, eduSyncRoomReq.updateRequestId())
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<io.agora.base.network.ResponseBody<String>> {
                    override fun onSuccess(res: io.agora.base.network.ResponseBody<String>?) {
                        /**请求同步RoomData的请求发送成功，数据同步已经开始，
                         * 屏蔽其他RTM消息针对room和userStream的改变*/
                        CMDDispatch.disableDataChangeEnable()
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

    /**
     * @param syncRoomInfoSuc 同步roomInfo是否成功 true or null
     * @param syncAllUserStreamSuc 同步syncUserStream是否成功 true or null*/
    fun syncRoomOrAllUserStreamSuccess(syncRoomInfoSuc: Boolean?, syncAllUserStreamSuc:
    Boolean?, syncIncrementUserStreamSuc: Boolean?) {
        syncRoomInfoSuc?.let { this.syncRoomInfoSuccess = true }
        syncAllUserStreamSuc?.let { this.syncAllUserStreamSuccess = true }
        syncIncrementUserStreamSuc?.let { this.syncIncrementUserStreamSuccess = true }
        val sync = this.syncRoomInfoSuccess && this.syncAllUserStreamSuccess &&
                this.syncIncrementUserStreamSuccess
        /**在同步roomInfo和allUserStream的过程中，每接收到一次数据就刷新一次超时任务；*/
        interruptRtmTimeout(!sync)
        if (sync && this.initUpdateSuccess) {
            /**join流程成功完成*/
            joinSuccess(localUser, studentJoinCallback as EduCallback<EduUser>)
        } else if (sync && !this.initUpdateSuccess) {
            Log.e("EduRoomImpl", "人流数据同步完成，人：" + Gson().toJson(eduUserInfoList))
            Log.e("EduRoomImpl", "人流数据同步完成，流：" + Gson().toJson(eduStreamInfoList))
            /**人流数据同步完成
             * 检查是否自动订阅远端流*/
//            checkAutoSubscribe(mediaOptions)
            /**初始化本地流*/
            initOrUpdateLocalStream(roomEntryRes, mediaOptions, object : EduCallback<Unit> {
                override fun onSuccess(res: Unit?) {
                    initUpdateSuccess = true
                    if (syncRoomInfoSuccess && syncAllUserStreamSuccess &&
                            syncIncrementUserStreamSuccess && initUpdateSuccess) {
                        joinSuccess(localUser, studentJoinCallback as EduCallback<EduUser>)
                    }
                }

                override fun onFailure(code: Int, reason: String?) {
                    initUpdateSuccess = false
                    joinFailed(code, reason, studentJoinCallback as EduCallback<EduUser>)
                }
            })
        }
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
                /**如果当前用户是观众则调用unPublishStream(刷新服务器上可能存在的旧流)*/
                if (Convert.convertUserRole(localUser.userInfo.role, getCurRoomType())
                        == EduUserRoleStr.audience.value) {
                    callback.onSuccess(Unit)
                } else {
                    /**大班课场景下为audience,小班课一对一都是broadcaster*/
                    RteEngineImpl.setClientRole(roomInfo.roomUuid, if (getCurRoomType() !=
                            RoomType.LARGE_CLASS) CLIENT_ROLE_BROADCASTER else CLIENT_ROLE_AUDIENCE)
                    if (mediaOptions.isAutoPublish()) {
                        val code = RteEngineImpl.publish(roomInfo.roomUuid)
                        Log.e("EduRoomImpl", "publish: $code")
                    }
                    callback.onSuccess(Unit)
                }
            }

            override fun onFailure(code: Int, reason: String?) {
                callback.onFailure(code, reason)
            }
        })
    }

    /**判断joining状态防止多次调用*/
    private fun joinSuccess(eduUser: EduUser, callback: EduCallback<EduUser>) {
        if (joining) {
            joining = false
            synchronized(joinSuccess) {
                Log.e("EduStudentImpl", "加入房间成功")
                /**维护本地存储的在线人数*/
                roomStatus.onlineUsersCount = getCurUserList().size
                callback.onSuccess(eduUser as EduStudent)
                eventListener?.onRemoteUsersInitialized(getCurUserList(), this@EduRoomImpl)
                eventListener?.onRemoteStreamsInitialized(getCurStreamList(), this@EduRoomImpl)
                joinSuccess = true
                /**判断是否有缓存数据，如果有，通过对应的接口回调出去*/
                val iterable = addedStreams.iterator()
                while (iterable.hasNext()) {
                    val element = iterable.next()
                    val streamInfo = element.modifiedStream
                    /**判断是否推本地流*/
                    if (streamInfo.publisher == localUser.userInfo) {
                        CMDDispatch.updateLocalStream(streamInfo)
                        Log.e("EduRoomImpl", "join成功，把添加的本地流回调出去")
                        localUser.eventListener?.onLocalStreamAdded(element)
                        /**把本地流*/
                        iterable.remove()
                    }
                }
                if (addedStreams.size > 0) {
                    eventListener?.onRemoteStreamsAdded(addedStreams, this)
                }
            }
        }
    }

    /**join失败的情况下，清楚所有本地已存在的缓存数据；判断joining状态防止多次调用
     * 并退出rtm和rtc*/
    private fun joinFailed(code: Int, reason: String?, callback: EduCallback<EduUser>) {
        if (joining) {
            joining = false
            synchronized(joinSuccess) {
                joinSuccess = false
                clearData(false)
                callback.onFailure(code, reason)
            }
        }
    }

    /**获取本地缓存中，人流信息的最新更新时间*/
    private fun getLastUpdatetime(): Long {
        var lastTime: Long = 0
        for (element in getCurUserList()) {
            if ((element as EduUserInfoImpl).updateTime!! > lastTime) {
                lastTime = element.updateTime!!
            }
        }
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

    /**清楚本地缓存，离开RTM的当前频道；退出RTM*/
    private fun clearData(release: Boolean) {
        handler?.removeCallbacksAndMessages(null)
        getCurUserList().clear()
        getCurStreamList().clear()
        if (!leaveRoom) {
            RteEngineImpl[roomInfo.roomUuid]?.leave()
            leaveRoom = true
        }
        if (release) {
            RteEngineImpl[roomInfo.roomUuid]?.release()
        }
    }

    override fun getStudentCount(): Int {
        return getStudentList().size
    }

    override fun getTeacherCount(): Int {
        return getTeacherList().size
    }

    override fun getStudentList(): MutableList<EduUserInfo> {
        val studentList = mutableListOf<EduUserInfo>()
        for (element in getFullUserList()) {
            if (element.role == EduUserRole.STUDENT) {
                studentList.add(element)
            }
        }
        return studentList
    }

    override fun getTeacherList(): MutableList<EduUserInfo> {
        val teacherList = mutableListOf<EduUserInfo>()
        for (element in getFullUserList()) {
            if (element.role == EduUserRole.TEACHER) {
                teacherList.add(element)
            }
        }
        return teacherList
    }

    override fun getFullStreamList(): MutableList<EduStreamInfo> {
        return eduStreamInfoList
    }

    /**获取本地缓存的所有用户数据
     * 当第一个用户进入新房间(暂无用户的房间)的时候，不会有人流数据同步过来，此时如果调用此函数
     * 需要把本地用户手动添加进去*/
    override fun getFullUserList(): MutableList<EduUserInfo> {
        if (eduUserInfoList.size == 0) {
            eduUserInfoList.add(localUser.userInfo)
        }
        return eduUserInfoList
    }

    override fun leave() {
        clearData(false)
    }

    override fun release() {
        clearData(true)
        handler = null
        eventListener = null
        localUser.eventListener = null
    }

    override fun onChannelMsgReceived(p0: RtmMessage?, p1: RtmChannelMember?) {
        p0?.text?.let {
            CMDDispatch.dispatchChannelMsg(p0?.text, this, eventListener)
        }
    }

    override fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {
    }


    override fun onConnectionStateChanged(p0: Int, p1: Int) {
        if (lastConnectionState == RtmStatusCode.ConnectionState.CONNECTION_STATE_RECONNECTING &&
                p0 == RtmStatusCode.ConnectionState.CONNECTION_STATE_CONNECTED) {
            /**断线重连成功
             * 增量更新，重走数据同步的第二阶段，同步房间信息和增量数据*/
            isReconnected = true
            val lastTime = getLastUpdatetime()
            eduSyncRoomReq.nextTs = lastTime
            interruptRtmTimeout(false)
            launchSyncRoomReq(object : EduCallback<Unit> {
                override fun onSuccess(res: Unit?) {
                }

                override fun onFailure(code: Int, reason: String?) {
                    if (joining) {
                        /**join流程失败*/
                        joinFailed(code, reason, studentJoinCallback as EduCallback<EduUser>)
                    } else if (joinSuccess) {
                        /**无限重试，保证数据同步成功*/
                        launchSyncRoomReq(this)
                    }
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
