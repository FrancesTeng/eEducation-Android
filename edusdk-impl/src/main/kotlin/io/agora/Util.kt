package io.agora

import io.agora.education.api.room.data.RoomType
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.impl.room.data.response.EduUserRes
import io.agora.rte.data.OffLineUserInfo

class Util {
    companion object {

        /**调用此函数之前须确保first和second代表的是同一个用户
         *
         * 比较first的数据是否比second的更为接近当前时间(即找出一个最新数据)
         * @return > 0（first >= second）
         *         !(> 0) first < second*/
        fun compareUserInfoTime(first: EduUserInfo, second: EduUserInfo): Long {
            /**判断更新时间是否为空(为空的有可能是原始数据)*/
            if (first.updateTime == null) {
                return -1
            }
            if (second.updateTime == null) {
                return first.updateTime!!
            }
            /**最终判断出最新数据*/
            return first.updateTime!!.minus(second.updateTime!!)
        }

        /**从 {@param userInfoList} 中过滤掉 离开课堂的用户 {@param offLineUserList}*/
        fun filterUserWithOffline(offLineUserList: MutableList<OffLineUserInfo>,
                                  userInfoList: MutableList<EduUserInfo>, roomType: RoomType):
                MutableList<EduUserInfo> {
            var validUserInfoList = mutableListOf<EduUserInfo>()
            for (element in offLineUserList) {
                val role = Convert.convertUserRole(element.role, roomType)
                val userInfo1 = EduUserInfo(element.userUuid, element.userName, role, element.updateTime)
                if (userInfoList.contains(userInfo1)) {
                    val index = userInfoList.indexOf(userInfo1)

                    /**获取已存在于集合中的用户*/
                    val userInfo2 = userInfoList[index]
                    /**找出最新数据并替换*/
                    if (compareUserInfoTime(userInfo1, userInfo2) > 0) {
                        /**剔除掉被过滤掉的用户*/
                        userInfoList.removeAt(index)
                        validUserInfoList.add(userInfo1)
                    }
                }
            }
            return validUserInfoList
        }

        fun addUserWithOnline(onlineUserList: MutableList<EduUserRes>,
                              userInfoList: MutableList<EduUserInfo>, roomType: RoomType):
                MutableList<EduUserInfo> {
            var validUserInfoList = mutableListOf<EduUserInfo>()
            for (element in onlineUserList) {
                val role = Convert.convertUserRole(element.role, roomType)
                val userInfo1 = EduUserInfo(element.userUuid, element.userName, role, element.updateTime)
                if (userInfoList.contains(userInfo1)) {
                    val index = userInfoList.indexOf(userInfo1)

                    /**获取已存在于集合中的用户*/
                    val userInfo2 = userInfoList[index]
                    if (compareUserInfoTime(userInfo1, userInfo2) > 0) {
                        /**更新用户的数据为最新数据*/
                        userInfoList[index] = userInfo1
                        validUserInfoList.add(userInfo1)
                    }
                } else {
                    userInfoList.add(userInfo1)
                    validUserInfoList.add(userInfo1)
                }
            }
            return validUserInfoList
        }

        fun modifyUserWithUserStateChange(userStateChangedList: MutableList<EduUserInfo>,
                                          eduUserInfos: MutableList<EduUserInfo>)
                : MutableList<EduUserInfo> {
            var validUserInfoList = mutableListOf<EduUserInfo>()
            for (element in userStateChangedList) {
                if (eduUserInfos.contains(element)) {
                    val index = eduUserInfos.indexOf(element)
                    /**获取已存在于集合中的用户*/
                    val userInfo2 = eduUserInfos[index]
                    if (compareUserInfoTime(element, userInfo2) > 0) {
                        /**更新用户的数据为最新数据*/
                        eduUserInfos[index] = element
                        validUserInfoList.add(element)
                    }
                }
            }
            return validUserInfoList
        }


        /**调用此函数之前须确保first和second代表的是同一个流
         *
         * 比较first的数据是否比second的更为接近当前时间(即找出一个最新数据)
         * @return > 0（first >= second）
         *         !(> 0) first < second*/
        fun compareStreamInfoTime(first: EduStreamInfo, second: EduStreamInfo): Long {
            /**判断更新时间是否为空(为空的有可能是原始数据)*/
            if (first.updateTime == null) {
                return -1
            }
            if (second.updateTime == null) {
                return first.updateTime!!
            }
            /**最终判断出最新数据*/
            return first.updateTime!!.minus(second.updateTime!!)
        }

        fun addStreamWithAction(addStreamList: MutableList<EduStreamInfo>,
                                streamInfoList: MutableList<EduStreamInfo>):
                MutableList<EduStreamInfo> {
            var validStreamList = mutableListOf<EduStreamInfo>()
            for (element in addStreamList) {
                if (streamInfoList.contains(element)) {
                    val index = streamInfoList.indexOf(element)

                    /**获取已存在于集合中的用户*/
                    val userInfo2 = streamInfoList[index]
                    if (compareStreamInfoTime(element, userInfo2) > 0) {
                        /**更新用户的数据为最新数据*/
                        streamInfoList[index] = element
                        validStreamList.add(element)
                    }
                } else {
                    streamInfoList.add(element)
                    validStreamList.add(element)
                }
            }
            return validStreamList
        }

        fun modifyStreamWithAction(modifyStreamList: MutableList<EduStreamInfo>,
                                   streamInfoList: MutableList<EduStreamInfo>):
                MutableList<EduStreamInfo> {
            var validStreamList = mutableListOf<EduStreamInfo>()
            for (element in modifyStreamList) {
                if (streamInfoList.contains(element)) {
                    val index = streamInfoList.indexOf(element)

                    /**获取已存在于集合中的用户*/
                    val userInfo2 = streamInfoList[index]
                    if (compareStreamInfoTime(element, userInfo2) > 0) {
                        /**更新用户的数据为最新数据*/
                        streamInfoList[index] = element
                        validStreamList.add(element)
                    }
                }
            }
            return validStreamList
        }

        fun removeStreamWithAction(modifyStreamList: MutableList<EduStreamInfo>,
                                   streamInfoList: MutableList<EduStreamInfo>):
                MutableList<EduStreamInfo> {
            var validStreamList = mutableListOf<EduStreamInfo>()
            for (element in modifyStreamList) {
                if (streamInfoList.contains(element)) {
                    val index = streamInfoList.indexOf(element)

                    /**获取已存在于集合中的用户*/
                    val userInfo2 = streamInfoList[index]
                    if (compareStreamInfoTime(element, userInfo2) > 0) {
                        /**更新用户的数据为最新数据*/
                        streamInfoList.removeAt(index)
                        validStreamList.add(element)
                    }
                }
            }
            return validStreamList
        }
    }
}
