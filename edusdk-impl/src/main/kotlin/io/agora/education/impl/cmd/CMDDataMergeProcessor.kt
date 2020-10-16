package io.agora.education.impl.cmd

import android.util.Log
import com.google.gson.Gson
import io.agora.education.impl.util.Convert
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.EduRoomState
import io.agora.education.api.room.data.RoomStatusEvent
import io.agora.education.api.room.data.RoomType
import io.agora.education.api.stream.data.EduAudioState
import io.agora.education.api.stream.data.EduStreamEvent
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.stream.data.EduVideoState
import io.agora.education.api.user.data.EduBaseUserInfo
import io.agora.education.api.user.data.EduChatState
import io.agora.education.api.user.data.EduUserEvent
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.impl.cmd.bean.*
import io.agora.education.impl.room.EduRoomImpl
import io.agora.education.impl.room.data.response.EduSnapshotRes
import io.agora.education.impl.stream.EduStreamInfoImpl
import io.agora.education.impl.user.data.EduUserInfoImpl

internal class CMDDataMergeProcessor : CMDProcessor() {
    companion object {
        const val TAG = "CMDDataMergeProcessor"

        /**从 {@param userInfoList} 中移除 离开课堂的用户 {@param offLineUserList}*/
        fun removeUserWithOffline(offlineUserList: MutableList<OfflineUserInfo>,
                                  userInfoList: MutableList<EduUserInfo>, roomType: RoomType):
                MutableList<EduUserEvent> {
            val validUserInfoList = mutableListOf<EduUserEvent>()
            synchronized(userInfoList) {
                for (element in offlineUserList) {
                    val role = Convert.convertUserRole(element.role, roomType)
                    val userInfo1: EduUserInfo = EduUserInfoImpl(element.userUuid, element.userName, role,
                            element.muteChat == EduChatState.Allow.value, element.updateTime)
                    userInfo1.streamUuid = element.streamUuid
                    userInfo1.userProperties = element.userProperties
                    if (userInfoList.contains(userInfo1)) {
                        val index = userInfoList.indexOf(userInfo1)

                        /**获取已存在于集合中的用户*/
                        val userInfo2 = userInfoList[index]
                        /**找出最新数据并替换*/
                        if (compareUserInfoTime(userInfo1, userInfo2) > 0) {
                            /**剔除掉被过滤掉的用户*/
                            userInfoList.removeAt(index)
                            /**构造userEvent并返回*/
                            val operator = getOperator(element.operator, userInfo1, roomType)
                            val userEvent = EduUserEvent(userInfo1, operator)
                            validUserInfoList.add(userEvent)
                        }
                    }
                }
                return validUserInfoList
            }
        }

        fun addUserWithOnline(onlineUserList: MutableList<OnlineUserInfo>,
                              userInfoList: MutableList<EduUserInfo>, roomType: RoomType):
                MutableList<EduUserInfo> {
            val validUserInfoList = mutableListOf<EduUserInfo>()
            synchronized(userInfoList) {
                for (element in onlineUserList) {
                    val role = Convert.convertUserRole(element.role, roomType)
                    val userInfo1 = EduUserInfoImpl(element.userUuid, element.userName, role,
                            element.muteChat == EduChatState.Allow.value, element.updateTime)
                    userInfo1.streamUuid = element.streamUuid
                    userInfo1.userProperties = element.userProperties
                    if (userInfoList.contains(userInfo1)) {
                        val index = userInfoList.indexOf(userInfo1)

                        /**获取已存在于集合中的用户*/
                        val userInfo2 = userInfoList[index]
                        if (compareUserInfoTime(userInfo1, userInfo2) > 0) {
                            /**更新用户的数据为最新数据*/
                            userInfoList[index] = userInfo1
//                            validUserInfoList.add(userInfo1)
                        }
                    } else {
                        userInfoList.add(userInfo1)
                        validUserInfoList.add(userInfo1)
                    }
                }
                return validUserInfoList
            }
        }

        fun updateUserWithUserStateChange(cmdUserStateMsg: CMDUserStateMsg,
                                          eduUserInfos: MutableList<EduUserInfo>, roomType: RoomType)
                : MutableList<EduUserEvent> {
            val userStateChangedList = mutableListOf<EduUserInfo>()
            userStateChangedList.add(Convert.convertUserInfo(cmdUserStateMsg, roomType))
            val validUserEventList = mutableListOf<EduUserEvent>()
            synchronized(eduUserInfos) {
                for (element in userStateChangedList) {
                    if (eduUserInfos.contains(element)) {
                        val index = eduUserInfos.indexOf(element)

                        /**获取已存在于集合中的用户*/
                        val userInfo2 = eduUserInfos[index]
                        if (compareUserInfoTime(element, userInfo2) > 0) {
                            /**更新用户的数据为最新数据*/
                            eduUserInfos[index] = element
                            /**构造userEvent并返回*/
                            val operator = getOperator(cmdUserStateMsg.operator, element, roomType)
                            val userEvent = EduUserEvent(element, operator)
                            validUserEventList.add(userEvent)
                        }
                    }
                }
                return validUserEventList
            }
        }

        fun updateUserPropertyWithChange(cmdUsrPropertyRes: CMDUserPropertyRes,
                                         eduUserInfos: MutableList<EduUserInfo>): EduUserInfo? {
            for (element in eduUserInfos) {
                if (cmdUsrPropertyRes.fromUser.userUuid == element.userUuid) {
                    element.userProperties = cmdUsrPropertyRes.userProperties
                    return element
                }
            }
            return null
        }

        fun addStreamWithUserOnline(onlineUserList: MutableList<OnlineUserInfo>,
                                    streamInfoList: MutableList<EduStreamInfo>, roomType: RoomType): MutableList<EduStreamEvent> {
            val validStreamList = mutableListOf<EduStreamEvent>()
            synchronized(streamInfoList) {
                for (element in onlineUserList) {
                    val role = Convert.convertUserRole(element.role, roomType)
                    val publisher = EduBaseUserInfo(element.userUuid, element.userName, role)
                    element.streams?.forEach {
                        val videoSourceType = Convert.convertVideoSourceType(it.videoSourceType)
                        val streamInfo = EduStreamInfoImpl(it.streamUuid, it.streamName, videoSourceType,
                                it.videoState == EduVideoState.Open.value, it.audioState == EduAudioState.Open.value,
                                publisher, it.updateTime)
//                        if (streamInfoList.contains(streamInfo)) {
                        val index = Convert.streamExistsInList(streamInfo, streamInfoList)
                        Log.e(TAG, "index的值:$index, 数组长度:${streamInfoList.size}")
                        if (index > -1) {
                            /**更新本地缓存为最新数据;因为onlineUserList经过了有效判断，所以此处不再比较updateTime，直接remove*/
                            streamInfoList[index] = streamInfo
//                            validStreamList.add(EduStreamEvent(streamInfo, null))
                        } else {
                            streamInfoList.add(streamInfo)
                            validStreamList.add(EduStreamEvent(streamInfo, null))
                        }
                    }
                }
            }
            return validStreamList
        }

        fun removeStreamWithUserOffline(offlineUserList: MutableList<OfflineUserInfo>,
                                        streamInfoList: MutableList<EduStreamInfo>, roomType: RoomType): MutableList<EduStreamEvent> {
            val validStreamList = mutableListOf<EduStreamEvent>()
            synchronized(streamInfoList) {
                for (element in offlineUserList) {
                    val role = Convert.convertUserRole(element.role, roomType)
                    val publisher = EduBaseUserInfo(element.userUuid, element.userName, role)
                    val operator = getOperator(element.operator, publisher, roomType)
                    element.streams?.forEach {
                        val videoSourceType = Convert.convertVideoSourceType(it.videoSourceType)
                        val streamInfo = EduStreamInfoImpl(it.streamUuid, it.streamName, videoSourceType,
                                it.audioState == EduAudioState.Open.value, it.videoState == EduVideoState.Open.value,
                                publisher, it.updateTime)
//                        if (streamInfoList.contains(streamInfo)) {
                        val index = Convert.streamExistsInList(streamInfo, streamInfoList);
                        if (index > -1) {
                            /**更新本地缓存为最新数据;因为offlineUserList经过了有效判断，所以此处不再比较updateTime，直接remove*/
                            streamInfoList.removeAt(index)
                            validStreamList.add(EduStreamEvent(streamInfo, operator))
                        }
                    }
                }
            }
            return validStreamList
        }


        /**调用此函数之前须确保first和second代表的是同一个流
         *
         * 比较first的数据是否比second的更为接近当前时间(即找出一个最新数据)
         * @return > 0（first > second）
         *         !(> 0) first <= second*/
        private fun compareStreamInfoTime(first: EduStreamInfo, second: EduStreamInfo): Long {
            /**判断更新时间是否为空(为空的有可能是原始数据)*/
            if ((first as EduStreamInfoImpl).updateTime == null) {
                return -1
            }
            if ((second as EduStreamInfoImpl).updateTime == null) {
                return first.updateTime!!
            }
            /**最终判断出最新数据*/
            return first.updateTime!!.minus(second.updateTime!!)
        }


        fun addStreamWithAction(cmdStreamActionMsg: CMDStreamActionMsg,
                                streamInfoList: MutableList<EduStreamInfo>, roomType: RoomType):
                MutableList<EduStreamEvent> {
            val validStreamList = mutableListOf<EduStreamEvent>()
            val streamInfos = mutableListOf<EduStreamInfo>()
            streamInfos.add(Convert.convertStreamInfo(cmdStreamActionMsg, roomType))
            synchronized(streamInfoList) {
                for (element in streamInfos) {
//                    if (streamInfoList.contains(element)) {
                    val index = Convert.streamExistsInList(element, streamInfoList)
                    Log.e(TAG, "index的值:$index, 数组长度:${streamInfoList.size}")
                    if (index > -1) {
                        /**获取已存在于集合中的用户*/
                        val userInfo2 = streamInfoList[index]
                        if (compareStreamInfoTime(element, userInfo2) > 0) {
                            /**更新用户的数据为最新数据*/
                            streamInfoList[index] = element
                            /**构造userEvent并返回*/
                            val operator = getOperator(cmdStreamActionMsg.operator, element.publisher, roomType)
                            val userEvent = EduStreamEvent(element, operator)
                            validStreamList.add(userEvent)
                        }
                    } else {
                        streamInfoList.add(element)
                        /**构造userEvent并返回*/
                        val operator = getOperator(cmdStreamActionMsg.operator, element.publisher, roomType)
                        val userEvent = EduStreamEvent(element, operator)
                        validStreamList.add(userEvent)
                    }
                }
                return validStreamList
            }
        }

        fun updateStreamWithAction(cmdStreamActionMsg: CMDStreamActionMsg,
                                   streamInfoList: MutableList<EduStreamInfo>, roomType: RoomType):
                MutableList<EduStreamEvent> {
            val validStreamList = mutableListOf<EduStreamEvent>()
            val streamInfos = mutableListOf<EduStreamInfo>()
            streamInfos.add(Convert.convertStreamInfo(cmdStreamActionMsg, roomType))
            Log.e(TAG, "本地流缓存:" + Gson().toJson(streamInfoList))
            synchronized(streamInfoList) {
                for (element in streamInfos) {
//                    if (streamInfoList.contains(element)) {
                    val index = Convert.streamExistsInList(element, streamInfoList)
                    Log.e(TAG, "index的值:$index, 数组长度:${streamInfoList.size}")
                    if (index > -1) {
                        /**获取已存在于集合中的用户*/
                        val userInfo2 = streamInfoList[index]
                        if (compareStreamInfoTime(element, userInfo2) > 0) {
                            /**更新用户的数据为最新数据*/
                            streamInfoList[index] = element
                            /**构造userEvent并返回*/
                            val operator = getOperator(cmdStreamActionMsg.operator, element.publisher, roomType)
                            val userEvent = EduStreamEvent(element, operator)
                            validStreamList.add(userEvent)
                        }
                    } else {
                        /**发现是修改流而且本地又没有那么直接添加到本地并作为有效数据；
                         * 服务端保证顺序，不会出现remove先到，modify后到的情况（modify先发生，remove后发生）*/
                        streamInfoList.add(element)
                        /**构造userEvent并返回*/
                        val operator = getOperator(cmdStreamActionMsg.operator, element.publisher, roomType)
                        val userEvent = EduStreamEvent(element, operator)
                        validStreamList.add(userEvent)
                    }
                }
                return validStreamList
            }
        }

        fun removeStreamWithAction(cmdStreamActionMsg: CMDStreamActionMsg,
                                   streamInfoList: MutableList<EduStreamInfo>, roomType: RoomType):
                MutableList<EduStreamEvent> {
            val validStreamList = mutableListOf<EduStreamEvent>()
            val streamInfos = mutableListOf<EduStreamInfo>()
            streamInfos.add(Convert.convertStreamInfo(cmdStreamActionMsg, roomType))
            synchronized(streamInfoList) {
                for (element in streamInfos) {
//                    if (streamInfoList.contains(element)) {
                    val index = Convert.streamExistsInList(element, streamInfoList)
                    Log.e(TAG, "index的值:$index, 数组长度:${streamInfoList.size}")
                    if (index > -1) {
                        /**更新用户的数据为最新数据*/
                        streamInfoList.removeAt(index)
                        /**构造userEvent并返回*/
                        val operator = getOperator(cmdStreamActionMsg.operator, element.publisher, roomType)
                        val userEvent = EduStreamEvent(element, operator)
                        validStreamList.add(userEvent)
                    }
                }
                return validStreamList
            }
        }

        /**同步房间的快照信息*/
        fun syncSnapshotToRoom(eduRoom: EduRoom, snapshotRes: EduSnapshotRes) {
            val snapshotRoomRes = snapshotRes.room
            eduRoom.getRoomInfo().roomName = snapshotRoomRes.roomInfo.roomName
            eduRoom.getRoomInfo().roomUuid = snapshotRoomRes.roomInfo.roomUuid
            val roomStatus = snapshotRoomRes.roomState
            eduRoom.getRoomStatus().isStudentChatAllowed = Convert.extractStudentChatAllowState(
                    roomStatus.muteChat, (eduRoom as EduRoomImpl).getCurRoomType())
            eduRoom.getRoomStatus().courseState = Convert.convertRoomState(roomStatus.state)
            if (roomStatus.state == EduRoomState.START.value) {
                eduRoom.getRoomStatus().startTime = roomStatus.startTime
            }
            eduRoom.roomProperties = snapshotRoomRes.roomProperties
            val snapshotUserRes = snapshotRes.users
            val validAddedUserList = addUserWithOnline(snapshotUserRes, eduRoom.getCurUserList(),
                    eduRoom.getCurRoomType())
            val validAddedStreamList = addStreamWithUserOnline(snapshotUserRes, eduRoom.getCurStreamList(),
                    eduRoom.getCurRoomType())
            eduRoom.getRoomStatus().onlineUsersCount = validAddedUserList.size
        }
    }
}
