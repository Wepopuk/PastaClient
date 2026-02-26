package fun.pasta.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventKey;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BindOption;
import fun.pasta.module.settings.ModeOption;
import fun.pasta.module.settings.SliderOption;
import fun.pasta.util.time.StopWatch;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CCloseWindowPacket;

import java.util.Locale;

public class AutoSwap extends Module {

    // low HP item - high HP item
    private final ModeOption mode = new ModeOption(
            "Mode",
            "Totem-Sphere",
            "Totem-Sphere",
            "Totem-Totem",
            "Sphere-Totem",
            "Sphere-Sphere"
    );

    private final BindOption bind = new BindOption("Bind", 0);
    private final SliderOption delay = new SliderOption("Delay", 80.0F, 0.0F, 400.0F, 10.0F);

    private final StopWatch swapTimer = new StopWatch();

    public AutoSwap() {
        super("AutoSwap", Category.COMBAT);
        add(mode, bind, delay);
    }

    @Subscribe
    public void onKey(EventKey event) {
        if (mc.player == null || mc.world == null || mc.playerController == null || mc.currentScreen != null) {
            return;
        }

        if (event.key != bind.key || bind.key == 0) {
            return;
        }

        if (!swapTimer.isreached((long) delay.getValue().floatValue())) {
            return;
        }

        ItemType left = getLeftType();
        ItemType right = getRightType();
        if (left == null || right == null) {
            return;
        }

        ItemStack offhand = mc.player.getHeldItemOffhand();
        ItemType target = matchesType(offhand, left) ? right : left;

        int slot = findInventorySlot(target);
        if (slot == -1 && target != right) {
            slot = findInventorySlot(right);
        }
        if (slot == -1 && target != left) {
            slot = findInventorySlot(left);
        }
        if (slot == -1) {
            return;
        }

        int containerSlot = slot < 9 ? slot + 36 : slot;
        mc.playerController.windowClick(0, containerSlot, 40, ClickType.SWAP, mc.player);
        if (mc.player.connection != null) {
            mc.player.connection.sendPacket(new CCloseWindowPacket(0));
        }
        swapTimer.reset();
    }

    private ItemType getLeftType() {
        switch (mode.get()) {
            case "Totem-Sphere":
            case "Totem-Totem":
                return ItemType.TOTEM;
            case "Sphere-Totem":
            case "Sphere-Sphere":
                return ItemType.SPHERE;
            default:
                return ItemType.TOTEM;
        }
    }

    private ItemType getRightType() {
        switch (mode.get()) {
            case "Totem-Sphere":
            case "Sphere-Sphere":
                return ItemType.SPHERE;
            case "Totem-Totem":
            case "Sphere-Totem":
                return ItemType.TOTEM;
            default:
                return ItemType.SPHERE;
        }
    }

    private int findInventorySlot(ItemType type) {
        int enchantedTotem = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (!matchesType(stack, type)) {
                continue;
            }

            if (type == ItemType.TOTEM && stack.getItem() == Items.TOTEM_OF_UNDYING && stack.isEnchanted()) {
                if (enchantedTotem == -1) {
                    enchantedTotem = i;
                }
                continue;
            }

            return i;
        }
        return enchantedTotem;
    }

    private boolean matchesType(ItemStack stack, ItemType type) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        String name = stack.getDisplayName().getString();
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);

        switch (type) {
            case TOTEM:
                return stack.getItem() == Items.TOTEM_OF_UNDYING
                        || lower.contains("\u0442\u0430\u043b\u0438\u0441\u043c\u0430\u043d")
                        || lower.contains("talisman")
                        || lower.contains("totem");
            case SPHERE:
                return stack.getItem() == Items.PLAYER_HEAD
                        || lower.contains("\u0448\u0430\u0440")
                        || lower.contains("sphere")
                        || lower.contains("orb");
            default:
                return false;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        swapTimer.reset();
    }

    private enum ItemType {
        TOTEM,
        SPHERE
    }
}
