package fun.pasta.util.time;

public class StopWatch {
    public long lastMS = System.currentTimeMillis();
    public void reset() {
        lastMS = System.currentTimeMillis();
    }
    public boolean isreached(long time) {
        return System.currentTimeMillis() - lastMS > time;
    }
    public void setLastMS(long newValue) {
        lastMS = System.currentTimeMillis() + newValue;
    }
    public void setTime(long time) {
        lastMS = time;
    }
    public boolean finished(final double delay) {
        return System.currentTimeMillis() - delay >= lastMS;
    }
    public long getTime() {
        return System.currentTimeMillis() - lastMS;
    }
    public boolean isrunning() {
        return System.currentTimeMillis() - lastMS <= 0;
    }
    public boolean hasTimeElapsed() {
        return lastMS < System.currentTimeMillis();
    }
    public long getRemainingSeconds() {
        long remaining = lastMS - System.currentTimeMillis();
        return remaining > 0 ? remaining / 1000 : 0;
    }

    public long getRemMS() {
        long remaining = lastMS - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
}