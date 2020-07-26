package io.agora.education.api.room.data

data class RoomMediaOptions(
        val autoSubscribeVideo: Boolean = true,
        val autoSubscribeAudio: Boolean = true,
        val autoPublishCamera: Boolean = true,
        val autoPublishMicrophone: Boolean = true
) {
    /**用户传了primaryStreamId,那么就用他当做streamUuid;如果没传，就是默认值，后端会生成一个streamUuid*/
    var primaryStreamId: Int = DefaultStreamId

    companion object {
        const val DefaultStreamId = 0
    }

    constructor(primaryStreamId: Int) : this() {
        this.primaryStreamId = primaryStreamId
    }
}

data class RoomJoinOptions(
        var userUuid: String,
        var userName: String,
        var mediaOptions: RoomMediaOptions
)
