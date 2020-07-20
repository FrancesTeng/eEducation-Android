package io.agora.education.impl.room.network

import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.impl.room.data.request.EduJoinClassroomReq
import io.agora.education.impl.room.data.request.RoomCreateOptionsReq
import io.agora.education.impl.room.data.response.*
import retrofit2.Call
import retrofit2.http.*

interface RoomService {

    /**@return 房间id(roomId)*/
    @PUT("/scenario/education/apps/{appId}/v1/rooms/{roomUuid}/config")
    fun createClassroom(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Body roomCreateOptionsReq: RoomCreateOptionsReq
    ): Call<ResponseBody<Int>>

    @POST("/scenario/education/apps/{appId}/v1/rooms/{roomId}/entry")
    fun joinClassroomAsTeacher(
            @Path("appId")  appId: String,
            @Path("roomUuid") roomUuid: String?,
            @Body                 eduJoinClassroomReq: EduJoinClassroomReq
    ): Call<ResponseBody<EduClassRoomEntryRes>>

    @POST("/scenario/education/apps/{appId}/v1/rooms/{roomId}/entry")
    fun joinClassroomAsStudent(
            @Path("appId")  appId: String,
            @Path("roomUuid") roomUuid: String?,
            @Body                 eduJoinClassroomReq: EduJoinClassroomReq
    ): Call<ResponseBody<EduClassRoomEntryRes>>

    /**@param role 角色, 多个逗号分隔 非必须参数（拉全量数据，不传此参数等于所有角色值全传）
     * @param nextId 下一页起始ID；非必须参数
     * @param count 返回条数	*/
    @GET("/scenario/education/apps/{appId}/v1/rooms/{roomUuid}/users")
    fun getFullUserList(
            @Header("userToken") userToken: String,
            @Path("appId")       appId: String,
            @Path("roomUuid")      roomUuid: String,
//            @Query("role")       role: String?,
            @Query("nextId")     nextId: String?,
            @Query("count")      count: Int
    ): Call<ResponseBody<EduUserListRes>>

    /**@param role 角色, 多个逗号分隔 非必须参数（拉全量数据，不传此参数等于所有角色值全传）
     * @param nextId 本次查询起始userId；非必须参数
     * @param count 返回条数	*/
    @GET("/scenario/education/apps/{appId}/v1/rooms/{roomUuid}/users/userStreams")
    fun getFullStreamList(
            @Header("userToken") userToken: String,
            @Path("appId")       appId: String,
            @Path("roomUuid")      roomUuid: String,
//            @Query("role")       role: String?,
            @Query("nextId")     nextId: String?,
            @Query("count")      count: Int
    ): Call<ResponseBody<EduStreamListRes>>

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