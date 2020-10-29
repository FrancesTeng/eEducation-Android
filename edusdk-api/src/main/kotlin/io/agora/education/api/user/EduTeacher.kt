package io.agora.education.api.user

import io.agora.education.api.EduCallback
import io.agora.education.api.room.data.Property
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.stream.data.ScreenStreamInitOptions
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.api.user.listener.EduTeacherEventListener

interface EduTeacher : EduUser {
    fun setEventListener(eventListener: EduTeacherEventListener)

    /**code:message
     * 1:parameter XXX is invalid
     * 2:internal error：可以内部订阅具体什么错误
     * 301:network error，透传后台错误msg字段*/
    fun beginClass(callback: EduCallback<Unit>)

    /**code:message
     * 1:parameter XXX is invalid
     * 2:internal error：可以内部订阅具体什么错误
     * 301:network error，透传后台错误msg字段*/
    fun endClass(callback: EduCallback<Unit>)

    /**code:message
     * 1:parameter XXX is invalid
     * 2:internal error：可以内部订阅具体什么错误
     * 301:network error，透传后台错误msg字段*/
    fun allowStudentChat(isAllow: Boolean, callback: EduCallback<Unit>)

    /**code:message
     * 1:parameter XXX is invalid
     * 2:internal error：可以内部订阅具体什么错误
     * 301:network error，透传后台错误msg字段*/
    fun allowRemoteStudentChat(isAllow: Boolean, remoteStudent: EduUserInfo, callback: EduCallback<Unit>)

    /**code:message
     * 1:parameter XXX is invalid
     * 2:internal error：可以内部订阅具体什么错误
     * 201:media error:code，透传rtc错误code或者message
     * 301:network error，透传后台错误msg字段*/
    fun startShareScreen(options: ScreenStreamInitOptions, callback: EduCallback<EduStreamInfo>)

    /**code:message
     * 1:parameter XXX is invalid
     * 2:internal error：可以内部订阅具体什么错误
     * 201:media error:code，透传rtc错误code或者message
     * 301:network error，透传后台错误msg字段*/
    fun stopShareScreen(callback: EduCallback<Unit>)

    /**code:message
     * 1:parameter XXX is invalid
     * 2:internal error：可以内部订阅具体什么错误
     * 301:network error，透传后台错误msg字段*/
    fun remoteStartStudentCamera(remoteStream: EduStreamInfo, callback: EduCallback<Unit>)

    /**code:message
     * 1:parameter XXX is invalid
     * 2:internal error：可以内部订阅具体什么错误
     * 301:network error，透传后台错误msg字段*/
    fun remoteStopStudentCamera(remoteStream: EduStreamInfo, callback: EduCallback<Unit>)

    /**code:message
     * 1:parameter XXX is invalid
     * 2:internal error：可以内部订阅具体什么错误
     * 301:network error，透传后台错误msg字段*/
    fun remoteStartStudentMicrophone(remoteStream: EduStreamInfo, callback: EduCallback<Unit>)

    /**code:message
     * 1:parameter XXX is invalid
     * 2:internal error：可以内部订阅具体什么错误
     * 301:network error，透传后台错误msg字段*/
    fun remoteStopStudentMicrophone(remoteStream: EduStreamInfo, callback: EduCallback<Unit>)
}
