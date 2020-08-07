package io.agora.education.classroom;

import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.google.android.material.tabs.TabLayout;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.agora.education.R;
import io.agora.education.api.message.EduChatMsg;
import io.agora.education.api.message.EduMsg;
import io.agora.education.api.room.EduRoom;
import io.agora.education.api.room.data.EduRoomState;
import io.agora.education.api.room.data.EduRoomStatus;
import io.agora.education.api.room.data.RoomStatusEvent;
import io.agora.education.api.statistics.ConnectionState;
import io.agora.education.api.statistics.ConnectionStateChangeReason;
import io.agora.education.api.statistics.NetworkQuality;
import io.agora.education.api.stream.data.EduStreamEvent;
import io.agora.education.api.stream.data.EduStreamInfo;
import io.agora.education.api.user.data.EduBaseUserInfo;
import io.agora.education.api.user.data.EduUserEvent;
import io.agora.education.api.user.data.EduUserInfo;
import io.agora.education.api.user.data.EduUserRole;
import io.agora.education.classroom.bean.channel.Room;
import io.agora.education.classroom.bean.msg.ChannelMsg;
import io.agora.education.classroom.widget.RtcVideoView;

public class LargeClassActivity extends BaseClassActivity implements TabLayout.OnTabSelectedListener {

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
    private EduUserInfo linkedUser;

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
    protected void initView() {
        super.initView();
        if (video_teacher == null) {
            video_teacher = new RtcVideoView(this);
            video_teacher.init(R.layout.layout_video_large_class, false);
        }
        removeFromParent(video_teacher);
        layout_video_teacher.addView(video_teacher, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        if (video_student == null) {
            video_student = new RtcVideoView(this);
            video_student.init(R.layout.layout_video_small_class, true);
            video_student.setOnClickAudioListener(v -> {
                if (isMineLink()) {
                    muteLocalAudio(!video_student.isAudioMuted());
                }
            });
            video_student.setOnClickVideoListener(v -> {
                if (isMineLink()) {
                    muteLocalVideo(!video_student.isVideoMuted());
                }
            });
        }
        removeFromParent(video_student);
        layout_video_student.addView(video_student, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        if (layout_tab != null) {
            layout_tab.addOnTabSelectedListener(this);
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
    }

    @OnClick(R.id.layout_hand_up)
    public void onClick(View view) {
        boolean isSelected = view.isSelected();
        if (isSelected) {
            /**取消举手(包括在老师处理前主动取消和老师同意后主动退出)*/
//            ((LargeClassContext) classContext).cancel();
        } else {
            /**举手*/
//            ((LargeClassContext) classContext).apply();
        }
    }

//    @Override
//    public void onTeacherMediaChanged(@Nullable User user) {
//        if (user == null) return;
//        video_teacher.setName(user.userName);
//        video_teacher.showRemote(user.uid);
//        video_teacher.muteVideo(!user.isVideoEnable());
//        video_teacher.muteAudio(!user.isAudioEnable());
//    }

    /**本地用户举手被老师同意*/
//    @Override
//    public void onLinkMediaChanged(@Nullable User user) {
//        linkUser = user;
//        resetHandState();
//        if (user == null) {
//            video_student.setVisibility(View.GONE);
//            video_student.setSurfaceView(null);
//        } else {
//            video_student.setName(user.userName);
//            if (user.uid == getLocal().uid) {
//                video_student.showLocal();
//            } else {
//                video_student.showRemote(user.uid);
//            }
//            // make sure the student video always on the top
//            video_student.getSurfaceView().setZOrderMediaOverlay(true);
//            video_student.muteVideo(!user.isVideoEnable());
//            video_student.muteAudio(!user.isAudioEnable());
//            video_student.setVisibility(View.VISIBLE);
//        }
//    }

    private boolean isMineLink() {
        return linkedUser != null && linkedUser.getUserUuid() == getLocalUser().getUserInfo().getUserUuid();
    }

    /**被取消连麦*/
//    @Override
//    public void onHandUpCanceled() {
//        layout_hand_up.setSelected(false);
//    }

    private void resetHandState() {
        if (isMineLink()) {
            layout_hand_up.setEnabled(true);
            layout_hand_up.setSelected(true);
        } else {
            layout_hand_up.setEnabled(linkedUser == null);
            layout_hand_up.setSelected(false);
        }
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        if (layout_materials == null) {
            return;
        }
        boolean showMaterials = tab.getPosition() == 0;
        layout_materials.setVisibility(showMaterials ? View.VISIBLE : View.GONE);
        layout_chat_room.setVisibility(showMaterials ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }





    @Override
    public void onRemoteUsersInitialized(@NotNull List<? extends EduUserInfo> users, @NotNull EduRoom fromClassRoom) {
        super.onRemoteUsersInitialized(users, fromClassRoom);
        title_view.setTitle(String.format(Locale.getDefault(), "%s(%d)", getRoomName(), getCurFullUser().size()));
    }

    @Override
    public void onRemoteUsersJoined(@NotNull List<? extends EduUserInfo> users, @NotNull EduRoom fromClassRoom) {
        super.onRemoteUsersJoined(users, fromClassRoom);
        title_view.setTitle(String.format(Locale.getDefault(), "%s(%d)", getRoomName(), getCurFullUser().size()));
    }

    @Override
    public void onRemoteUsersLeft(@NotNull List<EduUserEvent> userEvents, @NotNull EduRoom fromClassRoom) {
        super.onRemoteUsersLeft(userEvents, fromClassRoom);
        title_view.setTitle(String.format(Locale.getDefault(), "%s(%d)", getRoomName(), getCurFullUser().size()));
    }

    @Override
    public void onRemoteUserUpdated(@NotNull List<EduUserEvent> userEvents, @NotNull EduRoom fromClassRoom) {
        super.onRemoteUserUpdated(userEvents, fromClassRoom);
    }

    @Override
    public void onRoomMessageReceived(@NotNull EduMsg message, @NotNull EduRoom fromClassRoom) {
        super.onRoomMessageReceived(message, fromClassRoom);
    }

    @Override
    public void onUserMessageReceived(@NotNull EduMsg message, @NotNull EduRoom fromClassRoom) {
        super.onUserMessageReceived(message, fromClassRoom);
    }

    @Override
    public void onRoomChatMessageReceived(@NotNull EduChatMsg eduChatMsg, @NotNull EduRoom fromClassRoom) {
        super.onRoomChatMessageReceived(eduChatMsg, fromClassRoom);
        /**收到群聊消息，进行处理并展示*/
        ChannelMsg.ChatMsg chatMsg = new ChannelMsg.ChatMsg(eduChatMsg.getFromUser(), eduChatMsg.getMessage(),
                eduChatMsg.getTimeStamp(), eduChatMsg.getType());
        chatMsg.isMe = chatMsg.getFromUser().equals(fromClassRoom.localUser.getUserInfo());
        chatRoomFragment.addMessage(chatMsg);
    }

    @Override
    public void onUserChatMessageReceived(@NotNull EduChatMsg chatMsg, @NotNull EduRoom fromClassRoom) {
        super.onUserChatMessageReceived(chatMsg, fromClassRoom);
    }

    @Override
    public void onRemoteStreamsInitialized(@NotNull List<? extends EduStreamInfo> streams, @NotNull EduRoom fromClassRoom) {
        super.onRemoteStreamsInitialized(streams, fromClassRoom);
        /**大班课场景下，远端流就是老师的流;初始化成功后只可能有Camera的流*/
        EduStreamInfo streamInfo = getTeacherStream();
       if(streamInfo != null) {
           EduBaseUserInfo userInfo = streamInfo.getPublisher();
           video_teacher.setName(userInfo.getUserName());
           getLocalUser().setStreamView(streamInfo, video_teacher.getVideoLayout());
           video_teacher.muteVideo(!streamInfo.getHasVideo());
           video_teacher.muteAudio(!streamInfo.getHasAudio());
       }
    }

    @Override
    public void onRemoteStreamsAdded(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom fromClassRoom) {
        super.onRemoteStreamsAdded(streamEvents, fromClassRoom);
        for (EduStreamEvent streamEvent : streamEvents) {
            EduStreamInfo streamInfo = streamEvent.getModifiedStream();
            switch (streamInfo.getVideoSourceType()) {
                case CAMERA:
                    EduBaseUserInfo userInfo = streamInfo.getPublisher();
                    video_teacher.setName(userInfo.getUserName());
                    renderStream(streamInfo, video_teacher.getVideoLayout());
                    video_teacher.muteVideo(!streamInfo.getHasVideo());
                    video_teacher.muteAudio(!streamInfo.getHasAudio());
                    break;
                case SCREEN:
                    /**有屏幕分享的流进入，说明是老师打开了屏幕分享，此时把这个流渲染出来*/
                    layout_whiteboard.setVisibility(View.GONE);
                    layout_share_video.setVisibility(View.VISIBLE);
                    layout_share_video.removeAllViews();
                    renderStream(streamInfo, layout_share_video);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onRemoteStreamsUpdated(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom fromClassRoom) {
        super.onRemoteStreamsUpdated(streamEvents, fromClassRoom);
        /**屏幕分享流只有新建和移除，不会有修改行为，所以此处的流都是Camera类型的*/
        for (EduStreamEvent streamEvent : streamEvents) {
            EduStreamInfo streamInfo = streamEvent.getModifiedStream();
            EduBaseUserInfo userInfo = streamInfo.getPublisher();
            video_teacher.setName(userInfo.getUserName());
            renderStream(streamInfo, video_teacher.getVideoLayout());
            video_teacher.muteVideo(!streamInfo.getHasVideo());
            video_teacher.muteAudio(!streamInfo.getHasAudio());
        }
    }

    @Override
    public void onRemoteStreamsRemoved(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom fromClassRoom) {
        super.onRemoteStreamsRemoved(streamEvents, fromClassRoom);
        for (EduStreamEvent streamEvent : streamEvents) {
            EduStreamInfo streamInfo = streamEvent.getModifiedStream();
            if(streamInfo.getPublisher().getRole().equals(EduUserRole.TEACHER)) {
                switch (streamInfo.getVideoSourceType()) {
                    case CAMERA:
                        video_teacher.setName(streamInfo.getPublisher().getUserName());
                        renderStream(streamInfo, null);
                        video_teacher.muteVideo(!streamInfo.getHasVideo());
                        video_teacher.muteAudio(!streamInfo.getHasAudio());
                        break;
                    case SCREEN:
                        /**有屏幕分享的流离开，说明是老师关闭了屏幕分享，移除屏幕分享的布局*/
                        layout_share_video.removeAllViews();
                        layout_share_video.setVisibility(View.GONE);
                        renderStream(streamInfo, null);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @Override
    public void onRoomStatusChanged(@NotNull RoomStatusEvent event, @NotNull EduUserInfo operatorUser, @NotNull EduRoom fromClassRoom) {
        super.onRoomStatusChanged(event, operatorUser, fromClassRoom);
        EduRoomStatus roomStatus = fromClassRoom.getRoomStatus();
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
    public void onRoomPropertyChanged(@NotNull EduRoom fromClassRoom) {
        super.onRoomPropertyChanged(fromClassRoom);
    }

    @Override
    public void onRemoteUserPropertiesUpdated(@NotNull List<EduUserInfo> userInfos, @NotNull EduRoom fromClassRoom) {
    }

    @Override
    public void onConnectionStateChanged(@NotNull ConnectionState state, @NotNull ConnectionStateChangeReason reason, @NotNull EduRoom fromClassRoom) {
        super.onConnectionStateChanged(state, reason, fromClassRoom);
    }

    @Override
    public void onNetworkQualityChanged(@NotNull NetworkQuality quality, @NotNull EduUserInfo user, @NotNull EduRoom fromClassRoom) {
        super.onNetworkQualityChanged(quality, user, fromClassRoom);
        title_view.setNetworkQuality(quality.getValue());
    }

    @Override
    public void onLocalUserUpdated(@NotNull EduUserEvent userEvent) {
        super.onLocalUserUpdated(userEvent);
        /**更新用户信息*/
        EduUserInfo userInfo = userEvent.getModifiedUser();
        chatRoomFragment.setMuteLocal(!userInfo.isChatAllowed());
    }

    @Override
    public void onLocalUserPropertyUpdated(@NotNull EduUserInfo userInfo) {
        super.onLocalUserPropertyUpdated(userInfo);
    }

    @Override
    public void onLocalStreamAdded(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamAdded(streamEvent);
    }

    @Override
    public void onLocalStreamUpdated(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamUpdated(streamEvent);
    }

    @Override
    public void onLocalStreamRemoved(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamRemoved(streamEvent);
    }
}
