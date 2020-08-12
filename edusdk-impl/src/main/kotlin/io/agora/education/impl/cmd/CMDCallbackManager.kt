package io.agora.education.impl.cmd

import io.agora.education.api.room.EduRoom
import io.agora.education.api.stream.data.EduStreamEvent
import io.agora.education.api.user.data.EduUserEvent
import io.agora.education.api.user.data.EduUserInfo

internal class CMDCallbackManager {
    companion object {
        /**数据同步的第二阶段期间，发生改变的有效数据(是否有效取决于每条数据的updateTime)*/
        private val validOnlineUsersBySyncing = mutableListOf<EduUserInfo>()
        private val validModifiedUsersBySyncing = mutableListOf<EduUserEvent>()
        private val validOfflineUsersBySyncing = mutableListOf<EduUserEvent>()
        private val validAddedStreamsBySyncing = mutableListOf<EduStreamEvent>()
        private val validModifiedStreamsBySyncing = mutableListOf<EduStreamEvent>()
        private val validRemovedStreamsBySyncing = mutableListOf<EduStreamEvent>()

        /**数据同步的第二阶段期间，发生改变的本地有效数据*/
//        private var validModifiedLocalUsersBySyncing = mutableListOf<EduUserEvent>()
//        private var validAddedLocalStreamsBySyncing = mutableListOf<EduStreamEvent>()
//        private var validModifiedLocalStreamsBySyncing = mutableListOf<EduStreamEvent>()
//        private var validRemovedLocalStreamsBySyncing = mutableListOf<EduStreamEvent>()

        /**添加有效数据*/
        fun addValidDataBySyncing(validDatas: Array<MutableList<Any>>) {
            validOnlineUsersBySyncing.addAll(validDatas[0] as MutableList<EduUserInfo>)
            validModifiedUsersBySyncing.addAll(validDatas[1] as MutableList<EduUserEvent>)
            validOfflineUsersBySyncing.addAll(validDatas[2] as MutableList<EduUserEvent>)
            validAddedStreamsBySyncing.addAll(validDatas[3] as MutableList<EduStreamEvent>)
            validModifiedStreamsBySyncing.addAll(validDatas[4] as MutableList<EduStreamEvent>)
            validRemovedStreamsBySyncing.addAll(validDatas[5] as MutableList<EduStreamEvent>)
        }

        /**回调同步过程中的有效数据；本地和远端数据分开走不同的回调函数*/
        fun callbackValidData(eduRoom: EduRoom) {
            val validDataArray = arrayOf(validOnlineUsersBySyncing, validModifiedUsersBySyncing,
                    validOfflineUsersBySyncing, validAddedStreamsBySyncing,
                    validModifiedStreamsBySyncing, validRemovedStreamsBySyncing)

            /**处理同步过程中的有效数据，过滤出本地数据，并把本地数据从集合中remove掉*/
            val validLocal = CMDProcessor.processValidData(eduRoom, validDataArray)

            /**数据同步的第二阶段期间，发生改变的本地有效数据*/
            val validModifiedLocalUsersBySyncing = validLocal[0] as MutableList<EduUserEvent>
            val validAddedLocalStreamsBySyncing = validLocal[1] as MutableList<EduStreamEvent>
            val validModifiedLocalStreamsBySyncing = validLocal[2] as MutableList<EduStreamEvent>
            val validRemovedLocalStreamsBySyncing = validLocal[3] as MutableList<EduStreamEvent>

            if (validOnlineUsersBySyncing.size > 0) {
                eduRoom.eventListener?.onRemoteUsersJoined(validOnlineUsersBySyncing, eduRoom)
            }
            if (validModifiedUsersBySyncing.size > 0) {
                eduRoom.eventListener?.onRemoteUserUpdated(validModifiedUsersBySyncing, eduRoom)
            }
            validModifiedLocalUsersBySyncing?.forEach {
                eduRoom.localUser.eventListener?.onLocalUserUpdated(it)
            }
            if (validOfflineUsersBySyncing.size > 0) {
                eduRoom.eventListener?.onRemoteUsersLeft(validOfflineUsersBySyncing, eduRoom)
            }
            if (validAddedStreamsBySyncing.size > 0) {
                eduRoom.eventListener?.onRemoteStreamsAdded(validAddedStreamsBySyncing, eduRoom)
            }
            validAddedLocalStreamsBySyncing?.forEach {
                eduRoom.localUser.eventListener?.onLocalStreamAdded(it)
            }
            if (validModifiedStreamsBySyncing.size > 0) {
                eduRoom.eventListener?.onRemoteStreamsUpdated(validModifiedStreamsBySyncing, eduRoom)
            }
            validModifiedLocalStreamsBySyncing?.forEach {
                eduRoom.localUser.eventListener?.onLocalStreamUpdated(it)
            }
            if (validRemovedStreamsBySyncing.size > 0) {
                eduRoom.eventListener?.onRemoteStreamsRemoved(validRemovedStreamsBySyncing, eduRoom)
            }
            validRemovedLocalStreamsBySyncing?.forEach {
                eduRoom.localUser.eventListener?.onLocalStreamRemoved(it)
            }
            /**每次增量数据回调完成，都要清空集合*/
            validOnlineUsersBySyncing.clear()
            validModifiedUsersBySyncing.clear()
            validOfflineUsersBySyncing.clear()
            validAddedStreamsBySyncing.clear()
            validModifiedStreamsBySyncing.clear()
            validRemovedStreamsBySyncing.clear()
        }


    }
}