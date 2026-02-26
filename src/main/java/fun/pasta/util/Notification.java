package fun.pasta.util;

public class Notification {

    private final String text;
    private final boolean enabled;
    private final long startTime;
    private final long duration;

    public float anim;
    public float y;
    public boolean yInit;

    public Notification(String text, boolean enabled, long duration) {
        this.text = text;
        this.enabled = enabled;
        this.duration = duration;
        this.startTime = System.currentTimeMillis();
    }

    public String getText() {
        return text;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getDuration() {
        return duration;
    }
}
