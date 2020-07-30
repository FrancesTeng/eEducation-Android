package io.agora.education.impl.room

import io.agora.Convert
import io.agora.education.api.room.data.RoomType
import io.agora.education.api.stream.data.EduStreamEvent
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.user.data.EduUserEvent
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.impl.room.data.ValidState
import io.agora.education.impl.room.data.response.EduStreamRes
import io.agora.education.impl.room.data.response.EduUserRes

class RoomUtil {
    companion object {
        /**把增量数据合并到本地缓存中
         * 并返回join/left/update的数组*/
        fun mergeIncrementUserList(incrementUsers: MutableList<EduUserRes>,
                                   eduUserList: MutableList<EduUserInfo>, roomType: RoomType)
                : Array<Any> {
            val userJoinedList = mutableListOf<EduUserInfo>()
            val userLeftList = mutableListOf<EduUserEvent>()
            val userUpdatedList = mutableListOf<EduUserEvent>()
            synchronized(eduUserList) {
                for ((index, element) in incrementUsers.withIndex()) {
                    val userInfo = Convert.convertUserInfo(element, roomType)
                    if (element.state == ValidState.Invalid.value) {
                        if (eduUserList.contains(userInfo)) {
                            /**移除掉下线的用户*/
                            eduUserList.remove(userInfo)
                            userLeftList.add(EduUserEvent(userInfo, null))
                        }
                    } else {
                        val pos = eduUserList.indexOf(userInfo)
                        if (pos > -1) {
                            /**变化的数据*/
                            eduUserList[pos] = userInfo
                            userUpdatedList.add(EduUserEvent(userInfo, null))
                        } else {
                            /**新添加的数据*/
                            eduUserList.add(userInfo)
                            userJoinedList.add(userInfo)
                        }
                    }
                }
                return arrayOf(userJoinedList, userLeftList, userUpdatedList)
            }
        }

        /**把增量数据合并到本地缓存中
         * 并返回join/left/update的数组*/
        fun mergeIncrementStreamList(incrementStreams: MutableList<EduStreamRes>,
                                     eduStreamList: MutableList<EduStreamInfo>, roomType: RoomType)
                : Array<MutableList<EduStreamEvent>> {
            val streamJoinedList = mutableListOf<EduStreamEvent>()
            val streamLeftList = mutableListOf<EduStreamEvent>()
            val streamUpdatedList = mutableListOf<EduStreamEvent>()
            synchronized(eduStreamList) {
                for ((index, element) in incrementStreams.withIndex()) {
                    val streamInfo = Convert.convertStreamInfo(element, roomType)
                    if (element.state == ValidState.Invalid.value) {
                        if (eduStreamList.contains(streamInfo)) {
                            eduStreamList.remove(streamInfo)
                            streamLeftList.add(EduStreamEvent(streamInfo, null))
                        }
                    } else {
                        val pos = eduStreamList.indexOf(streamInfo)
                        if (pos > -1) {
                            /**变化的数据*/
                            eduStreamList[pos] = streamInfo
                            streamUpdatedList.add(EduStreamEvent(streamInfo, null))
                        } else {
                            /**新添加的数据*/
                            eduStreamList.add(streamInfo)
                            streamJoinedList.add(EduStreamEvent(streamInfo, null))
                        }
                    }
                }
                return arrayOf(streamJoinedList, streamLeftList, streamUpdatedList)
            }
        }
    }
}