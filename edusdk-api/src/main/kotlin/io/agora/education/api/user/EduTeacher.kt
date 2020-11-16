package io.agora.education.api.user

import io.agora.education.api.EduCallback
import io.agora.education.api.room.data.EduRoomState
import io.agora.education.api.room.data.Property
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.stream.data.ScreenStreamInitOptions
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.api.user.listener.EduTeacherEventListener

interface EduTeacher : EduUser {
    fun setEventListener(eventListener: EduTeacherEventListener?)

    /**code:message
     * 1:parameter XXX is invalid
     * 2:internal error：可以内部订阅具体什么错误
     * 301:network error，透传后台错误msg字段
     * @param roomState 目的状态*/
    fun updateCourseState(roomState: EduRoomState, callback: EduCallback<Unit>)

    /**code:message
     * 1:parameter XXX is invalid
     * 2:internal error：可以内部订阅具体什么错误
     * 301:network error，透传后台错误msg字段*/
    fun allowAllStudentChat(isAllow: Boolean, callback: EduCallback<Unit>)

    /**code:message
     * 1:parameter XXX is invalid
     * 2:internal error：可以内部订阅具体什么错误
     * 301:network error，透传后台错误msg字段*/
    fun allowStudentChat(isAllow: Boolean, studentInfo: EduUserInfo, callback: EduCallback<Unit>)

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

    /**为学生新建或者更新流
     * code:message
     * 1:parameter XXX is invalid
     * 2:internal error：可以内部订阅具体什么错误
     * 301:network error，透传后台错误msg字段*/
    fun createOrUpdateStudentStream(streamInfo: EduStreamInfo, callback: EduCallback<Unit>)
}
