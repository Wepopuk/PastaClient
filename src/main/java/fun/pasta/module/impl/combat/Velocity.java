package fun.pasta.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.network.play.server.SEntityVelocityPacket;
import fun.pasta.event.EventPacket;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;

public class Velocity extends Module {

    public Velocity() {
        super("Velocity", Category.COMBAT);
    }

    public int ticks = 0;

    @Subscribe
    public void onPacket(EventPacket e) {
        if (mc.player == null) return;
        if (e.isreceive()) {
            if (e.packet instanceof SEntityVelocityPacket p && p.getEntityID() != mc.player.getEntityId()) return;

            if (e.packet instanceof SEntityVelocityPacket) {
                e.cancel();
            }
        }
    }
}