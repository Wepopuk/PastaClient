package fun.pasta.module.impl.player;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventPacket;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.server.SHeldItemChangePacket;

public class ItemSwapFix extends Module {

    public ItemSwapFix() {
        super("ItemSwapFix", Category.PLAYER);
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (!enable || mc.player == null || mc.player.connection == null) {
            return;
        }
        if (!event.isreceive()) {
            return;
        }
        if (!(event.packet instanceof SHeldItemChangePacket)) {
            return;
        }

        mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
        event.cancel();
    }
}

