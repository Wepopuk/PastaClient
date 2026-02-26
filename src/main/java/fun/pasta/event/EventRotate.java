package fun.pasta.event;

public class EventRotate extends EventCancel {
    public double yaw, pitch;

    public EventRotate(double yaw, double pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }
}