package fun.pasta.module.impl.move;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.SliderOption;
import fun.pasta.util.inventory.InventoryUtil;
import fun.pasta.util.time.TimerUtil;
import net.minecraft.item.Items;

public class CatFly extends Module {

    private final SliderOption fireworkDelay = new SliderOption("Firework Delay", 4.0F, 1.0F, 15.0F, 1.0F);
    private final TimerUtil fireworkTimer = new TimerUtil();

    public CatFly() {
        super("CatFly", Category.MOVE);
        add(fireworkDelay);
        fireworkTimer.reset();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!enable || mc.player == null || mc.world == null) {
            return;
        }
        if (!mc.player.isElytraFlying()) {
            return;
        }

        long delayMs = (long) (fireworkDelay.getValue().floatValue() * 400.0F);
        if (!fireworkTimer.hasReached(delayMs)) {
            return;
        }

        if (InventoryUtil.findItem(Items.FIREWORK_ROCKET) != -1) {
            InventoryUtil.useItem(Items.FIREWORK_ROCKET);
        }
        fireworkTimer.reset();
    }
}

