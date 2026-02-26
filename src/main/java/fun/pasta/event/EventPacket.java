package fun.pasta.event;

import net.minecraft.network.IPacket;

public class EventPacket extends EventCancel {

    public IPacket<?> packet;
    public Type type;

    public boolean isSend() {
        return type == Type.SEND;
    }

    public boolean isreceive() {
        return type == Type.RECEIVE;
    }

    public enum Type {
        RECEIVE, SEND
    }

    public EventPacket(IPacket<?> packet, Type type) {
        this.packet = packet;
        this.type = type;
    }
}