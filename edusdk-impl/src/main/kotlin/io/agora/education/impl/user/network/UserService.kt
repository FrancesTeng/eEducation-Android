package io.agora.education.impl.user.network

import io.agora.education.impl.user.data.request.EduPublishStreamReq
import io.agora.education.impl.user.data.request.EduRoomMsgReq
import io.agora.education.impl.user.data.request.EduUserMsgReq
import io.agora.education.impl.user.data.response.EduPublishStreamRes
import io.agora.education.impl.user.data.response.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface UserService {

    @POST("/scenario/education/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}/streams/{streamUuid}")
    fun publishStream(
            @Header("userToken") userToken:String,
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid") userUuid: String,
            @Path("streamUuid") streamUuid: String,
            @Body eduPublishStreamReq: EduPublishStreamReq
    ): Call<ResponseBody<EduPublishStreamRes>>

    @DELETE("/scenario/education/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}/streams/{streamUuid}")
    fun unPublishStream(
            @Header("userToken") userToken: String,
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Path("userUuid") userUuid: String,
            @Path("streamUuid") streamUuid: String
    ): Call<io.agora.base.network.ResponseBody<String>>

    @POST("/scenario/education/apps/{appId}/v1/rooms/{roomUuid}/message/channel")
    fun sendRoomMessage(
            @Header("userToken") userToken: String,
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Body eduRoomMsgReq: EduRoomMsgReq
    ): Call<io.agora.base.network.ResponseBody<String>>

    @POST("/scenario/education/apps/{appId}/v1/rooms/{roomUuid}/message/peer")
    fun sendUserMessage(
            @Header("userToken") userToken: String,
            @Path("appId") appId: String,
            @Path("roomUuid") roomUuid: String,
            @Body eduUserMsgReq: EduUserMsgReq
    ): Call<io.agora.base.network.ResponseBody<String>>
}