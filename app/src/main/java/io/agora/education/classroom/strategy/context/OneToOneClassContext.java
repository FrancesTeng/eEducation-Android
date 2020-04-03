package io.agora.education.classroom.strategy.context;

import android.content.Context;

import androidx.annotation.NonNull;

import io.agora.base.Callback;
import io.agora.education.classroom.bean.channel.User;
import io.agora.education.classroom.strategy.ChannelStrategy;
import io.agora.rtc.Constants;
import io.agora.sdk.manager.RtcManager;

import static io.agora.education.classroom.bean.msg.ChannelMsg.UpdateMsg.Cmd.ACCEPT_CO_VIDEO;

public class OneToOneClassContext extends ClassContext {

    private final static int MAX_STUDENT_NUM = 16;

    OneToOneClassContext(Context context, ChannelStrategy strategy) {
        super(context, strategy);
    }

    @Override
    public void checkChannelEnterable(@NonNull Callback<Boolean> callback) {
        channelStrategy.queryChannelInfo(new Callback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                channelStrategy.queryOnlineStudentNum(new Callback<Integer>() {
                    @Override
                    public void onSuccess(Integer integer) {
                        callback.onSuccess(integer < MAX_STUDENT_NUM);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        callback.onFailure(throwable);
                    }
                });
            }

            @Override
            public void onFailure(Throwable throwable) {
                callback.onFailure(throwable);
            }
        });
    }

    @Override
    void preConfig() {
        RtcManager.instance().setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        RtcManager.instance().setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
        RtcManager.instance().enableDualStreamMode(false);
    }

    @Override
    public void onChannelInfoInit() {
        super.onChannelInfoInit();
        if (channelStrategy.getLocal().isGenerate) {
            channelStrategy.updateLocalAttribute(channelStrategy.getLocal(), new Callback<Void>() {
                @Override
                public void onSuccess(Void res) {
                    channelStrategy.getLocal().sendUpdateMsg(ACCEPT_CO_VIDEO);
                }

                @Override
                public void onFailure(Throwable throwable) {
                }
            });
        } else {
            channelStrategy.getLocal().sendUpdateMsg(ACCEPT_CO_VIDEO);
        }
    }

    @Override
    public void onTeacherChanged(User teacher) {
        super.onTeacherChanged(teacher);
        if (classEventListener instanceof OneToOneClassEventListener) {
            runListener(() -> ((OneToOneClassEventListener) classEventListener).onTeacherMediaChanged(teacher));
        }
    }

    @Override
    public void onLocalChanged(User local) {
        super.onLocalChanged(local);
        if (local.isGenerate) return;
        if (classEventListener instanceof OneToOneClassEventListener) {
            runListener(() -> ((OneToOneClassEventListener) classEventListener).onLocalMediaChanged(local));
        }
    }

    public interface OneToOneClassEventListener extends ClassEventListener {
        void onTeacherMediaChanged(User user);

        void onLocalMediaChanged(User user);
    }

}
