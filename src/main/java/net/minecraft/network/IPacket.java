package net.minecraft.network;

import net.minecraft.client.Minecraft;

import java.io.IOException;

public interface IPacket<T extends INetHandler>
{
    /**
     * Reads the raw packet data from the data stream.
     */
    void readPacketData(PacketBuffer buf) throws IOException;

    /**
     * Writes the raw packet data to the data stream.
     */
    void writePacketData(PacketBuffer buf) throws IOException;

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    void processPacket(T handler);

default boolean shouldSkipErrors()
    {
        return false;
    }

    default boolean sendSilent() {
        if (this != null) {
            Minecraft.getInstance().getConnection().sendPacket(this);
            return true;
        }
        return false;
    }
}
