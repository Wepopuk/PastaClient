package fun.pasta.util.aura;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.network.IPacket;
import net.minecraft.util.MovementInputFromOptions;

public class FakePlayer extends ClientPlayerEntity {

    private static final ClientPlayNetHandler NETWORK_HANDLER = new ClientPlayNetHandler(Minecraft.getInstance(), Minecraft.getInstance().currentScreen, Minecraft.getInstance().getConnection().getNetworkManager(), Minecraft.getInstance().player.getGameProfile()) {
        @Override
        public void sendPacket(IPacket<?> packetIn) {
            super.sendPacket(packetIn);
        }
    };

    public FakePlayer(int entityId) {
        super(Minecraft.getInstance(), Minecraft.getInstance().world, NETWORK_HANDLER, Minecraft.getInstance().player.getStats(), Minecraft.getInstance().player.getRecipeBook(), false, false);

        setEntityId(entityId);
        movementInput = new MovementInputFromOptions(mc.gameSettings);
    }

    public void spawn() {
        if (world != null) {
            world.addEntity(this);
        }
    }

    @Override
    public void livingTick() {
        super.livingTick();
    }

    @Override
    public void rotateTowards(double yaw, double pitch) {
        super.rotateTowards(yaw, pitch);
    }
}