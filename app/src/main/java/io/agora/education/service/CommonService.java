package io.agora.education.service;

import java.util.Map;

import io.agora.education.BuildConfig;
import io.agora.education.service.bean.ResponseBody;
import io.agora.education.service.bean.request.AllocateGroupReq;
import io.agora.education.service.bean.request.RoomCreateOptionsReq;
import io.agora.education.service.bean.response.AppVersionRes;
import io.agora.education.service.bean.response.EduRoomInfoRes;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface CommonService {

    // osType 1 iOS 2 Android
    // terminalType 1 phone 2 pad
    @GET("/edu/v1/app/version?appCode=" + BuildConfig.CODE + "&osType=2&terminalType=1&appVersion=" + BuildConfig.VERSION_NAME)
    Call<ResponseBody<AppVersionRes>> appVersion();

    @GET("/edu/v1/multi/language")
    Call<ResponseBody<Map<String, Map<Integer, String>>>> language();

    /**
     * 分配小组:请求服务端分配一个小教室
     */
    @POST("/grouping/apps/{appId}/v1/rooms/{roomUuid}/groups")
    Call<ResponseBody<EduRoomInfoRes>> allocateGroup(
            @Path("appId") String appId,
            @Path("roomUuid") String roomUuid,
            @Body AllocateGroupReq allocateGroupReq);

    /**创建房间*/
    /**
     * @return 房间id(roomId)
     */
    @POST("/scene/apps/{appId}/v1/rooms/{roomUuid}/config")
    Call<ResponseBody<String>> createClassroom(
            @Path("appId") String appId,
            @Path("roomUuid") String roomUuid,
            @Body RoomCreateOptionsReq roomCreateOptionsReq
    );
}
