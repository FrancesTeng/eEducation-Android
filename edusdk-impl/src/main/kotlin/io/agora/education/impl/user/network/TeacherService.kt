package io.agora.education.impl.user.network

import io.agora.base.network.ResponseBody
import io.agora.education.impl.user.data.request.EduRoomStatusReq
import io.agora.education.impl.user.data.request.EduStreamStatusReq
import io.agora.education.impl.user.data.request.EduUserStatusReq
import retrofit2.Call
import retrofit2.http.*

interface TeacherService : UserService {

    /**开始上课和下课*/
    @PUT("/scenario/education/apps/{appId}/v1/rooms/{roomUUid}/states/{state}")
    fun beginOrEndClass(
            @Header("userToken")userToken: String,
            @Path("appId")  appId: String,
            @Path("roomUUid") roomUUid: String,
            @Path("state") state: Int
    ): Call<ResponseBody<String>>

    /**是否全体禁言*/
    @PUT("/scenario/education/apps/{appId}/v1/rooms/{roomUuid}/roles/mute")
    fun allowStudentChat(
            @Header("userToken")userToken: String,
            @Path("appId")  appId: String,
            @Path("roomUuid") roomUuid: String,
            @Body eduRoomStatusReq: EduRoomStatusReq
    ): Call<ResponseBody<String>>

    /**是否允许某一个学生聊天
     * @param mute 可否聊天 1可 0否*/
    @PUT("/scenario/education/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}")
    fun allowStudentChat(
            @Header("userToken")userToken: String,
            @Path("appId")  appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid")  userUuid: String,
            @Body eduUserStatusReq: EduUserStatusReq
    ): Call<ResponseBody<String>>

    /**打开或关闭某一个学生的摄像头*/
    @PUT("/scenario/education/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}/streams/{streamUuid}")
    fun OpenOrCloseStudentCamera(
            @Header("userToken")userToken: String,
            @Path("appId")  appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid")  userUuid: String,
            @Path("streamUuid") streamUuid: String,
            @Body eduStreamStatusReq: EduStreamStatusReq
    ): Call<ResponseBody<String>>

    /**打开或关闭某一个学生的麦克风*/
    @POST("/scenario/education/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}/streams/{streamUuid}")
    fun OpenOrCloseStudentMicrophone(
            @Header("userToken")userToken: String,
            @Path("appId")  appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid")  userUuid: String,
            @Path("streamUuid") streamUuid: String,
            @Body eduStreamStatusReq: EduStreamStatusReq
    ): Call<ResponseBody<String>>
}