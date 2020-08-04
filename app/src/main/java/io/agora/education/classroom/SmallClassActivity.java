package io.agora.education.classroom;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

import butterknife.BindView;
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
import io.agora.education.api.user.data.EduUserEvent;
import io.agora.education.api.user.data.EduUserInfo;
import io.agora.education.classroom.adapter.ClassVideoAdapter;
import io.agora.education.classroom.bean.channel.Room;
import io.agora.education.classroom.bean.channel.User;
import io.agora.education.classroom.bean.msg.ChannelMsg;
import io.agora.education.classroom.fragment.UserListFragment;

public class SmallClassActivity extends BaseClassActivity implements TabLayout.OnTabSelectedListener {

    @BindView(R.id.rcv_videos)
    protected RecyclerView rcv_videos;
    @BindView(R.id.layout_im)
    protected View layout_im;
    @BindView(R.id.layout_tab)
    protected TabLayout layout_tab;

    private ClassVideoAdapter classVideoAdapter;
    private UserListFragment userListFragment;

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_small_class;
    }

    @Override
    protected void initData() {
        super.initData();
        classVideoAdapter = new ClassVideoAdapter();
    }

    @Override
    protected void initView() {
        super.initView();
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
                .hide(userListFragment)
                .commit();
    }

    @Override
    protected int getClassType() {
        return Room.Type.SMALL;
    }

    @OnClick(R.id.iv_float)
    public void onClick(View view) {
        boolean isSelected = view.isSelected();
        view.setSelected(!isSelected);
        layout_im.setVisibility(isSelected ? View.VISIBLE : View.GONE);
    }

//    @Override
//    public void onGrantWhiteboard(boolean granted) {
//        whiteboardFragment.disableDeviceInputs(!granted);
//    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (tab.getPosition() == 0) {
            transaction.show(chatRoomFragment).hide(userListFragment);
        } else {
            transaction.show(userListFragment).hide(chatRoomFragment);
        }
        transaction.commit();
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
        userListFragment.setUserList(getCurFullUser());
        title_view.setTitle(String.format(Locale.getDefault(), "%s(%d)", getRoomName(), getCurFullUser().size()));
    }

    @Override
    public void onRemoteUsersJoined(@NotNull List<? extends EduUserInfo> users, @NotNull EduRoom fromClassRoom) {
        super.onRemoteUsersJoined(users, fromClassRoom);
        userListFragment.setUserList(getCurFullUser());
        title_view.setTitle(String.format(Locale.getDefault(), "%s(%d)", getRoomName(), getCurFullUser().size()));
    }

    @Override
    public void onRemoteUsersLeft(@NotNull List<EduUserEvent> userEvents, @NotNull EduRoom fromClassRoom) {
        super.onRemoteUsersLeft(userEvents, fromClassRoom);
        userListFragment.setUserList(getCurFullUser());
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
        classVideoAdapter.setDiffNewData(getCurFullStream());
    }

    @Override
    public void onRemoteStreamsAdded(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom fromClassRoom) {
        super.onRemoteStreamsAdded(streamEvents, fromClassRoom);
        boolean notify = false;
        for (EduStreamEvent streamEvent : streamEvents) {
            EduStreamInfo streamInfo = streamEvent.getModifiedStream();
            switch (streamInfo.getVideoSourceType()) {
                case CAMERA:
                    notify = true;
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
        /**有远端Camera流被移除，刷新视频列表*/
        if(notify) {
            classVideoAdapter.setDiffNewData(getCurFullStream());
        }
    }

    @Override
    public void onRemoteStreamsUpdated(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom fromClassRoom) {
        super.onRemoteStreamsUpdated(streamEvents, fromClassRoom);
        /**屏幕分享流只有新建和移除，不会有修改行为，所以此处的流都是Camera类型的*/
        classVideoAdapter.setDiffNewData(getCurFullStream());
    }

    @Override
    public void onRemoteStreamsRemoved(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom fromClassRoom) {
        super.onRemoteStreamsRemoved(streamEvents, fromClassRoom);
        boolean notify = false;
        for (EduStreamEvent streamEvent : streamEvents) {
            EduStreamInfo streamInfo = streamEvent.getModifiedStream();
            switch (streamInfo.getVideoSourceType()) {
                case CAMERA:
                    notify = true;
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
        /**有远端Camera流被移除，刷新视频列表*/
        if(notify) {
            classVideoAdapter.setDiffNewData(getCurFullStream());
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
        /**更新用户信息*/
        EduUserInfo userInfo = userEvent.getModifiedUser();
        chatRoomFragment.setMuteLocal(!userInfo.isChatAllowed());
        classVideoAdapter.setDiffNewData(getCurFullStream());
        userListFragment.updateLocalStream(getLocalStream());
    }

    @Override
    public void onLocalStreamAdded(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamAdded(streamEvent);
        classVideoAdapter.setDiffNewData(getCurFullStream());
        userListFragment.updateLocalStream(getLocalStream());
    }

    @Override
    public void onLocalStreamUpdated(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamUpdated(streamEvent);
        classVideoAdapter.setDiffNewData(getCurFullStream());
        userListFragment.updateLocalStream(getLocalStream());
    }

    @Override
    public void onLocalStreamRemoved(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamRemoved(streamEvent);
        /**小班课场景下，此回调被调用就说明classroom结束，人员退出；所以此回调可以不处理*/
    }
}
