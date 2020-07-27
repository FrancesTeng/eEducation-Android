package io.agora.education.impl.user.network

import io.agora.education.impl.room.data.response.EduStreamListRes
import io.agora.education.impl.room.data.response.EduUserListRes
import io.agora.education.impl.user.data.request.*
import io.agora.education.impl.ResponseBody
import io.agora.education.impl.room.data.request.EduJoinClassroomReq
import io.agora.education.impl.room.data.response.EduEntryRes
import retrofit2.Call
import retrofit2.http.*

interface UserService {

    /**加入房间*/
    @POST("/scenario/education/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}/entry")
    fun joinClassroom(
            @Path("appId")  appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid") userUuid: String,
            @Body                 eduJoinClassroomReq: EduJoinClassroomReq
    ): Call<ResponseBody<EduEntryRes>>

    /**@param role 角色, 多个逗号分隔 非必须参数（拉全量数据，不传此参数等于所有角色值全传）
     * @param nextId 下一页起始ID；非必须参数
     * @param count 返回条数	*/
    @GET("/scenario/education/apps/{appId}/v1/rooms/{roomUuid}/users")
    fun getUserList(
            @Header("userToken") userToken: String,
            @Path("appId")       appId: String,
            @Path("roomUuid")      roomUuid: String,
//            @Query("role")       role: String?,
            @Query("nextId")     nextId: String?,
            @Query("count")      count: Int,
            @Query("updateTimeOffset") updateTimeOffset: Long?,
            @Query("includeOffline") includeOffline: Int?
    ): Call<ResponseBody<EduUserListRes>>

    /**@param role 角色, 多个逗号分隔 非必须参数（拉全量数据，不传此参数等于所有角色值全传）
     * @param nextId 本次查询起始userId；非必须参数
     * @param count 返回条数	*/
    @GET("/scenario/education/apps/{appId}/v1/rooms/{roomUuid}/users/userStreams")
    fun getStreamList(
            @Header("userToken") userToken: String,
            @Path("appId")       appId: String,
            @Path("roomUuid")      roomUuid: String,
//            @Query("role")       role: String?,
            @Query("nextId")     nextId: String?,
            @Query("count")      count: Int,
            @Query("updateTimeOffset") updateTimeOffset: Long?,
            @Query("includeOffline") includeOffline: Int?
    ): Call<ResponseBody<EduStreamListRes>>


    /**更新某一个用户的禁止聊天状态
     * @param mute 可否聊天 1可 0否*/
    @PUT("/scenario/education/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}")
    fun updateUserMuteState(
            @Header("userToken")userToken: String,
            @Path("appId")  appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid")  userUuid: String,
            @Body eduUserStatusReq: EduUserStatusReq
    ): Call<io.agora.base.network.ResponseBody<String>>


    /**调用此接口需要添加header->userToken
     * 此处的返回值没有写错，确实只返回code 和 msg*/
    @POST("/scenario/education/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}/exit")
    fun leaveClassroom(
            @Header("userToken") userToken: String,
            @Path("appId")       appId: String,
            @Path("roomUuid")      roomUuid: String,
            @Path("userUuid")      userUuid: String
    ): Call<io.agora.base.network.ResponseBody<String>>


}