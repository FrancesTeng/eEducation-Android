package io.agora.education.api.user

import android.view.ViewGroup
import io.agora.education.api.EduCallback
import io.agora.education.api.message.EduMsg
import io.agora.education.api.stream.data.*
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.api.user.listener.EduUserEventListener

interface EduUser {
    var userInfo: EduUserInfo
    var videoEncoderConfig: VideoEncoderConfig

    var eventListener: EduUserEventListener?

    fun initOrUpdateLocalStream(options: LocalStreamInitOptions, callback: EduCallback<EduStreamInfo>)

    fun switchCamera()

    fun subscribeStream(stream: EduStreamInfo, options: StreamSubscribeOptions)

    fun unSubscribeStream(stream: EduStreamInfo)

    fun publishStream(stream: EduStreamInfo, callback: EduCallback<Boolean>)

    fun unPublishStream(stream: EduStreamInfo, callback: EduCallback<Boolean>)

    fun sendRoomMessage(message: String, callback: EduCallback<EduMsg>)

    /**
     * @param user 消息接收方的userInfo*/
    fun sendUserMessage(message: String, user: EduUserInfo, callback: EduCallback<EduMsg>)

    /**发送聊天消息*/
    fun sendRoomChatMessage(message: String, callback: EduCallback<EduMsg>)

    fun sendUserChatMessage(message: String, remoteUser: EduUserInfo, callback: EduCallback<EduMsg>)

    fun setStreamView(stream: EduStreamInfo, viewGroup: ViewGroup, config: VideoRenderConfig = VideoRenderConfig())

    fun setStreamView(stream: EduStreamInfo, viewGroup: ViewGroup)
}
