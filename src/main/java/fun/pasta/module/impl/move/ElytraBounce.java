package fun.pasta.module.impl.move;

import com.google.common.eventbus.Subscribe;
import net.minecraft.item.Items;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;

public class ElytraBounce extends Module {

    public ElytraBounce() {
        super("Elytra Bounce", Category.MOVE);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;
        if (mc.player.inventory.getStackInSlot(38).getItem() == Items.ELYTRA && mc.player.movementInput.jump) {
            mc.player.movementInput.jump = false;
        }
    }
}
