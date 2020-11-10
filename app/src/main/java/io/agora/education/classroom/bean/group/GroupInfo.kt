package io.agora.education.classroom.bean.group


class GroupInfo(
        val groupUuid: String,
        val groupName: String,
        val memberLimit: Int,
        val memberCount: Int,
        /*组员id集合*/
        val members: MutableList<String>,
        /*小组内的自定义属性*/
        val groupProperties: MutableMap<String, Any>,
        val createTime: Long
) {
}