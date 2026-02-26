package fun.pasta.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import fun.pasta.Pasta;
import fun.pasta.event.EventPacket;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.client.CUseEntityPacket;

public class NoFriendDamage extends Module {

    public NoFriendDamage() {
        super("NoFriendDamage", Category.COMBAT);
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (!event.isSend() || mc.world == null || Pasta.instance.friendManager == null) {
            return;
        }
        if (event.packet instanceof CUseEntityPacket packet && packet.getAction() == CUseEntityPacket.Action.ATTACK) {
            Entity entity = packet.getEntityFromWorld(mc.world);
            if (entity instanceof PlayerEntity && Pasta.instance.friendManager.isFriend(entity)) {
                event.cancel();
            }
        }
    }
}
