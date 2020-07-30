package io.agora.education.classroom;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import io.agora.education.R;
import io.agora.education.api.message.EduChatMsg;
import io.agora.education.api.message.EduMsg;
import io.agora.education.api.room.EduRoom;
import io.agora.education.api.room.data.EduRoomInfo;
import io.agora.education.api.room.data.EduRoomState;
import io.agora.education.api.room.data.EduRoomStatus;
import io.agora.education.api.room.data.RoomStatusEvent;
import io.agora.education.api.statistics.ConnectionState;
import io.agora.education.api.statistics.ConnectionStateChangeReason;
import io.agora.education.api.statistics.NetworkQuality;
import io.agora.education.api.stream.data.EduStreamEvent;
import io.agora.education.api.stream.data.EduStreamInfo;
import io.agora.education.api.stream.data.VideoSourceType;
import io.agora.education.api.user.data.EduUserEvent;
import io.agora.education.api.user.data.EduUserInfo;
import io.agora.education.classroom.bean.channel.Room;
import io.agora.education.classroom.bean.channel.User;
import io.agora.education.classroom.bean.msg.ChannelMsg;
import io.agora.education.classroom.strategy.context.OneToOneClassContext;
import io.agora.education.classroom.widget.RtcVideoView;
import io.agora.sdk.manager.RtcManager;

public class OneToOneClassActivity extends BaseClassActivity {
    private static final String TAG = OneToOneClassActivity.class.getSimpleName();

    @BindView(R.id.layout_video_teacher)
    protected RtcVideoView video_teacher;
    @BindView(R.id.layout_video_student)
    protected RtcVideoView video_student;
    @BindView(R.id.layout_im)
    protected View layout_im;

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_one2one_class;
    }

    @Override
    protected void initView() {
        super.initView();
        video_teacher.init(R.layout.layout_video_one2one_class, false);
        video_student.init(R.layout.layout_video_one2one_class, true);
        video_student.setOnClickAudioListener(v -> OneToOneClassActivity.this.muteLocalAudio(!video_student.isAudioMuted()));
        video_student.setOnClickVideoListener(v -> OneToOneClassActivity.this.muteLocalVideo(!video_student.isVideoMuted()));
    }

    @Override
    protected int getClassType() {
        return Room.Type.ONE2ONE;
    }

    @OnClick(R.id.iv_float)
    public void onClick(View view) {
        boolean isSelected = view.isSelected();
        view.setSelected(!isSelected);
        layout_im.setVisibility(isSelected ? View.VISIBLE : View.GONE);
    }


    @Override
    public void onRemoteUsersInitialized(@NotNull List<? extends EduUserInfo> users, @NotNull EduRoom fromClassRoom) {
        super.onRemoteUsersInitialized(users, fromClassRoom);
    }

    @Override
    public void onRemoteUsersJoined(@NotNull List<? extends EduUserInfo> users, @NotNull EduRoom fromClassRoom) {
        super.onRemoteUsersJoined(users, fromClassRoom);
    }

    @Override
    public void onRemoteUsersLeft(@NotNull List<EduUserEvent> userEvents, @NotNull EduRoom fromClassRoom) {
        super.onRemoteUsersLeft(userEvents, fromClassRoom);
    }

    @Override
    public void onRemoteUserUpdated(@NotNull List<EduUserEvent> userEvents, @NotNull EduRoom fromClassRoom) {
        super.onRemoteUserUpdated(userEvents, fromClassRoom);
        for (EduUserEvent userEvent : userEvents) {
            /**判断是否有针对本地用户的更新*/
            EduUserInfo userInfo = userEvent.getModifiedUser();
            if (userInfo == fromClassRoom.getLocalUser().getUserInfo()) {
                /**更新可能改变的用户信息*/
                chatRoomFragment.setMuteLocal(!userInfo.isChatAllowed());
                video_student.setName(userInfo.getUserName());
            }
        }
    }

    /**
     * 群聊自定义消息回调
     */
    @Override
    public void onRoomMessageReceived(@NotNull EduMsg message, @NotNull EduRoom fromClassRoom) {
        super.onRoomMessageReceived(message, fromClassRoom);
    }

    /**
     * 私聊自定义消息回调
     */
    @Override
    public void onUserMessageReceived(@NotNull EduMsg message, @NotNull EduRoom fromClassRoom) {
        super.onUserMessageReceived(message, fromClassRoom);
    }

    /**
     * 群聊消息回调
     */
    @Override
    public void onRoomChatMessageReceived(@NotNull EduChatMsg eduChatMsg, @NotNull EduRoom fromClassRoom) {
        super.onRoomChatMessageReceived(eduChatMsg, fromClassRoom);
        /**收到群聊消息，进行处理并展示*/
        ChannelMsg.ChatMsg chatMsg = new ChannelMsg.ChatMsg(eduChatMsg.getFromUser(), eduChatMsg.getMessage(),
                eduChatMsg.getTimeStamp(), eduChatMsg.getType());
        chatMsg.isMe = chatMsg.getFromUser().equals(fromClassRoom.localUser);
        chatRoomFragment.addMessage(chatMsg);
    }

    /**
     * 私聊消息回调
     */
    @Override
    public void onUserChatMessageReceived(@NotNull EduChatMsg chatMsg, @NotNull EduRoom fromClassRoom) {
        super.onUserChatMessageReceived(chatMsg, fromClassRoom);
    }

    @Override
    public void onRemoteStreamsInitialized(@NotNull List<? extends EduStreamInfo> streams, @NotNull EduRoom fromClassRoom) {
        super.onRemoteStreamsInitialized(streams, fromClassRoom);
    }

    @Override
    public void onRemoteStreamsAdded(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom fromClassRoom) {
        super.onRemoteStreamsAdded(streamEvents, fromClassRoom);
        for (EduStreamEvent streamEvent : streamEvents) {
            EduStreamInfo streamInfo = streamEvent.getModifiedStream();
            /**小班课场景下，远端流就是老师的流*/
            switch (streamInfo.getVideoSourceType()) {
                case CAMERA:
                    video_teacher.setName(streamInfo.getPublisher().getUserName());
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
        for (EduStreamEvent streamEvent : streamEvents) {
            EduStreamInfo streamInfo = streamEvent.getModifiedStream();
            /**小班课场景下，远端流就是老师的流*/
            switch (streamInfo.getVideoSourceType()) {
                case CAMERA:
                    video_teacher.setName(streamInfo.getPublisher().getUserName());
                    video_teacher.muteVideo(!streamInfo.getHasVideo());
                    video_teacher.muteAudio(!streamInfo.getHasAudio());
                    break;
                /**屏幕分享流只有新建和移除，不会有修改行为，所以此处不做处理*/
                default:
                    break;
            }
        }
    }

    @Override
    public void onRemoteStreamsRemoved(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom fromClassRoom) {
        super.onRemoteStreamsRemoved(streamEvents, fromClassRoom);
        for (EduStreamEvent streamEvent : streamEvents) {
            EduStreamInfo streamInfo = streamEvent.getModifiedStream();
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
        EduUserInfo eduUserInfo = userEvent.getModifiedUser();
        video_student.setName(eduUserInfo.getUserName());
    }

    @Override
    public void onLocalStreamAdded(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamAdded(streamEvent);
        localStream = streamEvent.getModifiedStream();
        EduStreamInfo streamInfo = streamEvent.getModifiedStream();
        renderStream(streamInfo, video_student.getVideoLayout());
        video_student.muteVideo(!streamInfo.getHasVideo());
        video_student.muteAudio(!streamInfo.getHasAudio());
        Log.e(TAG, "姓名显示状态1:" + video_student.getTv_name().getVisibility());
    }

    @Override
    public void onLocalStreamUpdated(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamUpdated(streamEvent);
        localStream = streamEvent.getModifiedStream();
        EduStreamInfo streamInfo = streamEvent.getModifiedStream();
        video_student.muteVideo(!streamInfo.getHasVideo());
        video_student.muteAudio(!streamInfo.getHasAudio());
        Log.e(TAG, "姓名显示状态2:" + video_student.getTv_name().getVisibility());
    }

    @Override
    public void onLocalSteamRemoved(@NotNull EduStreamEvent streamEvent) {
        super.onLocalSteamRemoved(streamEvent);
        localStream = null;
        EduStreamInfo streamInfo = streamEvent.getModifiedStream();
        renderStream(streamInfo, null);
        video_student.muteVideo(!streamInfo.getHasVideo());
        video_student.muteAudio(!streamInfo.getHasAudio());
        Log.e(TAG, "姓名显示状态3:" + video_student.getTv_name().getVisibility());
    }
}
