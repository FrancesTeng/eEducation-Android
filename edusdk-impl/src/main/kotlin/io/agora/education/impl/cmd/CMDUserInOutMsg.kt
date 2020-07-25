package io.agora.education.impl.cmd

import io.agora.education.impl.room.data.response.EduUserRes

/**人员进出时，RTM回调出来的数据结构*/
class RtmUserInOutMsg(val total: Int, val onlineUsers: MutableList<EduUserRes>,
                      val offlineUsers: MutableList<OffLineUserInfo>) {
}

class OffLineUserInfo(userUuid: String, userName: String, role: String, muteChat: Int, updateTime: Long?,
                      val operator: EduUserRes?)
    : EduUserRes(userUuid, userName, role, muteChat, updateTime) {

}