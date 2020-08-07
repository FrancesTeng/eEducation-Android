package io.agora.education.classroom;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import io.agora.base.callback.ThrowableCallback;
import io.agora.base.network.RetrofitManager;
import io.agora.education.EduApplication;
import io.agora.education.R;
import io.agora.education.RoomEntry;
import io.agora.education.api.EduCallback;
import io.agora.education.api.message.EduChatMsg;
import io.agora.education.api.message.EduMsg;
import io.agora.education.api.room.EduRoom;
import io.agora.education.api.room.data.EduRoomInfo;
import io.agora.education.api.room.data.RoomCreateOptions;
import io.agora.education.api.room.data.RoomJoinOptions;
import io.agora.education.api.room.data.RoomMediaOptions;
import io.agora.education.api.room.data.RoomStatusEvent;
import io.agora.education.api.room.listener.EduRoomEventListener;
import io.agora.education.api.statistics.ConnectionState;
import io.agora.education.api.statistics.ConnectionStateChangeReason;
import io.agora.education.api.stream.data.EduStreamEvent;
import io.agora.education.api.stream.data.EduStreamInfo;
import io.agora.education.api.stream.data.LocalStreamInitOptions;
import io.agora.education.api.user.EduStudent;
import io.agora.education.api.user.EduUser;
import io.agora.education.api.user.data.EduUserEvent;
import io.agora.education.api.user.data.EduUserInfo;
import io.agora.education.api.user.data.EduUserRole;
import io.agora.education.api.user.listener.EduUserEventListener;
import io.agora.education.base.BaseActivity;
import io.agora.education.classroom.bean.board.BoardBean;
import io.agora.education.classroom.bean.board.BoardFollowMode;
import io.agora.education.classroom.bean.channel.Room;
import io.agora.education.classroom.fragment.ChatRoomFragment;
import io.agora.education.classroom.fragment.WhiteBoardFragment;
import io.agora.education.classroom.widget.TitleView;
import io.agora.education.service.BoardService;
import io.agora.education.service.bean.ResponseBody;
import io.agora.education.widget.ConfirmDialog;

import static io.agora.education.BuildConfig.API_BASE_URL;
import static io.agora.education.MainActivity.CODE;
import static io.agora.education.MainActivity.REASON;
import static io.agora.education.classroom.bean.board.BoardBean.BOARD;

public abstract class BaseClassActivity extends BaseActivity implements EduRoomEventListener, EduUserEventListener {
    private static final String TAG = BaseClassActivity.class.getSimpleName();

    public static final String ROOMENTRY = "roomEntry";
    public static final int RESULT_CODE = 808;

    @BindView(R.id.title_view)
    protected TitleView title_view;
    @BindView(R.id.layout_whiteboard)
    protected FrameLayout layout_whiteboard;
    @BindView(R.id.layout_share_video)
    protected FrameLayout layout_share_video;

    protected SurfaceView surface_share_video;
    protected WhiteBoardFragment whiteboardFragment = new WhiteBoardFragment();
    protected ChatRoomFragment chatRoomFragment = new ChatRoomFragment();

    private RoomEntry roomEntry;
    private boolean isJoining;
    private EduRoom eduRoom;
    private EduStreamInfo localCameraStream, localScreenStream;
    protected BoardBean boardBean;

    @Override
    protected void onCreate(@androidx.annotation.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate");
    }

    @Override
    protected void initData() {
        roomEntry = getIntent().getParcelableExtra(ROOMENTRY);
        createRoom(roomEntry.getRoomName(), roomEntry.getUserUuid(), roomEntry.getRoomName(),
                roomEntry.getRoomUuid(), roomEntry.getRoomType());
    }

    @Override
    protected void initView() {
    }

    protected void onJoinRoomSuccess() {
        title_view.setTitle(getRoomName());
        getSupportFragmentManager().beginTransaction()
                .remove(whiteboardFragment)
                .remove(chatRoomFragment)
                .commitNow();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.layout_whiteboard, whiteboardFragment)
                .add(R.id.layout_chat_room, chatRoomFragment)
                .show(whiteboardFragment)
                .show(chatRoomFragment)
                .commitNow();
    }

    private void createRoom(String yourNameStr, String yourUuid, String roomNameStr, String roomUuid, int roomType) {
        if (isJoining) {
            return;
        }
        isJoining = true;
        /**createClassroom时，room不存在则新建，存在则返回room信息(此接口非必须调用)，
         * 只要保证在调用joinClassroom之前，classroom在服务端存在即可*/
        RoomCreateOptions options = new RoomCreateOptions(roomUuid, roomNameStr, roomType, true);
        EduApplication.getEduManager().createClassroom(options, new EduCallback<EduRoom>() {
            @Override
            public void onSuccess(@Nullable EduRoom res) {
                eduRoom = res;
                eduRoom.setEventListener(BaseClassActivity.this);
                joinRoom(eduRoom, yourNameStr, yourUuid);
            }

            @Override
            public void onFailure(int code, @Nullable String reason) {
                isJoining = false;
                joinFailed(code, reason);
            }
        });
    }

    private void joinRoom(EduRoom eduRoom, String yourNameStr, String yourUuid) {
        RoomJoinOptions options = new RoomJoinOptions(yourUuid, yourNameStr, new RoomMediaOptions());
        eduRoom.joinClassroomAsStudent(options, new EduCallback<EduStudent>() {
            @Override
            public void onSuccess(@Nullable EduStudent res) {
                isJoining = false;
                eduRoom.getLocalUser().setEventListener(BaseClassActivity.this);
                onJoinRoomSuccess();
            }

            @Override
            public void onFailure(int code, @Nullable String reason) {
                isJoining = false;
                joinFailed(code, reason);
            }
        });
    }

    /**
     * 加入失败，回传数据并结束当前页面
     */
    private void joinFailed(int code, String reason) {
        Intent intent = getIntent().putExtra(CODE, code).putExtra(REASON, reason);
        setResult(RESULT_CODE, intent);
        finish();
    }


    /**
     * 禁止本地音频
     */
    public final void muteLocalAudio(boolean isMute) {
        switchLocalVideoAudio(localCameraStream.getHasVideo(), !isMute);
//        classContext.muteLocalAudio(isMute);
    }

    public final void muteLocalVideo(boolean isMute) {
        switchLocalVideoAudio(!isMute, localCameraStream.getHasAudio());
//        classContext.muteLocalVideo(isMute);
    }

    private void switchLocalVideoAudio(boolean openVideo, boolean openAudio) {
        /**先更新本地流信息和rte状态*/
        eduRoom.localUser.initOrUpdateLocalStream(new LocalStreamInitOptions(localCameraStream.getStreamUuid(),
                openVideo, openAudio), new EduCallback<EduStreamInfo>() {
            @Override
            public void onSuccess(@Nullable EduStreamInfo res) {
                /**把更新后的流信息同步至服务器*/
                eduRoom.localUser.publishStream(res, new EduCallback<Boolean>() {
                    @Override
                    public void onSuccess(@Nullable Boolean res) {
                    }

                    @Override
                    public void onFailure(int code, @Nullable String reason) {
                    }
                });
            }

            @Override
            public void onFailure(int code, @Nullable String reason) {
            }
        });
    }

    public final EduUser getLocalUser() {
        return eduRoom.localUser;
    }

    public EduStreamInfo getLocalCameraStream() {
        return localCameraStream;
    }

    public final void sendRoomChatMsg(String msg, EduCallback<EduChatMsg> callback) {
        eduRoom.getLocalUser().sendRoomChatMessage(msg, callback);
    }

    protected List<EduStreamInfo> getCurFullStream() {
        return eduRoom.getFullStreamList();
    }

    protected List<EduUserInfo> getCurFullUser() {
        return eduRoom.getFullUserList();
    }

    protected EduStreamInfo getTeacherStream() {
        for (EduStreamInfo streamInfo : getCurFullStream()) {
            if (streamInfo.getPublisher().getRole().equals(EduUserRole.TEACHER)) {
                return streamInfo;
            }
        }
        return null;
    }

    @Room.Type
    protected abstract int getClassType();

    @Override
    protected void onDestroy() {
        /**退出activity之前释放eduRoom资源*/
        EduApplication.getEduManager().releaseRoom(eduRoom.getRoomInfo().getRoomUuid());
        whiteboardFragment.releaseBoard();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        showLeaveDialog();
    }

    public final void showLeaveDialog() {
        ConfirmDialog.normal(getString(R.string.confirm_leave_room_content), confirm -> {
            if (confirm) {
                /**退出activity之前离开eduRoom*/
                if (eduRoom != null) {
                    eduRoom.leave();
                    BaseClassActivity.this.finish();
                }
            }
        }).show(getSupportFragmentManager(), null);
    }

    private EduRoomInfo getRoomFromIntent() {
        return eduRoom.getRoomInfo();
    }

    public final String getRoomUuid() {
        return getRoomFromIntent().getRoomUuid();
    }

    public final String getRoomName() {
        return getRoomFromIntent().getRoomName();
    }

    /**
     * 为流(主要是视频流)设置一个渲染区域
     */
    public final void renderStream(EduStreamInfo eduStreamInfo, ViewGroup viewGroup) {
        runOnUiThread(() -> eduRoom.getLocalUser().setStreamView(eduStreamInfo, viewGroup));
    }

    protected String getProperty(Map<String, Object> properties, String key) {
        if (properties != null) {
            for (Map.Entry<String, Object> property : properties.entrySet()) {
                if (property.getKey().equals(key)) {
                    return String.valueOf(property.getValue());
                }
            }
        }
        return null;
    }

    /**
     * @Override public void onTeacherInit(User teacher) {
     * if (teacher == null) {
     * ToastManager.showShort(R.string.there_is_no_teacher_in_this_classroom);
     * }
     * }
     * @Override public void onWhiteboardChanged(String uuid, String roomToken) {
     * whiteboardFragment.initBoardWithRoomToken(uuid, roomToken);
     * }
     * @Override public void onLockWhiteboard(boolean locked) {
     * whiteboardFragment.disableCameraTransform(locked);
     * }
     */


//
    @Override
    public void onRemoteUsersInitialized(@NotNull List<? extends EduUserInfo> users, @NotNull EduRoom fromClassRoom) {
        Map<String, Object> roomProperties = fromClassRoom.getRoomProperties();
        /**判断roomProperties中是否有白板属性信息，如果没有，发起请求,等待RTM通知*/
        String boardJson = getProperty(roomProperties, BOARD);
        if (TextUtils.isEmpty(boardJson)) {
            RetrofitManager.instance().getService(API_BASE_URL, BoardService.class)
                    .getBoardInfo(getString(R.string.agora_app_id), fromClassRoom.getRoomInfo().getRoomUuid())
                    .enqueue(new RetrofitManager.Callback(0, new ThrowableCallback<ResponseBody<BoardBean>>() {
                        @Override
                        public void onFailure(@androidx.annotation.Nullable Throwable throwable) {
                        }

                        @Override
                        public void onSuccess(@androidx.annotation.Nullable ResponseBody<BoardBean> res) {
                        }
                    }));
        } else {
            boardBean = new Gson().fromJson(boardJson, BoardBean.class);
            whiteboardFragment.initBoardWithRoomToken(boardBean.getInfo().getBoardId(),
                    boardBean.getInfo().getBoardToken());
            if (boardBean.getState().getFollow() == BoardFollowMode.FOLLOW) {
                whiteboardFragment.disableCameraTransform(true);
            }
        }
    }

    @Override
    public void onRemoteUsersJoined(@NotNull List<? extends EduUserInfo> users, @NotNull EduRoom fromClassRoom) {

    }

    @Override
    public void onRemoteUsersLeft(@NotNull List<EduUserEvent> userEvents, @NotNull EduRoom fromClassRoom) {

    }

    @Override
    public void onRemoteUserUpdated(@NotNull List<EduUserEvent> userEvents, @NotNull EduRoom fromClassRoom) {

    }

    @Override
    public void onRoomMessageReceived(@NotNull EduMsg message, @NotNull EduRoom fromClassRoom) {

    }

    @Override
    public void onUserMessageReceived(@NotNull EduMsg message, @NotNull EduRoom fromClassRoom) {

    }

    @Override
    public void onRoomChatMessageReceived(@NotNull EduChatMsg chatMsg, @NotNull EduRoom fromClassRoom) {

    }

    @Override
    public void onUserChatMessageReceived(@NotNull EduChatMsg chatMsg, @NotNull EduRoom fromClassRoom) {

    }

    @Override
    public void onRemoteStreamsInitialized(@NotNull List<? extends EduStreamInfo> streams, @NotNull EduRoom fromClassRoom) {
    }

    /**
     * 尝试获取本地流数据
     */
//    private void attemptUpdateLocalStream(@NonNull List list)
//    {
//        if(list.size() == 0) {
//            return;
//        }
//        if(list.get(0) instanceof EduStreamInfo){
//            for (EduStreamInfo streamInfo : ((List<EduStreamInfo>) list)) {
//                if (streamInfo.getPublisher().equals(eduRoom.localUser)) {
//                    localStream = streamInfo;
//                }
//            }
//        }
//        else if(list.get(0) instanceof EduStreamEvent) {
//            for (EduStreamEvent streamEvent : ((List<EduStreamEvent>) list)) {
//                if(streamEvent.getModifiedStream().getPublisher().equals(eduRoom.localUser)) {
//                    localStream = streamEvent.getModifiedStream();
//                }
//            }
//        }
//    }
    @Override
    public void onRemoteStreamsAdded(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom fromClassRoom) {
    }

    @Override
    public void onRemoteStreamsUpdated(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom fromClassRoom) {
    }

    @Override
    public void onRemoteStreamsRemoved(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom fromClassRoom) {
    }

    @Override
    public void onRoomStatusChanged(@NotNull RoomStatusEvent event, @NotNull EduUserInfo operatorUser, @NotNull EduRoom fromClassRoom) {
    }

    @Override
    public void onRoomPropertyChanged(@NotNull EduRoom fromClassRoom) {
        Log.e(TAG, "收到roomProperty改变的数据");
        Map<String, Object> roomProperties = fromClassRoom.getRoomProperties();
        String boardJson = getProperty(roomProperties, BOARD);
        if (boardBean == null) {
            Log.e(TAG, "首次获取到白板信息");
            /**首次获取到白板信息*/
            boardBean = new Gson().fromJson(boardJson, BoardBean.class);
            whiteboardFragment.initBoardWithRoomToken(boardBean.getInfo().getBoardId(),
                    boardBean.getInfo().getBoardToken());
            if (boardBean.getState().getFollow() == BoardFollowMode.FOLLOW) {
                whiteboardFragment.disableCameraTransform(true);
            }
        } else {
            Log.e(TAG, "更新白板信息");
            /**更新白板信息*/
            boardBean = new Gson().fromJson(boardJson, BoardBean.class);
            whiteboardFragment.disableCameraTransform(boardBean.getState().getFollow() ==
                    BoardFollowMode.FOLLOW);
        }
    }

    @Override
    public void onRemoteUserPropertiesUpdated(@NotNull List<EduUserInfo> userInfos, @NotNull EduRoom fromClassRoom) {
    }

    @Override
    public void onConnectionStateChanged(@NotNull ConnectionState state, @NotNull ConnectionStateChangeReason reason, @NotNull EduRoom fromClassRoom) {

    }

    @Override
    public void onNetworkQualityChanged(@NotNull io.agora.education.api.statistics.NetworkQuality quality, @NotNull EduUserInfo user, @NotNull EduRoom fromClassRoom) {

    }


    @Override
    public void onLocalUserUpdated(@NotNull EduUserEvent userEvent) {

    }

    @Override
    public void onLocalUserPropertyUpdated(@NotNull EduUserInfo userInfo) {

    }

    protected String aaa = "-1";

    @Override
    public void onLocalStreamAdded(@NotNull EduStreamEvent streamEvent) {
        Log.e(TAG, "收到添加本地流的回调");
        switch (streamEvent.getModifiedStream().getVideoSourceType()) {
            case CAMERA:
                aaa = "100";
                localCameraStream = streamEvent.getModifiedStream();
                Log.e(TAG, "收到添加本地Camera流的回调");
                break;
            case SCREEN:
                localScreenStream = streamEvent.getModifiedStream();
                break;
            default:
                break;
        }
    }

    @Override
    public void onLocalStreamUpdated(@NotNull EduStreamEvent streamEvent) {
        Log.e(TAG, "收到更新本地流的回到");
        switch (streamEvent.getModifiedStream().getVideoSourceType()) {
            case CAMERA:
                localCameraStream = streamEvent.getModifiedStream();
                break;
            case SCREEN:
                localScreenStream = streamEvent.getModifiedStream();
                break;
            default:
                break;
        }
    }

    @Override
    public void onLocalStreamRemoved(@NotNull EduStreamEvent streamEvent) {
        Log.e(TAG, "收到移除本地流的回到");
        switch (streamEvent.getModifiedStream().getVideoSourceType()) {
            case CAMERA:
                localCameraStream = null;
                break;
            case SCREEN:
                localScreenStream = null;
                break;
            default:
                break;
        }
    }
}
