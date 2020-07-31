package io.agora.education.impl.cmd

class CMDSyncUsrStreamRes(
        val step: Int,
        val isFinished: Int,
        val count: Int,
        val total: Int,
        val nextId: Int,
        val nextTs: Long,
        val list: MutableList<CMDSyncUsrRes>
) {
}


class CMDSyncUsrRes(
        val userName: String,
        val userUuid: String,
        val role: String,
        val muteChat: Int,
        val userProperties: Any,
        val updateTime: Long,
        /**标识此用户是新下线用户还是新上线用户(ValidState)*/
        val state: Int,
        val streams: MutableList<CMDSyncStreamRes>
) {
}


class CMDSyncStreamRes(
        val streamUuid: String,
        val streamName: String,
        val videoSourceType: Int,
        val audioSourceType: Int,
        val videoState: Int,
        val audioState: Int,
        val updateTime: Long,
        /**标识此流是新下线还是新上线(ValidState)*/
        val state: Int
) {

}