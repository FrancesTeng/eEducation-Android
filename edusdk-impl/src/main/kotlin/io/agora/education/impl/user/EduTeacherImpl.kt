package io.agora.education.impl.user

import io.agora.Constants.Companion.API_BASE_URL
import io.agora.Constants.Companion.APPID
import io.agora.Constants.Companion.USERTOKEN
import io.agora.base.callback.ThrowableCallback
import io.agora.base.network.BusinessException
import io.agora.base.network.ResponseBody
import io.agora.base.network.RetrofitManager
import io.agora.education.api.EduCallback
import io.agora.education.api.room.data.EduRoomState
import io.agora.education.api.stream.data.AudioSourceType
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.stream.data.ScreenStreamInitOptions
import io.agora.education.api.stream.data.VideoSourceType
import io.agora.education.api.user.EduTeacher
import io.agora.education.api.user.data.EduChatState
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.api.user.listener.EduTeacherEventListener
import io.agora.Convert
import io.agora.education.impl.room.network.RoomService
import io.agora.education.impl.stream.network.StreamService
import io.agora.education.impl.user.data.request.EduRoomMuteStateReq
import io.agora.education.impl.user.data.request.EduStreamStatusReq
import io.agora.education.impl.user.data.request.EduUserStatusReq
import io.agora.education.impl.user.network.UserService

internal class EduTeacherImpl(
        userInfo: EduUserInfo
) : EduUserImpl(userInfo), EduTeacher {

    override fun setEventListener(eventListener: EduTeacherEventListener) {
        this.eventListener = eventListener
    }

    override fun beginClass(callback: EduCallback<Unit>) {
        RetrofitManager.instance().getService(API_BASE_URL, RoomService::class.java)
                .updateClassroomState(USERTOKEN, APPID, roomInfo.roomUuid, EduRoomState.START.value)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        callback.onSuccess(Unit)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        val error = throwable as BusinessException
                        callback.onFailure(error.code, error.message)
                    }
                }))
    }

    override fun endClass(callback: EduCallback<Unit>) {
        RetrofitManager.instance().getService(API_BASE_URL, RoomService::class.java)
                .updateClassroomState(USERTOKEN, APPID, roomInfo.roomUuid, EduRoomState.END.value)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        callback.onSuccess(Unit)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        val error = throwable as BusinessException
                        callback.onFailure(error.code, error.message)
                    }
                }))
    }

    override fun allowStudentChat(isAllow: Boolean, callback: EduCallback<Unit>) {
        val eduRoomStatusReq = EduRoomMuteStateReq(EduChatState.Disable)
        RetrofitManager.instance().getService(API_BASE_URL, RoomService::class.java)
                .updateClassroomMuteState(USERTOKEN, APPID, roomInfo.roomUuid, eduRoomStatusReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        callback.onSuccess(Unit)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        val error = throwable as BusinessException
                        callback.onFailure(error.code, error.message)
                    }
                }))
    }

    override fun allowStudentChat(isAllow: Boolean, user: EduUserInfo, callback: EduCallback<Unit>) {
        val role = Convert.convertUserRole(user.role, roomInfo.roomType)
        val eduUserStatusReq = EduUserStatusReq(user.userName, if(isAllow) 0 else 1, role)
        RetrofitManager.instance().getService(API_BASE_URL, UserService::class.java)
                .updateUserMuteState(USERTOKEN, APPID, roomInfo.roomUuid, user.userUuid, eduUserStatusReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        callback.onSuccess(Unit)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        val error = throwable as BusinessException
                        callback.onFailure(error.code, error.message)
                    }
                }))
    }

    override fun startShareScreen(options: ScreenStreamInitOptions, callback: EduCallback<EduStreamInfo>) {
        TODO("Not yet implemented")
    }

    override fun stopShareScreen(callback: EduCallback<Unit>) {
        TODO("Not yet implemented")
    }

    override fun openStudentCamera(stream: EduStreamInfo, callback: EduCallback<Unit>) {
        stream.videoSourceType = VideoSourceType.CAMERA
        stream.hasVideo = true
        val eduStreamStatusReq = EduStreamStatusReq(stream.streamName, stream.videoSourceType.value,
                AudioSourceType.MICROPHONE.value, if (stream.hasVideo) 1 else 0, if (stream.hasAudio) 1 else 0)
        RetrofitManager.instance().getService(API_BASE_URL, StreamService::class.java)
                .updateStreamInfo(USERTOKEN, APPID, roomInfo.roomUuid, stream.publisher.userUuid,
                                          stream.streamUuid, eduStreamStatusReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        callback.onSuccess(Unit)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        val error = throwable as BusinessException
                        callback.onFailure(error.code, error.message)
                    }
                }))
    }

    override fun closeStudentCamera(stream: EduStreamInfo, callback: EduCallback<Unit>) {
        stream.videoSourceType = VideoSourceType.CAMERA
        stream.hasVideo = false
        val eduStreamStatusReq = EduStreamStatusReq(stream.streamName, stream.videoSourceType.value,
                AudioSourceType.MICROPHONE.value, if (stream.hasVideo) 1 else 0, if (stream.hasAudio) 1 else 0)
        RetrofitManager.instance().getService(API_BASE_URL, StreamService::class.java)
                .updateStreamInfo(USERTOKEN, APPID, roomInfo.roomUuid, stream.publisher.userUuid,
                                          stream.streamUuid, eduStreamStatusReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        callback.onSuccess(Unit)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        val error = throwable as BusinessException
                        callback.onFailure(error.code, error.message)
                    }
                }))
    }

    override fun openStudentMicrophone(stream: EduStreamInfo, callback: EduCallback<Unit>) {
        stream.hasAudio = true
        val eduStreamStatusReq = EduStreamStatusReq(stream.streamName, stream.videoSourceType.value,
                AudioSourceType.MICROPHONE.value, if (stream.hasVideo) 1 else 0, if (stream.hasAudio) 1 else 0)
        RetrofitManager.instance().getService(API_BASE_URL, StreamService::class.java)
                .updateStreamInfo(USERTOKEN, APPID, roomInfo.roomUuid, stream.publisher.userUuid,
                                              stream.streamUuid, eduStreamStatusReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        callback.onSuccess(Unit)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        val error = throwable as BusinessException
                        callback.onFailure(error.code, error.message)
                    }
                }))
    }

    override fun closeStudentMicrophone(stream: EduStreamInfo, callback: EduCallback<Unit>) {
        stream.hasAudio = false
        val eduStreamStatusReq = EduStreamStatusReq(stream.streamName, stream.videoSourceType.value,
                AudioSourceType.MICROPHONE.value, if (stream.hasVideo) 1 else 0, if (stream.hasAudio) 1 else 0)
        RetrofitManager.instance().getService(API_BASE_URL, StreamService::class.java)
                .updateStreamInfo(USERTOKEN, APPID, roomInfo.roomUuid, stream.publisher.userUuid,
                                              stream.streamUuid, eduStreamStatusReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        callback.onSuccess(Unit)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        val error = throwable as BusinessException
                        callback.onFailure(error.code, error.message)
                    }
                }))
    }
}
