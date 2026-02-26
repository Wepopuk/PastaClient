package fun.pasta.event;

import net.minecraft.network.IPacket;

public class EventSendPacket extends Event {

    private final IPacket<?> packet; // пакет, который мы отправляем

    public EventSendPacket(IPacket<?> packet) {
        this.packet = packet;
    }

    // Геттер для пакета
    public IPacket<?> getPacket() {
        return packet;
    }

    // Можно отменить событие
    @Override
    public void setCancelled(boolean cancelled) {
        super.setCancelled(cancelled);
    }
}
