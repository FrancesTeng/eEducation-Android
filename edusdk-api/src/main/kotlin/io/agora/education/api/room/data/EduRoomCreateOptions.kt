package io.agora.education.api.room.data

enum class RoomType(var value: Int) {
    ONE_ON_ONE(0),
    SMALL_CLASS(1),
    LARGE_CLASS(2)
}

data class Property(
        val key: String,
        val value: String
) {
    companion object {
        const val KEY_TEACHER_LIMIT = "TeacherLimit"
        const val KEY_STUDENT_LIMIT = "StudentLimit"
    }
}

/**@param createRemoteClassroom 是否调用远端接口创建房间*/
class RoomCreateOptions(
        var roomUuid: String,
        var roomName: String,
        val roomType: Int,
        val createRemoteClassroom: Boolean
) {
    val roomProperties: MutableList<Property> = mutableListOf()

    init {

        roomProperties.add(Property(Property.KEY_TEACHER_LIMIT, when (roomType) {
            RoomType.ONE_ON_ONE.value -> "1"
            RoomType.SMALL_CLASS.value -> "1"
            RoomType.LARGE_CLASS.value -> "1"
            /**-1表示不做限制*/
            else -> "-1"
        }))
        roomProperties.add(Property(Property.KEY_STUDENT_LIMIT, when (roomType) {
            RoomType.ONE_ON_ONE.value -> "1"
            RoomType.SMALL_CLASS.value -> "16"
            RoomType.LARGE_CLASS.value -> "-1"
            else -> "-1"
        }))
    }
}
