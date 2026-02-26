package fun.pasta.module.impl.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CEntityActionPacket;
import fun.pasta.event.EventKey;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BindOption;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.util.NotificationManager;
import fun.pasta.util.inventory.InventoryUtil;

public class ElytraHelper extends Module {

    public final BindOption swapKey = new BindOption("Swap Elytra", 0);
    public final BindOption fireworkKey = new BindOption("Firework", 0);
    public final BooleanOption auto = new BooleanOption("Auto", false);

    public ElytraHelper() {
        super("Elytra Helper", Category.PLAYER);
        add(swapKey, fireworkKey, auto);
    }

    @Subscribe
    public void onKey(EventKey event) {
        if (mc.player == null || mc.world == null) return;

        if (event.key == swapKey.key) {
            swap();
        }

        if (event.key == fireworkKey.key && mc.player.isElytraFlying()) {
            InventoryUtil.useItem(Items.FIREWORK_ROCKET);
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null || !auto.get()) return;

        if (mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST).getItem() == Items.ELYTRA
                && !mc.player.isInLava()
                && !mc.player.isInWater()
                && mc.player.isOnGround()
                && !mc.gameSettings.keyBindJump.isKeyDown()) {
            mc.player.jump();
        } else if (ElytraItem.isUsable(Items.ELYTRA.getDefaultInstance())
                && mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST).getItem() == Items.ELYTRA
                && !mc.player.isElytraFlying()
                && !mc.player.isOnGround()) {
            mc.player.startFallFlying();
            if (mc.player.connection != null) {
                mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.START_FALL_FLYING));
            }
        }
    }

    private void swap() {
        if (mc.player == null) return;

        boolean elytraOnChest = mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST).getItem() == Items.ELYTRA;
        int slot = elytraOnChest ? InventoryUtil.findChestplateSlot() : InventoryUtil.findItem(Items.ELYTRA);

        if (slot != -1) {
            InventoryUtil.swapToChestSlot(slot);
            NotificationManager.push("Swapped to " + (elytraOnChest ? "chestplate" : "elytra"), true, 2000L);
        } else {
            NotificationManager.push("No " + (elytraOnChest ? "chestplate" : "elytra") + " found", false, 2000L);
        }
    }
}
