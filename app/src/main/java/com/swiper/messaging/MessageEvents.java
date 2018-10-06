package com.swiper.messaging;

public class MessageEvents {
    public static class SwipeEvent {
        public final int count;

        public SwipeEvent(int count) {
            this.count = count;
        }
    }

    public static class PauseEvent {
        public PauseEvent() {
        }
    }

    public static class PlayEvent {
        public PlayEvent() {
        }
    }

    public static class LogoutEvent {
        public LogoutEvent() {
        }
    }
}
