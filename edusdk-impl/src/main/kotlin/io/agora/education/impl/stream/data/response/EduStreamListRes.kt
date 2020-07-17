package io.agora.education.impl.stream.data.response

import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.impl.room.data.response.EduUserRes
import io.agora.education.impl.stream.EduStreamInfoImpl
import io.agora.education.impl.user.data.EduUserInfoImpl

class EduStreamListRes(
        var count: Int,
        var total: Int,
        var nextId: String,
        var list: MutableList<EduStreamRes>
) {

    fun getStreamInfoList(): MutableList<EduStreamInfo> {
        if (list?.size == 0) {
            return mutableListOf()
        }
        var streamInfoList: MutableList<EduStreamInfo> = mutableListOf()
        for ((index, element) in list.withIndex()) {
            var eduStream = EduStreamInfoImpl(element.streamId.toInt(),
                    element.streamUuid, element.streamName,
                    element.videoSourceType,
                    element.hasVideo, element.hasAudio,
                    element.fromUser)
            streamInfoList.add(index, eduStream)
        }
        return streamInfoList
    }
}