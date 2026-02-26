package fun.pasta.event;

public class EventCancel {

    private boolean isCancel;

    public void cancel() {
        isCancel = true;
    }
    public void setCancel(boolean cancel) {
        this.isCancel = cancel;
    }
    public boolean isCancel() {
        return isCancel;
    }
    public void open() {
        isCancel = false;
    }
}