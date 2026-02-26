package fun.pasta.module.impl.move;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventTick;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.ModeOption;
import fun.pasta.util.IMinecraft;
import net.minecraft.network.play.client.CEntityActionPacket;

public class Sprint extends Module implements IMinecraft {

    public final ModeOption mode = new ModeOption("Mode", "Vanilla", "Vanilla", "Packet");
    private boolean serverSprinting;

    public Sprint() {
        super("Sprint", Category.MOVE);
        add(mode);
    }

    @Subscribe
    public void onTick(EventTick e) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        boolean shouldSprint = canSprint();

        if (mode.get().equalsIgnoreCase("Packet")) {
            handlePacketSprint(shouldSprint);
            return;
        }

        if (shouldSprint) {
            mc.player.setSprinting(true);
        } else if (mc.player.isSprinting()) {
            mc.player.setSprinting(false);
        }
    }

    private boolean canSprint() {
        if (mc.player == null) {
            return false;
        }

        return !mc.player.isSneaking()
                && !mc.player.collidedHorizontally
                && (mc.player.getFoodStats().getFoodLevel() > 6 || mc.player.abilities.isCreativeMode)
                && !mc.player.isInWater()
                && mc.player.moveForward > 0;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        serverSprinting = false;
    }

    @Override
    public void onDisable() {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (serverSprinting) {
            sendSprintPacket(false);
            serverSprinting = false;
        }
        mc.player.setSprinting(false);
        super.onDisable();
    }

    private void handlePacketSprint(boolean shouldSprint) {
        if (shouldSprint) {
            if (!serverSprinting) {
                sendSprintPacket(true);
                serverSprinting = true;
            }
            mc.player.setSprinting(true);
            return;
        }

        if (serverSprinting) {
            sendSprintPacket(false);
            serverSprinting = false;
        }
        if (mc.player.isSprinting()) {
            mc.player.setSprinting(false);
        }
    }

    private void sendSprintPacket(boolean start) {
        if (mc.player.connection == null) {
            return;
        }
        CEntityActionPacket.Action action = start
                ? CEntityActionPacket.Action.START_SPRINTING
                : CEntityActionPacket.Action.STOP_SPRINTING;
        mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, action));
    }
}
