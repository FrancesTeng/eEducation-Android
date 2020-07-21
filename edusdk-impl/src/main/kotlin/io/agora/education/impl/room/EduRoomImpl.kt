package io.agora.education.impl.room

import android.text.TextUtils
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
import io.agora.education.api.room.data.EduRoomInfo
import io.agora.education.api.room.data.EduRoomStatus
import io.agora.education.api.room.data.RoomJoinOptions
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.user.EduStudent
import io.agora.education.api.user.EduTeacher
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.api.user.data.EduUserRole
import io.agora.education.impl.Convert
import io.agora.education.impl.ResponseBody
import io.agora.education.impl.board.EduBoardImpl
import io.agora.education.impl.record.EduRecordImpl
import io.agora.education.impl.room.data.EduRoomInfoImpl
import io.agora.education.impl.room.data.request.EduJoinClassroomReq
import io.agora.education.impl.room.data.response.*
import io.agora.education.impl.user.EduStudentImpl
import io.agora.education.impl.user.EduTeacherImpl
import io.agora.education.impl.user.EduUserImpl
import io.agora.education.impl.user.network.UserService
import io.agora.rtc.models.ChannelMediaOptions
import io.agora.rte.RteEngineImpl

internal class EduRoomImpl(
        roomInfo: EduRoomInfo,
        roomStatus: EduRoomStatus
) : EduRoom(roomInfo, roomStatus) {

    init {
        RteEngineImpl.createChannel(roomInfo.roomUuid)
        record = EduRecordImpl()
        board = EduBoardImpl()
    }

    private var eduUserInfoList = mutableListOf<EduUserInfo>()
    private var eduStreamInfoList = mutableListOf<EduStreamInfo>()
    private val count = 20

    override fun joinClassroomAsTeacher(options: RoomJoinOptions, callback: EduCallback<EduTeacher>) {
        /**用户传了primaryStreamId,那么就用他当做streamUuid;如果没传，
         * 则把userUuid赋值给primaryStreamId当做streamUuid*/
        if (TextUtils.isEmpty(options.mediaOptions.primaryStreamId)) {
            options.mediaOptions.primaryStreamId = options.userUuid
        }
        val localUserInfo = EduUserInfo(options.userUuid, options.userName, EduUserRole.TEACHER)
        /**此处需要把localUserInfo设置进localUser中*/
        localUser = EduUserImpl(localUserInfo)
        (localUser as EduUserImpl).roomMediaOptions = options.mediaOptions
        /**根据classroomType和用户传的角色值转化出一个角色字符串来和后端交互*/
        val roomType = (roomInfo as EduRoomInfoImpl).roomType
        val role = Convert.convertUserRole(localUserInfo.role, roomType)
        val eduJoinClassroomReq = EduJoinClassroomReq(localUserInfo.userName,
                role, options.mediaOptions.primaryStreamId)
        RetrofitManager.instance().getService(API_BASE_URL, UserService::class.java)
                .joinClassroom(APPID, roomInfo.roomUuid, localUserInfo.userUuid, eduJoinClassroomReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<EduClassRoomEntryRes>> {
                    override fun onSuccess(res: ResponseBody<EduClassRoomEntryRes>?) {
                        /**接口调用成功，把localUser对象回调出去，仅代表接口调用成功，不作为成功加入房间的标志；
                         * 成功加入房间与否依赖于RTM的通知*/
                        callback.onSuccess(localUser as EduTeacherImpl)
                        val classRoomEntryRes = res?.data!!
                        /**设置全局静态数据*/
                        USERTOKEN = classRoomEntryRes.userToken
                        RTMTOKEN = classRoomEntryRes.rtmToken
                        RTCTOKEN = classRoomEntryRes.rtcToken
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
        val localUserInfo = EduUserInfo(options.userUuid, options.userName, EduUserRole.STUDENT)
        /**此处需要把localUserInfo设置进localUser中*/
        localUser = EduUserImpl(localUserInfo)
        (localUser as EduUserImpl).roomMediaOptions = options.mediaOptions
        /**根据classroomType和用户传的角色值转化出一个角色字符串来和后端交互*/
        val roomType = (roomInfo as EduRoomInfoImpl).roomType
        val role = Convert.convertUserRole(localUserInfo.role, roomType)
        val eduJoinClassroomReq = EduJoinClassroomReq(localUserInfo.userName, role,
                options.mediaOptions.primaryStreamId)
        RetrofitManager.instance().getService(API_BASE_URL, UserService::class.java)
                .joinClassroom(APPID, roomInfo.roomUuid, localUserInfo.userUuid, eduJoinClassroomReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<EduClassRoomEntryRes>> {
                    override fun onSuccess(res: ResponseBody<EduClassRoomEntryRes>?) {
                        /**接口调用成功，把localUser对象回调出去，仅代表接口调用成功，不作为成功加入房间的标志；
                         * 成功加入房间与否依赖于RTM的通知*/
                        callback.onSuccess(localUser as EduStudentImpl)
                        val classRoomEntryRes = res?.data!!
                        USERTOKEN = classRoomEntryRes.userToken
                        RTMTOKEN = classRoomEntryRes.rtmToken
                        RTCTOKEN = classRoomEntryRes.rtcToken
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
                        val streamInfoList = Convert.getStreamInfoList(res?.data, (roomInfo as EduRoomInfoImpl).roomType)
                        eduStreamInfoList.addAll(streamInfoList)
                        if(eduStreamInfoList.size < eduStreamListRes?.total!!)
                        {
                            syncStreamList(eduStreamListRes?.nextId!!, count, callback)
                        }
                        else
                        {
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
                        val userInfoList = Convert.getUserInfoList(res?.data, (roomInfo as EduRoomInfoImpl).roomType)
                        eduUserInfoList.addAll(userInfoList)
                        if(eduUserInfoList.size < eduUserListRes?.total!!)
                        {
                            syncUserList(eduUserListRes?.nextId!!, count, callback)
                        }
                        else
                        {
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
}
