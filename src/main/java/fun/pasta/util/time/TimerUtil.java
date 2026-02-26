package fun.pasta.util.time;

public class TimerUtil {
    private long lastMS = 0L;

    public boolean hasReached(long milliseconds) {
        return getCurrentMS() - lastMS >= milliseconds;
    }

    public void reset() {
        lastMS = getCurrentMS();
    }

    public long getCurrentMS() {
        return System.currentTimeMillis();
    }
}