package io.agora.education.classroom.bean.group

class RoomGroupInfo() {
    companion object {
        const val GROUPSTATES = "groupsStates"
        const val INTERACTOUTGROUPS = "interactOutGroups"
        const val GROUPS = "groups"

        /*班级的全体人员名单(包含在线和不在线)*/
        const val ALLMEMBERS = "allMembers"
    }

    /*分组状态*/
    var groupStates: GroupStateInfo? = null

    /*参与组外互动的小组id集合*/
    var interactOutGroups: MutableList<String>? = null

    /*分组后的各小组信息集合*/
    var groups: MutableList<GroupInfo>? = null

    /**是否开启分组*/
    fun enableGroup(): Boolean {
        groupStates?.let {
            return it.state == GroupState.ENABLE.value
        }
        return false
    }

    /**是否开启PK*/
    fun enablePK(): Boolean {
        groupStates?.let {
            return enableGroup() && it.interactOutGroup == InteractState.ENABLE.value
        }
        return false
    }
}