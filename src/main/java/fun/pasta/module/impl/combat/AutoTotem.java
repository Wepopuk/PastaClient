package fun.pasta.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CCloseWindowPacket;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.SliderOption;
import fun.pasta.util.time.StopWatch;

public class AutoTotem extends Module {

    private final SliderOption healthSetting = new SliderOption("Здоровье", 4.0f, 1.0f, 20.0f, 0.5f);

    private final StopWatch stopWatch = new StopWatch();
    private int oldItem = -1;

    public AutoTotem() {
        super("AutoTotem", Category.COMBAT);
        add(healthSetting);
    }

    @Subscribe
    public void onEvent(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.getHealth() <= healthSetting.getValue().floatValue()) {
            swapToTotem();
        } else if (oldItem != -1) {
            returnOldItem();
        }
    }

    private void swapToTotem() {
        if (mc.player.getHeldItemOffhand().getItem() == Items.TOTEM_OF_UNDYING) return;

        int totemSlot = findTotemSlot();
        if (totemSlot == -1) return;

        if (!stopWatch.isreached(100)) return;

        if (totemSlot < 9) {
            totemSlot += 36;
        }

        ItemStack offhandItem = mc.player.getHeldItemOffhand();
        if (!offhandItem.isEmpty() && oldItem == -1) {
            oldItem = totemSlot;
        }

        mc.playerController.windowClick(0, totemSlot, 40, ClickType.SWAP, mc.player);
        mc.player.connection.sendPacket(new CCloseWindowPacket(0));
        stopWatch.reset();
    }

    private void returnOldItem() {
        if (mc.player.getHealth() > healthSetting.getValue().floatValue()) {
            mc.playerController.windowClick(0, oldItem, 40, ClickType.SWAP, mc.player);
            mc.player.connection.sendPacket(new CCloseWindowPacket(0));
            oldItem = -1;
        }
    }

    private int findTotemSlot() {
        int enchantedTotemSlot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                if (!stack.isEnchanted()) {
                    return i;
                }
                if (enchantedTotemSlot == -1) {
                    enchantedTotemSlot = i;
                }
            }
        }
        return enchantedTotemSlot;
    }

    @Override
    public void onDisable() {
        oldItem = -1;
        super.onDisable();
    }
}
