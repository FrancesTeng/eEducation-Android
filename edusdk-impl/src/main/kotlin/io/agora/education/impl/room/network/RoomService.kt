package io.agora.education.impl.room.network

import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.impl.room.data.response.EduClassRoomEntryRes
import io.agora.education.impl.room.data.request.RoomCreateOptionsReq
import io.agora.education.impl.room.data.response.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface RoomService {

    /**@return 房间id(roomId)*/
    @POST("/scenario/education/apps/{appId}/v1/rooms")
    fun createClassroom(
            @Path("appId") appId: String,
            @Body roomCreateOptionsReq: RoomCreateOptionsReq
    ): Call<ResponseBody<Int>>

    @POST("/scenario/education/apps/{appId}/v1/rooms/{roomId}/entry")
    fun joinClassroomAsTeacher(
            @Path("appId")  appId: String,
            @Path("roomId") roomId: String?,
            @Body                 eduUserInfo: EduUserInfo
    ): Call<ResponseBody<EduClassRoomEntryRes>>

    @POST("/scenario/education/apps/{appId}/v1/rooms/{roomId}/entry")
    fun joinClassroomAsStudent(
            @Path("appId")  appId: String,
            @Path("roomId") roomId: String?,
            @Body                 eduUserInfo: EduUserInfo
    ): Call<ResponseBody<EduClassRoomEntryRes>>

    /**@param role 角色 1老师 2学生 可传多个；非必须参数
     * @param nextId 本次查询起始userId；非必须参数
     * @param count 返回条数	*/
    @GET("/scenario/education/apps/{appId}/v1/rooms/{roomId}/users/page")
    fun getFullUserList(
            @Header("userToken") userToken: String,
            @Path("appId")       appId: String,
            @Path("roomId")      roomId: String,
            @Field("role")       role: Int?,
            @Field("nextId")     nextId: String?,
            @Field("count")      count: Int
    ): Call<ResponseBody<Page>>

    /**@param role 角色 1老师 2学生 可传多个；非必须参数
     * @param isPublish 1发布 0不发布；非必须参数
     * @param nextId 本次查询起始userId；非必须参数
     * @param count 返回条数	*/
    @GET("/scenario/education/apps/{appId}/v1/rooms/{roomId}/streams")
    fun getFullStreamList(
            @Header("userToken") userToken: String,
            @Path("appId")       appId: String,
            @Path("roomId")      roomId: String,
            @Field("role")       role: Int,
            @Field("isPublish")  isPublish: Int,
            @Field("nextId")     nextId: String,
            @Field("count")      count: Int
    ): Call<ResponseBody<Page>>

    /**调用此接口需要添加header->userToken*/
    @POST("/scenario/education/apps/{appId}/v1/rooms/{roomId}/exit")
    fun leaveClassroom(
            @Header("userToken") userToken: String,
            @Path("appId")       appId: String,
            @Path("roomId")      roomId: String
    ): Call<ResponseBody<Boolean>>

}