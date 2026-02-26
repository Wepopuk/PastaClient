package fun.pasta.event;

import net.minecraft.network.IPacket;

public class EventReceivePacket extends Event {

    private final IPacket<?> packet; // пакет, который мы получаем

    public EventReceivePacket(IPacket<?> packet) {
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
