package fun.pasta.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventPacket;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import net.minecraft.network.play.client.CResourcePackStatusPacket;
import net.minecraft.network.play.server.SSendResourcePackPacket;

public class SRpSpoofer extends Module {

    private static final int LOAD_DELAY_TICKS = 500;

    private ResourcePackAction currentAction = ResourcePackAction.WAIT;
    private int counter;

    public SRpSpoofer() {
        super("SRPSpoofer", Category.MISC);
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.player.connection == null || !event.isreceive()) {
            return;
        }

        if (event.packet instanceof SSendResourcePackPacket) {
            event.cancel();
            currentAction = ResourcePackAction.ACCEPT;
            counter = 0;
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.player.connection == null) {
            return;
        }

        if (currentAction == ResourcePackAction.ACCEPT) {
            mc.player.connection.sendPacket(new CResourcePackStatusPacket(CResourcePackStatusPacket.Action.ACCEPTED));
            currentAction = ResourcePackAction.SEND;
            counter = 0;
            return;
        }

        if (currentAction == ResourcePackAction.SEND && ++counter >= LOAD_DELAY_TICKS) {
            mc.player.connection.sendPacket(new CResourcePackStatusPacket(CResourcePackStatusPacket.Action.SUCCESSFULLY_LOADED));
            currentAction = ResourcePackAction.WAIT;
            counter = 0;
        }
    }

    @Override
    public void onDisable() {
        currentAction = ResourcePackAction.WAIT;
        counter = 0;
        super.onDisable();
    }

    private enum ResourcePackAction {
        WAIT,
        ACCEPT,
        SEND
    }
}
