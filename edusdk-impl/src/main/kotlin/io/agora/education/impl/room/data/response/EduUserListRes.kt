package io.agora.education.impl.room.data.response

import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.impl.user.data.EduUserInfoImpl

class EduUserListRes(
        var count: Int,
        var total: Int,
        var nextId: String,
        var list: MutableList<EduUserRes>
) {
    /**把EduUserRes转换为EduUserInfo*/
    fun getUserInfoList(): MutableList<EduUserInfo> {
        if (list?.size == 0) {
            return mutableListOf()
        }
        var userInfoList: MutableList<EduUserInfo> = mutableListOf()
        for ((index, element) in list.withIndex()) {
            var eduUser = EduUserInfoImpl(element.userId, element.userUuid,
                    element.userName, element.role)
            userInfoList.add(index, eduUser)
        }
        return userInfoList
    }
}