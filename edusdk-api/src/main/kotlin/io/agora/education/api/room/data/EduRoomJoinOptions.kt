package io.agora.education.api.room.data

data class RoomMediaOptions(
        var autoSubscribeVideo: Boolean = true,
        var autoSubscribeAudio: Boolean = true,
        var autoPublishCamera: Boolean = true,
        var autoPublishMicrophone: Boolean = true
) {
    /**用户传了primaryStreamId,那么就用他当做streamUuid;如果没传，就是默认值，后端会生成一个streamUuid*/
    var primaryStreamId: Int = DefaultStreamId

    companion object {
        const val DefaultStreamId = 0
    }

    constructor(primaryStreamId: Int) : this() {
        this.primaryStreamId = primaryStreamId
    }

    fun isAutoPublish(): Boolean {
        return this.autoPublishCamera || this.autoPublishMicrophone
    }

    fun isAutoSubscribe(): Boolean {
        return this.autoSubscribeVideo || this.autoSubscribeAudio
    }
}

data class RoomJoinOptions(
        var userUuid: String,
        var userName: String,
        var mediaOptions: RoomMediaOptions
) {
    fun closeAutoPublish() {
        mediaOptions.autoPublishCamera = false
        mediaOptions.autoPublishMicrophone = false
    }
}
