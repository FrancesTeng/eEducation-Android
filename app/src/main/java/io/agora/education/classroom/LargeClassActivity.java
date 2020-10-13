package io.agora.education.classroom;

import android.content.res.Configuration;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.agora.base.ToastManager;
import io.agora.education.R;
import io.agora.education.api.EduCallback;
import io.agora.education.api.message.EduChatMsg;
import io.agora.education.api.message.EduMsg;
import io.agora.education.api.room.EduRoom;
import io.agora.education.api.room.data.AutoPublishItem;
import io.agora.education.api.room.data.EduRoomState;
import io.agora.education.api.room.data.EduRoomStatus;
import io.agora.education.api.room.data.RoomStatusEvent;
import io.agora.education.api.statistics.ConnectionState;
import io.agora.education.api.statistics.ConnectionStateChangeReason;
import io.agora.education.api.statistics.NetworkQuality;
import io.agora.education.api.stream.data.EduStreamEvent;
import io.agora.education.api.stream.data.EduStreamInfo;
import io.agora.education.api.stream.data.LocalStreamInitOptions;
import io.agora.education.api.stream.data.StreamSubscribeOptions;
import io.agora.education.api.stream.data.VideoSourceType;
import io.agora.education.api.stream.data.VideoStreamType;
import io.agora.education.api.user.EduStudent;
import io.agora.education.api.user.data.EduBaseUserInfo;
import io.agora.education.api.user.data.EduUserEvent;
import io.agora.education.api.user.data.EduUserInfo;
import io.agora.education.api.user.data.EduUserRole;
import io.agora.education.classroom.bean.channel.Room;
import io.agora.education.classroom.bean.msg.PeerMsg;
import io.agora.education.classroom.widget.RtcVideoView;
import io.agora.rte.RteEngineImpl;

import static io.agora.education.classroom.bean.msg.PeerMsg.CoVideoMsg.Status.Applying;
import static io.agora.education.classroom.bean.msg.PeerMsg.CoVideoMsg.Status.CoVideoing;
import static io.agora.education.classroom.bean.msg.PeerMsg.CoVideoMsg.Status.DisCoVideo;
import static io.agora.education.classroom.bean.msg.PeerMsg.CoVideoMsg.Type.ABORT;
import static io.agora.education.classroom.bean.msg.PeerMsg.CoVideoMsg.Type.ACCEPT;
import static io.agora.education.classroom.bean.msg.PeerMsg.CoVideoMsg.Type.CANCEL;
import static io.agora.education.classroom.bean.msg.PeerMsg.CoVideoMsg.Type.EXIT;
import static io.agora.education.classroom.bean.msg.PeerMsg.CoVideoMsg.Type.REJECT;

public class LargeClassActivity extends BaseClassActivity implements TabLayout.OnTabSelectedListener {
    private static final String TAG = "LargeClassActivity";

    @BindView(R.id.layout_video_teacher)
    protected FrameLayout layout_video_teacher;
    @BindView(R.id.layout_video_student)
    protected FrameLayout layout_video_student;
    @Nullable
    @BindView(R.id.layout_tab)
    protected TabLayout layout_tab;
    @BindView(R.id.layout_chat_room)
    protected FrameLayout layout_chat_room;
    @Nullable
    @BindView(R.id.layout_materials)
    protected FrameLayout layout_materials;
    @BindView(R.id.layout_hand_up)
    protected CardView layout_hand_up;

    private RtcVideoView video_teacher;
    private RtcVideoView video_student;
    private AppCompatTextView textView_unRead;
    private ConstraintLayout layout_unRead;

    /**
     * 当前本地用户是否在连麦中
     */
    private int localCoVideoStatus = DisCoVideo;
    /**
     * 当前连麦用户
     */
    private EduBaseUserInfo curLinkedUser;

    private int unReadCount = 0;

    @Override
    protected int getLayoutResId() {
        Configuration configuration = getResources().getConfiguration();
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            return R.layout.activity_large_class_portrait;
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            return R.layout.activity_large_class_landscape;
        }
    }

    @Override
    protected void initData() {
        super.initData();
        joinRoom(getMainEduRoom(), roomEntry.getUserName(), roomEntry.getUserUuid(), true,
                AutoPublishItem.NoAutoPublish, true,
                new EduCallback<EduStudent>() {
                    @Override
                    public void onSuccess(@org.jetbrains.annotations.Nullable EduStudent res) {
                        runOnUiThread(() -> showFragmentWithJoinSuccess());
                    }

                    @Override
                    public void onFailure(int code, @org.jetbrains.annotations.Nullable String reason) {
                        joinFailed(code, reason);
                    }
                });
    }

    @Override
    protected void initView() {
        super.initView();
        if (video_teacher == null) {
            video_teacher = new RtcVideoView(this);
            video_teacher.init(R.layout.layout_video_large_class, false);
        }
        removeFromParent(video_teacher);
        layout_video_teacher.addView(video_teacher, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        video_teacher.setVisibility(View.VISIBLE);

        if (video_student == null) {
            video_student = new RtcVideoView(this);
            video_student.init(R.layout.layout_video_small_class, true);
            video_student.setOnClickAudioListener(v -> {
                if (localCoVideoStatus == CoVideoing) {
                    muteLocalAudio(!video_student.isAudioMuted());
                }
            });
            video_student.setOnClickVideoListener(v -> {
                if (localCoVideoStatus == CoVideoing) {
                    muteLocalVideo(!video_student.isVideoMuted());
                }
            });
        }
        removeFromParent(video_student);
        layout_video_student.addView(video_student, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        video_student.setViewVisibility(View.GONE);

        if (layout_tab != null) {
            /*不为空说明是竖屏*/
            layout_tab.addOnTabSelectedListener(this);
            layout_tab.getTabAt(1).setCustomView(R.layout.layout_largeclass_chatroom);
            layout_unRead = findViewById(R.id.layout_unRead);
            textView_unRead = findViewById(R.id.textView_unRead);
        }

        // disable operation in large class
        whiteboardFragment.disableDeviceInputs(true);
        whiteboardFragment.setWritable(false);

        if (surface_share_video != null) {
            removeFromParent(surface_share_video);
            layout_share_video.addView(surface_share_video, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        resetHandState();
    }

    @Override
    protected int getClassType() {
        return Room.Type.LARGE;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(getLayoutResId());
        ButterKnife.bind(this);
        initView();
        recoveryFragmentWithConfigChanged();
    }

    @OnClick(R.id.layout_hand_up)
    public void onClick(View view) {
        boolean status = localCoVideoStatus == DisCoVideo;
        if (!status) {
            /*取消举手(包括在老师处理前主动取消和老师同意后主动退出)*/
            cancelCoVideo(new EduCallback<EduMsg>() {
                @Override
                public void onSuccess(@Nullable EduMsg res) {
                    Log.e(TAG, "取消举手成功");
                }

                @Override
                public void onFailure(int code, @Nullable String reason) {
                    Log.e(TAG, "取消举手失败");
                }
            });
        } else {
            /*举手*/
            applyCoVideo(new EduCallback<EduMsg>() {
                @Override
                public void onSuccess(@Nullable EduMsg res) {
                    Log.e(TAG, "举手成功");
                }

                @Override
                public void onFailure(int code, @Nullable String reason) {
                    Log.e(TAG, "举手失败");
                    ToastManager.showShort(R.string.function_error, code, reason);
                }
            });
        }
    }

    /**
     * 申请举手连麦
     */
    private void applyCoVideo(EduCallback<EduMsg> callback) {
        PeerMsg.CoVideoMsg coVideoMsg = new PeerMsg.CoVideoMsg(
                PeerMsg.CoVideoMsg.Type.APPLY,
                getLocalUser().getUserInfo().getUserUuid(),
                getLocalUser().getUserInfo().getUserName());
        PeerMsg peerMsg = new PeerMsg(PeerMsg.Cmd.CO_VIDEO, coVideoMsg);
        EduUserInfo teacher = getTeacher();
        if (teacher != null) {
            localCoVideoStatus = Applying;
            resetHandState();
            getLocalUser().sendUserMessage(peerMsg.toJsonString(), getTeacher(), callback);
        } else {
            ToastManager.showShort(R.string.there_is_no_teacher_disable_covideo);
        }
    }

    /**
     * 取消举手(包括在老师处理前主动取消和老师同意后主动退出)
     */
    private void cancelCoVideo(EduCallback<EduMsg> callback) {
        PeerMsg.CoVideoMsg coVideoMsg = new PeerMsg.CoVideoMsg(
                (localCoVideoStatus == CoVideoing) ? EXIT : CANCEL,
                getLocalUser().getUserInfo().getUserUuid(),
                getLocalUser().getUserInfo().getUserName());
        PeerMsg peerMsg = new PeerMsg(PeerMsg.Cmd.CO_VIDEO, coVideoMsg);
        if (localCoVideoStatus == CoVideoing) {
            /*连麦过程中取消
             * 1：关闭本地流
             * 2：更新流信息到服务器
             * 3：发送取消的点对点消息给老师
             * 4：更新本地记录的连麦状态*/
            LocalStreamInitOptions options = new LocalStreamInitOptions(
                    getLocalCameraStream().getStreamUuid(), false, false);
            options.setStreamName(getLocalCameraStream().getStreamName());
            getLocalUser().initOrUpdateLocalStream(options, new EduCallback<EduStreamInfo>() {
                @Override
                public void onSuccess(@Nullable EduStreamInfo res) {
                    localCoVideoStatus = DisCoVideo;
                    curLinkedUser = null;
                    resetHandState();
                    video_student.setName(getLocalUserInfo().getUserName());
                    renderStream(getMainEduRoom(), getLocalCameraStream(), null);
                    video_student.muteVideo(!getLocalCameraStream().getHasVideo());
                    video_student.muteAudio(!getLocalCameraStream().getHasAudio());
                    video_student.setViewVisibility(View.GONE);
                    getLocalUser().sendUserMessage(peerMsg.toJsonString(), getTeacher(), callback);
                    getLocalUser().unPublishStream(res, new EduCallback<Boolean>() {
                        @Override
                        public void onSuccess(@Nullable Boolean res) {
                        }

                        @Override
                        public void onFailure(int code, @Nullable String reason) {
                            callback.onFailure(code, reason);
                        }
                    });
                }

                @Override
                public void onFailure(int code, @Nullable String reason) {
                    Log.e(TAG, "举手过程中取消失败");
                    callback.onFailure(code, reason);
                }
            });
        } else {
            /*举手过程中取消(老师还未处理)；直接发送取消的点对点消息给老师即可*/
            getLocalUser().sendUserMessage(peerMsg.toJsonString(), getTeacher(), callback);
            localCoVideoStatus = DisCoVideo;
            runOnUiThread(() -> {
                resetHandState();
            });
        }
    }

    /**
     * 本地用户(举手、连麦)被老师同意/(拒绝、打断)
     *
     * @param coVideoing 是否正在连麦过程中
     */
    public void onLinkMediaChanged(boolean coVideoing) {
        if (!coVideoing) {
            video_student.setViewVisibility(View.GONE);
            /**正在连麦中时才会记录本地流；申请中取消或被拒绝本地不会记录流*/
            if (localCoVideoStatus == CoVideoing) {
                Log.e(TAG, "连麦过程中被打断");
                /**连麦被打断，停止发流*/
                LocalStreamInitOptions options = new LocalStreamInitOptions(
                        getLocalCameraStream().getStreamUuid(), false, false);
                options.setStreamName(getLocalCameraStream().getStreamName());
                getLocalUser().initOrUpdateLocalStream(options, new EduCallback<EduStreamInfo>() {
                    @Override
                    public void onSuccess(@Nullable EduStreamInfo res) {
                        video_student.setName(getLocalUserInfo().getUserName());
                        renderStream(getMainEduRoom(), getLocalCameraStream(), null);
                        video_student.muteVideo(!getLocalCameraStream().getHasVideo());
                        video_student.muteAudio(!getLocalCameraStream().getHasAudio());
                        video_student.setViewVisibility(View.GONE);
                        getLocalUser().unPublishStream(res, new EduCallback<Boolean>() {
                            @Override
                            public void onSuccess(@Nullable Boolean res) {
                                Log.e(TAG, "连麦过程中被打断，停止发流成功");
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
        } else {
            /**连麦中，发流*/
            EduStreamInfo streamInfo = new EduStreamInfo(getLocalUserInfo().getStreamUuid(), null,
                    VideoSourceType.CAMERA, true, true, getLocalUserInfo());
            getLocalUser().publishStream(streamInfo, new EduCallback<Boolean>() {
                @Override
                public void onSuccess(@Nullable Boolean res) {
                    setLocalCameraStream(streamInfo);
                    video_student.setViewVisibility(View.VISIBLE);
                    video_student.setName(getLocalUserInfo().getUserName());
                    renderStream(getMainEduRoom(), getLocalCameraStream(), video_student.getVideoLayout());
                    video_student.muteVideo(!getLocalCameraStream().getHasVideo());
                    video_student.muteAudio(!getLocalCameraStream().getHasAudio());
                }

                @Override
                public void onFailure(int code, @Nullable String reason) {

                }
            });
        }
        localCoVideoStatus = coVideoing ? CoVideoing : DisCoVideo;
        curLinkedUser = coVideoing ? getLocalUserInfo() : null;
        resetHandState();
    }

    /**
     * 被取消连麦
     */
    private void resetHandState() {
        runOnUiThread(() -> {
            boolean hasTeacher = getTeacher() != null;
            /**有老师的情况下才显示*/
//            layout_hand_up.setVisibility(hasTeacher ? View.VISIBLE : View.GONE);
            /**当前连麦用户不是本地用户时，隐藏*/
            if (curLinkedUser != null) {
//                layout_hand_up.setVisibility((curLinkedUser.equals(getLocalUserInfo()) ?
//                        View.VISIBLE : View.GONE));
                layout_hand_up.setEnabled(curLinkedUser.equals(getLocalUserInfo()));
                layout_hand_up.setSelected(true);
            } else {
                layout_hand_up.setEnabled(true);
                layout_hand_up.setSelected(false);
            }
//            if (hasTeacher) {
//                layout_hand_up.setSelected(localCoVideoStatus != DisCoVideo);
//            }
        });
    }

    private boolean chatRoomShowing() {
        return layout_chat_room.getVisibility() == View.VISIBLE;
    }

    private void updateUnReadCount(boolean gone) {
        if (gone) {
            unReadCount = 0;
        } else {
            unReadCount++;
            textView_unRead.setText(String.valueOf(unReadCount));
        }
        textView_unRead.setVisibility(gone ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        if (layout_materials == null) {
            return;
        }
        boolean showMaterials = tab.getPosition() == 0;
        layout_materials.setVisibility(showMaterials ? View.VISIBLE : View.GONE);
        layout_chat_room.setVisibility(showMaterials ? View.GONE : View.VISIBLE);
        if (!showMaterials) {
            updateUnReadCount(true);
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }


    @Override
    public void onRemoteUsersInitialized(@NotNull List<? extends EduUserInfo> users, @NotNull EduRoom classRoom) {
        super.onRemoteUsersInitialized(users, classRoom);
        title_view.setTitle(String.format(Locale.getDefault(), "%s", getMediaRoomName()));
        /**老师不在的时候不能举手*/
        resetHandState();
    }

    @Override
    public void onRemoteUsersJoined(@NotNull List<? extends EduUserInfo> users, @NotNull EduRoom classRoom) {
        super.onRemoteUsersJoined(users, classRoom);
        title_view.setTitle(String.format(Locale.getDefault(), "%s", getMediaRoomName()));
        /**老师不在的时候不能举手*/
        resetHandState();

    }

    @Override
    public void onRemoteUsersLeft(@NotNull List<EduUserEvent> userEvents, @NotNull EduRoom classRoom) {
        super.onRemoteUsersLeft(userEvents, classRoom);
        title_view.setTitle(String.format(Locale.getDefault(), "%s", getMediaRoomName()));
        /**老师不在的时候不能举手*/
        resetHandState();
    }

    @Override
    public void onRemoteUserUpdated(@NotNull List<EduUserEvent> userEvents, @NotNull EduRoom classRoom) {
        super.onRemoteUserUpdated(userEvents, classRoom);
    }

    @Override
    public void onRoomMessageReceived(@NotNull EduMsg message, @NotNull EduRoom classRoom) {
        super.onRoomMessageReceived(message, classRoom);
    }

    @Override
    public void onRoomChatMessageReceived(@NotNull EduChatMsg eduChatMsg, @NotNull EduRoom classRoom) {
        super.onRoomChatMessageReceived(eduChatMsg, classRoom);
        runOnUiThread(() -> updateUnReadCount(chatRoomShowing()));
    }

    @Override
    public void onRemoteStreamsInitialized(@NotNull List<? extends EduStreamInfo> streams, @NotNull EduRoom classRoom) {
        super.onRemoteStreamsInitialized(streams, classRoom);
        /**大班课场景下，远端流可能包括老师和远端学生连麦的流*/
        for (EduStreamInfo streamInfo : streams) {
            EduBaseUserInfo publisher = streamInfo.getPublisher();
            if (publisher.getRole().equals(EduUserRole.TEACHER)) {
                switch (streamInfo.getVideoSourceType()) {
                    case CAMERA:
                        video_teacher.setName(publisher.getUserName());
                        renderStream(getMainEduRoom(), streamInfo, video_teacher.getVideoLayout());
//                        RteEngineImpl.INSTANCE.publish(getMediaRoomUuid());
//                        new Handler(getMainLooper()).postDelayed(() ->
//                                RteEngineImpl.INSTANCE.unpublish(getMediaRoomUuid()), 300);
                        video_teacher.muteVideo(!streamInfo.getHasVideo());
                        video_teacher.muteAudio(!streamInfo.getHasAudio());
                        break;
                    case SCREEN:
                        layout_whiteboard.setVisibility(View.GONE);
                        layout_share_video.setVisibility(View.VISIBLE);
                        layout_share_video.removeAllViews();
                        renderStream(getMainEduRoom(), streamInfo, layout_share_video);
                        break;
                    default:
                        break;
                }
            } else {
                Log.e(TAG, "发现有远端连麦流,立即渲染");
                video_student.setViewVisibility(View.VISIBLE);
                video_student.setName(streamInfo.getPublisher().getUserName());
                renderStream(getMainEduRoom(), streamInfo, video_student.getVideoLayout());
                video_student.muteVideo(!streamInfo.getHasVideo());
                video_student.muteAudio(!streamInfo.getHasAudio());
                curLinkedUser = streamInfo.getPublisher();
                resetHandState();
            }
        }
    }

    @Override
    public void onRemoteStreamsAdded(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom classRoom) {
        super.onRemoteStreamsAdded(streamEvents, classRoom);
        for (EduStreamEvent streamEvent : streamEvents) {
            EduStreamInfo streamInfo = streamEvent.getModifiedStream();
            EduBaseUserInfo userInfo = streamInfo.getPublisher();
            if (userInfo.getRole().equals(EduUserRole.TEACHER)) {
                switch (streamInfo.getVideoSourceType()) {
                    case CAMERA:
                        /**老师的远端流*/
                        video_teacher.setName(userInfo.getUserName());
                        renderStream(getMainEduRoom(), streamInfo, video_teacher.getVideoLayout());
//                        RteEngineImpl.INSTANCE.publish(getMediaRoomUuid());
//                        new Handler(getMainLooper()).postDelayed(() ->
//                                RteEngineImpl.INSTANCE.unpublish(getMediaRoomUuid()), 300);
                        video_teacher.muteVideo(!streamInfo.getHasVideo());
                        video_teacher.muteAudio(!streamInfo.getHasAudio());
                        break;
                    default:
                        break;
                }
            } else {
                /**远端用户连麦时的流*/
                video_student.setViewVisibility(View.VISIBLE);
                video_student.setName(streamInfo.getPublisher().getUserName());
                renderStream(getMainEduRoom(), streamInfo, video_student.getVideoLayout());
                video_student.muteVideo(!streamInfo.getHasVideo());
                video_student.muteAudio(!streamInfo.getHasAudio());
                curLinkedUser = streamInfo.getPublisher();
                resetHandState();
            }
        }
    }

    @Override
    public void onRemoteStreamsUpdated(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom classRoom) {
        super.onRemoteStreamsUpdated(streamEvents, classRoom);
        /**屏幕分享流只有新建和移除，不会有修改行为，所以此处的流都是Camera类型的*/
        for (EduStreamEvent streamEvent : streamEvents) {
            EduStreamInfo streamInfo = streamEvent.getModifiedStream();
            EduBaseUserInfo userInfo = streamInfo.getPublisher();
            if (userInfo.getRole().equals(EduUserRole.TEACHER)) {
                switch (streamInfo.getVideoSourceType()) {
                    case CAMERA:
                        video_teacher.setName(userInfo.getUserName());
                        renderStream(getMainEduRoom(), streamInfo, video_teacher.getVideoLayout());
                        video_teacher.muteVideo(!streamInfo.getHasVideo());
                        video_teacher.muteAudio(!streamInfo.getHasAudio());
                        break;
                    default:
                        break;
                }
            } else {
                video_student.setViewVisibility(View.VISIBLE);
                video_student.setName(streamInfo.getPublisher().getUserName());
                renderStream(getMainEduRoom(), streamInfo, video_student.getVideoLayout());
                video_student.muteVideo(!streamInfo.getHasVideo());
                video_student.muteAudio(!streamInfo.getHasAudio());
                curLinkedUser = streamInfo.getPublisher();
                resetHandState();
            }
        }
    }

    @Override
    public void onRemoteStreamsRemoved(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom classRoom) {
        super.onRemoteStreamsRemoved(streamEvents, classRoom);
        for (EduStreamEvent streamEvent : streamEvents) {
            EduStreamInfo streamInfo = streamEvent.getModifiedStream();
            EduBaseUserInfo userInfo = streamInfo.getPublisher();
            if (userInfo.getRole().equals(EduUserRole.TEACHER)) {
                switch (streamInfo.getVideoSourceType()) {
                    case CAMERA:
                        video_teacher.setName(streamInfo.getPublisher().getUserName());
                        renderStream(getMainEduRoom(), streamInfo, null);
                        video_teacher.muteVideo(!streamInfo.getHasVideo());
                        video_teacher.muteAudio(!streamInfo.getHasAudio());
                        break;
                    default:
                        break;
                }
            } else {
                video_student.setName(streamInfo.getPublisher().getUserName());
                renderStream(getMainEduRoom(), streamInfo, null);
                video_student.muteVideo(!streamInfo.getHasVideo());
                video_student.muteAudio(!streamInfo.getHasAudio());
                video_student.setViewVisibility(View.GONE);
                if (curLinkedUser.equals(streamInfo.getPublisher())) {
                    curLinkedUser = null;
                }
                resetHandState();
            }
        }
    }

    @Override
    public void onRoomStatusChanged(@NotNull RoomStatusEvent event, @NotNull EduUserInfo operatorUser, @NotNull EduRoom classRoom) {
        super.onRoomStatusChanged(event, operatorUser, classRoom);
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
    public void onRoomPropertyChanged(@NotNull EduRoom classRoom, @Nullable Map<String, Object> cause) {
        super.onRoomPropertyChanged(classRoom, cause);
        /*处理可能收到的录制的消息*/
        runOnUiThread(() -> {
            if (revRecordMsg) {
                revRecordMsg = false;
                updateUnReadCount(chatRoomShowing());
            }
        });
    }

    @Override
    public void onRemoteUserPropertiesUpdated(@NotNull List<EduUserInfo> userInfos, @NotNull EduRoom classRoom, @Nullable Map<String, Object> cause) {
    }

    @Override
    public void onNetworkQualityChanged(@NotNull NetworkQuality quality, @NotNull EduUserInfo user, @NotNull EduRoom classRoom) {
        super.onNetworkQualityChanged(quality, user, classRoom);
        title_view.setNetworkQuality(quality);
    }

    @Override
    public void onLocalUserUpdated(@NotNull EduUserEvent userEvent) {
        super.onLocalUserUpdated(userEvent);
    }

    @Override
    public void onLocalUserPropertyUpdated(@NotNull EduUserInfo userInfo, @Nullable Map<String, Object> cause) {
        super.onLocalUserPropertyUpdated(userInfo, cause);
    }

    @Override
    public void onLocalStreamAdded(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamAdded(streamEvent);
    }

    @Override
    public void onLocalStreamUpdated(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamUpdated(streamEvent);
        if (localCoVideoStatus == CoVideoing) {
            /**本地流(连麦的Camera流)被修改*/
            video_student.setViewVisibility(View.VISIBLE);
            video_student.setName(getLocalUserInfo().getUserName());
            renderStream(getMainEduRoom(), getLocalCameraStream(), video_student.getVideoLayout());
            video_student.muteVideo(!getLocalCameraStream().getHasVideo());
            video_student.muteAudio(!getLocalCameraStream().getHasAudio());
        }
    }

    @Override
    public void onLocalStreamRemoved(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamRemoved(streamEvent);
    }

    @Override
    public void onUserMessageReceived(@NotNull EduMsg message) {
        super.onUserMessageReceived(message);
        PeerMsg peerMsg = PeerMsg.fromJson(message.getMessage(), PeerMsg.class);
        if (peerMsg.cmd == PeerMsg.Cmd.CO_VIDEO) {
            PeerMsg.CoVideoMsg coVideoMsg = peerMsg.getMsg(PeerMsg.CoVideoMsg.class);
            switch (coVideoMsg.type) {
                case REJECT:
                    onLinkMediaChanged(false);
                    ToastManager.showShort(R.string.reject_interactive);
                    break;
                case ACCEPT:
                    onLinkMediaChanged(true);
                    ToastManager.showShort(R.string.accept_interactive);
                    break;
                case ABORT:
                    onLinkMediaChanged(false);
                    ToastManager.showShort(R.string.abort_interactive);
                    break;
            }
        }
    }

    @Override
    public void onUserChatMessageReceived(@NotNull EduChatMsg chatMsg) {
        super.onUserChatMessageReceived(chatMsg);
    }

    @Override
    public void onConnectionStateChanged(@NotNull ConnectionState state, @NotNull ConnectionStateChangeReason reason) {
        super.onConnectionStateChanged(state, reason);
    }
}
