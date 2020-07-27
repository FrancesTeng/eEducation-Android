package io.agora

import io.agora.education.api.room.data.EduMuteState
import io.agora.education.api.room.data.EduRoomState
import io.agora.education.api.room.data.RoomType
import io.agora.education.api.statistics.ConnectionState
import io.agora.education.api.statistics.ConnectionStateChangeReason
import io.agora.education.api.stream.data.*
import io.agora.education.api.user.data.EduChatState
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.api.user.data.EduUserRole
import io.agora.education.impl.role.data.EduUserRoleStr
import io.agora.education.impl.room.data.response.*
import io.agora.education.impl.stream.EduStreamInfoImpl
import io.agora.education.impl.user.data.EduUserInfoImpl
import io.agora.education.impl.cmd.CMDStreamActionMsg
import io.agora.education.impl.cmd.CMDUserStateMsg
import io.agora.rtc.video.VideoEncoderConfiguration
import io.agora.rtm.RtmStatusCode
import io.agora.rtm.RtmStatusCode.ConnectionChangeReason.*
import io.agora.rtm.RtmStatusCode.ConnectionState.CONNECTION_STATE_CONNECTED
import io.agora.rtm.RtmStatusCode.ConnectionState.CONNECTION_STATE_DISCONNECTED

class Convert {
    companion object {

        fun convertVideoEncoderConfig(videoEncoderConfig: VideoEncoderConfig): VideoEncoderConfiguration {
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

        /**根据EduUserRole枚举返回角色字符串;大班课状态下，如果学生自动发流，那么他就是broadcaster*/
        fun convertUserRole(role: EduUserRole, roomType: RoomType, autoPublish: Boolean): String {
            return if (role == EduUserRole.TEACHER) {
                EduUserRoleStr.host.name
            } else {
                when (roomType) {
                    RoomType.ONE_ON_ONE -> {
                        EduUserRoleStr.broadcaster.name
                    }
                    RoomType.SMALL_CLASS -> {
                        EduUserRoleStr.broadcaster.name
                    }
                    RoomType.LARGE_CLASS -> {
                        if (autoPublish) {
                            EduUserRoleStr.broadcaster.name
                        } else {
                            EduUserRoleStr.audience.name
                        }
                    }
                }
            }
        }

        /**根据角色字符串返回EduUserRole枚举值*/
        fun convertUserRole(role: String, roomType: RoomType): EduUserRole {
            when (role) {
                EduUserRoleStr.host.name -> {
                    return EduUserRole.TEACHER
                }
                EduUserRoleStr.broadcaster.name -> {
                    if (roomType == RoomType.ONE_ON_ONE || roomType == RoomType.SMALL_CLASS) {
                        return EduUserRole.STUDENT
                    }
                }
                EduUserRoleStr.audience.name -> {
                    if (roomType == RoomType.LARGE_CLASS) {
                        return EduUserRole.STUDENT
                    }
                }
            }
            return EduUserRole.STUDENT
        }

        /**根据返回的用户和stream列表提取出用户列表*/
        fun getUserInfoList(eduUserListRes: EduUserListRes?, roomType: RoomType): MutableList<EduUserInfo> {
            val list = eduUserListRes?.list
            if (list?.size == 0) {
                return mutableListOf()
            }
            val userInfoList: MutableList<EduUserInfo> = mutableListOf()
            for ((index, element) in list?.withIndex()!!) {
                val eduUser = convertUserInfo(element, roomType)
                userInfoList.add(index, eduUser)
            }
            return userInfoList
        }

        fun convertUserInfo(eduUserRes: EduUserRes, roomType: RoomType): EduUserInfo {
            val role = convertUserRole(eduUserRes.role, roomType)
            return EduUserInfoImpl(eduUserRes.userUuid, eduUserRes.userName, role,
                    eduUserRes.muteChat == EduChatState.Allow.value, eduUserRes.updateTime)
        }

        fun convertUserInfo(eduUserRes: EduFromUserRes, roomType: RoomType): EduUserInfo {
            val role = convertUserRole(eduUserRes.role, roomType)
            return EduUserInfoImpl(eduUserRes.userUuid, eduUserRes.userName, role, false, null)
        }

        /**根据返回的用户和stream列表提取出stream列表*/
        fun getStreamInfoList(eduStreamListRes: EduStreamListRes?, roomType: RoomType): MutableList<EduStreamInfo> {
            val userResList = eduStreamListRes?.list
            if (userResList?.size == 0) {
                return mutableListOf()
            }
            val streamInfoList: MutableList<EduStreamInfo> = mutableListOf()
            for ((index, element) in userResList?.withIndex()!!) {
                val eduUserInfo = convertUserInfo(element.fromUser, roomType)
                val videoSourceType = if (element.videoSourceType == 1) VideoSourceType.CAMERA else VideoSourceType.SCREEN
                val hasVideo = element.videoState == EduVideoState.Open.value
                val hasAudio = element.audioState == EduAudioState.Open.value
                val eduStreamInfo = EduStreamInfoImpl(element.streamUuid, element.streamName, videoSourceType,
                        hasVideo, hasAudio, eduUserInfo, element.updateTime)
                streamInfoList.add(index, eduStreamInfo)
            }
            return streamInfoList
        }

        fun convertRoomState(state: Int): EduRoomState {
            return when (state) {
                EduRoomState.INIT.value -> {
                    EduRoomState.INIT
                }
                EduRoomState.START.value -> {
                    EduRoomState.START
                }
                EduRoomState.END.value -> {
                    EduRoomState.END
                }
                else -> {
                    EduRoomState.INIT
                }
            }
        }

        fun convertStreamInfo(eduStreamRes: EduStreamRes, roomType: RoomType): EduStreamInfo {
            val fromUserInfo = convertUserInfo(eduStreamRes.fromUser, roomType)
            return EduStreamInfoImpl(eduStreamRes.streamUuid, eduStreamRes.streamName,
                    convertVideoSourceType(eduStreamRes.videoSourceType),
                    eduStreamRes.videoState == EduVideoState.Open.value,
                    eduStreamRes.audioState == EduAudioState.Open.value,
                    fromUserInfo, eduStreamRes.updateTime)
        }

        fun convertStreamInfo(cmdStreamActionMsg: CMDStreamActionMsg, roomType: RoomType): EduStreamInfo {
            val fromUserInfo = convertUserInfo(cmdStreamActionMsg.fromUser, roomType)
            return EduStreamInfoImpl(cmdStreamActionMsg.streamUuid, cmdStreamActionMsg.streamName,
                    convertVideoSourceType(cmdStreamActionMsg.videoSourceType),
                    cmdStreamActionMsg.videoState == EduVideoState.Open.value,
                    cmdStreamActionMsg.audioState == EduAudioState.Open.value,
                    fromUserInfo, cmdStreamActionMsg.updateTime)
        }

        fun convertVideoSourceType(value: Int): VideoSourceType {
            return when (value) {
                VideoSourceType.CAMERA.value -> {
                    VideoSourceType.CAMERA
                }
                VideoSourceType.SCREEN.value -> {
                    VideoSourceType.SCREEN
                }
                else -> {
                    VideoSourceType.CAMERA
                }
            }
        }

        fun convertUserInfo(cmdUserStateMsg: CMDUserStateMsg, roomType: RoomType): EduUserInfo {
            val role = convertUserRole(cmdUserStateMsg.role, roomType)
            return EduUserInfoImpl(cmdUserStateMsg.userUuid, cmdUserStateMsg.userName, role,
                    cmdUserStateMsg.muteChat == EduChatState.Allow.value,
                    cmdUserStateMsg.updateTime)
        }

        /**根据roomType从RTM返回的教室信息(EduEntryRoomStateRes)中提取muteChat(针对student而言)的状态*/
        fun extractStudentChatAllowState(eduEntryRoomStateRes: EduEntryRoomStateRes, roomType: RoomType): Boolean {
            var allow = false
            when (roomType) {
                RoomType.ONE_ON_ONE, RoomType.SMALL_CLASS -> {
                    eduEntryRoomStateRes.muteChat?.audience?.let {
                        allow = eduEntryRoomStateRes.muteChat?.broadcaster?.toInt() == EduMuteState.Enable.value
                    }
                }
                RoomType.LARGE_CLASS -> {
                    allow = eduEntryRoomStateRes.muteChat?.audience?.toInt() == EduMuteState.Enable.value
                }
            }
            return allow
        }

        fun convertConnectionState(connectionState: Int): ConnectionState {
            return when (connectionState) {
                CONNECTION_STATE_DISCONNECTED -> {
                    ConnectionState.DISCONNECTED
                }
                CONNECTION_STATE_DISCONNECTED -> {
                    ConnectionState.CONNECTING
                }
                CONNECTION_STATE_DISCONNECTED -> {
                    ConnectionState.CONNECTED
                }
                CONNECTION_STATE_DISCONNECTED -> {
                    ConnectionState.RECONNECTING
                }
                CONNECTION_STATE_DISCONNECTED -> {
                    ConnectionState.ABORTED
                }
                else -> {
                    ConnectionState.DISCONNECTED
                }
            }
        }

        fun convertConnectionStateChangeReason(changeReason: Int): ConnectionStateChangeReason {
            return when (changeReason) {
                CONNECTION_CHANGE_REASON_LOGIN -> {
                    ConnectionStateChangeReason.LOGIN
                }
                CONNECTION_CHANGE_REASON_LOGIN_SUCCESS -> {
                    ConnectionStateChangeReason.LOGIN_SUCCESS
                }
                CONNECTION_CHANGE_REASON_LOGIN_FAILURE -> {
                    ConnectionStateChangeReason.LOGIN_FAILURE
                }
                CONNECTION_CHANGE_REASON_LOGIN_TIMEOUT -> {
                    ConnectionStateChangeReason.LOGIN_TIMEOUT
                }
                CONNECTION_CHANGE_REASON_INTERRUPTED -> {
                    ConnectionStateChangeReason.INTERRUPTED
                }
                CONNECTION_CHANGE_REASON_LOGOUT -> {
                    ConnectionStateChangeReason.LOGOUT
                }
                CONNECTION_CHANGE_REASON_BANNED_BY_SERVER -> {
                    ConnectionStateChangeReason.BANNED_BY_SERVER
                }
                CONNECTION_CHANGE_REASON_REMOTE_LOGIN -> {
                    ConnectionStateChangeReason.REMOTE_LOGIN
                }
                else -> {
                    ConnectionStateChangeReason.LOGIN
                }
            }
        }
    }
}

