package io.agora.education.impl.room.network

import io.agora.education.impl.ResponseBody
import io.agora.education.impl.room.data.request.RoomCreateOptionsReq
import io.agora.education.impl.room.data.response.EduEntryRoomRes
import io.agora.education.impl.user.data.request.*
import io.agora.education.impl.user.data.request.EduRoomChatMsgReq
import io.agora.education.impl.user.data.request.EduRoomMsgReq
import io.agora.education.impl.user.data.request.EduUserChatMsgReq
import io.agora.education.impl.user.data.request.EduUserMsgReq
import retrofit2.Call
import retrofit2.http.*

internal interface RoomService {

    /**创建房间*/
    /**@return 房间id(roomId)*/
    @PUT("/scenario/education/apps/{appId}/v1/rooms/{roomUuid}/config")
    fun createClassroom(
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Body roomCreateOptionsReq: RoomCreateOptionsReq
    ): Call<io.agora.base.network.ResponseBody<String>>

    /**查询房间信息*/
    @GET("/scenario/education/apps/{appId}/v1/rooms/{roomUuid}/info")
    fun queryClassroomState(
            @Header("userToken") userToken: String,
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String
    ): Call<ResponseBody<EduEntryRoomRes>>

    /**更新课堂状态*/
    @PUT("/scenario/education/apps/{appId}/v1/rooms/{roomUUid}/states/{state}")
    fun updateClassroomState(
            @Header("userToken")userToken: String,
            @Path("appId")  appId: String,
            @Path("roomUUid") roomUUid: String,
            @Path("state") state: Int
    ): Call<io.agora.base.network.ResponseBody<String>>

    /**更新课堂中对应角色的禁用状态
     * 包括禁止聊天、禁止摄像头、禁用麦克风*/
    @PUT("/scenario/education/apps/{appId}/v1/rooms/{roomUuid}/roles/mute")
    fun updateClassroomMuteState(
            @Header("userToken")userToken: String,
            @Path("appId")  appId: String,
            @Path("roomUuid") roomUuid: String,
            @Body eduRoomMuteStateReq: EduRoomMuteStateReq
    ): Call<io.agora.base.network.ResponseBody<String>>

    /**发送自定义的频道消息*/
    @POST("/scenario/education/apps/{appId}/v1/rooms/{roomUuid}/message/channel")
    fun sendChannelCustomMessage(
            @Header("userToken") userToken: String,
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Body eduRoomMsgReq: EduRoomMsgReq
    ): Call<io.agora.base.network.ResponseBody<String>>

    /**发送自定义的点对点消息*/
    @POST("/scenario/education/apps/{appId}/v1/rooms/{roomUuid}/message/peer")
    fun sendPeerCustomMessage(
            @Header("userToken") userToken: String,
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("toUserUuid") toUserUuid: String,
            @Body eduUserMsgReq: EduUserMsgReq
    ): Call<io.agora.base.network.ResponseBody<String>>

    /**发送课堂内群聊消息*/
    @POST("/scenario/education/apps/{appId}/v1/rooms/{roomUuid}/message/chat")
    fun sendRoomChatMsg(
            @Header("userToken") userToken: String,
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Body eduRoomChatMsgReq: EduRoomChatMsgReq
    ): Call<io.agora.base.network.ResponseBody<String>>

    /**发送用户间的私聊消息*/
    @POST("/scenario/education/apps/{appId}/v1/rooms/{roomUuid}/message/peer")
    fun sendPeerChatMsg(
            @Header("userToken") userToken: String,
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("toUserUuid") toUserUuid: String,
            @Body eduUserChatMsgReq: EduUserChatMsgReq
    ): Call<io.agora.base.network.ResponseBody<String>>


}