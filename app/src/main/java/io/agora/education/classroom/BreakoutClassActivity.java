package io.agora.education.classroom;

import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import io.agora.base.network.RetrofitManager;
import io.agora.education.R;
import io.agora.education.api.EduCallback;
import io.agora.education.api.message.EduChatMsg;
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
import io.agora.education.api.statistics.ConnectionState;
import io.agora.education.api.statistics.ConnectionStateChangeReason;
import io.agora.education.api.statistics.NetworkQuality;
import io.agora.education.api.stream.data.EduStreamEvent;
import io.agora.education.api.stream.data.EduStreamInfo;
import io.agora.education.api.stream.data.StreamSubscribeOptions;
import io.agora.education.api.stream.data.VideoStreamType;
import io.agora.education.api.user.EduStudent;
import io.agora.education.api.user.data.EduBaseUserInfo;
import io.agora.education.api.user.data.EduUserEvent;
import io.agora.education.api.user.data.EduUserInfo;
import io.agora.education.api.user.data.EduUserRole;
import io.agora.education.classroom.BaseClassActivity;
import io.agora.education.classroom.adapter.ClassVideoAdapter;
import io.agora.education.classroom.bean.channel.Room;
import io.agora.education.classroom.fragment.UserListFragment;
import io.agora.education.classroom.widget.RtcVideoView;


public class BreakoutClassActivity extends BaseClassActivity implements TabLayout.OnTabSelectedListener {
    private static final String TAG = "SmallClassActivity";

    @BindView(R.id.rcv_videos)
    protected RecyclerView rcv_videos;
    @BindView(R.id.layout_im)
    protected View layout_im;
    @BindView(R.id.layout_tab)
    protected TabLayout layout_tab;
    @BindView(R.id.layout_video_teacher)
    FrameLayout layout_video_teacher;

    RtcVideoView video_teacher;

    private ClassVideoAdapter classVideoAdapter;
    private UserListFragment userListFragment;

    private EduRoom subEduRoom;

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_breakout_class;
    }

    @Override
    protected void initData() {
        super.initData();
        /**needUserListener为false,将不会收到大班级中的任何local回调*/
        joinRoom(getMainEduRoom(), roomEntry.getUserName(), roomEntry.getUserUuid(), true, false, true,
                new EduCallback<EduStudent>() {
                    @Override
                    public void onSuccess(@Nullable EduStudent res) {
                        joinSubEduRoom(getMainEduRoom(), roomEntry.getUserUuid(), roomEntry.getUserName());
                    }

                    @Override
                    public void onFailure(int code, @Nullable String reason) {
                        joinFailed(code, reason);
                    }
                });
        classVideoAdapter = new ClassVideoAdapter();
    }

    /**
     * 根据主教室的信息去请求服务端分配一个小教室
     *
     * @param mainRoom 大教室
     * @param userUuid 学生uuid
     */
    private void joinSubEduRoom(EduRoom mainRoom, String userUuid, String userName) {
        mainRoom.allocateGroup(mainRoom.getRoomInfo().getRoomUuid(), userUuid, new EduCallback<EduRoomInfo>() {
            @Override
            public void onSuccess(@Nullable EduRoomInfo res) {
                if (res != null) {
                    RoomCreateOptions createOptions = new RoomCreateOptions(res.getRoomUuid(),
                            res.getRoomUuid(), RoomType.BREAKOUT_CLASS.getValue(), true);
                    subEduRoom = buildEduRoom(createOptions, mainRoom.getRoomInfo().getRoomUuid());
                    joinRoom(getMainEduRoom(), userName, userUuid, true, false, false, new EduCallback<EduStudent>() {
                        @Override
                        public void onSuccess(@Nullable EduStudent res) {
                            runOnUiThread(() -> showFragmentWithJoinSuccess());
                        }

                        @Override
                        public void onFailure(int code, @Nullable String reason) {
                            joinFailed(code, reason);
                        }
                    });
                }
            }

            @Override
            public void onFailure(int code, @Nullable String reason) {
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

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rcv_videos.setLayoutManager(layoutManager);
        rcv_videos.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                if (parent.getChildAdapterPosition(view) > 0) {
                    outRect.left = getResources().getDimensionPixelSize(R.dimen.dp_2_5);
                }
            }
        });
        rcv_videos.setAdapter(classVideoAdapter);
        layout_tab.addOnTabSelectedListener(this);
        userListFragment = new UserListFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.layout_chat_room, userListFragment)
                .show(userListFragment)
                .commitNow();
    }

    @Override
    protected int getClassType() {
        return Room.Type.BREAKOUT;
    }

    @OnClick(R.id.iv_float)
    public void onClick(View view) {
        boolean isSelected = view.isSelected();
        view.setSelected(!isSelected);
        layout_im.setVisibility(isSelected ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (tab.getPosition() == 0) {
            transaction.show(chatRoomFragment).hide(userListFragment);
        } else {
            transaction.show(userListFragment).hide(chatRoomFragment);
        }
        transaction.commitNow();
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
        if (classRoom.equals(subEduRoom)) {
            userListFragment.setUserList(getCurFullUser());
            title_view.setTitle(String.format(Locale.getDefault(), "%s(%d)", getRoomName(), getCurFullUser().size()));
        }
    }

    @Override
    public void onRemoteUsersJoined(@NotNull List<? extends EduUserInfo> users, @NotNull EduRoom classRoom) {
        super.onRemoteUsersJoined(users, classRoom);
        if (classRoom.equals(subEduRoom)) {
            userListFragment.setUserList(getCurFullUser());
            title_view.setTitle(String.format(Locale.getDefault(), "%s(%d)", getRoomName(), getCurFullUser().size()));
        }
    }

    @Override
    public void onRemoteUsersLeft(@NotNull List<EduUserEvent> userEvents, @NotNull EduRoom classRoom) {
        super.onRemoteUsersLeft(userEvents, classRoom);
        if (classRoom.equals(subEduRoom)) {
            userListFragment.setUserList(getCurFullUser());
            title_view.setTitle(String.format(Locale.getDefault(), "%s(%d)", getRoomName(), getCurFullUser().size()));
        }
    }

    @Override
    public void onRemoteUserUpdated(@NotNull List<EduUserEvent> userEvents, @NotNull EduRoom classRoom) {
        super.onRemoteUserUpdated(userEvents, classRoom);
        if (classRoom.equals(subEduRoom)) {
        }
    }

    @Override
    public void onRoomMessageReceived(@NotNull EduMsg message, @NotNull EduRoom classRoom) {
        super.onRoomMessageReceived(message, classRoom);
        if (classRoom.equals(subEduRoom)) {
        }
    }

    @Override
    public void onUserMessageReceived(@NotNull EduMsg message, @NotNull EduRoom classRoom) {
        super.onUserMessageReceived(message, classRoom);
        if (classRoom.equals(subEduRoom)) {
        }
    }

    @Override
    public void onRoomChatMessageReceived(@NotNull EduChatMsg eduChatMsg, @NotNull EduRoom classRoom) {
        super.onRoomChatMessageReceived(eduChatMsg, classRoom);
        if (classRoom.equals(subEduRoom)) {
        }
    }

    @Override
    public void onUserChatMessageReceived(@NotNull EduChatMsg chatMsg, @NotNull EduRoom classRoom) {
        super.onUserChatMessageReceived(chatMsg, classRoom);
        if (classRoom.equals(subEduRoom)) {
        }
    }

    @Override
    public void onRemoteStreamsInitialized(@NotNull List<? extends EduStreamInfo> streams, @NotNull EduRoom classRoom) {
        super.onRemoteStreamsInitialized(streams, classRoom);
        if (classRoom.equals(subEduRoom)) {
            userListFragment.setLocalUserUuid(classRoom.localUser.getUserInfo().getUserUuid());
            classVideoAdapter.setNewList(getCurFullStream());
        } else {
            for (EduStreamInfo streamInfo : streams) {
                EduBaseUserInfo publisher = streamInfo.getPublisher();
                if (publisher.getRole().equals(EduUserRole.TEACHER)) {
                    switch (streamInfo.getVideoSourceType()) {
                        case CAMERA:
                            video_teacher.setName(publisher.getUserName());
                            renderStream(streamInfo, video_teacher.getVideoLayout());
                            video_teacher.muteVideo(!streamInfo.getHasVideo());
                            video_teacher.muteAudio(!streamInfo.getHasAudio());
                            break;
                        case SCREEN:
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
        }
    }

    @Override
    public void onRemoteStreamsAdded(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom classRoom) {
        super.onRemoteStreamsAdded(streamEvents, classRoom);
        if (classRoom.equals(subEduRoom)) {
            boolean notify = false;
            for (EduStreamEvent streamEvent : streamEvents) {
                EduStreamInfo streamInfo = streamEvent.getModifiedStream();
                switch (streamInfo.getVideoSourceType()) {
                    case CAMERA:
                        notify = true;
                        break;
                    default:
                        break;
                }
            }
            /**有远端Camera流添加，刷新视频列表*/
            if (notify) {
                Log.e(TAG, "有远端Camera流添加，刷新视频列表");
                classVideoAdapter.setNewList(getCurFullStream());
            }
        } else {
            for (EduStreamEvent streamEvent : streamEvents) {
                EduStreamInfo streamInfo = streamEvent.getModifiedStream();
                EduBaseUserInfo userInfo = streamInfo.getPublisher();
                if (userInfo.getRole().equals(EduUserRole.TEACHER)) {
                    switch (streamInfo.getVideoSourceType()) {
                        case CAMERA:
                            /**老师的远端流*/
                            video_teacher.setName(userInfo.getUserName());
                            renderStream(streamInfo, video_teacher.getVideoLayout());
                            video_teacher.muteVideo(!streamInfo.getHasVideo());
                            video_teacher.muteAudio(!streamInfo.getHasAudio());
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    @Override
    public void onRemoteStreamsUpdated(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom classRoom) {
        super.onRemoteStreamsUpdated(streamEvents, classRoom);
        if (classRoom.equals(subEduRoom)) {
            boolean notify = false;
            for (EduStreamEvent streamEvent : streamEvents) {
                EduStreamInfo streamInfo = streamEvent.getModifiedStream();
                switch (streamInfo.getVideoSourceType()) {
                    case CAMERA:
                        notify = true;
                        break;
                    default:
                        break;
                }
            }
            /**有远端Camera流添加，刷新视频列表*/
            if (notify) {
                Log.e(TAG, "有远端Camera流被修改，刷新视频列表");
                classVideoAdapter.setNewList(getCurFullStream());
            }
        } else {
            /**老师的屏幕分享流只有新建和移除，不会有修改行为，所以此处的流都是Camera类型的*/
            for (EduStreamEvent streamEvent : streamEvents) {
                EduStreamInfo streamInfo = streamEvent.getModifiedStream();
                EduBaseUserInfo userInfo = streamInfo.getPublisher();
                if (userInfo.getRole().equals(EduUserRole.TEACHER)) {
                    switch (streamInfo.getVideoSourceType()) {
                        case CAMERA:
                            video_teacher.setName(userInfo.getUserName());
                            renderStream(streamInfo, video_teacher.getVideoLayout());
                            video_teacher.muteVideo(!streamInfo.getHasVideo());
                            video_teacher.muteAudio(!streamInfo.getHasAudio());
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    @Override
    public void onRemoteStreamsRemoved(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom classRoom) {
        super.onRemoteStreamsRemoved(streamEvents, classRoom);
        if (classRoom.equals(subEduRoom)) {
            boolean notify = false;
            for (EduStreamEvent streamEvent : streamEvents) {
                EduStreamInfo streamInfo = streamEvent.getModifiedStream();
                switch (streamInfo.getVideoSourceType()) {
                    case CAMERA:
                        notify = true;
                        break;
                    default:
                        break;
                }
            }
            /**有远端Camera流被移除，刷新视频列表*/
            if (notify) {
                Log.e(TAG, "有远端Camera流被移除，刷新视频列表");
                classVideoAdapter.setNewList(getCurFullStream());
            }
        } else {
            for (EduStreamEvent streamEvent : streamEvents) {
                EduStreamInfo streamInfo = streamEvent.getModifiedStream();
                EduBaseUserInfo userInfo = streamInfo.getPublisher();
                if (userInfo.getRole().equals(EduUserRole.TEACHER)) {
                    switch (streamInfo.getVideoSourceType()) {
                        case CAMERA:
                            video_teacher.setName(streamInfo.getPublisher().getUserName());
                            renderStream(streamInfo, null);
                            video_teacher.muteVideo(!streamInfo.getHasVideo());
                            video_teacher.muteAudio(!streamInfo.getHasAudio());
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    @Override
    public void onRoomStatusChanged(@NotNull RoomStatusEvent event, @NotNull EduUserInfo operatorUser, @NotNull EduRoom classRoom) {
        /**不调用父类中的super方法*/
        if (classRoom.equals(subEduRoom)) {
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
    }

    @Override
    public void onRoomPropertyChanged(@NotNull EduRoom classRoom) {
        super.onRoomPropertyChanged(classRoom);
        if (classRoom.equals(subEduRoom)) {
        }
    }

    @Override
    public void onRemoteUserPropertiesUpdated(@NotNull List<EduUserInfo> userInfos, @NotNull EduRoom classRoom) {
        super.onRemoteUserPropertiesUpdated(userInfos, classRoom);
        if (classRoom.equals(subEduRoom)) {
        }
    }

    @Override
    public void onConnectionStateChanged(@NotNull ConnectionState state, @NotNull ConnectionStateChangeReason reason, @NotNull EduRoom classRoom) {
        super.onConnectionStateChanged(state, reason, classRoom);
        if (classRoom.equals(subEduRoom)) {
        }
    }

    @Override
    public void onNetworkQualityChanged(@NotNull NetworkQuality quality, @NotNull EduUserInfo user, @NotNull EduRoom classRoom) {
        super.onNetworkQualityChanged(quality, user, classRoom);
        if (classRoom.equals(subEduRoom)) {
            title_view.setNetworkQuality(quality.getValue());
        }
    }

    @Override
    public void onLocalUserUpdated(@NotNull EduUserEvent userEvent) {
        super.onLocalUserUpdated(userEvent);
        /**更新用户信息*/
        classVideoAdapter.setNewList(getCurFullStream());
        userListFragment.updateLocalStream(getLocalCameraStream());
    }

    @Override
    public void onLocalUserPropertyUpdated(@NotNull EduUserInfo userInfo) {
        super.onLocalUserPropertyUpdated(userInfo);
    }

    @Override
    public void onLocalStreamAdded(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamAdded(streamEvent);
        classVideoAdapter.setNewList(getCurFullStream());
        userListFragment.updateLocalStream(getLocalCameraStream());
    }

    @Override
    public void onLocalStreamUpdated(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamUpdated(streamEvent);
        classVideoAdapter.setNewList(getCurFullStream());
        userListFragment.updateLocalStream(getLocalCameraStream());
    }

    @Override
    public void onLocalStreamRemoved(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamRemoved(streamEvent);
        /**此回调被调用就说明classroom结束，人员退出；所以此回调可以不处理*/
        Log.e(TAG, "本地流被移除:" + streamEvent.getModifiedStream().getStreamUuid());
    }
}
