package io.agora.education.classroom;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import butterknife.BindView;
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
import io.agora.education.api.user.data.EduUserEvent;
import io.agora.education.api.user.data.EduUserInfo;
import io.agora.education.api.user.listener.EduUserEventListener;
import io.agora.education.base.BaseActivity;
import io.agora.education.classroom.bean.channel.Room;
import io.agora.education.classroom.fragment.ChatRoomFragment;
import io.agora.education.classroom.fragment.WhiteBoardFragment;
import io.agora.education.classroom.widget.TitleView;
import io.agora.education.widget.ConfirmDialog;
import kotlin.Unit;

import static io.agora.education.MainActivity.CODE;
import static io.agora.education.MainActivity.REASON;

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
    protected EduStreamInfo localStream;

    @Override
    protected void onCreate(@androidx.annotation.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

    protected final void init() {
        title_view.setTitle(getRoomName());

        getSupportFragmentManager().beginTransaction()
                .remove(whiteboardFragment)
                .remove(chatRoomFragment)
                .commitNow();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.layout_whiteboard, whiteboardFragment)
                .add(R.id.layout_chat_room, chatRoomFragment)
                .show(chatRoomFragment)
                .commit();
    }

    private void createRoom(String yourNameStr, String yourUuid, String roomNameStr, String roomUuid, int roomType) {
        if (isJoining) {
            return;
        }
        isJoining = true;
        /**createClassroom时，room不存在则新建，存在则返回room信息(此接口非必须调用)，
         * 只要保证在调用joinClassroom之前，classroom在服务端存在即可*/
        RoomCreateOptions options = new RoomCreateOptions(roomUuid, roomNameStr, roomType);
        EduApplication.getEduManager().createClassroom(options, new EduCallback<EduRoom>() {
            @Override
            public void onSuccess(@Nullable EduRoom res) {
                eduRoom = res;
                eduRoom.setEventListener(BaseClassActivity.this);
                joinRoom(res, yourNameStr, yourUuid);
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
                init();
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
        switchLocalVideoAudio(localStream.getHasVideo(), !isMute);
//        classContext.muteLocalAudio(isMute);
    }

    public final void muteLocalVideo(boolean isMute) {
        switchLocalVideoAudio(!isMute, localStream.getHasAudio());
//        classContext.muteLocalVideo(isMute);
    }

    private void switchLocalVideoAudio(boolean openVideo, boolean openAudio) {
        /**先更新本地流信息和rte状态*/
        eduRoom.localUser.initOrUpdateLocalStream(new LocalStreamInitOptions(localStream.getStreamUuid(),
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

    public final EduUserInfo getLocal() {
        return eduRoom.localUser.getUserInfo();
    }

    public final void sendRoomChatMsg(String msg, EduCallback<EduChatMsg> callback) {
        eduRoom.getLocalUser().sendRoomChatMessage(msg, callback);
    }

    @Room.Type
    protected abstract int getClassType();

    @Override
    protected void onDestroy() {
        eduRoom.leave(new EduCallback<Unit>() {
            @Override
            public void onSuccess(@Nullable Unit res) {
                Log.e(TAG, "成功退出房间");
            }

            @Override
            public void onFailure(int code, @Nullable String reason) {
                Log.e(TAG, "退出房间失败：" + reason);
            }
        });
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
                BaseClassActivity.this.finish();
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
     * 为流(主要是食品流)设置一个渲染区域
     */
    public final void renderStream(EduStreamInfo eduStreamInfo, ViewGroup viewGroup) {
        runOnUiThread(() -> eduRoom.getLocalUser().setStreamView(eduStreamInfo, viewGroup));
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

//    @Override
//    public void onNetworkQualityChanged(@NetworkQuality int quality) {
//        title_view.setNetworkQuality(quality);
//    }

//    @Override
//    public void onClassStateChanged(boolean isBegin, long time) {
//        title_view.setTimeState(isBegin, time);
//    }

//    @Override
//    public void onMuteLocalChat(boolean muted) {
//        chatRoomFragment.setMuteLocal(muted);
//    }

//    @Override
//    public void onMuteAllChat(boolean muted) {
//        chatRoomFragment.setMuteAll(muted);
//    }

//    @Override
//    public void onChatMsgReceived(ChannelMsg.ChatMsg msg) {
//        chatRoomFragment.addMessage(msg);
//    }

//    @Override
//    public void onScreenShareJoined(int uid) {
//        if (surface_share_video == null) {
//            surface_share_video = RtcManager.instance().createRendererView(this);
//        }
//        layout_whiteboard.setVisibility(View.GONE);
//        layout_share_video.setVisibility(View.VISIBLE);
//
//        removeFromParent(surface_share_video);
//        surface_share_video.setTag(uid);
//        layout_share_video.addView(surface_share_video, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//        RtcManager.instance().setupRemoteVideo(surface_share_video, VideoCanvas.RENDER_MODE_FIT, uid);
//    }


//    @Override
//    public void onScreenShareOffline(int uid) {
//        Object tag = surface_share_video.getTag();
//        if (tag instanceof Integer) {
//            if ((int) tag == uid) {
//                layout_whiteboard.setVisibility(View.VISIBLE);
//                layout_share_video.setVisibility(View.GONE);
//
//                removeFromParent(surface_share_video);
//                surface_share_video = null;
//            }
//        }
//    }
    @Override
    public void onRemoteUsersInitialized(@NotNull List<? extends EduUserInfo> users, @NotNull EduRoom fromClassRoom) {

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

    /**尝试获取并本地流数据*/
    private void attemptUpdateLocalStream(@NonNull List list)
    {
        if(list.size() == 0) {
            return;
        }
        if(list.get(0) instanceof EduStreamInfo){
            for (EduStreamInfo streamInfo : ((List<EduStreamInfo>) list)) {
                if (streamInfo.getPublisher().equals(eduRoom.localUser)) {
                    localStream = streamInfo;
                }
            }
        }
        else if(list.get(0) instanceof EduStreamEvent) {
            for (EduStreamEvent streamEvent : ((List<EduStreamEvent>) list)) {
                if(streamEvent.getModifiedStream().getPublisher().equals(eduRoom.localUser)) {
                    localStream = streamEvent.getModifiedStream();
                }
            }
        }
    }

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
    public void onConnectionStateChanged(@NotNull ConnectionState state, @NotNull ConnectionStateChangeReason reason, @NotNull EduRoom fromClassRoom) {

    }

    @Override
    public void onNetworkQualityChanged(@NotNull io.agora.education.api.statistics.NetworkQuality quality, @NotNull EduUserInfo user, @NotNull EduRoom fromClassRoom) {

    }


    @Override
    public void onLocalUserUpdated(@NotNull EduUserEvent userEvent) {

    }

    @Override
    public void onLocalStreamAdded(@NotNull EduStreamEvent streamInfo) {

    }

    @Override
    public void onLocalStreamUpdated(@NotNull EduStreamEvent streamInfo) {

    }

    @Override
    public void onLocalSteamRemoved(@NotNull EduStreamEvent streamInfo) {

    }
}
