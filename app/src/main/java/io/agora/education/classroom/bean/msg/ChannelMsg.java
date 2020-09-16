package io.agora.education.classroom.bean.msg;

import androidx.annotation.IntDef;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import io.agora.education.api.message.EduChatMsg;
import io.agora.education.api.user.data.EduUserInfo;
import io.agora.education.classroom.bean.JsonBean;
import io.agora.education.classroom.bean.channel.Room;
import io.agora.education.classroom.bean.channel.User;

public class ChannelMsg extends JsonBean {

    @Cmd
    public int cmd;
    public Object data;

    @IntDef({Cmd.CHAT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Cmd {
        /**
         * simple chat msg
         */
        int CHAT = 1;
    }

    public static class ChatMsg extends EduChatMsg {
        public transient boolean isMe;

        public ChatMsg(@NotNull EduUserInfo fromUser, @NotNull String message, int type) {
            super(fromUser, message, type);
        }
    }

    public static class BreakoutChatMsgContent extends JsonBean {
        private String content;
        private String fromRoomUuid;

        public BreakoutChatMsgContent(String content, String fromRoomUuid) {
            this.content = content;
            this.fromRoomUuid = fromRoomUuid;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getFromRoomUuid() {
            return fromRoomUuid;
        }

        public void setFromRoomUuid(String fromRoomUuid) {
            this.fromRoomUuid = fromRoomUuid;
        }

        @Override
        public String toJsonString() {
            return super.toJsonString();
        }
    }

    public <T> T getMsg(Class<T> tClass) {
        return new Gson().fromJson(new Gson().toJson(data), tClass);
    }

    @Override
    public String toJsonString() {
        return super.toJsonString();
    }

}
