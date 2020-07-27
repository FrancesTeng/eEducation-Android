package io.agora.education.impl.user

import io.agora.Constants.Companion.API_BASE_URL
import io.agora.Constants.Companion.APPID
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
import io.agora.education.api.user.data.EduUserInfo
import io.agora.Convert
import io.agora.education.api.room.data.EduMuteState
import io.agora.education.api.user.listener.EduTeacherEventListener
import io.agora.education.impl.room.data.EduRoomInfoImpl
import io.agora.education.impl.room.network.RoomService
import io.agora.education.impl.stream.network.StreamService
import io.agora.education.impl.user.data.request.EduRoomMuteStateReq
import io.agora.education.impl.user.data.request.EduStreamStatusReq
import io.agora.education.impl.user.data.request.EduUserStatusReq
import io.agora.education.impl.user.data.request.RoleMuteConfig
import io.agora.education.impl.user.network.UserService

internal class EduTeacherImpl(
        userInfo: EduUserInfo
) : EduUserImpl(userInfo), EduTeacher {
    override fun setEventListener(eventListener: EduTeacherEventListener) {
        this.eventListener = eventListener
    }

    override fun beginClass(callback: EduCallback<Unit>) {
        RetrofitManager.instance().getService(API_BASE_URL, RoomService::class.java)
                .updateClassroomState(USERTOKEN, APPID, eduRoom.roomInfo.roomUuid, EduRoomState.START.value)
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
                .updateClassroomState(USERTOKEN, APPID, eduRoom.roomInfo.roomUuid, EduRoomState.END.value)
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
        val chatState = if (isAllow) EduMuteState.Enable else EduMuteState.Disable
        val eduRoomStatusReq = EduRoomMuteStateReq(
                RoleMuteConfig(null, EduMuteState.Disable.value.toString(), EduMuteState.Disable.value.toString()),
                null, null)
        RetrofitManager.instance().getService(API_BASE_URL, RoomService::class.java)
                .updateClassroomMuteState(USERTOKEN, APPID, eduRoom.roomInfo.roomUuid, eduRoomStatusReq)
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

    override fun allowRemoteStudentChat(isAllow: Boolean, remoteStudent: EduUserInfo, callback: EduCallback<Unit>) {
        /***/
        val role = Convert.convertUserRole(remoteStudent.role, (eduRoom.roomInfo as EduRoomInfoImpl).roomType,
                false)
        val eduUserStatusReq = EduUserStatusReq(remoteStudent.userName, if (isAllow) 0 else 1, role)
        RetrofitManager.instance().getService(API_BASE_URL, UserService::class.java)
                .updateUserMuteState(USERTOKEN, APPID, eduRoom.roomInfo.roomUuid, remoteStudent.userUuid, eduUserStatusReq)
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

    override fun remoteStartStudentCamera(remoteStream: EduStreamInfo, callback: EduCallback<Unit>) {
        remoteStream.videoSourceType = VideoSourceType.CAMERA
        remoteStream.hasVideo = true
        val eduStreamStatusReq = EduStreamStatusReq(remoteStream.streamName, remoteStream.videoSourceType.value,
                AudioSourceType.MICROPHONE.value, if (remoteStream.hasVideo) 1 else 0, if (remoteStream.hasAudio) 1 else 0)
        RetrofitManager.instance().getService(API_BASE_URL, StreamService::class.java)
                .updateStreamInfo(USERTOKEN, APPID, eduRoom.roomInfo.roomUuid, remoteStream.publisher.userUuid,
                        remoteStream.streamUuid, eduStreamStatusReq)
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

    override fun remoteStopStudentCamera(remoteStream: EduStreamInfo, callback: EduCallback<Unit>) {
        remoteStream.videoSourceType = VideoSourceType.CAMERA
        remoteStream.hasVideo = false
        val eduStreamStatusReq = EduStreamStatusReq(remoteStream.streamName, remoteStream.videoSourceType.value,
                AudioSourceType.MICROPHONE.value, if (remoteStream.hasVideo) 1 else 0, if (remoteStream.hasAudio) 1 else 0)
        RetrofitManager.instance().getService(API_BASE_URL, StreamService::class.java)
                .updateStreamInfo(USERTOKEN, APPID, eduRoom.roomInfo.roomUuid, remoteStream.publisher.userUuid,
                        remoteStream.streamUuid, eduStreamStatusReq)
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

    override fun remoteStartStudentMicrophone(remoteStream: EduStreamInfo, callback: EduCallback<Unit>) {
        remoteStream.hasAudio = true
        val eduStreamStatusReq = EduStreamStatusReq(remoteStream.streamName, remoteStream.videoSourceType.value,
                AudioSourceType.MICROPHONE.value, if (remoteStream.hasVideo) 1 else 0, if (remoteStream.hasAudio) 1 else 0)
        RetrofitManager.instance().getService(API_BASE_URL, StreamService::class.java)
                .updateStreamInfo(USERTOKEN, APPID, eduRoom.roomInfo.roomUuid, remoteStream.publisher.userUuid,
                        remoteStream.streamUuid, eduStreamStatusReq)
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

    override fun remoteStopStudentMicrophone(remoteStream: EduStreamInfo, callback: EduCallback<Unit>) {
        remoteStream.hasAudio = false
        val eduStreamStatusReq = EduStreamStatusReq(remoteStream.streamName, remoteStream.videoSourceType.value,
                AudioSourceType.MICROPHONE.value, if (remoteStream.hasVideo) 1 else 0, if (remoteStream.hasAudio) 1 else 0)
        RetrofitManager.instance().getService(API_BASE_URL, StreamService::class.java)
                .updateStreamInfo(USERTOKEN, APPID, eduRoom.roomInfo.roomUuid, remoteStream.publisher.userUuid,
                        remoteStream.streamUuid, eduStreamStatusReq)
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
