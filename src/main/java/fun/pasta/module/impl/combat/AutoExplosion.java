package fun.pasta.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventMotion;
import fun.pasta.event.EventUpdate;
import fun.pasta.event.PlaceObsidianEvent;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.util.inventory.InventoryUtil;
import fun.pasta.util.time.StopWatch;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;

import java.util.List;
import java.util.stream.Collectors;

public class AutoExplosion extends Module {

    private final BooleanOption safeSelf = new BooleanOption("Safe Self", true);

    private Entity crystalEntity;
    private BlockPos obsidianPosition;
    private int oldCurrentItemSlot = -1;
    private Vector2f rotationVectorToPosition = new Vector2f(0.0f, 0.0f);
    private final StopWatch nextAttackStopWatch = new StopWatch();
    private final StopWatch placeCrystalStopWatch = new StopWatch();
    private boolean canPlaceCrystal;
    private boolean itemInOffHand;

    public AutoExplosion() {
        super("AutoExplosion", Category.COMBAT);
        add(safeSelf);
    }

    @Subscribe
    public void onObsidianPlace(PlaceObsidianEvent event) {
        if (event == null || event.getPos() == null) {
            return;
        }

        placeCrystalStopWatch.reset();
        obsidianPosition = event.getPos();
        canPlaceCrystal = true;
    }

    @Subscribe
    private void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null || mc.playerController == null) {
            return;
        }

        int slotInHotbar = findCrystalSlot();
        if (shouldPlaceCrystal(slotInHotbar)) {
            setAndUseCrystal(slotInHotbar, obsidianPosition);
        }

        if (obsidianPosition != null) {
            findEnderCrystals(obsidianPosition).forEach(this::attackCrystal);
        }

        if (crystalEntity != null && !crystalEntity.isAlive()) {
            reset();
        }
    }

    @Subscribe
    private void onMotion(EventMotion event) {
        if (mc.player == null || crystalEntity == null) {
            return;
        }

        if (!isValid(crystalEntity)) {
            return;
        }

        rotationVectorToPosition = getRotationsToEntity(crystalEntity);
        event.yaw = rotationVectorToPosition.x;
        event.pitch = rotationVectorToPosition.y;
        mc.player.renderYawOffset = rotationVectorToPosition.x;
        mc.player.rotationYawHead = rotationVectorToPosition.x;
    }

    private boolean shouldPlaceCrystal(int slotInHotbar) {
        itemInOffHand = mc.player.getHeldItemOffhand().getItem() == Items.END_CRYSTAL;

        return (slotInHotbar != -1 || itemInOffHand)
                && obsidianPosition != null
                && placeCrystalStopWatch.isreached(50L)
                && canPlaceCrystal;
    }

    private int findCrystalSlot() {
        int hotbarSlot = InventoryUtil.findHotbarItem(Items.END_CRYSTAL);
        if (hotbarSlot != -1) {
            return hotbarSlot;
        }

        int inventorySlot = InventoryUtil.findItem(Items.END_CRYSTAL);
        if (inventorySlot == -1) {
            return -1;
        }

        int targetHotbarSlot = InventoryUtil.findBestSlotInHotBar();
        if (targetHotbarSlot == -1) {
            return -1;
        }

        boolean putBackRequired = !mc.player.inventory.getStackInSlot(targetHotbarSlot).isEmpty();
        InventoryUtil.moveItem(inventorySlot, targetHotbarSlot + 36, putBackRequired);
        return targetHotbarSlot;
    }

    private void setAndUseCrystal(int slot, BlockPos pos) {
        if (pos == null) {
            return;
        }

        oldCurrentItemSlot = mc.player.inventory.currentItem;
        Vector3d blockCenter = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Hand hand = itemInOffHand ? Hand.OFF_HAND : Hand.MAIN_HAND;

        if (!itemInOffHand) {
            mc.player.inventory.currentItem = slot;
            mc.playerController.syncCurrentPlayItem();
        }

        ActionResultType result = mc.playerController.func_217292_a(
                mc.player,
                mc.world,
                hand,
                new BlockRayTraceResult(blockCenter, Direction.UP, pos, false)
        );

        if (result.isSuccessOrConsume()) {
            mc.player.swingArm(hand);
        }

        if (!itemInOffHand) {
            mc.player.inventory.currentItem = oldCurrentItemSlot;
            mc.playerController.syncCurrentPlayItem();
        }

        canPlaceCrystal = false;
    }

    private List<Entity> findEnderCrystals(BlockPos position) {
        return mc.world.getEntitiesWithinAABBExcludingEntity(
                        null,
                        new AxisAlignedBB(
                                position.getX(),
                                position.getY(),
                                position.getZ(),
                                position.getX() + 1.0,
                                position.getY() + 2.0,
                                position.getZ() + 1.0
                        )
                ).stream()
                .filter(entity -> entity instanceof EnderCrystalEntity)
                .collect(Collectors.toList());
    }

    private void attackCrystal(Entity entity) {
        if (isValid(entity) && mc.player.getCooledAttackStrength(1.0f) == 1.0f && nextAttackStopWatch.hasTimeElapsed()) {
            nextAttackStopWatch.setLastMS(500L);
            mc.playerController.attackEntity(mc.player, entity);
            mc.player.swingArm(Hand.MAIN_HAND);
            crystalEntity = entity;
        }

        if (!entity.isAlive()) {
            reset();
        }
    }

    private boolean isValid(Entity entity) {
        if (entity == null || obsidianPosition == null) {
            return false;
        }

        if (!(entity instanceof EnderCrystalEntity)) {
            return false;
        }

        if (safeSelf.get() && mc.player.getPosY() > obsidianPosition.getY()) {
            return false;
        }

        return isCorrectDistance();
    }

    private boolean isCorrectDistance() {
        if (obsidianPosition == null) {
            return false;
        }

        Vector3d obsidianVec = new Vector3d(obsidianPosition.getX(), obsidianPosition.getY(), obsidianPosition.getZ());
        return mc.player.getPositionVec().distanceTo(obsidianVec) <= mc.playerController.getBlockReachDistance();
    }

    private Vector2f getRotationsToEntity(Entity entity) {
        Vector3d eyePos = mc.player.getEyePosition(1.0f);
        Vector3d targetPos = entity.getPositionVec().add(0.0, entity.getHeight() * 0.5, 0.0);
        Vector3d diff = targetPos.subtract(eyePos);

        float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z))));
        pitch = MathHelper.clamp(pitch, -90.0f, 90.0f);

        return new Vector2f(yaw, pitch);
    }

    @Override
    public void onDisable() {
        reset();
        super.onDisable();
    }

    private void reset() {
        crystalEntity = null;
        if (mc.player != null) {
            rotationVectorToPosition = new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);
        } else {
            rotationVectorToPosition = new Vector2f(0.0f, 0.0f);
        }
        oldCurrentItemSlot = -1;
        obsidianPosition = null;
        canPlaceCrystal = false;
        itemInOffHand = false;
        placeCrystalStopWatch.reset();
    }
}
