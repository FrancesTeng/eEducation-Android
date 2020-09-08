package io.agora.education.classroom;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.gson.Gson;
import com.herewhite.sdk.domain.GlobalState;

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
import io.agora.education.api.manager.listener.EduManagerEventListener;
import io.agora.education.api.message.EduChatMsg;
import io.agora.education.api.message.EduChatMsgType;
import io.agora.education.api.message.EduMsg;
import io.agora.education.api.room.EduRoom;
import io.agora.education.api.room.data.EduRoomInfo;
import io.agora.education.api.room.data.EduRoomState;
import io.agora.education.api.room.data.EduRoomStatus;
import io.agora.education.api.room.data.RoomCreateOptions;
import io.agora.education.api.room.data.RoomJoinOptions;
import io.agora.education.api.room.data.RoomMediaOptions;
import io.agora.education.api.room.data.RoomStatusEvent;
import io.agora.education.api.room.data.RoomType;
import io.agora.education.api.room.listener.EduRoomEventListener;
import io.agora.education.api.statistics.ConnectionState;
import io.agora.education.api.statistics.ConnectionStateChangeReason;
import io.agora.education.api.stream.data.EduStreamEvent;
import io.agora.education.api.stream.data.EduStreamInfo;
import io.agora.education.api.stream.data.LocalStreamInitOptions;
import io.agora.education.api.stream.data.VideoSourceType;
import io.agora.education.api.user.EduStudent;
import io.agora.education.api.user.EduUser;
import io.agora.education.api.user.data.EduLocalUserInfo;
import io.agora.education.api.user.data.EduUserEvent;
import io.agora.education.api.user.data.EduUserInfo;
import io.agora.education.api.user.data.EduUserRole;
import io.agora.education.api.user.listener.EduUserEventListener;
import io.agora.education.base.BaseActivity;
import io.agora.education.classroom.bean.board.BoardBean;
import io.agora.education.classroom.bean.board.BoardFollowMode;
import io.agora.education.classroom.bean.board.BoardInfo;
import io.agora.education.classroom.bean.board.BoardState;
import io.agora.education.classroom.bean.channel.Room;
import io.agora.education.classroom.bean.msg.ChannelMsg;
import io.agora.education.classroom.bean.record.RecordBean;
import io.agora.education.classroom.bean.record.RecordMsg;
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
import static io.agora.education.classroom.bean.record.RecordBean.RECORD;
import static io.agora.education.classroom.bean.record.RecordState.END;

public abstract class BaseClassActivity extends BaseActivity implements EduRoomEventListener, EduUserEventListener,
        EduManagerEventListener, WhiteBoardFragment.GlobalStateChangeListener {
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

    protected RoomEntry roomEntry;
    private boolean isJoining = false, joinSuccess = false;
    private EduRoom mainEduRoom;
    private EduStreamInfo localCameraStream, localScreenStream;
    protected BoardBean mainBoardBean;
    private RecordBean mainRecordBean;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate");
    }

    @Override
    protected void initData() {
        EduApplication.getManager().setEduManagerEventListener(this);
        roomEntry = getIntent().getParcelableExtra(ROOMENTRY);
        RoomCreateOptions createOptions = new RoomCreateOptions(roomEntry.getRoomUuid(),
                roomEntry.getRoomName(), roomEntry.getRoomType(), true);
        mainEduRoom = buildEduRoom(createOptions, null);
    }

    @Override
    protected void initView() {
    }

    protected void showFragmentWithJoinSuccess() {
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

    /**
     * @param options        创建room对象需要的参数
     * @param parentRoomUuid 父房间的uuid
     */
    protected EduRoom buildEduRoom(RoomCreateOptions options, String parentRoomUuid) {
        int roomType = options.getRoomType();
        if (options.getRoomType() == RoomType.BREAKOUT_CLASS.getValue()) {
            roomType = TextUtils.isEmpty(parentRoomUuid) ? RoomType.LARGE_CLASS.getValue() :
                    RoomType.SMALL_CLASS.getValue();
        }
        EduRoomInfo eduRoomInfo = EduRoomInfo.Companion.create(roomType, options.getRoomUuid(),
                options.getRoomName());
        EduRoomStatus status = new EduRoomStatus(EduRoomState.INIT, 0, true, 0);
        EduRoom room = EduRoom.Companion.create(eduRoomInfo, status);
        room.setEventListener(BaseClassActivity.this);
        return room;
    }

    protected void joinRoom(EduRoom eduRoom, String yourNameStr, String yourUuid, boolean autoSubscribe,
                            boolean autoPublish, boolean needUserListener, EduCallback<EduStudent> callback) {
        if (isJoining) {
            return;
        }
        isJoining = true;
        RoomJoinOptions options = new RoomJoinOptions(yourUuid, yourNameStr, EduUserRole.STUDENT,
                new RoomMediaOptions(autoSubscribe, autoPublish));
        eduRoom.joinClassroom(options, new EduCallback<EduStudent>() {
            @Override
            public void onSuccess(@Nullable EduStudent res) {
                joinSuccess = true;
                isJoining = false;
                if (needUserListener) {
                    eduRoom.getLocalUser().setEventListener(BaseClassActivity.this);
                }
                callback.onSuccess(res);
            }

            @Override
            public void onFailure(int code, @Nullable String reason) {
                isJoining = false;
                callback.onFailure(code, reason);
            }
        });
    }

    /**
     * 加入失败，回传数据并结束当前页面
     */
    protected void joinFailed(int code, String reason) {
        Intent intent = getIntent().putExtra(CODE, code).putExtra(REASON, reason);
        setResult(RESULT_CODE, intent);
        finish();
    }

    protected void recoveryFragmentWithConfigChanged() {
        if (joinSuccess) {
            showFragmentWithJoinSuccess();
        }
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
        mainEduRoom.localUser.initOrUpdateLocalStream(new LocalStreamInitOptions(localCameraStream.getStreamUuid(),
                openVideo, openAudio), new EduCallback<EduStreamInfo>() {
            @Override
            public void onSuccess(@Nullable EduStreamInfo res) {
                /**把更新后的流信息同步至服务器*/
                mainEduRoom.localUser.publishStream(res, new EduCallback<Boolean>() {
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

    public EduRoom getMainEduRoom() {
        return mainEduRoom;
    }

    public final EduUser getLocalUser() {
        return mainEduRoom.localUser;
    }

    public final EduUserInfo getLocalUserInfo() {
        return mainEduRoom.localUser.getUserInfo();
    }

    public EduStreamInfo getLocalCameraStream() {
        return localCameraStream;
    }

    public void setLocalCameraStream(EduStreamInfo streamInfo) {
        this.localCameraStream = streamInfo;
    }

    public final void sendRoomChatMsg(String msg, EduCallback<EduChatMsg> callback) {
        mainEduRoom.getLocalUser().sendRoomChatMessage(msg, callback);
    }

    protected List<EduStreamInfo> getCurFullStream() {
        return (mainEduRoom != null) ? mainEduRoom.getFullStreamList() : null;
    }

    protected List<EduUserInfo> getCurFullUser() {
        return (mainEduRoom != null) ? mainEduRoom.getFullUserList() : null;
    }

    protected EduStreamInfo getTeacherStream() {
        for (EduStreamInfo streamInfo : getCurFullStream()) {
            if (streamInfo.getPublisher().getRole().equals(EduUserRole.TEACHER)) {
                return streamInfo;
            }
        }
        return null;
    }

    protected EduUserInfo getTeacher() {
        List<EduUserInfo> users = getCurFullUser();
        if (users != null) {
            for (EduUserInfo userInfo : users) {
                if (userInfo.getRole().equals(EduUserRole.TEACHER)) {
                    return userInfo;
                }
            }
        }
        return null;
    }

    @Room.Type
    protected abstract int getClassType();

    @Override
    protected void onDestroy() {
        /**退出activity之前释放eduRoom资源*/
        mainEduRoom = null;
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
                if (mainEduRoom != null) {
                    mainEduRoom.leave();
                    BaseClassActivity.this.finish();
                }
            }
        }).show(getSupportFragmentManager(), null);
    }

    private EduRoomInfo getRoomInfo() {
        return mainEduRoom.getRoomInfo();
    }

    public final String getRoomUuid() {
        return getRoomInfo().getRoomUuid();
    }

    public final String getRoomName() {
        return getRoomInfo().getRoomName();
    }

    /**
     * 为流(主要是视频流)设置一个渲染区域
     */
    public final void renderStream(EduStreamInfo eduStreamInfo, @Nullable ViewGroup viewGroup) {
        runOnUiThread(() -> mainEduRoom.getLocalUser().setStreamView(eduStreamInfo, getRoomUuid(), viewGroup));
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
     * 当前白板是否开启跟随模式
     */
    private boolean whiteBoardIsFollowMode(BoardState state) {
        if (state == null) {
            return false;
        }
        return state.getFollow() == BoardFollowMode.FOLLOW;
    }

    /**
     * 当前本地用户是否得到白板授权
     */
    private boolean whiteBoardIsGranted(BoardState state) {
        if (state != null) {
            if (state.getGrantUsers() != null) {
                for (String uuid : state.getGrantUsers()) {
                    if (uuid.equals(getLocalUserInfo().getUserUuid())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void onRemoteUsersInitialized(@NotNull List<? extends EduUserInfo> users, @NotNull EduRoom classRoom) {
        Map<String, Object> roomProperties = classRoom.getRoomProperties();
        /**判断roomProperties中是否有白板属性信息，如果没有，发起请求,等待RTM通知*/
        String boardJson = getProperty(roomProperties, BOARD);
        if (TextUtils.isEmpty(boardJson)) {
            RetrofitManager.instance().getService(API_BASE_URL, BoardService.class)
                    .getBoardInfo(((EduLocalUserInfo) mainEduRoom.localUser.getUserInfo()).getUserToken(),
                            getString(R.string.agora_app_id), classRoom.getRoomInfo().getRoomUuid())
                    .enqueue(new RetrofitManager.Callback(0, new ThrowableCallback<ResponseBody<BoardBean>>() {
                        @Override
                        public void onFailure(@androidx.annotation.Nullable Throwable throwable) {
                        }

                        @Override
                        public void onSuccess(@androidx.annotation.Nullable ResponseBody<BoardBean> res) {
                        }
                    }));
        } else {
            mainBoardBean = new Gson().fromJson(boardJson, BoardBean.class);
            BoardInfo info = mainBoardBean.getInfo();
            BoardState state = mainBoardBean.getState();
            Log.e(TAG, "白板信息已存在->" + boardJson);
            runOnUiThread(() -> {
                whiteboardFragment.initBoardWithRoomToken(info.getBoardId(),
                        info.getBoardToken(), getLocalUserInfo().getUserUuid());
                boolean follow = whiteBoardIsFollowMode(state);
                whiteboardFragment.disableCameraTransform(follow);
                boolean granted = whiteBoardIsGranted((state));
                whiteboardFragment.disableDeviceInputs(!granted);
                if (follow) {
                    layout_whiteboard.setVisibility(View.VISIBLE);
                    layout_share_video.setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    public void onRemoteUsersJoined(@NotNull List<? extends EduUserInfo> users, @NotNull EduRoom classRoom) {
        Log.e(TAG, "收到远端用户加入的回调");
    }

    @Override
    public void onRemoteUsersLeft(@NotNull List<EduUserEvent> userEvents, @NotNull EduRoom classRoom) {
        Log.e(TAG, "收到远端用户离开的回调");
    }

    @Override
    public void onRemoteUserUpdated(@NotNull List<EduUserEvent> userEvents, @NotNull EduRoom classRoom) {
        Log.e(TAG, "收到远端用户修改的回调");
    }

    @Override
    public void onRoomMessageReceived(@NotNull EduMsg message, @NotNull EduRoom classRoom) {

    }

    @Override
    public void onRoomChatMessageReceived(@NotNull EduChatMsg eduChatMsg, @NotNull EduRoom classRoom) {
        /**收到群聊消息，进行处理并展示*/
        ChannelMsg.ChatMsg chatMsg = new ChannelMsg.ChatMsg(eduChatMsg.getFromUser(), eduChatMsg.getMessage(),
                eduChatMsg.getTimeStamp(), eduChatMsg.getType());
        chatMsg.isMe = chatMsg.getFromUser().equals(classRoom.localUser.getUserInfo());
        chatRoomFragment.addMessage(chatMsg);
        Log.e(TAG, "成功添加一条聊天消息");
    }

    @Override
    public void onRemoteStreamsInitialized(@NotNull List<? extends EduStreamInfo> streams, @NotNull EduRoom classRoom) {
    }

    @Override
    public void onRemoteStreamsAdded(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom classRoom) {
        Log.e(TAG, "收到添加远端流的回调");
        for (EduStreamEvent streamEvent : streamEvents) {
            EduStreamInfo streamInfo = streamEvent.getModifiedStream();
            if (streamInfo.getPublisher().getRole() == EduUserRole.TEACHER
                    && streamInfo.getVideoSourceType().equals(VideoSourceType.SCREEN)) {
                /**老师打开了屏幕分享，此时把这个流渲染出来*/
                runOnUiThread(() -> {
                    layout_whiteboard.setVisibility(View.GONE);
                    layout_share_video.setVisibility(View.VISIBLE);
                    layout_share_video.removeAllViews();
                    renderStream(streamInfo, layout_share_video);
                });
                break;
            }
        }
    }

    @Override
    public void onRemoteStreamsUpdated(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom classRoom) {
        Log.e(TAG, "收到修改远端流的回调");
        for (EduStreamEvent streamEvent : streamEvents) {
            EduStreamInfo streamInfo = streamEvent.getModifiedStream();
            if (streamInfo.getPublisher().getRole() == EduUserRole.TEACHER
                    && streamInfo.getVideoSourceType().equals(VideoSourceType.SCREEN)) {
                runOnUiThread(() -> {
                    layout_whiteboard.setVisibility(View.GONE);
                    layout_share_video.setVisibility(View.VISIBLE);
                    layout_share_video.removeAllViews();
                    renderStream(streamInfo, layout_share_video);
                });
                break;
            }
        }
    }

    @Override
    public void onRemoteStreamsRemoved(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom classRoom) {
        Log.e(TAG, "收到移除远端流的回调");
        for (EduStreamEvent streamEvent : streamEvents) {
            EduStreamInfo streamInfo = streamEvent.getModifiedStream();
            if (streamInfo.getPublisher().getRole() == EduUserRole.TEACHER
                    && streamInfo.getVideoSourceType().equals(VideoSourceType.SCREEN)) {
                /**老师关闭了屏幕分享，移除屏幕分享的布局*/
                runOnUiThread(() -> {
                    layout_whiteboard.setVisibility(View.VISIBLE);
                    layout_share_video.setVisibility(View.GONE);
                    layout_share_video.removeAllViews();
                    renderStream(streamInfo, null);
                });
                break;
            }
        }
    }

    @Override
    public void onRoomStatusChanged(@NotNull RoomStatusEvent event, @NotNull EduUserInfo operatorUser, @NotNull EduRoom classRoom) {
        EduRoomStatus roomStatus = classRoom.getRoomStatus();
        switch (event) {
            case COURSE_STATE:
                title_view.setTimeState(roomStatus.getCourseState() == EduRoomState.START,
                        System.currentTimeMillis() - roomStatus.getStartTime());
                break;
            case STUDENT_CHAT:
                chatRoomFragment.setMuteAll(!roomStatus.isStudentChatAllowed());
                break;
            default:
                break;
        }
    }

    @Override
    public void onRoomPropertyChanged(@NotNull EduRoom classRoom) {
        Log.e(TAG, "收到roomProperty改变的数据");
        Map<String, Object> roomProperties = classRoom.getRoomProperties();
        String boardJson = getProperty(roomProperties, BOARD);
        if (mainBoardBean == null) {
            Log.e(TAG, "首次获取到白板信息->" + boardJson);
            /**首次获取到白板信息*/
            mainBoardBean = new Gson().fromJson(boardJson, BoardBean.class);
            runOnUiThread(() -> {
                whiteboardFragment.initBoardWithRoomToken(mainBoardBean.getInfo().getBoardId(),
                        mainBoardBean.getInfo().getBoardToken(), getLocalUserInfo().getUserUuid());
                boolean follow = whiteBoardIsFollowMode(mainBoardBean.getState());
                whiteboardFragment.disableCameraTransform(follow);
                boolean granted = whiteBoardIsGranted((mainBoardBean.getState()));
                whiteboardFragment.disableDeviceInputs(!granted);
                if (follow) {
                    layout_whiteboard.setVisibility(View.VISIBLE);
                    layout_share_video.setVisibility(View.GONE);
                }
            });
        }
        String recordJson = getProperty(roomProperties, RECORD);
        if (!TextUtils.isEmpty(recordJson)) {
            RecordBean tmp = RecordBean.fromJson(recordJson, RecordBean.class);
            if (mainRecordBean == null || tmp.getState() != mainRecordBean.getState()) {
                mainRecordBean = tmp;
                if (mainRecordBean.getState() == END) {
                    RecordMsg recordMsg = new RecordMsg(getRoomUuid(), getLocalUserInfo(), getString(R.string.replay_link),
                            System.currentTimeMillis(), EduChatMsgType.Text.getValue());
                    recordMsg.isMe = true;
                    chatRoomFragment.addMessage(recordMsg);
                }
            }
        }
    }

    @Override
    public void onRemoteUserPropertiesUpdated(@NotNull List<EduUserInfo> userInfos, @NotNull EduRoom classRoom) {
    }

    @Override
    public void onNetworkQualityChanged(@NotNull io.agora.education.api.statistics.NetworkQuality quality, @NotNull EduUserInfo user, @NotNull EduRoom classRoom) {

    }


    @Override
    public void onLocalUserUpdated(@NotNull EduUserEvent userEvent) {
        /**更新用户信息*/
        EduUserInfo userInfo = userEvent.getModifiedUser();
        chatRoomFragment.setMuteLocal(!userInfo.isChatAllowed());
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

    /**
     * eduManager的回调
     */
    @Override
    public void onUserMessageReceived(@NotNull EduMsg message) {

    }

    @Override
    public void onUserChatMessageReceived(@NotNull EduChatMsg chatMsg) {

    }

    @Override
    public void onConnectionStateChanged(@NotNull ConnectionState state, @NotNull ConnectionStateChangeReason reason) {

    }

    /**
     * 白板的全局回调
     */
    @Override
    public void onGlobalStateChanged(GlobalState state) {
        if (getClassType() == RoomType.ONE_ON_ONE.getValue()) {
            /**一对一不受此控制*/
            return;
        }
        BoardState boardState = (BoardState) state;
        boolean follow = whiteBoardIsFollowMode(boardState);
        whiteboardFragment.disableCameraTransform(follow);
        boolean granted = whiteBoardIsGranted(boardState);
        whiteboardFragment.disableDeviceInputs(!granted);
    }
}
