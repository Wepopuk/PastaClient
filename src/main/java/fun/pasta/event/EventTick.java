package fun.pasta.event;

public class EventTick {
    private final Phase phase;

    // Конструктор по умолчанию
    public EventTick() {
        this.phase = Phase.PRE; // Или Phase.POST
    }

    // Конструктор с параметром
    public EventTick(Phase phase) {
        this.phase = phase;
    }

    public Phase getPhase() {
        return phase;
    }

    public enum Phase {
        PRE, POST
    }
}