package fun.pasta.module.impl.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.item.Items;
import fun.pasta.event.EventKey;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BindOption;
import fun.pasta.util.inventory.InventoryUtil;

public class ClickPearl extends Module {

    BindOption keyUse = new BindOption("Кнопка", -98);

    public ClickPearl() {
        super("ClickPearl", Category.PLAYER);
        add(keyUse);
    }

    @Subscribe
    public void onUpdate(EventKey event) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;

        if (event.key == keyUse.key) {
            throwPearl();
        }
    }

    private void throwPearl() {
        if (mc.player.getCooldownTracker().hasCooldown(Items.ENDER_PEARL)) return;
        InventoryUtil.useItem(Items.ENDER_PEARL);
    }
}
