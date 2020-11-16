package io.agora.education.classroom.bean.group;

import io.agora.education.api.stream.data.EduStreamInfo;

public class StageStreamInfo {
    private EduStreamInfo streamInfo;
    /**
     * 此流的发送者在组内的信息
     */
    private GroupMemberInfo publisher;

    public StageStreamInfo(EduStreamInfo streamInfo, GroupMemberInfo publisher) {
        this.streamInfo = streamInfo;
        this.publisher = publisher;
    }

    public EduStreamInfo getStreamInfo() {
        return streamInfo;
    }

    public void setStreamInfo(EduStreamInfo streamInfo) {
        this.streamInfo = streamInfo;
    }

    public GroupMemberInfo getPublisher() {
        return publisher;
    }

    public void setPublisher(GroupMemberInfo publisher) {
        this.publisher = publisher;
    }
}
