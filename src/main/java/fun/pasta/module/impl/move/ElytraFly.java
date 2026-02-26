package fun.pasta.module.impl.move;

import com.google.common.eventbus.Subscribe;
import net.minecraft.item.Items;
import fun.pasta.Pasta;
import fun.pasta.event.EventMotion;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.ModeOption;
import fun.pasta.util.chat.ChatUtil;
import fun.pasta.util.inventory.InventoryUtil;
import fun.pasta.util.time.StopWatch;

public class ElytraFly extends Module {

    public final ModeOption mode = new ModeOption("Mode", "Fireworks", "Fireworks");
    private final StopWatch swapTimer = new StopWatch();

    public ElytraFly() {
        super("Elytra Fly", Category.MOVE);
        add(mode);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;
        if (!"Fireworks".equalsIgnoreCase(mode.get())) return;

        if (!mc.gameSettings.keyBindJump.isKeyDown()) {
            mc.gameSettings.keyBindJump.setPressed(true);
        }

        int fireworkSlot = InventoryUtil.findItem(Items.FIREWORK_ROCKET);
        if (fireworkSlot == -1) {
            ChatUtil.addChatMessage("Elytra Fly needs fireworks.");
            toggle();
            return;
        }

        int elytraSlot = InventoryUtil.findItem(Items.ELYTRA);
        if (elytraSlot == -1) {
            ChatUtil.addChatMessage("Elytra Fly needs an elytra.");
            toggle();
            return;
        }

        if (!mc.player.isOnGround() && mc.player.fallDistance > 0 && !mc.player.isInWater() && !mc.player.isInLava()) {
            if (swapTimer.isreached(500L)) {
                InventoryUtil.swapToChestSlot(elytraSlot);
                InventoryUtil.useItem(Items.FIREWORK_ROCKET);
                swapTimer.reset();
            }
        }
    }

    @Subscribe
    public void onMotion(EventMotion event) {
        if (mc.player == null || !mc.player.isElytraFlying()) return;

        if (Pasta.instance.manager.killAura != null && Pasta.instance.manager.killAura.target != null) {
            event.yaw = Pasta.instance.manager.killAura.getRotationVector().x;
            event.pitch = Pasta.instance.manager.killAura.getRotationVector().y;
        }
    }
}
