package io.agora.education.impl.user

import android.view.ViewGroup
import io.agora.Constants.Companion.API_BASE_URL
import io.agora.Constants.Companion.APPID
import io.agora.Convert
import io.agora.base.callback.ThrowableCallback
import io.agora.base.network.BusinessException
import io.agora.base.network.ResponseBody
import io.agora.base.network.RetrofitManager
import io.agora.education.api.EduCallback
import io.agora.education.api.message.EduChatMsgType
import io.agora.education.api.message.EduMsg
import io.agora.education.api.stream.data.*
import io.agora.education.api.user.EduUser
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.api.user.listener.EduUserEventListener
import io.agora.education.impl.room.EduRoomImpl
import io.agora.education.impl.room.network.RoomService
import io.agora.education.impl.stream.network.StreamService
import io.agora.education.impl.user.data.request.*
import io.agora.education.impl.user.data.request.EduRoomChatMsgReq
import io.agora.education.impl.user.data.request.EduRoomMsgReq
import io.agora.education.impl.user.data.request.EduUserMsgReq
import io.agora.rtc.Constants
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rte.RteEngineImpl

internal open class EduUserImpl(
        override var userInfo: EduUserInfo
) : EduUser {
    override var videoEncoderConfig = VideoEncoderConfig()

    override var eventListener: EduUserEventListener? = null

    lateinit var eduRoom: EduRoomImpl
    lateinit var USERTOKEN: String

    override fun initOrUpdateLocalStream(options: LocalStreamInitOptions, callback: EduCallback<EduStreamInfo>) {
        RteEngineImpl.rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        RteEngineImpl.rtcEngine.setVideoEncoderConfiguration(
                Convert.convertVideoEncoderConfig(videoEncoderConfig))
        RteEngineImpl.rtcEngine.enableVideo()
        /**enableCamera和enableMicrophone控制是否打开摄像头和麦克风*/
        RteEngineImpl.rtcEngine.enableLocalVideo(options.enableCamera)
        RteEngineImpl.rtcEngine.enableLocalAudio(options.enableMicrophone)

        /**根据当前配置生成一个流信息*/
        val streamInfo = EduStreamInfo(options.streamUuid, options.streamName, VideoSourceType.CAMERA,
                options.enableCamera, options.enableMicrophone, this.userInfo)
        callback.onSuccess(streamInfo)
    }

    override fun switchCamera() {
        RteEngineImpl.rtcEngine.switchCamera()
    }

    override fun subscribeStream(stream: EduStreamInfo, options: StreamSubscribeOptions) {
        /**订阅远端流*/
        RteEngineImpl.rtcEngine.muteRemoteAudioStream(stream.streamUuid.toInt(), options.subscribeAudio)
        RteEngineImpl.rtcEngine.muteRemoteVideoStream(stream.streamUuid.toInt(), options.subscribeVideo)
    }

    override fun unSubscribeStream(stream: EduStreamInfo) {
        RteEngineImpl.rtcEngine.muteRemoteAudioStream(stream.streamUuid.toInt(), true)
        RteEngineImpl.rtcEngine.muteRemoteVideoStream(stream.streamUuid.toInt(), true)
    }

    /**
     * 修改本地流状态流程（新建流的情况下先走接口再走SDK本地mute）
     * 如果mute自己的时候，对应是先mute本地，再网络请求， 如果请求失败，提示客户。  客户自己手动重试。
     * 如果unmute自己的时候，对应先网络请求， 如果请求失败，提示客户。  否则开启推流。
     * 目前流程和设计不符，需要确认！！！
     * */
    override fun publishStream(streamInfo: EduStreamInfo, callback: EduCallback<Boolean>) {
        val eduStreamStatusReq = EduStreamStatusReq(streamInfo.streamName, streamInfo.videoSourceType.value,
                AudioSourceType.MICROPHONE.value, if (streamInfo.hasVideo) 1 else 0,
                if (streamInfo.hasAudio) 1 else 0)
        var pos = eduRoom.streamExistsInLocal(streamInfo)
        if (pos > -1) {
            /**流信息存在于本地，说明是更新*/
            if (eduRoom.getCurStreamList()[pos].hasAudio || eduRoom.getCurStreamList()[pos].hasVideo) {
                /**unMute*/
                RetrofitManager.instance().getService(API_BASE_URL, StreamService::class.java)
                        .updateStreamInfo(USERTOKEN, APPID, eduRoom.roomInfo.roomUuid, userInfo.userUuid,
                                streamInfo.streamUuid, eduStreamStatusReq)
                        .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                            override fun onSuccess(res: ResponseBody<String>?) {
                                RteEngineImpl.rtcEngine.muteLocalVideoStream(!streamInfo.hasVideo)
                                RteEngineImpl.rtcEngine.muteLocalAudioStream(!streamInfo.hasAudio)
                                callback.onSuccess(true)
                            }

                            override fun onFailure(throwable: Throwable?) {
                                val error = throwable as BusinessException
                                callback.onFailure(error.code, error.message)
                            }
                        }))
            } else {
                /**mute*/
                RteEngineImpl.rtcEngine.muteLocalVideoStream(!streamInfo.hasVideo)
                RteEngineImpl.rtcEngine.muteLocalAudioStream(!streamInfo.hasAudio)
                RetrofitManager.instance().getService(API_BASE_URL, StreamService::class.java)
                        .updateStreamInfo(USERTOKEN, APPID, eduRoom.roomInfo.roomUuid, userInfo.userUuid,
                                streamInfo.streamUuid, eduStreamStatusReq)
                        .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                            override fun onSuccess(res: ResponseBody<String>?) {
                                callback.onSuccess(true)
                            }

                            override fun onFailure(throwable: Throwable?) {
                                val error = throwable as BusinessException
                                callback.onFailure(error.code, error.message)
                            }
                        }))
            }
        } else {
            /**流信息不存在于本地，说明是新建*/
            RetrofitManager.instance().getService(API_BASE_URL, StreamService::class.java)
                    .createStream(USERTOKEN, APPID, eduRoom.roomInfo.roomUuid, userInfo.userUuid,
                            streamInfo.streamUuid, eduStreamStatusReq)
                    .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                        override fun onSuccess(res: ResponseBody<String>?) {
                            RteEngineImpl.rtcEngine.muteLocalVideoStream(!streamInfo.hasVideo)
                            RteEngineImpl.rtcEngine.muteLocalAudioStream(!streamInfo.hasAudio)
                            callback.onSuccess(true)
                        }

                        override fun onFailure(throwable: Throwable?) {
                            val error = throwable as BusinessException
                            callback.onFailure(error.code, error.message)
                        }
                    }))
        }
    }

    override fun unPublishStream(streamInfo: EduStreamInfo, callback: EduCallback<Boolean>) {
        RetrofitManager.instance().getService(API_BASE_URL, StreamService::class.java)
                .deleteStream(USERTOKEN, APPID, eduRoom.roomInfo.roomUuid, userInfo.userUuid,
                        streamInfo.streamUuid)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        RteEngineImpl.rtcEngine.muteLocalAudioStream(true)
                        RteEngineImpl.rtcEngine.muteLocalVideoStream(true)
                        callback.onSuccess(true)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        val error = throwable as BusinessException
                        callback.onFailure(error.code, error.message)
                    }
                }))
    }

    override fun sendRoomMessage(message: String, callback: EduCallback<EduMsg>) {
        val roomMsgReq = EduRoomMsgReq(message)
        RetrofitManager.instance().getService(API_BASE_URL, RoomService::class.java)
                .sendChannelCustomMessage(USERTOKEN, APPID, eduRoom.roomInfo.roomUuid, roomMsgReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        val textMessage = EduMsg(userInfo, message, System.currentTimeMillis())
                        callback.onSuccess(textMessage)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        val error = throwable as BusinessException
                        callback.onFailure(error.code, error.message)
                    }
                }))
    }

    override fun sendUserMessage(message: String, remoteUser: EduUserInfo, callback: EduCallback<EduMsg>) {
        val userMsgReq = EduUserMsgReq(message)
        RetrofitManager.instance().getService(API_BASE_URL, RoomService::class.java)
                .sendPeerCustomMessage(USERTOKEN, APPID, eduRoom.roomInfo.roomUuid, remoteUser.userUuid,
                        userMsgReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        val textMessage = EduMsg(userInfo, message, System.currentTimeMillis())
                        callback.onSuccess(textMessage)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        val error = throwable as BusinessException
                        callback.onFailure(error.code, error.message)
                    }
                }))
    }

    override fun sendRoomChatMessage(message: String, callback: EduCallback<EduMsg>) {
        val roomChatMsgReq = EduRoomChatMsgReq(message, EduChatMsgType.Text.value)
        RetrofitManager.instance().getService(API_BASE_URL, RoomService::class.java)
                .sendRoomChatMsg(USERTOKEN, APPID, eduRoom.roomInfo.roomUuid, roomChatMsgReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        val textMessage = EduMsg(userInfo, message, System.currentTimeMillis())
                        callback.onSuccess(textMessage)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        val error = throwable as BusinessException
                        callback.onFailure(error.code, error.message)
                    }
                }))
    }

    override fun sendUserChatMessage(message: String, remoteUser: EduUserInfo, callback: EduCallback<EduMsg>) {
        val userChatMsgReq = EduUserChatMsgReq(message, EduChatMsgType.Text.value)
        RetrofitManager.instance().getService(API_BASE_URL, RoomService::class.java)
                .sendPeerChatMsg(USERTOKEN, APPID, eduRoom.roomInfo.roomUuid, remoteUser.userUuid,
                        userChatMsgReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        val textMessage = EduMsg(userInfo, message, System.currentTimeMillis())
                        callback.onSuccess(textMessage)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        val error = throwable as BusinessException
                        callback.onFailure(error.code, error.message)
                    }
                }))
    }

    /**
     * @param viewGroup 视频画面的父布局(在UI布局上最好保持独立)*/
    override fun setStreamView(stream: EduStreamInfo, viewGroup: ViewGroup, config: VideoRenderConfig) {
        val surfaceView = RtcEngine.CreateRendererView(viewGroup.context)
        surfaceView.setZOrderMediaOverlay(true)
        val videoCanvas = VideoCanvas(surfaceView, config.renderMode.value, stream.streamUuid.toInt())
        if (stream.publisher.userUuid == this.userInfo.userUuid) {
            RteEngineImpl.rtcEngine.setupLocalVideo(videoCanvas)
        } else {
            RteEngineImpl.rtcEngine.setupRemoteVideo(videoCanvas)
        }
    }

    override fun setStreamView(stream: EduStreamInfo, viewGroup: ViewGroup) {
        val surfaceView = RtcEngine.CreateRendererView(viewGroup.context)
        surfaceView.setZOrderMediaOverlay(true)
        val videoCanvas = VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, stream.streamUuid.toInt())
        if (stream.publisher.userUuid == this.userInfo.userUuid) {
            RteEngineImpl.rtcEngine.setupLocalVideo(videoCanvas)
        } else {
            RteEngineImpl.rtcEngine.setupRemoteVideo(videoCanvas)
        }
    }
}
