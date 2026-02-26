package fun.pasta.util;

import java.util.ArrayList;
import java.util.List;

public final class NotificationManager {

    private static final List<Notification> NOTIFICATIONS = new ArrayList<>();

    private NotificationManager() {
    }

    public static void push(String text, boolean enabled, long durationMs) {
        if (text == null || text.isEmpty()) {
            return;
        }
        NOTIFICATIONS.add(new Notification(text, enabled, durationMs));
    }

    public static List<Notification> getNotifications() {
        return NOTIFICATIONS;
    }
}
