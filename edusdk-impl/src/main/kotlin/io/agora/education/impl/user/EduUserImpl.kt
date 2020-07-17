package io.agora.education.impl.user

import android.view.SurfaceView
import android.view.View
import io.agora.Constants.Companion.API_BASE_URL
import io.agora.Constants.Companion.APPID
import io.agora.Constants.Companion.USERTOKEN
import io.agora.base.callback.ThrowableCallback
import io.agora.base.network.BusinessException
import io.agora.base.network.RetrofitManager
import io.agora.education.api.EduCallback
import io.agora.education.api.message.EduTextMessage
import io.agora.education.api.room.data.RoomJoinOptions
import io.agora.education.api.room.data.RoomMediaOptions
import io.agora.education.api.stream.data.*
import io.agora.education.api.user.EduUser
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.api.user.listener.EduUserEventListener
import io.agora.education.impl.stream.EduStreamInfoImpl
import io.agora.education.impl.user.data.EduUserInfoImpl
import io.agora.education.impl.user.data.request.EduPublishStreamReq
import io.agora.education.impl.user.network.UserService
import io.agora.education.impl.stream.data.response.EduStreamListRes
import io.agora.education.impl.user.data.request.EduRoomMsgReq
import io.agora.education.impl.user.data.request.EduUserMsgReq
import io.agora.education.impl.user.data.response.EduPublishStreamRes
import io.agora.education.impl.user.data.response.ResponseBody
import io.agora.rtc.Constants
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

    lateinit var roomId: String
    lateinit var roomMediaOptions: RoomMediaOptions

    override fun initOrUpdateLocalStream(options: LocalStreamInitOptions, callback: EduCallback<EduStreamInfo>) {
        /**enableCamera和enableMicrophone控制是否打开摄像头和麦克风*/
        if(options.enableCamera)
        {
            RteEngineImpl.rtcEngine.enableVideo()
            RteEngineImpl.rtcEngine.enableLocalVideo(options.enableCamera)
        }
        RteEngineImpl.rtcEngine.enableLocalAudio(options.enableMicrophone)
        RteEngineImpl.rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        RteEngineImpl.rtcEngine.setVideoEncoderConfiguration(getVideoEncoderConfiguration())
        var streamInfo = EduStreamInfo(options.streamUuid, options.streamName, VideoSourceType.CAMERA,
                options.enableCamera, options.enableMicrophone, this.userInfo)
        if(roomMediaOptions.autoSubscribeVideo || roomMediaOptions.autoSubscribeAudio)
        {
            var options = StreamSubscribeOptions(roomMediaOptions.autoSubscribeAudio,
                                                 roomMediaOptions.autoSubscribeVideo,
                                                 VideoStreamType.LOW)
            subscribeStream(streamInfo, options)
        }
        /**autoPublishCamera和autoPublishMicrophone控制是否自动推流*/
        if(roomMediaOptions.autoPublishCamera || roomMediaOptions.autoPublishMicrophone)
        {
            publishStream(streamInfo, object : EduCallback<Boolean> {
                override fun onSuccess(res: Boolean?) {
                    if(res!!)
                    {return}
                    RteEngineImpl.rtcEngine.muteLocalVideoStream(!roomMediaOptions.autoPublishCamera)
                    RteEngineImpl.rtcEngine.muteLocalAudioStream(!roomMediaOptions.autoPublishMicrophone)
                }

                override fun onFailure(code: Int, reason: String?) {
                    TODO("Not yet implemented")
                }
            })
        }
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

    override fun publishStream(streamInfo: EduStreamInfo, callback: EduCallback<Boolean>) {
        var publishStreamReq = EduPublishStreamReq((streamInfo.publisher as EduUserInfoImpl).userId,
                streamInfo.streamUuid, streamInfo.streamName, streamInfo.videoSourceType,
                if (streamInfo.hasVideo) 1 else 0, if (streamInfo.hasAudio) 1 else 0)
        RetrofitManager.instance().getService(API_BASE_URL, UserService::class.java)
                .publishStream(USERTOKEN, APPID, this.roomId, publishStreamReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<EduPublishStreamRes>> {
                    override fun onSuccess(res: ResponseBody<EduPublishStreamRes>?) {
                        callback.onSuccess(true)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as BusinessException
                        callback.onFailure(error?.code, error.message)
                    }
                }))
    }

    override fun unPublishStream(streamInfo: EduStreamInfo, callback: EduCallback<Boolean>) {
        RetrofitManager.instance().getService(API_BASE_URL, UserService::class.java)
                .unPublishStream(USERTOKEN, APPID, this.roomId, (streamInfo as EduStreamInfoImpl).streamId.toString())
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<Boolean>> {
                    override fun onSuccess(res: ResponseBody<Boolean>?) {
                        callback.onSuccess(res?.data)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as BusinessException
                        callback.onFailure(error?.code, error.message)
                    }
                }))
    }

    override fun sendRoomMessage(message: String, callback: EduCallback<EduTextMessage>) {
        var roomMsgReq = EduRoomMsgReq(message)
        RetrofitManager.instance().getService(API_BASE_URL, UserService::class.java)
                .sendRoomMessage(USERTOKEN, APPID, this.roomId, roomMsgReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<Boolean>> {
                    override fun onSuccess(res: ResponseBody<Boolean>?) {
                        var textMessage = EduTextMessage(userInfo.userUuid, userInfo.userName,
                                message, System.currentTimeMillis())
                        callback.onSuccess(textMessage)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as BusinessException
                        callback.onFailure(error?.code, error.message)
                    }
                }))
    }

    override fun sendUserMessage(message: String, user: EduUserInfo, callback: EduCallback<EduTextMessage>) {
        var userMsgReq = EduUserMsgReq((user as EduUserInfoImpl).userId, message)
        RetrofitManager.instance().getService(API_BASE_URL, UserService::class.java)
                .sendUserMessage(USERTOKEN, APPID, this.roomId, userMsgReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<Boolean>> {
                    override fun onSuccess(res: ResponseBody<Boolean>?) {
                        var textMessage = EduTextMessage(userInfo.userUuid, userInfo.userName,
                                message, System.currentTimeMillis())
                        callback.onSuccess(textMessage)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        var error = throwable as BusinessException
                        callback.onFailure(error?.code, error.message)
                    }
                }))
    }

    override fun setStreamView(stream: EduStreamInfo, view: SurfaceView, config: VideoRenderConfig) {
        var videoCanvas = VideoCanvas(view, config.renderMode.ordinal, stream.streamUuid.toInt())
        if (stream.publisher.userUuid == this.userInfo.userUuid) {
            RteEngineImpl.rtcEngine.setupLocalVideo(videoCanvas)
        } else {
            RteEngineImpl.rtcEngine.setupRemoteVideo(videoCanvas)
        }
        TODO("Not yet implemented")
    }
}
