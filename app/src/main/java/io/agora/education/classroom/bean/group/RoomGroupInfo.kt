package io.agora.education.classroom.bean.group

class RoomGroupInfo() {
    companion object {
        const val GROUPSTATES = "groupStates"
        const val INTERACTOUTGROUPS = "interactOutGroups"
        const val GROUPS = "groups"
    }

    /*分组状态*/
    lateinit var groupStates: GroupStateInfo

    /*参与组外互动的小组id集合*/
    lateinit var interactOutGroups: MutableList<String>

    /*分组后的各小组信息集合*/
    lateinit var groups: MutableList<GroupInfo>
}