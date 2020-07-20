package io.agora.education.api.stream.data

import io.agora.education.api.user.data.EduUserInfo

enum class VideoSourceType(var value: Int) {
    CAMERA(1),
    SCREEN(2)
}

enum class AudioSourceType(var value: Int) {
    MICROPHONE(1)
}

open class EduStreamInfo(
        var streamUuid: String,
        var streamName: String,
        val videoSourceType: VideoSourceType,
        var hasVideo: Boolean,
        var hasAudio: Boolean,

        val publisher: EduUserInfo
)

enum class EduVideoState(var value: Int) {
    Off(0),
    Open(1),
    Disable(2)
}

enum class EduAudioState(var value: Int) {
    Off(0),
    Open(1),
    Disable(2)
}
