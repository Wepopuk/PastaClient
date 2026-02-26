package fun.pasta.event;

public class EventCancelOverlay extends EventCancel {

    public final Overlays overlayType;

    public EventCancelOverlay(Overlays overlays) {
        this.overlayType = overlays;
    }

    public enum Overlays {
        FIRE_OVERLAY, BOSS_LINE, SCOREBOARD, TITLES, TOTEM, FOG, HURT,
    }
}