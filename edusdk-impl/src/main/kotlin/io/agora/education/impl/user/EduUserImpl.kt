package io.agora.education.impl.user

import android.view.ViewGroup
import io.agora.Constants.Companion.API_BASE_URL
import io.agora.Constants.Companion.APPID
import io.agora.Constants.Companion.USERTOKEN
import io.agora.base.callback.ThrowableCallback
import io.agora.base.network.BusinessException
import io.agora.base.network.ResponseBody
import io.agora.base.network.RetrofitManager
import io.agora.education.api.EduCallback
import io.agora.education.api.message.EduMsg
import io.agora.education.api.room.data.RoomMediaOptions
import io.agora.education.api.stream.data.*
import io.agora.education.api.user.EduUser
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.api.user.listener.EduUserEventListener
import io.agora.education.impl.room.EduRoomImpl
import io.agora.education.impl.room.network.RoomService
import io.agora.education.impl.stream.network.StreamService
import io.agora.education.impl.user.data.request.EduRoomMsgReq
import io.agora.education.impl.user.data.request.EduStreamStatusReq
import io.agora.education.impl.user.data.request.EduUserMsgReq
import io.agora.rtc.Constants
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import io.agora.rte.RteEngineImpl

internal open class EduUserImpl(
        override var userInfo: EduUserInfo
) : EduUser {
    override var videoEncoderConfig = VideoEncoderConfig()

    private fun getVideoEncoderConfiguration(): VideoEncoderConfiguration {
        var videoDimensions = VideoEncoderConfiguration.VideoDimensions(
                videoEncoderConfig.videoDimensionWidth,
                videoEncoderConfig.videoDimensionHeight)
        var videoEncoderConfiguration = VideoEncoderConfiguration()
        videoEncoderConfiguration.dimensions = videoDimensions
        videoEncoderConfiguration.frameRate = videoEncoderConfig.fps
        when (videoEncoderConfig.orientationMode) {
            OrientationMode.ADAPTIVE -> {
                videoEncoderConfiguration.orientationMode = VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
            }
            OrientationMode.FIXED_LANDSCAPE -> {
                videoEncoderConfiguration.orientationMode = VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_LANDSCAPE
            }
            OrientationMode.FIXED_PORTRAIT -> {
                videoEncoderConfiguration.orientationMode = VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
            }
        }
        when (videoEncoderConfig.degradationPreference) {
            DegradationPreference.MAINTAIN_QUALITY -> {
                videoEncoderConfiguration.degradationPrefer = VideoEncoderConfiguration.DEGRADATION_PREFERENCE.MAINTAIN_QUALITY
            }
            DegradationPreference.MAINTAIN_FRAME_RATE -> {
                videoEncoderConfiguration.degradationPrefer = VideoEncoderConfiguration.DEGRADATION_PREFERENCE.MAINTAIN_FRAMERATE
            }
            DegradationPreference.MAINTAIN_BALANCED -> {
                videoEncoderConfiguration.degradationPrefer = VideoEncoderConfiguration.DEGRADATION_PREFERENCE.MAINTAIN_BALANCED
            }
        }
        return videoEncoderConfiguration
    }

    override var eventListener: EduUserEventListener? = null

    lateinit var eduRoom: EduRoomImpl
    lateinit var roomMediaOptions: RoomMediaOptions

    override fun initOrUpdateLocalStream(options: LocalStreamInitOptions, callback: EduCallback<EduStreamInfo>) {
        /**enableCamera和enableMicrophone控制是否打开摄像头和麦克风*/
        if (options.enableCamera) {
            RteEngineImpl.rtcEngine.enableVideo()
            RteEngineImpl.rtcEngine.enableLocalVideo(options.enableCamera)
        }
        RteEngineImpl.rtcEngine.enableLocalAudio(options.enableMicrophone)
        RteEngineImpl.rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        RteEngineImpl.rtcEngine.setVideoEncoderConfiguration(getVideoEncoderConfiguration())
        val streamInfo = EduStreamInfo(options.streamUuid, options.streamName, VideoSourceType.CAMERA,
                options.enableCamera, options.enableMicrophone, this.userInfo, null)
        if (roomMediaOptions.autoSubscribeVideo || roomMediaOptions.autoSubscribeAudio) {
            val subscribeOptions = StreamSubscribeOptions(roomMediaOptions.autoSubscribeAudio,
                    roomMediaOptions.autoSubscribeVideo,
                    VideoStreamType.LOW)
            subscribeStream(streamInfo, subscribeOptions)
        }
        /**autoPublishCamera和autoPublishMicrophone控制是否自动推流*/
        if (roomMediaOptions.autoPublishCamera || roomMediaOptions.autoPublishMicrophone) {
            publishStream(streamInfo, object : EduCallback<Boolean> {
                override fun onSuccess(res: Boolean?) {
                    if (res!!) {
                        return
                    }
                    RteEngineImpl.rtcEngine.muteLocalVideoStream(!roomMediaOptions.autoPublishCamera)
                    RteEngineImpl.rtcEngine.muteLocalAudioStream(!roomMediaOptions.autoPublishMicrophone)
                    callback.onSuccess(streamInfo)
                }

                override fun onFailure(code: Int, reason: String?) {
                    callback.onFailure(code, reason)
                }
            })
        }
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

    override fun publishStream(streamInfo: EduStreamInfo, callback: EduCallback<Boolean>) {
        val eduStreamStatusReq = EduStreamStatusReq(streamInfo.streamName, streamInfo.videoSourceType.value,
                AudioSourceType.MICROPHONE.value, if (streamInfo.hasVideo) 1 else 0,
                if (streamInfo.hasAudio) 1 else 0)
        RetrofitManager.instance().getService(API_BASE_URL, StreamService::class.java)
                .createStream(USERTOKEN, APPID, eduRoom.roomInfo.roomUuid, userInfo.userUuid,
                        roomMediaOptions.primaryStreamId, eduStreamStatusReq)
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

    override fun unPublishStream(streamInfo: EduStreamInfo, callback: EduCallback<Boolean>) {
        RetrofitManager.instance().getService(API_BASE_URL, StreamService::class.java)
                .deleteStream(USERTOKEN, APPID, eduRoom.roomInfo.roomUuid, userInfo.userUuid, roomMediaOptions.primaryStreamId)
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

    override fun sendRoomMessage(message: String, callback: EduCallback<EduMsg>) {
        val roomMsgReq = EduRoomMsgReq(message)
        RetrofitManager.instance().getService(API_BASE_URL, RoomService::class.java)
                .sendRoomMessage(USERTOKEN, APPID, eduRoom.roomInfo.roomUuid, roomMsgReq)
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

    override fun sendUserMessage(message: String, user: EduUserInfo, callback: EduCallback<EduMsg>) {
        val userMsgReq = EduUserMsgReq(user.userUuid, message)
        RetrofitManager.instance().getService(API_BASE_URL, RoomService::class.java)
                .sendPeerMessage(USERTOKEN, APPID, eduRoom.roomInfo.roomUuid, userMsgReq)
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
}
