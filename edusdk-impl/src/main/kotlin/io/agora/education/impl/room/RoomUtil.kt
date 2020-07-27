package io.agora.education.impl.room

import io.agora.Convert
import io.agora.education.api.room.data.RoomType
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.user.data.EduChatState
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.impl.room.data.OnlineState
import io.agora.education.impl.room.data.response.EduStreamRes
import io.agora.education.impl.room.data.response.EduUserRes

class RoomUtil {
    companion object {
        fun mergeIncrementUserList(incrementUsers: MutableList<EduUserRes>,
                                   eduUserList: MutableList<EduUserInfo>, roomType: RoomType) {
            for ((index, element) in incrementUsers.withIndex()) {
                val userInfo = Convert.convertUserInfo(element, roomType)
                if (element.state == OnlineState.Offline.value) {
                    if (eduUserList.contains(userInfo)) {
                        /**移除掉下线的用户*/
                        eduUserList.remove(userInfo)
                    }
                } else {
                    /**添加一个上线的用户*/
                    if(!eduUserList.contains(userInfo)) {
                        eduUserList.add(userInfo)
                    }
                }
            }
        }

        fun mergeIncrementStreamList(incrementStreams: MutableList<EduStreamRes>,
                                     eduStreamList: MutableList<EduStreamInfo>, roomType: RoomType) {
            for ((index, element) in incrementStreams.withIndex()) {
                val streamInfo = Convert.convertStreamInfo(element, roomType)
                if(element.state == OnlineState.Offline.value) {
                    if(eduStreamList.contains(streamInfo)) {
                        eduStreamList.remove(streamInfo)
                    }
                }
                else {
                    if(!eduStreamList.contains(streamInfo)) {
                        eduStreamList.add(streamInfo)
                    }
                }
            }
        }
    }
}