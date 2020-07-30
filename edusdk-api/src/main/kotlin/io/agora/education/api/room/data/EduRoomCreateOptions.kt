package io.agora.education.api.room.data

enum class RoomType(var value: Int) {
    ONE_ON_ONE(0),
    SMALL_CLASS(1),
    LARGE_CLASS(2)
}

data class RoomProperty(
        val key: String,
        val value: String?
) {
    companion object {
        const val KEY_TEACHER_LIMIT = "TeacherLimit"
        const val KEY_STUDENT_LIMIT = "StudentLimit"
    }
}

class RoomCreateOptions(
        var roomUuid: String,
        var roomName: String,
        val roomType: Int
) {
    val properties: MutableList<RoomProperty> = mutableListOf()

    init {

        properties.add(RoomProperty(RoomProperty.KEY_TEACHER_LIMIT, when (roomType) {
            RoomType.ONE_ON_ONE.value -> "1"
            RoomType.SMALL_CLASS.value -> "1"
            RoomType.LARGE_CLASS.value -> "1"
            /**-1表示不做限制*/
            else -> "-1"
        }))
        properties.add(RoomProperty(RoomProperty.KEY_STUDENT_LIMIT, when (roomType) {
            RoomType.ONE_ON_ONE.value -> "1"
            RoomType.SMALL_CLASS.value -> "16"
            RoomType.LARGE_CLASS.value -> "-1"
            else -> "-1"
        }))
    }
}
