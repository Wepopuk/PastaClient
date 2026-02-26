package fun.pasta.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import fun.pasta.event.EventMotion;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.util.inventory.InventoryUtil;
import fun.pasta.util.math.MoveUtils;
import fun.pasta.util.time.StopWatch;

import java.util.Comparator;
import java.util.List;

public class AutoPotion extends Module {

    public AutoPotion() {
        super("AutoPotion", Category.COMBAT);
    }

    float previousPitch;
    final StopWatch stopWatch = new StopWatch();


    @Subscribe
    public void onMotion(EventMotion e) {
        if (!canThrowPotion()) {
            return;
        }

        if (isActive()) {
            Vector3d posPoint = findNearestCollision();
            Vector2f rotationVector = new Vector2f(mc.player.rotationYaw, 90.0F);

            previousPitch = rotationVector.y;
            e.yaw = (rotationVector.x);
            e.pitch = (previousPitch);
        }

        for (Potions potion : Potions.values()) {
            potion.state = true;

            if (shouldUsePotion(potion)) {
                return;
            }
        }

        e.postMotion = (() -> {

            if (!stopWatch.isreached(200)) return;

            boolean pitchIsValid = previousPitch == mc.player.lastReportedPitch;
            int oldCurrentItem = mc.player.inventory.currentItem;

            for (Potions potion : Potions.values()) {
                potion.state = true;

                if (shouldUsePotion(potion)) {
                    continue;
                }
                if (potion.state && pitchIsValid) {
                    sendPotion(potion);
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(oldCurrentItem));
                    mc.playerController.syncCurrentPlayItem();
                }
            }
            stopWatch.reset();
        });
    }


    public boolean isActive() {
        for (Potions potionType : Potions.values()) {
            if (shouldUsePotion(potionType)) {
                continue;
            }
            if (potionType.state) {
                return true;
            }
        }
        return false;
    }


    public boolean canThrowPotion() {
        boolean isOnGround = !MoveUtils.isBlockUnder(0.5F) || mc.player.isOnGround();
        boolean ticksExisted = mc.player.ticksExisted > 25;
        boolean yaw = mc.player.rotationYaw != -180;

        return isOnGround && ticksExisted;
    }


    private boolean shouldUsePotion(Potions potions) {
        if (mc.player.isPotionActive(potions.potion)) {
            potions.state = false;
            return true;
        }
        int potionId = potions.id;

        if (findPotionSlot(potionId, true) == -1 && findPotionSlot(potionId, false) == -1) {
            potions.state = false;
            return true;
        }
        return false;
    }


    private void sendPotion(Potions potions) {
        int potionId = potions.id;

        int hotBarSlot = findPotionSlot(potionId, true);
        int inventorySlot = findPotionSlot(potionId, false);

        if (mc.player.isPotionActive(potions.potion)) {
            potions.state = false;
        }

        if (hotBarSlot != -1) {
            sendUsePacket(hotBarSlot, Hand.MAIN_HAND);
            return;
        }

        if (inventorySlot != -1) {
            int bestSlotInHotBar = InventoryUtil.findBestSlotInHotBar();
            ItemStack bestSlotStack = mc.player.inventory.getStackInSlot(bestSlotInHotBar);

            InventoryUtil.moveItem(inventorySlot, bestSlotInHotBar + 36, bestSlotStack.getItem() != Items.AIR);
            sendUsePacket(bestSlotInHotBar, Hand.MAIN_HAND);
        }
    }

    private void sendUsePacket(int slot, Hand hand) {
        mc.player.connection.sendPacket(new CHeldItemChangePacket(slot));
        mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(hand));
        mc.player.swingArm(Hand.MAIN_HAND);
        previousPitch = 0;

        stopWatch.reset();
    }


    private int findPotionSlot(int id, boolean inHotBar) {
        int start = inHotBar ? 0 : 9;
        int end = inHotBar ? 9 : 36;

        for (int i = start; i < end; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);

            if (stack.isEmpty()) {
                continue;
            }

            if (!(stack.getItem() instanceof SplashPotionItem)) {
                continue;
            }

            List<EffectInstance> potionEffects = PotionUtils.getEffectsFromStack(stack);

            for (EffectInstance effectInstance : potionEffects) {
                if (effectInstance.getPotion() == Effect.get(id)) {
                    return i;
                }
            }
        }

        return -1;
    }


    private Vector3d findNearestCollision() {
        return mc.world.getCollisionShapes(mc.player, mc.player.getBoundingBox().grow(0.0, 0.5, 0.0))
                .toList()
                .stream()
                .min(Comparator.comparingDouble(box ->
                        box.getBoundingBox().getCenter().squareDistanceTo(mc.player.getPositionVec())))
                .map(box -> box.getBoundingBox().getCenter())
                .orElse(null);
    }

    public enum Potions {
        STRENGHT(Effects.STRENGTH, 5),
        SPEED(Effects.SPEED, 1),
        FIRE_RESIST(Effects.FIRE_RESISTANCE, 12);

        final Effect potion;
        final int id;
        boolean state;

        Potions(Effect potion, int potionId) {
            this.potion = potion;
            this.id = potionId;
        }
    }
}