package io.agora.rte

import android.content.Context
import androidx.annotation.NonNull
import io.agora.education.api.EduCallback
import io.agora.rtm.ResultCallback

interface IRteEngine {
    fun init(context: Context, appId: String)

    fun loginRtm(rtmUid: String, rtmToken: String, @NonNull callback: EduCallback<Unit>)

    fun logoutRtm()

    fun createChannel(channelId: String, eventListener: RteChannelEventListener): IRteChannel

    fun enableLocalMedia(audio: Boolean, video: Boolean)

    fun setClientRole(channelId: String, role: Int)

    fun publish(channelId: String): Int

    fun unpublish(channelId: String): Int

    fun updateLocalStream(hasAudio: Boolean, hasVideo: Boolean)

    /**作用于rtcChannel*/
    fun muteRemoteStream(channelId: String, uid: Int, muteAudio: Boolean, muteVideo: Boolean): Int

    /**作用于全局*/
    fun muteLocalStream(muteAudio: Boolean, muteVideo: Boolean): Int
}
