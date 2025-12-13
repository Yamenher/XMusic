package com.xapps.media.xmusic.helper;

public interface ServiceCallback {
    
    public static int CALLBACK_COLORS_UPDATE = 1;
    public static int CALLBACK_PROGRESS_UPDATE = 2;
    
    void onServiceEvent(int data);

    class Hub {
        private static ServiceCallback callback;

        public static void set(ServiceCallback cb) {
            callback = cb;
        }

        @SuppressWarnings("unchecked")
        public static void send(int data) {
            if (callback != null) ((ServiceCallback) callback).onServiceEvent(data);
        }
    }
}