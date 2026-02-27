package fun.pasta.module.impl.player;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;

public class AutoTool extends Module {

    private final BooleanOption sword = new BooleanOption("Sword On Attack", true);
    private final BooleanOption packetSwap = new BooleanOption("Packet Swap", true);

    private int oldSlot = -1;
    private boolean swapped;

    public AutoTool() {
        super("AutoTool", Category.PLAYER);
        add(sword, packetSwap);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!enable || mc.player == null || mc.world == null || mc.playerController == null) {
            return;
        }

        if (!mc.gameSettings.keyBindAttack.isKeyDown()) {
            restoreSlot();
            return;
        }

        RayTraceResult mouseOver = mc.objectMouseOver;
        if (mouseOver == null) {
            return;
        }

        int bestSlot = -1;
        if (mouseOver.getType() == RayTraceResult.Type.BLOCK && mouseOver instanceof BlockRayTraceResult) {
            BlockRayTraceResult blockHit = (BlockRayTraceResult) mouseOver;
            BlockState state = mc.world.getBlockState(blockHit.getPos());
            bestSlot = findBestToolForBlock(state);
        } else if (mouseOver.getType() == RayTraceResult.Type.ENTITY && sword.get() && mouseOver instanceof EntityRayTraceResult) {
            Entity entity = ((EntityRayTraceResult) mouseOver).getEntity();
            if (entity != null) {
                bestSlot = findBestSwordSlot();
            }
        }

        if (bestSlot != -1 && bestSlot != mc.player.inventory.currentItem) {
            if (!swapped) {
                oldSlot = mc.player.inventory.currentItem;
                swapped = true;
            }
            changeSlot(bestSlot);
        }
    }

    private int findBestToolForBlock(BlockState state) {
        float best = 1.0F;
        int bestSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            float speed = stack.getDestroySpeed(state);
            if (speed > best) {
                best = speed;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private int findBestSwordSlot() {
        float bestDamage = -1.0F;
        int bestSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof SwordItem)) continue;
            SwordItem swordItem = (SwordItem) stack.getItem();
            float damage = swordItem.getAttackDamage();
            if (damage > bestDamage) {
                bestDamage = damage;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private void changeSlot(int slot) {
        mc.player.inventory.currentItem = slot;
        if (packetSwap.get() && mc.player.connection != null) {
            mc.player.connection.sendPacket(new CHeldItemChangePacket(slot));
        }
    }

    private void restoreSlot() {
        if (!swapped || oldSlot < 0 || oldSlot > 8) {
            return;
        }
        if (mc.player.inventory.currentItem != oldSlot) {
            changeSlot(oldSlot);
        }
        swapped = false;
        oldSlot = -1;
    }

    @Override
    public void onDisable() {
        restoreSlot();
        super.onDisable();
    }
}

