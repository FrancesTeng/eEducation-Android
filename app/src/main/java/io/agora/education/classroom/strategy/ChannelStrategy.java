package io.agora.education.classroom.strategy;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import io.agora.base.Callback;
import io.agora.education.classroom.bean.channel.ChannelInfo;
import io.agora.education.classroom.bean.channel.Room;
import io.agora.education.classroom.bean.channel.User;
import io.agora.log.LogManager;
import io.agora.sdk.listener.RtcEventListener;
import io.agora.sdk.manager.RtcManager;

public abstract class ChannelStrategy<T> {

    private final LogManager log = new LogManager(this.getClass().getSimpleName());

    private String channelId;
    private ChannelInfo channelInfo;
    private List<Integer> rtcUsers;
    ChannelEventListener channelEventListener;

    ChannelStrategy(String channelId, User local) {
        this.channelId = channelId;
        this.channelInfo = new ChannelInfo(local);
        this.rtcUsers = new ArrayList<>();
        RtcManager.instance().registerListener(rtcEventListener);
    }

    public String getChannelId() {
        return channelId;
    }

    public Room getRoom() {
        return channelInfo.room;
    }

    public void setRoom(Room room) {
        String json = room.toJsonString();
        if (TextUtils.equals(json, new Gson().toJson(getRoom()))) {
            return;
        }
        log.d("setRoom %s", json);
        channelInfo.room = room;
        if (channelEventListener != null) {
            channelEventListener.onRoomChanged(getRoom());
        }
    }

    public User getLocal() {
        try {
            return channelInfo.local.clone();
        } catch (CloneNotSupportedException e) {
            return new User(true);
        }
    }

    void setLocal(User local) {
        String json = local.toJsonString();
        if (getLocal().isGenerate == local.isGenerate && TextUtils.equals(json, getLocal().toJsonString())) {
            return;
        }
        log.d("setLocal %s", json);
        channelInfo.local = local;
        if (channelEventListener != null) {
            channelEventListener.onLocalChanged(getLocal());
        }
    }

    @Nullable
    public User getTeacher() {
        return channelInfo.teacher;
    }

    protected void setTeacher(User teacher) {
        String json = teacher.toJsonString();
        if (TextUtils.equals(json, new Gson().toJson(getTeacher()))) {
            return;
        }
        log.d("setTeacher %s", json);
        channelInfo.teacher = teacher;
        checkRtcOnline(channelInfo.teacher);
        if (channelEventListener != null) {
            channelEventListener.onTeacherChanged(getTeacher());
        }
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        if (getTeacher() != null) {
            users.add(getTeacher());
        }
        users.addAll(getAllStudents());
        return users;
    }

    public List<User> getAllStudents() {
        List<User> students = new ArrayList<>();
        if (!getLocal().isGenerate)
            students.add(0, getLocal());
        students.addAll(getStudents());
        return students;
    }

    public List<User> getStudents() {
        return channelInfo.students;
    }

    void setStudents(List<User> students) {
        Gson gson = new Gson();
        String json = gson.toJson(students);
        if (TextUtils.equals(json, gson.toJson(getStudents()))) {
            return;
        }
        log.d("setStudents %s", json);
        channelInfo.students.clear();
        channelInfo.students.addAll(students);
        checkStudentsRtcOnline();
    }

    private void checkRtcOnline(User user) {
        user.disableCoVideo(!rtcUsers.contains(user.uid));
    }

    private void checkStudentsRtcOnline() {
        for (User student : getStudents()) {
            checkRtcOnline(student);
        }
        if (channelEventListener != null) {
            channelEventListener.onStudentsChanged(getStudents());
        }
    }

    public void setChannelEventListener(ChannelEventListener listener) {
        channelEventListener = listener;
    }

    public void release() {
        channelEventListener = null;
        RtcManager.instance().unregisterListener(rtcEventListener);
    }

    public abstract void joinChannel();

    public abstract void leaveChannel();

    public abstract void queryOnlineStudentNum(@NonNull Callback<Integer> callback);

    public abstract void queryChannelInfo(@Nullable Callback<Void> callback);

    public abstract void parseChannelInfo(T data);

    public abstract void updateLocalAttribute(User local, @Nullable Callback<Void> callback);

    public abstract void clearLocalAttribute(@Nullable Callback<Void> callback);

    private RtcEventListener rtcEventListener = new RtcEventListener() {
        @Override
        public void onUserJoined(int uid, int elapsed) {
            User teacher = getTeacher();
            if (teacher != null) {
                if (uid != teacher.screenId) {
                    rtcUsers.add(uid);
                    checkStudentsRtcOnline();
                }
            }
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            User teacher = getTeacher();
            if (teacher != null) {
                if (uid != teacher.screenId) {
                    rtcUsers.remove(Integer.valueOf(uid));
                    checkStudentsRtcOnline();
                }
            }
        }
    };

}
