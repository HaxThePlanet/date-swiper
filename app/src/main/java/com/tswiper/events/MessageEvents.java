package com.tswiper.events;

public class MessageEvents {
    public static class SwipeEvent {
        public final int count;

        public SwipeEvent(int count) {
            this.count = count;
        }
    }

    public static class ButtonPauseEvent {
        public ButtonPauseEvent() {
        }
    }

    public static class ButtonPlayEvent {
        public ButtonPlayEvent() {
        }
    }

    public static class LogoutEvent {
        public LogoutEvent() {
        }
    }

    public static class CloseLoading {
        public CloseLoading() {
        }
    }

    public static class StopWebview {
        public StopWebview() {
        }
    }

    public static class StartWebview {
        public StartWebview() {
        }
    }

    public static class AppPurchased {
        public AppPurchased() {
        }
    }

    public static class AppNotPurchased {
        public AppNotPurchased() {
        }
    }

    public static class CheckBilling {
        public CheckBilling() {
        }
    }
}
