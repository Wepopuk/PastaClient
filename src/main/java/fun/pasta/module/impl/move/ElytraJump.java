package fun.pasta.module.impl.move;

import com.google.common.eventbus.Subscribe;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Items;
import net.minecraft.util.math.vector.Vector2f;
import fun.pasta.Pasta;
import fun.pasta.event.EventFireworkMotion;
import fun.pasta.event.EventInput;
import fun.pasta.event.EventMotion;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.module.settings.ModeOption;
import fun.pasta.util.chat.ChatUtil;
import fun.pasta.util.inventory.InventoryUtil;
import fun.pasta.util.time.StopWatch;

import java.util.concurrent.ThreadLocalRandom;

public class ElytraJump extends Module {

    public final ModeOption mode = new ModeOption("Mode", "Fireworks", "Fireworks", "Exploit");
    private final BooleanOption auto = new BooleanOption("Auto", false).setVisible(() -> "Exploit".equalsIgnoreCase(mode.get()));
    private final BooleanOption bypass = new BooleanOption("Bypass", false).setVisible(() -> "Exploit".equalsIgnoreCase(mode.get()));
    private final StopWatch timer = new StopWatch();

    public ElytraJump() {
        super("Elytra Jump", Category.MOVE);
        add(mode, auto, bypass);
    }

    @Subscribe
    public void onInput(EventInput event) {
        if (mc.player == null || !"Exploit".equalsIgnoreCase(mode.get())) return;

        if (mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST).getItem() == Items.ELYTRA) {
            event.forward = 0;
            event.strafe = 0;
            if (!mc.player.isElytraFlying()) {
                event.jump = false;
            }
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        if ("Exploit".equalsIgnoreCase(mode.get())) {
            if (auto.get() && mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST).getItem() != Items.ELYTRA) {
                int elytraSlot = InventoryUtil.findItem(Items.ELYTRA);
                if (elytraSlot != -1) {
                    InventoryUtil.swapToChestSlot(elytraSlot);
                }
            }

            if (mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST).getItem() == Items.ELYTRA) {
                if (mc.player.isElytraFlying()) {
                    if (bypass.get() && timer.isreached(500L)) {
                        InventoryUtil.useItem(Items.FIREWORK_ROCKET);
                        timer.reset();
                    }
                    mc.player.setMotion(mc.player.getMotion().add(0, ThreadLocalRandom.current().nextDouble(0.06, 0.061), 0));
                } else {
                    mc.gameSettings.keyBindJump.setPressed(true);
                }
            }
            return;
        }

        if (!mc.gameSettings.keyBindJump.isKeyDown()) {
            mc.gameSettings.keyBindJump.setPressed(true);
        }

        int fireworkSlot = InventoryUtil.findItem(Items.FIREWORK_ROCKET);
        if (fireworkSlot == -1) {
            ChatUtil.addChatMessage("Elytra Jump needs fireworks.");
            toggle();
            return;
        }

        int elytraSlot = InventoryUtil.findItem(Items.ELYTRA);
        if (elytraSlot == -1) {
            ChatUtil.addChatMessage("Elytra Jump needs an elytra.");
            toggle();
            return;
        }

        if (!mc.player.isOnGround() && mc.player.fallDistance > 0 && !mc.player.isInWater() && !mc.player.isInLava()) {
            if (timer.isreached(500L)) {
                InventoryUtil.swapToChestSlot(elytraSlot);
                InventoryUtil.useItem(Items.FIREWORK_ROCKET);
                timer.reset();
            }
        }
    }

    @Subscribe
    public void onFirework(EventFireworkMotion event) {
        if (!"Exploit".equalsIgnoreCase(mode.get()) || !bypass.get()) return;
        event.setVector(net.minecraft.util.math.vector.Vector3d.ZERO);
        event.setCancelled(true);
    }

    @Subscribe
    public void onMotion(EventMotion event) {
        if (mc.player == null || !mc.player.isElytraFlying()) return;

        if (Pasta.instance.manager.killAura != null && Pasta.instance.manager.killAura.target != null) {
            Vector2f rot = Pasta.instance.manager.killAura.getRotationVector();
            event.yaw = rot.x;
        } else {
            event.yaw = mc.player.rotationYaw;
        }

        if ("Exploit".equalsIgnoreCase(mode.get())) {
            event.pitch = 0F;
        } else {
            event.pitch = -90F;
        }
    }

    @Override
    public void onDisable() {
        if ("Exploit".equalsIgnoreCase(mode.get()) && auto.get()) {
            int chestSlot = InventoryUtil.findChestplateSlot();
            if (chestSlot != -1) {
                InventoryUtil.swapToChestSlot(chestSlot);
            }
        }
        super.onDisable();
    }
}
