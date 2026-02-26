package fun.pasta.event;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.pasta.Pasta;

public abstract class Event {

    private boolean cancelled = false;

    // Метод для отмены события
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    // Проверка, отменено ли событие
    public boolean isCancelled() {
        return cancelled;
    }

    // Хук для рендера
    public <T extends Event> T hookRender() {
        RenderSystem.runAsFancy(() -> Pasta.getEventManager().call(this));
        return (T) this;
    }

    // Общий хук
    public <T extends Event> T hook() {
        Pasta.getEventManager().call(this);
        return (T) this;
    }
}
