package io.agora.education.classroom.bean.group;

import io.agora.education.api.stream.data.EduStreamInfo;

public class StageStreamInfo {
    private EduStreamInfo streamInfo;
    private String reward;

    public StageStreamInfo(EduStreamInfo streamInfo, String reward) {
        this.streamInfo = streamInfo;
        this.reward = reward;
    }

    public EduStreamInfo getStreamInfo() {
        return streamInfo;
    }

    public void setStreamInfo(EduStreamInfo streamInfo) {
        this.streamInfo = streamInfo;
    }

    public String getReward() {
        return reward;
    }

    public void setReward(String reward) {
        this.reward = reward;
    }
}
