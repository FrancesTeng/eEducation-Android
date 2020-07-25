package io.agora.education.impl.room

import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.agora.Constants.Companion.API_BASE_URL
import io.agora.Constants.Companion.APPID
import io.agora.Constants.Companion.RTCTOKEN
import io.agora.Constants.Companion.RTMTOKEN
import io.agora.Constants.Companion.USERTOKEN
import io.agora.base.callback.ThrowableCallback
import io.agora.base.network.BusinessException
import io.agora.base.network.RetrofitManager
import io.agora.education.api.EduCallback
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.*
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.user.EduStudent
import io.agora.education.api.user.EduTeacher
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.api.user.data.EduUserRole
import io.agora.Convert
import io.agora.education.api.stream.data.LocalStreamInitOptions
import io.agora.education.api.user.data.EduChatState
import io.agora.education.impl.ResponseBody
import io.agora.education.impl.board.EduBoardImpl
import io.agora.education.impl.cmd.*
import io.agora.education.impl.record.EduRecordImpl
import io.agora.education.impl.room.data.EduRoomInfoImpl
import io.agora.education.impl.room.data.request.EduJoinClassroomReq
import io.agora.education.impl.room.data.response.EduEntryRes
import io.agora.education.impl.room.data.response.EduStreamListRes
import io.agora.education.impl.room.data.response.EduUserListRes
import io.agora.education.impl.user.EduStudentImpl
import io.agora.education.impl.user.EduTeacherImpl
import io.agora.education.impl.user.EduUserImpl
import io.agora.education.impl.user.network.UserService
import io.agora.rtc.models.ChannelMediaOptions
import io.agora.rte.RteChannelEventListener
import io.agora.rte.RteEngineEventListener
import io.agora.rte.RteEngineImpl
import io.agora.rtm.RtmChannelMember
import io.agora.rtm.RtmMessage
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

    private var eduUserInfoList = Collections.synchronizedList(mutableListOf<EduUserInfo>())
    private var eduStreamInfoList = Collections.synchronizedList(mutableListOf<EduStreamInfo>())
    private val count = 1000

    fun getCurRoomType(): RoomType {
        return (roomInfo as EduRoomInfoImpl).roomType
    }

    fun getCurUserList(): MutableList<EduUserInfo> {
        return eduUserInfoList
    }

    fun getCurStreamList(): MutableList<EduStreamInfo> {
        return eduStreamInfoList
    }

    override fun joinClassroomAsTeacher(options: RoomJoinOptions, callback: EduCallback<EduTeacher>) {
        /**用户传了primaryStreamId,那么就用他当做streamUuid;如果没传，
         * 则把userUuid赋值给primaryStreamId当做streamUuid*/
        if (TextUtils.isEmpty(options.mediaOptions.primaryStreamId)) {
            options.mediaOptions.primaryStreamId = options.userUuid
        }
        val localUserInfo = EduUserInfo(options.userUuid, options.userName, EduUserRole.TEACHER, null)
        /**此处需要把localUserInfo设置进localUser中*/
        localUser = EduUserImpl(localUserInfo)
        (localUser as EduUserImpl).roomMediaOptions = options.mediaOptions
        /**根据classroomType和用户传的角色值转化出一个角色字符串来和后端交互*/
        val roomType = getCurRoomType()
        val role = Convert.convertUserRole(localUserInfo.role, roomType)
        val eduJoinClassroomReq = EduJoinClassroomReq(localUserInfo.userName,
                role, options.mediaOptions.primaryStreamId)
        RetrofitManager.instance().getService(API_BASE_URL, UserService::class.java)
                .joinClassroom(APPID, roomInfo.roomUuid, localUserInfo.userUuid, eduJoinClassroomReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<EduEntryRes>> {
                    override fun onSuccess(res: ResponseBody<EduEntryRes>?) {
                        val classRoomEntryRes = res?.data!!
                        /**设置全局静态数据*/
                        USERTOKEN = classRoomEntryRes.user.userToken
                        RTMTOKEN = classRoomEntryRes.user.rtmToken
                        RTCTOKEN = classRoomEntryRes.user.rtcToken
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
                        joinRte(RTCTOKEN, options.userUuid.toInt(), channelMediaOptions)
                        /**同步用户和流的全量数据*/
                        syncUserList(null, count, object : EduCallback<Unit> {
                            override fun onSuccess(res: Unit?) {
                                eventListener?.onRemoteUsersInitialized(eduUserInfoList, this@EduRoomImpl)
                            }

                            override fun onFailure(code: Int, reason: String?) {
                                TODO("Not yet implemented")
                            }
                        })
                        syncStreamList(null, count, object : EduCallback<Unit> {
                            override fun onSuccess(res: Unit?) {
                                eventListener?.onRemoteStreamsInitialized(eduStreamInfoList, this@EduRoomImpl)
                            }

                            override fun onFailure(code: Int, reason: String?) {
                                TODO("Not yet implemented")
                            }
                        })
                        callback.onSuccess(localUser as EduTeacherImpl)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        val error = throwable as BusinessException
                        callback.onFailure(error.code, error.message)
                    }
                }))
    }

    override fun joinClassroomAsStudent(options: RoomJoinOptions, callback: EduCallback<EduStudent>) {
        /**用户传了primaryStreamId,那么就用他当做streamUuid;如果没传，
         * 则把userUuid赋值给primaryStreamId当做streamUuid*/
        if (TextUtils.isEmpty(options.mediaOptions.primaryStreamId)) {
            options.mediaOptions.primaryStreamId = options.userUuid
        }
        val localUserInfo = EduUserInfo(options.userUuid, options.userName, EduUserRole.STUDENT, null)
        /**此处需要把localUserInfo设置进localUser中*/
        localUser = EduUserImpl(localUserInfo)
        (localUser as EduUserImpl).roomMediaOptions = options.mediaOptions
        /**根据classroomType和用户传的角色值转化出一个角色字符串来和后端交互*/
        val roomType = getCurRoomType()
        val role = Convert.convertUserRole(localUserInfo.role, roomType)
        val eduJoinClassroomReq = EduJoinClassroomReq(localUserInfo.userName, role,
                options.mediaOptions.primaryStreamId)
        RetrofitManager.instance().getService(API_BASE_URL, UserService::class.java)
                .joinClassroom(APPID, roomInfo.roomUuid, localUserInfo.userUuid, eduJoinClassroomReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<EduEntryRes>> {
                    override fun onSuccess(res: ResponseBody<EduEntryRes>?) {
                        val classRoomEntryRes = res?.data!!
                        /**解析返回的user相关数据*/
                        USERTOKEN = classRoomEntryRes.user.userToken
                        RTMTOKEN = classRoomEntryRes.user.rtmToken
                        RTCTOKEN = classRoomEntryRes.user.rtcToken
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
                        joinRte(RTCTOKEN, options.userUuid.toInt(), channelMediaOptions)
                        /**同步用户和流的全量数据*/
                        syncUserList(null, count, object : EduCallback<Unit> {
                            override fun onSuccess(res: Unit?) {
                                /**维护本地存储的在线人数*/
                                roomStatus.onlineUsersCount = eduUserInfoList.size
                                eventListener?.onRemoteUsersInitialized(eduUserInfoList, this@EduRoomImpl)
                            }

                            override fun onFailure(code: Int, reason: String?) {
                                TODO("Not yet implemented")
                            }
                        })
                        syncStreamList(null, count, object : EduCallback<Unit> {
                            override fun onSuccess(res: Unit?) {
                                eventListener?.onRemoteStreamsInitialized(eduStreamInfoList, this@EduRoomImpl)
                            }

                            override fun onFailure(code: Int, reason: String?) {
                                TODO("Not yet implemented")
                            }
                        })
                        /**初始化本地用户的本地流
                         * 此处的options数据从哪来呢？*/
                        localUser.initOrUpdateLocalStream(LocalStreamInitOptions(), object : EduCallback<EduStreamInfo> {
                            override fun onSuccess(res: EduStreamInfo?) {
                                TODO("Not yet implemented")
                            }

                            override fun onFailure(code: Int, reason: String?) {
                                TODO("Not yet implemented")
                            }
                        })
                        callback.onSuccess(localUser as EduStudentImpl)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        val error = throwable as BusinessException
                        callback.onFailure(error.code, error.message)
                    }
                }))
    }

    private fun joinRte(rtcToken: String, uid: Int, channelMediaOptions: ChannelMediaOptions) {
        RteEngineImpl[roomInfo.roomUuid]?.join(rtcToken, uid, channelMediaOptions)
    }

    override fun getFullStreamList(): MutableList<EduStreamInfo> {
        return eduStreamInfoList
    }

    /**递归调用，同步全量数据*/
    private fun syncStreamList(nextId: String?, count: Int, callback: EduCallback<Unit>) {
        RetrofitManager.instance().getService(API_BASE_URL, UserService::class.java)
                .getStreamList(USERTOKEN, APPID, roomInfo.roomUuid, nextId, count)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<EduStreamListRes>> {
                    override fun onSuccess(res: ResponseBody<EduStreamListRes>?) {
                        val eduStreamListRes = res?.data

                        /**转换类型*/
                        val streamInfoList = Convert.getStreamInfoList(res?.data, getCurRoomType())
                        eduStreamInfoList.addAll(streamInfoList)
                        if (eduStreamInfoList.size < eduStreamListRes?.total!!) {
                            syncStreamList(eduStreamListRes.nextId, count, callback)
                        } else {
                            /**拉去全量用户数据成功完成*/
                            callback.onSuccess(Unit)
                        }
                    }

                    override fun onFailure(throwable: Throwable?) {
                        val error = throwable as BusinessException
                        callback.onFailure(error.code, error.message)
                    }
                }))
    }

    override fun getFullUserList(): MutableList<EduUserInfo> {
        return eduUserInfoList
    }

    /**递归调用，同步全量数据*/
    private fun syncUserList(nextId: String?, count: Int, callback: EduCallback<Unit>) {
        RetrofitManager.instance().getService(API_BASE_URL, UserService::class.java)
                .getUserList(USERTOKEN, APPID, roomInfo.roomUuid, nextId, count)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<EduUserListRes>> {
                    override fun onSuccess(res: ResponseBody<EduUserListRes>?) {
                        val eduUserListRes = res?.data

                        /**转换类型*/
                        val userInfoList = Convert.getUserInfoList(res?.data, getCurRoomType())
                        eduUserInfoList.addAll(userInfoList)
                        if (eduUserInfoList.size < eduUserListRes?.total!!) {
                            syncUserList(eduUserListRes.nextId, count, callback)
                        } else {
                            /**拉去全量用户数据成功完成*/
                            callback.onSuccess(Unit)
                        }
                    }

                    override fun onFailure(throwable: Throwable?) {
                        val error = throwable as BusinessException
                        callback.onFailure(error.code, error.message)
                    }
                }))
    }

    override fun leave(callback: EduCallback<Unit>) {
        RetrofitManager.instance().getService(API_BASE_URL, UserService::class.java)
                .leaveClassroom(USERTOKEN, APPID, roomInfo.roomUuid, localUser.userInfo.userUuid)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<io.agora.base.network.ResponseBody<String>> {
                    override fun onSuccess(res: io.agora.base.network.ResponseBody<String>?) {
                        RteEngineImpl[roomInfo.roomUuid]?.leave()
                        RteEngineImpl[roomInfo.roomUuid]?.release()
                        /**此处也是会有离开房间的RTM的通知的，但是离开房间只是通知一下后台并不依赖RTM通知，
                         * 所以此处的回调可以作为成功离开房间的依据*/
                        callback.onSuccess(Unit)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        val error = throwable as BusinessException
                        callback.onFailure(error.code, error.message)
                    }
                }))
    }


    override fun onChannelMsgReceived(p0: RtmMessage?, p1: RtmChannelMember?) {
        val text = p0?.text
        val cmdResponseBody = Gson().fromJson<CMDResponseBody<String>>(text,
                object : TypeToken<CMDResponseBody<String>>() {}.type)
        CMDDispatch.dispatch(cmdResponseBody, this, eventListener)
    }

    override fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {
        TODO("Not yet implemented")
    }

    override fun onConnectionStateChanged(p0: Int, p1: Int) {
        TODO("Not yet implemented")
    }

    override fun onPeerMsgReceived(p0: RtmMessage?, p1: String?) {
        TODO("Not yet implemented")
    }
}
