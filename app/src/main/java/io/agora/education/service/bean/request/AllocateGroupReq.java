package io.agora.education.service.bean.request;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AllocateGroupReq {
    @NonNull
    private int memberLimit = 4;
    @Nullable
    private RoleConfig roleConfig;

    public AllocateGroupReq() {
    }

    public class RoleConfig {
        private LimitConfig host;
        private LimitConfig assistant;
        private LimitConfig broadcaster;

        public RoleConfig(LimitConfig host, LimitConfig assistant, LimitConfig broadcaster) {
            this.host = host;
            this.assistant = assistant;
            this.broadcaster = broadcaster;
        }

        public LimitConfig getHost() {
            return host;
        }

        public void setHost(LimitConfig host) {
            this.host = host;
        }

        public LimitConfig getAssistant() {
            return assistant;
        }

        public void setAssistant(LimitConfig assistant) {
            this.assistant = assistant;
        }

        public LimitConfig getBroadcaster() {
            return broadcaster;
        }

        public void setBroadcaster(LimitConfig broadcaster) {
            this.broadcaster = broadcaster;
        }
    }

    public class LimitConfig {
        /**
         * 角色人数上限，-1不限
         */
        private int limit = -1;
        /**
         * 验证类型, 0: 允许匿名, 1: 不允许匿名 ，默认0
         */
        private int verifyType = 0;
        /**
         * 0不订阅 1订阅，默认1
         */
        private int subscribe = 1;

        public LimitConfig(int limit) {
            this.limit = limit;
        }

        public LimitConfig(int limit, int verifyType, int subscribe) {
            this.limit = limit;
            this.verifyType = verifyType;
            this.subscribe = subscribe;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public int getVerifyType() {
            return verifyType;
        }

        public void setVerifyType(int verifyType) {
            this.verifyType = verifyType;
        }

        public int getSubscribe() {
            return subscribe;
        }

        public void setSubscribe(int subscribe) {
            this.subscribe = subscribe;
        }
    }

    public static class VerifyType {
        public final int Allow = 0;
        public final int NotAllow = 1;
    }

    public static class SubscribeType {
        public final int NotSubscribe = 0;
        public final int Subscribe = 1;
    }
}
