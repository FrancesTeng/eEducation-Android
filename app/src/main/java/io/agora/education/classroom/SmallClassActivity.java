package io.agora.education.classroom;

import android.graphics.Rect;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;
import io.agora.education.R;
import io.agora.education.api.EduCallback;
import io.agora.education.api.message.EduActionMessage;
import io.agora.education.api.message.EduActionType;
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
import io.agora.education.api.user.EduStudent;
import io.agora.education.api.user.data.EduBaseUserInfo;
import io.agora.education.api.user.data.EduStartActionConfig;
import io.agora.education.api.user.data.EduStopActionConfig;
import io.agora.education.api.user.data.EduUserEvent;
import io.agora.education.api.user.data.EduUserInfo;
import io.agora.education.api.user.data.EduUserRole;
import io.agora.education.classroom.adapter.ClassVideoAdapter;
import io.agora.education.classroom.bean.channel.Room;
import io.agora.education.classroom.fragment.UserListFragment;
import kotlin.Unit;

public class SmallClassActivity extends BaseClassActivity implements TabLayout.OnTabSelectedListener {
    private static final String TAG = "SmallClassActivity";

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
        joinRoom(getMainEduRoom(), roomEntry.getUserName(), roomEntry.getUserUuid(), true,
                AutoPublishItem.AutoPublish, true,
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
                .show(userListFragment)
                .commitNow();

        findViewById(R.id.send1).setOnClickListener((v) -> {
            Map<String, String> map = new HashMap<>(1);
            map.put("name", "tom");
            EduBaseUserInfo userInfo = new EduBaseUserInfo("1012", "101", EduUserRole.STUDENT);
            EduStartActionConfig config = new EduStartActionConfig("444",
                    EduActionType.EduActionTypeApply, userInfo, 1000, map);
            getLocalUser().startActionWithConfig(config, new EduCallback<Unit>() {
                @Override
                public void onSuccess(@Nullable Unit res) {
                    Log.e(TAG, "成功1");
                }

                @Override
                public void onFailure(int code, @Nullable String reason) {
                    Log.e(TAG, "失败1");
                }
            });
        });
        findViewById(R.id.send2).setOnClickListener((v) -> {
            Map<String, String> map = new HashMap<>(1);
            map.put("name", "jerry");
            EduStopActionConfig config = new EduStopActionConfig("444", EduActionType.EduActionTypeAccept, map);
            getLocalUser().stopActionWithConfig(config, new EduCallback<Unit>() {
                @Override
                public void onSuccess(@Nullable Unit res) {
                    Log.e(TAG, "成功2");
                }

                @Override
                public void onFailure(int code, @Nullable String reason) {
                    Log.e(TAG, "失败2");
                }
            });
        });
        findViewById(R.id.send3).setOnClickListener((v) -> {
            Map.Entry<String, String> property = new HashMap.SimpleEntry<>("prey", "jerry");
            Map<String, String> cause = new HashMap<>(1);
            cause.put("hunter", "tom");
            getLocalUser().updateRoomProperty(property, cause, new EduCallback<Unit>() {
                @Override
                public void onSuccess(@Nullable Unit res) {
                    Log.e(TAG, "成功3");
                }

                @Override
                public void onFailure(int code, @Nullable String reason) {
                    Log.e(TAG, "失败3");
                }
            });
        });
        findViewById(R.id.send4).setOnClickListener((v) -> {
            Map.Entry<String, String> property = new HashMap.SimpleEntry<>("prey", "jerry");
            Map<String, String> cause = new HashMap<>(1);
            cause.put("hunter", "tom");
            EduUserInfo userInfo = new EduUserInfo("1012", "101", EduUserRole.STUDENT, true);
            getLocalUser().updateUserProperty(property, cause, userInfo, new EduCallback<Unit>() {
                @Override
                public void onSuccess(@Nullable Unit res) {
                    Log.e(TAG, "成功4");
                }

                @Override
                public void onFailure(int code, @Nullable String reason) {
                    Log.e(TAG, "失败4");
                }
            });
        });
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
        userListFragment.setUserList(getCurFullUser());
        title_view.setTitle(String.format(Locale.getDefault(), "%s(%d)", getMediaRoomName(), getCurFullUser().size()));
    }

    @Override
    public void onRemoteUsersJoined(@NotNull List<? extends EduUserInfo> users, @NotNull EduRoom classRoom) {
        super.onRemoteUsersJoined(users, classRoom);
        userListFragment.setUserList(getCurFullUser());
        title_view.setTitle(String.format(Locale.getDefault(), "%s(%d)", getMediaRoomName(), getCurFullUser().size()));
    }

    @Override
    public void onRemoteUsersLeft(@NotNull List<EduUserEvent> userEvents, @NotNull EduRoom classRoom) {
        super.onRemoteUsersLeft(userEvents, classRoom);
        userListFragment.setUserList(getCurFullUser());
        title_view.setTitle(String.format(Locale.getDefault(), "%s(%d)", getMediaRoomName(), getCurFullUser().size()));
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
    public void onUserMessageReceived(@NotNull EduMsg message) {
        super.onUserMessageReceived(message);
    }

    @Override
    public void onRoomChatMessageReceived(@NotNull EduChatMsg eduChatMsg, @NotNull EduRoom classRoom) {
        super.onRoomChatMessageReceived(eduChatMsg, classRoom);
    }

    @Override
    public void onUserChatMessageReceived(@NotNull EduChatMsg chatMsg) {
        super.onUserChatMessageReceived(chatMsg);
    }

    @Override
    public void onRemoteStreamsInitialized(@NotNull List<? extends EduStreamInfo> streams, @NotNull EduRoom classRoom) {
        super.onRemoteStreamsInitialized(streams, classRoom);
        for (EduStreamInfo streamInfo : streams) {
            switch (streamInfo.getVideoSourceType()) {
                case SCREEN:
                    runOnUiThread(() -> {
                        layout_whiteboard.setVisibility(View.GONE);
                        layout_share_video.setVisibility(View.VISIBLE);
                        layout_share_video.removeAllViews();
                        renderStream(getMainEduRoom(), streamInfo, layout_share_video);
                    });
                    break;
                default:
                    break;
            }
        }
        userListFragment.setLocalUserUuid(classRoom.getLocalUser().getUserInfo().getUserUuid());
        classVideoAdapter.setNewList(getCurFullStream());
    }

    @Override
    public void onRemoteStreamsAdded(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom classRoom) {
        super.onRemoteStreamsAdded(streamEvents, classRoom);
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
    }

    @Override
    public void onRemoteStreamsUpdated(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom classRoom) {
        super.onRemoteStreamsUpdated(streamEvents, classRoom);
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
    }

    @Override
    public void onRemoteStreamsRemoved(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom classRoom) {
        super.onRemoteStreamsRemoved(streamEvents, classRoom);
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
    }

    @Override
    public void onRemoteUserPropertiesUpdated(@NotNull List<EduUserInfo> userInfos, @NotNull EduRoom classRoom, @Nullable Map<String, Object> cause) {
        super.onRemoteUserPropertiesUpdated(userInfos, classRoom, cause);
    }

    @Override
    public void onConnectionStateChanged(@NotNull ConnectionState state, @NotNull ConnectionStateChangeReason reason) {
        super.onConnectionStateChanged(state, reason);
    }

    @Override
    public void onNetworkQualityChanged(@NotNull NetworkQuality quality, @NotNull EduUserInfo user, @NotNull EduRoom classRoom) {
        super.onNetworkQualityChanged(quality, user, classRoom);
        title_view.setNetworkQuality(quality.getValue());
    }

    @Override
    public void onLocalUserUpdated(@NotNull EduUserEvent userEvent) {
        super.onLocalUserUpdated(userEvent);
        /**更新用户信息*/
        classVideoAdapter.setNewList(getCurFullStream());
        userListFragment.updateLocalStream(getLocalCameraStream());
    }

    @Override
    public void onLocalUserPropertyUpdated(@NotNull EduUserInfo userInfo, @Nullable Map<String, Object> cause) {
        super.onLocalUserPropertyUpdated(userInfo, cause);
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
        /**小班课场景下，此回调被调用就说明classroom结束，人员退出；所以此回调可以不处理*/
    }

    @Override
    public void onUserActionMessageReceived(@NotNull EduActionMessage actionMessage) {
        super.onUserActionMessageReceived(actionMessage);
        Log.e(TAG, "action->" + new Gson().toJson(actionMessage));
    }
}
