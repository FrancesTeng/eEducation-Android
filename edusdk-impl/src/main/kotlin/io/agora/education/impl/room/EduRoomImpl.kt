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
                        /***/
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
                        val classRoomEntryRes = res?.data!!
                        USERTOKEN = classRoomEntryRes.userToken
                        RTMTOKEN = classRoomEntryRes.rtmToken
                        RTCTOKEN = classRoomEntryRes.rtcToken
                        /**加入rte(包括rtm和rtc)*/
                        val channelMediaOptions = ChannelMediaOptions()
                        channelMediaOptions.autoSubscribeAudio = options.mediaOptions.autoSubscribeAudio
                        channelMediaOptions.autoSubscribeVideo = options.mediaOptions.autoSubscribeVideo
                        joinRte(RTCTOKEN, options.userUuid.toInt(), channelMediaOptions)
                        /***/
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

    override fun getFullStreamList(nextId: String, count: Int, callback: EduCallback<MutableList<EduStreamInfo>>) {
        RetrofitManager.instance().getService(API_BASE_URL, UserService::class.java)
                .getStreamList(USERTOKEN, APPID, roomInfo.roomUuid, nextId, count)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<EduStreamListRes>> {
                    override fun onSuccess(res: ResponseBody<EduStreamListRes>?) {
                        /**转换类型*/
                        val streamInfoList: MutableList<EduStreamInfo> = Convert.getStreamInfoList(res?.data, (roomInfo as EduRoomInfoImpl).roomType)
                        callback.onSuccess(streamInfoList)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        val error = throwable as BusinessException
                        callback.onFailure(error.code, error.message)
                    }
                }))
    }

    override fun getFullUserList(nextId: String, count: Int, callback: EduCallback<MutableList<EduUserInfo>>) {
        RetrofitManager.instance().getService(API_BASE_URL, UserService::class.java)
                .getUserList(USERTOKEN, APPID, roomInfo.roomUuid, nextId, count)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<EduUserListRes>> {
                    override fun onSuccess(res: ResponseBody<EduUserListRes>?) {
                        /**转换类型*/
                        val userList: MutableList<EduUserInfo>? = Convert.getUserInfoList(res?.data, (roomInfo as EduRoomInfoImpl).roomType)
                        callback.onSuccess(userList)
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
                        /**处理rte*/
                        RteEngineImpl[roomInfo.roomUuid]?.leave()
                        RteEngineImpl[roomInfo.roomUuid]?.release()
                        callback.onSuccess(Unit)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        val error = throwable as BusinessException
                        callback.onFailure(error.code, error.message)
                    }
                }))
    }
}
