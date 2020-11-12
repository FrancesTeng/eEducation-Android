package io.agora.education.classroom;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.herewhite.sdk.domain.GlobalState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import io.agora.education.R;
import io.agora.education.api.EduCallback;
import io.agora.education.api.base.EduError;
import io.agora.education.api.message.EduActionMessage;
import io.agora.education.api.message.EduChatMsg;
import io.agora.education.api.message.EduMsg;
import io.agora.education.api.room.EduRoom;
import io.agora.education.api.room.data.EduRoomChangeType;
import io.agora.education.api.statistics.ConnectionState;
import io.agora.education.api.statistics.NetworkQuality;
import io.agora.education.api.stream.data.EduStreamEvent;
import io.agora.education.api.stream.data.EduStreamInfo;
import io.agora.education.api.stream.data.EduStreamStateChangeType;
import io.agora.education.api.user.EduStudent;
import io.agora.education.api.user.data.EduBaseUserInfo;
import io.agora.education.api.user.data.EduLocalUserInfo;
import io.agora.education.api.user.data.EduUserEvent;
import io.agora.education.api.user.data.EduUserInfo;
import io.agora.education.api.user.data.EduUserRole;
import io.agora.education.api.user.data.EduUserStateChangeType;
import io.agora.education.classroom.bean.board.BoardBean;
import io.agora.education.classroom.bean.board.BoardInfo;
import io.agora.education.classroom.bean.channel.Room;
import io.agora.education.classroom.bean.group.GroupInfo;
import io.agora.education.classroom.bean.group.GroupState;
import io.agora.education.classroom.bean.group.GroupStateInfo;
import io.agora.education.classroom.bean.group.InteractState;
import io.agora.education.classroom.bean.group.RoomGroupInfo;
import io.agora.education.classroom.fragment.ChatRoomFragment;
import io.agora.education.classroom.fragment.StudentGroupListFragment;
import io.agora.education.classroom.fragment.StudentListFragment;
import io.agora.education.classroom.widget.RtcVideoView;
import io.agora.raisehand.AgoraEduCoVideoView;

import static io.agora.education.EduApplication.getAppId;
import static io.agora.education.classroom.bean.board.BoardBean.BOARD;
import static io.agora.education.classroom.bean.group.RoomGroupInfo.GROUPS;
import static io.agora.education.classroom.bean.group.RoomGroupInfo.GROUPSTATES;
import static io.agora.education.classroom.bean.group.RoomGroupInfo.INTERACTOUTGROUPS;

public class IntermediateClassActivity extends BaseClassActivity implements TabLayout.OnTabSelectedListener {
    private static final String TAG = IntermediateClassActivity.class.getSimpleName();

    @BindView(R.id.layout_video_teacher)
    FrameLayout layoutVideoTeacher;
    @BindView(R.id.pk_videos_one)
    RecyclerView pkVideosOne;
    @BindView(R.id.pk_videos_two)
    RecyclerView pkVideosTwo;
    @BindView(R.id.coVideoView)
    AgoraEduCoVideoView agoraEduCoVideoView;
    @BindView(R.id.layout_tab)
    TabLayout tabLayout;

    private RtcVideoView videoTeacher;
    private StudentListFragment studentListFragment;
    private StudentGroupListFragment studentGroupListFragment;
    /*分组讨论时，本地用户所处的小组房间对象*/
    private EduRoom curGroupRoom;
    /*当前班级的分组情况*/
    private RoomGroupInfo roomGroupInfo = new RoomGroupInfo();

    @Override
    protected int getClassType() {
        return Room.Type.INTERMEDIATE;
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_intermediate_class;
    }

    @Override
    protected void initData() {
        super.initData();
        joinRoom(getMainEduRoom(), roomEntry.getUserName(), roomEntry.getUserUuid(), true, false, true,
                new EduCallback<EduStudent>() {
                    @Override
                    public void onSuccess(@org.jetbrains.annotations.Nullable EduStudent res) {
                        runOnUiThread(() -> {
                            showFragmentWithJoinSuccess();
                            /*disable operation in intermediateClass`s mainClass*/
                            whiteboardFragment.disableDeviceInputs(true);
                            whiteboardFragment.setWritable(false);
                        });
                    }

                    @Override
                    public void onFailure(@NotNull EduError error) {
                        joinFailed(error.getType(), error.getMsg());
                    }
                });
    }

    @Override
    protected void initView() {
        super.initView();

        tabLayout.addOnTabSelectedListener(this);

        if (videoTeacher == null) {
            videoTeacher = new RtcVideoView(this);
            videoTeacher.init(R.layout.layout_video_large_class, false);
        }
        removeFromParent(videoTeacher);
        layoutVideoTeacher.addView(videoTeacher, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        studentListFragment = new StudentListFragment(roomEntry.getUserUuid());
        getSupportFragmentManager().beginTransaction()
                .add(R.id.layout_chat_room, studentListFragment)
                .show(studentListFragment)
                .hide(studentListFragment)
                .commitNowAllowingStateLoss();

        studentGroupListFragment = new StudentGroupListFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.layout_chat_room, studentGroupListFragment)
                .show(studentGroupListFragment)
                .hide(studentGroupListFragment)
                .commitNowAllowingStateLoss();
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (tab.getPosition() == 0) {
            Fragment fragment = roomGroupInfo.enableGroup() ? studentGroupListFragment : studentListFragment;
            transaction.show(fragment).hide(chatRoomFragment);
        } else {
            transaction.show(chatRoomFragment).hide(studentListFragment).hide(studentGroupListFragment);
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
    protected void showFragmentWithJoinSuccess() {
        super.showFragmentWithJoinSuccess();
        getSupportFragmentManager().beginTransaction()
                .hide(chatRoomFragment)
                .commitNowAllowingStateLoss();
    }

    private void switchUserFragment(boolean showGroup) {
        if ((showGroup && studentGroupListFragment.isVisible()) ||
                (!showGroup && studentListFragment.isVisible())) {
            return;
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (showGroup) {
            transaction = transaction.show(studentGroupListFragment)
                    .hide(studentListFragment);
        } else {
            transaction = transaction.show(studentListFragment)
                    .hide(studentGroupListFragment);
        }
        transaction.commitNowAllowingStateLoss();
    }

    private void showTeacherStream(EduStreamInfo stream, FrameLayout viewGroup) {
        switch (stream.getVideoSourceType()) {
            case CAMERA:
                videoTeacher.setName(stream.getPublisher().getUserName());
                renderStream(getMainEduRoom(), stream, viewGroup);
                videoTeacher.muteVideo(!stream.getHasVideo());
                videoTeacher.muteAudio(!stream.getHasAudio());
                break;
            case SCREEN:
                runOnUiThread(() -> {
                    layout_whiteboard.setVisibility(View.GONE);
                    layout_share_video.setVisibility(View.VISIBLE);
                    layout_share_video.removeAllViews();
                    renderStream(getMainEduRoom(), stream, layout_share_video);
                });
                break;
            default:
                break;
        }
    }

    private void initBoard(BoardBean boardBean) {
        BoardInfo info = boardBean.getInfo();
        getLocalUserInfo(new EduCallback<EduUserInfo>() {
            @Override
            public void onSuccess(@Nullable EduUserInfo userInfo) {
                runOnUiThread(() -> whiteboardFragment.initBoardWithRoomToken(
                        info.getBoardId(), info.getBoardToken(), userInfo.getUserUuid()));
            }

            @Override
            public void onFailure(@NotNull EduError error) {
            }
        });
    }

    private void parseRoomGroupProperty(Map<String, Object> roomProperties) {
        String groupStatesJson = getProperty(roomProperties, GROUPSTATES);
        if (!TextUtils.isEmpty(groupStatesJson)) {
            GroupStateInfo groupStateInfo = new Gson().fromJson(groupStatesJson, GroupStateInfo.class);
            roomGroupInfo.setGroupStates(groupStateInfo);
        }
        String interactOutGroupsJson = getProperty(roomProperties, INTERACTOUTGROUPS);
        if (!TextUtils.isEmpty(interactOutGroupsJson)) {
            List<String> interactOutGroups = new Gson().fromJson(interactOutGroupsJson,
                    new TypeToken<List<String>>() {
                    }.getType());
            roomGroupInfo.setInteractOutGroups(interactOutGroups);
        }
        String groupsJson = getProperty(roomProperties, GROUPS);
        if (!TextUtils.isEmpty(groupsJson)) {
            List<GroupInfo> groups = new Gson().fromJson(groupsJson,
                    new TypeToken<List<GroupInfo>>() {
                    }.getType());
            roomGroupInfo.setGroups(groups);
        }
    }

    private void getCurAllStudent(EduCallback<List<EduUserInfo>> callback) {
        getCurFullUser(new EduCallback<List<EduUserInfo>>() {
            @Override
            public void onSuccess(@Nullable List<EduUserInfo> res) {
                if (res != null) {
                    List<EduUserInfo> students = new ArrayList<>();
                    Iterator<EduUserInfo> iterator = res.iterator();
                    while (iterator.hasNext()) {
                        EduUserInfo element = iterator.next();
                        if (element.getRole().equals(EduUserRole.STUDENT)) {
                            students.add(element);
                        }
                    }
                    callback.onSuccess(students);
                } else {
                    callback.onFailure(EduError.Companion.customMsgError("current room no stream!"));
                }
            }

            @Override
            public void onFailure(@NotNull EduError error) {
                callback.onFailure(error);
            }
        });
    }

    /**
     * 显示用户列表
     * 分为分组显示和直接列表显示
     */
    private void notifyUserList() {
        if (roomGroupInfo.enableGroup()) {
            /*开启了分组，需要分组显示学生列表*/
            switchUserFragment(true);
            //TODO 显示分组列表
//            List<GroupInfo> groupInfos = new ArrayList<>();
//            List<String> groupUuids = new ArrayList<>();
//            groupUuids.add("123");
//            groupUuids.add("456");
//            groupUuids.add("789");
//            for (int i = 0; i < 5; i++) {
//                GroupInfo groupInfo = new GroupInfo("123-" + i, "组" + i, 6, 6, groupUuids,
//                        new HashMap<>(), 123456789);
//                groupInfos.add(groupInfo);
//            }
//            studentGroupListFragment.updateGroupList(groupInfos);
        } else {
            /*未开启分组，直接列表显示学生*/
            switchUserFragment(false);
            getCurAllStudent(new EduCallback<List<EduUserInfo>>() {
                @Override
                public void onSuccess(@Nullable List<EduUserInfo> res) {
                    studentListFragment.setStudentList(res);
                }

                @Override
                public void onFailure(@NotNull EduError error) {
                }
            });
        }
    }

    private void notifyPKVideoList() {
        if (roomGroupInfo.enablePK()) {
            /*正在PK*/
            List<GroupInfo> groupInfos = roomGroupInfo.getGroups();
            List<String> pkGroupIds = roomGroupInfo.getInteractOutGroups();
            List<GroupInfo> pkGroups = new ArrayList<>(2);
            Iterator<GroupInfo> iterator = groupInfos.iterator();
            while (iterator.hasNext()) {
                GroupInfo element = iterator.next();
                if (pkGroupIds.contains(element.getGroupUuid())) {
                    pkGroups.add(element);
                }
            }
            //TODO 显示PK列表
        }
    }

    @Override
    public void onRemoteUsersInitialized(@NotNull List<? extends EduUserInfo> users, @NotNull EduRoom classRoom) {
        if (classRoom.equals(getMainEduRoom())) {
            /*初始化举手连麦组件*/
            agoraEduCoVideoView.init(getMainEduRoom());
            /*判断班级中的roomProperties中是否有白板信息，如果没有，发起请求,等待RTM通知*/
            if (mainBoardBean == null) {
                Log.e(TAG, "请求大房间的白板信息");
                getLocalUserInfo(new EduCallback<EduUserInfo>() {
                    @Override
                    public void onSuccess(@Nullable EduUserInfo userInfo) {
                        requestBoardInfo(((EduLocalUserInfo) userInfo).getUserToken(),
                                getAppId(), roomEntry.getRoomUuid());
                    }

                    @Override
                    public void onFailure(@NotNull EduError error) {
                    }
                });
            } else {
                initBoard(mainBoardBean);
            }
            /*获取班级的roomProperties中可能存在的分组信息*/
            Map<String, Object> roomProperties = getMainEduRoom().getRoomProperties();
            parseRoomGroupProperty(roomProperties);
            notifyUserList();
            notifyPKVideoList();
            /*刷新title数据*/
            setTitleData();
        }
    }

    @Override
    public void onRemoteUsersJoined(@NotNull List<? extends EduUserInfo> users, @NotNull EduRoom classRoom) {
        super.onRemoteUsersJoined(users, classRoom);
        notifyUserList();
    }

    @Override
    public void onRemoteUserLeft(@NotNull EduUserEvent userEvent, @NotNull EduRoom classRoom) {
        super.onRemoteUserLeft(userEvent, classRoom);
        notifyUserList();
    }

    @Override
    public void onRemoteUserUpdated(@NotNull EduUserEvent userEvent, @NotNull EduUserStateChangeType type,
                                    @NotNull EduRoom classRoom) {
        super.onRemoteUserUpdated(userEvent, type, classRoom);
        notifyUserList();
    }

    @Override
    public void onRoomMessageReceived(@NotNull EduMsg message, @NotNull EduRoom classRoom) {
        super.onRoomMessageReceived(message, classRoom);
    }

    @Override
    public void onRoomChatMessageReceived(@NotNull EduChatMsg eduChatMsg, @NotNull EduRoom classRoom) {
        super.onRoomChatMessageReceived(eduChatMsg, classRoom);
    }

    @Override
    public void onRemoteStreamsInitialized(@NotNull List<? extends EduStreamInfo> streams, @NotNull EduRoom classRoom) {
        if (classRoom.equals(getMainEduRoom())) {
            /*显示老师的流*/
            for (EduStreamInfo streamInfo : streams) {
                EduBaseUserInfo publisher = streamInfo.getPublisher();
                if (publisher.getRole().equals(EduUserRole.TEACHER)) {
                    showTeacherStream(streamInfo, videoTeacher.getVideoLayout());
                }
            }
        }
    }

    @Override
    public void onRemoteStreamsAdded(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom classRoom) {
        if (classRoom.equals(getMainEduRoom())) {
            super.onRemoteStreamsAdded(streamEvents, classRoom);
            for (EduStreamEvent streamEvent : streamEvents) {
                EduStreamInfo streamInfo = streamEvent.getModifiedStream();
                EduBaseUserInfo userInfo = streamInfo.getPublisher();
                if (userInfo.getRole().equals(EduUserRole.TEACHER)) {
                    showTeacherStream(streamInfo, videoTeacher.getVideoLayout());
                }
            }
        }
    }

    @Override
    public void onRemoteStreamUpdated(@NotNull EduStreamEvent streamEvent, @NotNull EduStreamStateChangeType type, @NotNull EduRoom classRoom) {
        if (classRoom.equals(getMainEduRoom())) {
            super.onRemoteStreamUpdated(streamEvent, type, classRoom);
            EduStreamInfo streamInfo = streamEvent.getModifiedStream();
            EduBaseUserInfo userInfo = streamInfo.getPublisher();
            if (userInfo.getRole().equals(EduUserRole.TEACHER)) {
                showTeacherStream(streamInfo, videoTeacher.getVideoLayout());
            }
        }
    }

    @Override
    public void onRemoteStreamsRemoved(@NotNull List<EduStreamEvent> streamEvents, @NotNull EduRoom classRoom) {
        if (classRoom.equals(getMainEduRoom())) {
            super.onRemoteStreamsRemoved(streamEvents, classRoom);
            for (EduStreamEvent streamEvent : streamEvents) {
                EduStreamInfo streamInfo = streamEvent.getModifiedStream();
                EduBaseUserInfo userInfo = streamInfo.getPublisher();
                if (userInfo.getRole().equals(EduUserRole.TEACHER)) {
                    showTeacherStream(streamInfo, null);
                }
            }
        }
    }

    @Override
    public void onRoomStatusChanged(@NotNull EduRoomChangeType event, @NotNull EduUserInfo operatorUser, @NotNull EduRoom classRoom) {
        super.onRoomStatusChanged(event, operatorUser, classRoom);
    }

    @Override
    public void onRoomPropertyChanged(@NotNull EduRoom classRoom, @Nullable Map<String, Object> cause) {
        if (classRoom.equals(getMainEduRoom())) {
            Log.e(TAG, "收到大房间的roomProperty改变的数据");
            Map<String, Object> roomProperties = classRoom.getRoomProperties();
            /*处理白板信息*/
            String boardJson = getProperty(roomProperties, BOARD);
            if (!TextUtils.isEmpty(boardJson) && mainBoardBean == null) {
                Log.e(TAG, "首次获取到大房间的白板信息->" + boardJson);
                /*首次获取到白板信息*/
                mainBoardBean = new Gson().fromJson(boardJson, BoardBean.class);
                initBoard(mainBoardBean);
            }
            /*处理分组信息*/
            parseRoomGroupProperty(roomProperties);
            notifyUserList();
            notifyPKVideoList();
        }
    }

    @Override
    public void onRemoteUserPropertyUpdated(@NotNull EduUserInfo userInfo, @NotNull EduRoom classRoom, @Nullable Map<String, Object> cause) {
        /*远端用户Property发生改变*/
    }

    @Override
    public void onNetworkQualityChanged(@NotNull NetworkQuality quality, @NotNull EduUserInfo user, @NotNull EduRoom classRoom) {
        super.onNetworkQualityChanged(quality, user, classRoom);
    }

    @Override
    public void onConnectionStateChanged(@NotNull ConnectionState state, @NotNull EduRoom classRoom) {
        super.onConnectionStateChanged(state, classRoom);
    }

    @Override
    public void onLocalUserUpdated(@NotNull EduUserEvent userEvent, @NotNull EduUserStateChangeType type) {
    }

    @Override
    public void onLocalUserPropertyUpdated(@NotNull EduUserInfo userInfo, @Nullable Map<String, Object> cause) {
        super.onLocalUserPropertyUpdated(userInfo, cause);
        /*本地用户的Property发生改变*/
    }

    @Override
    public void onLocalStreamAdded(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamAdded(streamEvent);
    }

    @Override
    public void onLocalStreamUpdated(@NotNull EduStreamEvent streamEvent, @NotNull EduStreamStateChangeType type) {
        super.onLocalStreamUpdated(streamEvent, type);
    }

    @Override
    public void onLocalStreamRemoved(@NotNull EduStreamEvent streamEvent) {
        super.onLocalStreamRemoved(streamEvent);
    }

    @Override
    public void onUserActionMessageReceived(@NotNull EduActionMessage actionMessage) {
        super.onUserActionMessageReceived(actionMessage);
    }

    @Override
    public void onGlobalStateChanged(GlobalState state) {
        super.onGlobalStateChanged(state);
    }


}
