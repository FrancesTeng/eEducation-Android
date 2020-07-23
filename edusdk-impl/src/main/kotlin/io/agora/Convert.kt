package io.agora

import io.agora.education.api.room.data.EduRoomState
import io.agora.education.api.room.data.RoomType
import io.agora.education.api.stream.data.EduAudioState
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.stream.data.EduVideoState
import io.agora.education.api.stream.data.VideoSourceType
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.api.user.data.EduUserRole
import io.agora.education.impl.role.data.EduUserRoleStr
import io.agora.education.impl.room.data.response.*
import io.agora.education.impl.stream.EduStreamInfoImpl
import io.agora.education.impl.user.data.EduUserInfoImpl
import io.agora.rte.data.RtmStreamActionMsg
import io.agora.rte.data.RtmUserStateMsg

class Convert {
    companion object {

        /**根据EduUserRole枚举返回角色字符串*/
        fun convertUserRole(role: EduUserRole, roomType: RoomType): String {
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
                        EduUserRoleStr.audience.name
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

        private fun convertUserInfo(eduUserRes: EduUserRes, roomType: RoomType): EduUserInfo {
            val role = convertUserRole(eduUserRes.role, roomType)
            return EduUserInfoImpl(eduUserRes.userUuid, eduUserRes.userName, role, eduUserRes.updateTime)
        }

        private fun convertUserInfo(eduUserRes: EduFromUserRes, roomType: RoomType): EduUserInfo {
            val role = convertUserRole(eduUserRes.role, roomType)
            return EduUserInfoImpl(eduUserRes.userUuid, eduUserRes.userName, role, null)
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
                val eduStreamInfo = EduStreamInfoImpl(null, element.streamUuid, element.streamName,
                        videoSourceType, hasVideo, hasAudio, eduUserInfo, element.updateTime)
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

        fun convertStreamInfo(rtmStreamActionMsg: RtmStreamActionMsg, roomType: RoomType): EduStreamInfo {
            val fromUserInfo = convertUserInfo(rtmStreamActionMsg.fromUser, roomType)
            return EduStreamInfo(rtmStreamActionMsg.streamUuid, rtmStreamActionMsg.streamName,
                    convertVideoSourceType(rtmStreamActionMsg.videoSourceType),
                    rtmStreamActionMsg.videoState == EduVideoState.Open.value,
                    rtmStreamActionMsg.audioState == EduAudioState.Open.value,
                    fromUserInfo, rtmStreamActionMsg.updateTime)
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

        fun convertUserInfo(rtmUserStateMsg: RtmUserStateMsg, roomType: RoomType): EduUserInfo {
            val role = convertUserRole(rtmUserStateMsg.role, roomType)
            return EduUserInfo(rtmUserStateMsg.userUuid, rtmUserStateMsg.userName,
                    role, rtmUserStateMsg.updateTime)
        }
    }
}

