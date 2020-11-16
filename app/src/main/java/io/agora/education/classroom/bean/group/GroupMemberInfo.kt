package io.agora.education.classroom.bean.group

import io.agora.education.api.user.data.EduBaseUserInfo
import io.agora.education.api.user.data.EduUserInfo

class GroupMemberInfo(
        userInfo: EduUserInfo
) {
    private val REWARD = "reward"

    var userInfo: EduBaseUserInfo = userInfo
    var reward: Int = 0
    var online: Boolean = false

    init {
        /*设置积分*/
        val properties = userInfo.userProperties
        if (properties != null) {
            for ((key, value) in properties.entries) {
                if (key == REWARD) {
                    this.reward = value as Int
                    break
                }
            }
        }
    }

    /**用户上台*/
    fun online() {
        online = true
    }

    fun offLine() {
        online = false
    }


}