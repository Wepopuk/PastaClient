package fun.pasta.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.material.Material;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.datafix.fixes.VillagerLevelAndXpFix;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import fun.pasta.Pasta;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.module.settings.ModeOption;
import fun.pasta.module.settings.MultiBoxOption;
import fun.pasta.module.settings.SliderOption;
import fun.pasta.util.aura.*;
import fun.pasta.util.math.SensitivityUtil;
import fun.pasta.util.time.StopWatch;


import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.hypot;
import static net.minecraft.util.math.MathHelper.clamp;
import static net.minecraft.util.math.MathHelper.wrapDegrees;



public class KillAura extends Module {

    public final SliderOption attackRange = new SliderOption("Attack Range", 3f, 3f, 6f, 0.1f);
    public final BooleanOption onlyCrit = new BooleanOption("Only Crits", true);
    public final ModeOption rotateMode = new ModeOption("Rotate Mode", "Vanilla", "Vanilla", "Smooth", "Snap");
    public final BooleanOption moveFix = new BooleanOption("Move Fix", true);
    public final BooleanOption attackFriends = new BooleanOption("Attack Friends", false);

    public final MultiBoxOption targets = new MultiBoxOption("Targets",
            new BooleanOption("Players", true),
            new BooleanOption("Naked", false),
            new BooleanOption("Mobs", false),
            new BooleanOption("Monsters", false));

    public LivingEntity target;
    private LivingEntity lastTarget;
    private Vector2f rotateVector = new Vector2f(0, 0);
    private float lastYaw;
    private float lastPitch;
    private boolean wasSprinting;
    private boolean snapReturnPending;
    private float snapReturnYaw;
    private float snapReturnPitch;
    final StopWatch cooldownTimer = new StopWatch();

    public KillAura() {
        super("KillAura", Category.COMBAT);
        add(attackRange, onlyCrit, rotateMode, targets, attackFriends, moveFix);
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {

        if (mc.player == null || mc.world == null) return;

        processSnapReturn();
        checkFocus();

        if (target != null) {
            defaultAttack();
        } else {
            reset();
        }
    }

    private void defaultAttack() {
        boolean attacking = shouldAttack(target, false, onlyCrit.get(), -50L);
        if (!Pasta.instance.manager.autoPotion.isActive()) {
            updateRotation(80, 35, attacking);
        }
        if (attacking) {
            updateAttack();
            cooldownTimer.reset();
        }
    }

    private void checkFocus() {
        if (!isValid(target)) {
            updateTarget();
        }
    }

    private void updateTarget() {
        List<LivingEntity> targets = new ArrayList<>();

        if (mc.player == null || mc.world == null) return;

        for (Entity entity : mc.world.getAllEntities()) {
            if (entity instanceof LivingEntity living && isValid(living)) {
                targets.add(living);
            }
        }

        if (targets.isEmpty()) {
            target = null;
            return;
        }

        if (targets.size() == 1) {
            target = targets.get(0);
            return;
        }

        if (targets.size() > 1) {
            targets.sort(Comparator.comparingDouble(this::getDistance).thenComparingDouble(this::getEntityHealth));
        }

        target = targets.get(0);
    }

    private void updateRotation(float rotationYawSpeed, float rotationPitchSpeed, boolean attacking) {
        if ("Smooth".equalsIgnoreCase(rotateMode.get()) || "RW".equalsIgnoreCase(rotateMode.get())) {
            updateRotationSmooth(attacking);
            return;
        }
        if ("Snap".equalsIgnoreCase(rotateMode.get())) {
            updateRotationSnap(attacking);
            return;
        }
        updateRotationVanilla(rotationYawSpeed, rotationPitchSpeed);
    }

    private void updateRotationVanilla(float rotationYawSpeed, float rotationPitchSpeed) {
        Vector3d bestHitbox = getVector(target);

        if (bestHitbox == null) {
            bestHitbox = target.getEyePosition(1);
        }

        double x = bestHitbox.x - mc.player.getPosX();
        double y = bestHitbox.y - mc.player.getEyePosition(1).y;
        double z = bestHitbox.z - mc.player.getPosZ();

        float yawToTarget = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(z, x)) - 90);
        float pitchToTarget = (float) (-Math.toDegrees(Math.atan2(y, hypot(x, z))));

        float yawDelta = (wrapDegrees(yawToTarget - mc.player.rotationYaw));
        float pitchDelta = (wrapDegrees(pitchToTarget - mc.player.rotationPitch));

        float clampedYaw = Math.min(Math.max(Math.abs(yawDelta), 1.0f), rotationYawSpeed);
        float clampedPitch = Math.min(Math.max(Math.abs(pitchDelta), 1.0f), rotationPitchSpeed);

        float yaw = mc.player.rotationYaw + (yawDelta > 0 ? clampedYaw : -clampedYaw);
        float pitch = clamp(mc.player.rotationPitch + (pitchDelta > 0 ? clampedPitch : -clampedPitch), -89.0F, 89.0F);

        float gcd = SensitivityUtil.getSensValue();
        yaw -= (yaw - mc.player.rotationYaw) % gcd;
        pitch -= (pitch - mc.player.rotationPitch) % gcd;

        rotateVector = new Vector2f(yaw, pitch);
        lastYaw = yaw;
        lastPitch = pitch;
        lastTarget = target;

        RotationService.update(new Rotation(yaw, pitch), 360, 360, 4, 5);
    }

    private void updateRotationSmooth(boolean attacking) {
        if (target == null) {
            return;
        }

        Vector3d bestHitbox = getVector(target);
        if (bestHitbox == null) {
            bestHitbox = target.getEyePosition(1);
        }

        Vector3d vec = bestHitbox.subtract(mc.player.getEyePosition(1));
        float targetYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(vec.y, hypot(vec.x, vec.z))));
        targetPitch = clamp(targetPitch, -89.0F, 89.0F);

        if (rotateVector == null) {
            rotateVector = new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);
        }

        float yawDelta = wrapDegrees(targetYaw - rotateVector.x);
        float pitchDelta = wrapDegrees(targetPitch - rotateVector.y);

        float newYaw;
        float newPitch;

        float baseYawSpeed = 52.0f;
        float basePitchSpeed = 44.0f;

        boolean targetChanged = target != lastTarget;
        if (attacking && targetChanged) {
            float snapFactor = 0.88f + (Math.min(Math.abs(yawDelta) / 90.0f, 1.0f) * 0.12f);
            newYaw = rotateVector.x + yawDelta * snapFactor;
            newPitch = rotateVector.y + pitchDelta * snapFactor;
        } else {
            float yawSpeed = Math.min(Math.abs(yawDelta), baseYawSpeed);
            float pitchSpeed = Math.min(Math.abs(pitchDelta), basePitchSpeed);
            newYaw = rotateVector.x + (yawDelta > 0 ? yawSpeed : -yawSpeed);
            newPitch = rotateVector.y + (pitchDelta > 0 ? pitchSpeed : -pitchSpeed);
        }

        long time = System.currentTimeMillis();
        float timeFactor = time * 0.001f;
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        if (mc.player.ticksExisted % (3 + (int) (Math.sin(timeFactor) * 2)) == 0) {
            float shakeIntensity = 0.12f + (float) Math.sin(timeFactor * 2.5f) * 0.06f;
            newYaw += (rand.nextFloat() - 0.5f) * shakeIntensity;
            newPitch += (rand.nextFloat() - 0.5f) * shakeIntensity * 0.8f;
        }

        newYaw += (rand.nextFloat() - 0.5f) * 0.018f;
        newPitch += (rand.nextFloat() - 0.5f) * 0.014f;

        float gcd = SensitivityUtil.getSensValue();
        float gcdVariation = 0.985f + (rand.nextFloat() * 0.03f);
        newYaw -= (newYaw - rotateVector.x) % (gcd * gcdVariation);
        newPitch -= (newPitch - rotateVector.y) % (gcd * gcdVariation * 0.95f);

        float maxChange = attacking ? 38.0f : 28.0f;
        newYaw = rotateVector.x + MathHelper.clamp(newYaw - rotateVector.x, -maxChange, maxChange);
        newPitch = rotateVector.y + MathHelper.clamp(newPitch - rotateVector.y, -maxChange * 0.85f, maxChange * 0.85f);

        newPitch = clamp(newPitch, -89.0F, 89.0F);

        float smoothFactor = 0.78f + (rand.nextFloat() * 0.22f);
        newYaw = rotateVector.x + (newYaw - rotateVector.x) * smoothFactor;
        newPitch = rotateVector.y + (newPitch - rotateVector.y) * smoothFactor;

        rotateVector = new Vector2f(newYaw, newPitch);

        if (moveFix.get()) {
            mc.player.renderYawOffset = newYaw;
            if (mc.player.ticksExisted % 8 == 0) {
                mc.player.rotationYawHead += (rand.nextFloat() - 0.5f) * 0.6f;
            }
        }

        lastYaw = newYaw;
        lastPitch = newPitch;

        if (time % 2000L < 100L) {
            float randomizer = 0.9f + (rand.nextFloat() * 0.2f);
            rotateVector = new Vector2f(
                    rotateVector.x * randomizer,
                    clamp(rotateVector.y * randomizer, -89.0F, 89.0F)
            );
        }

        lastTarget = target;
        RotationService.update(new Rotation(rotateVector.x, rotateVector.y), 360, 360, 4, 5);
    }

    private void updateRotationSnap(boolean attacking) {
        if (target == null || !attacking) {
            return;
        }

        Vector2f targetRotation = getRotationToTarget(target);
        if (targetRotation == null) {
            return;
        }

        snapReturnYaw = mc.player.rotationYaw;
        snapReturnPitch = mc.player.rotationPitch;

        mc.player.rotationYaw = targetRotation.x;
        mc.player.rotationPitch = targetRotation.y;

        rotateVector = new Vector2f(targetRotation.x, targetRotation.y);
        lastYaw = targetRotation.x;
        lastPitch = targetRotation.y;
        lastTarget = target;
        snapReturnPending = true;

        // Keep free-rotation enabled so the local player camera does not visually snap.
        FreerotationHelper.active = true;
        RotationService.currentTask = RotationService.RotationTask.IDLE;
        RotationService.currentPriority = 0;
    }

    private void processSnapReturn() {
        if (!snapReturnPending || mc.player == null) {
            return;
        }

        mc.player.rotationYaw = snapReturnYaw;
        mc.player.rotationPitch = snapReturnPitch;

        rotateVector = new Vector2f(snapReturnYaw, snapReturnPitch);
        lastYaw = snapReturnYaw;
        lastPitch = snapReturnPitch;
        snapReturnPending = false;
    }

    private Vector2f getRotationToTarget(LivingEntity livingTarget) {
        if (livingTarget == null || mc.player == null) {
            return null;
        }

        Vector3d bestHitbox = getVector(livingTarget);
        if (bestHitbox == null) {
            bestHitbox = livingTarget.getEyePosition(1);
        }

        double x = bestHitbox.x - mc.player.getPosX();
        double y = bestHitbox.y - mc.player.getEyePosition(1).y;
        double z = bestHitbox.z - mc.player.getPosZ();

        float yawToTarget = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(z, x)) - 90);
        float pitchToTarget = (float) (-Math.toDegrees(Math.atan2(y, hypot(x, z))));
        pitchToTarget = clamp(pitchToTarget, -89.0F, 89.0F);

        return new Vector2f(yawToTarget, pitchToTarget);
    }

    private void updateAttack() {
        if ((mc.player.isInWater() || getBlock(0, -0.2f, 0) == Blocks.WATER) && getBlock(0, 1, 0) == Blocks.AIR && mc.gameSettings.keyBindJump.isKeyDown()) {
            if (mc.player.fallDistance == 0.0f) {
                return;
            }
        }

        preSprintReset();

        if (mc.player.isActiveItemStackBlocking()) {
            mc.playerController.onStoppedUsingItem(mc.player);
        }

        if (target.getActiveItemStack().getItem().getUseAction(target.getActiveItemStack()) == UseAction.BLOCK) {
            breakShield(target);
        } else {
            mc.playerController.attackEntity(mc.player, target);
            mc.player.swingArm(Hand.MAIN_HAND);
        }

        postSprintReset();
    }

    private void breakShield(LivingEntity target) {
        if (target instanceof PlayerEntity) {
            PlayerEntity playerTarget = (PlayerEntity) target;
            if (playerTarget.isActiveItemStackBlocking()) {
                ItemStack activeItem = playerTarget.getActiveItemStack();
                if (activeItem.getItem().getUseAction(activeItem) == UseAction.BLOCK) {
                    int axeSlot = -1;
                    ItemStack mainHand = mc.player.getHeldItemMainhand();

                    if (mainHand.getItem() instanceof AxeItem) {
                        axeSlot = mc.player.inventory.currentItem;
                    } else {
                        for (int i = 0; i < 9; i++) {
                            ItemStack stack = mc.player.inventory.getStackInSlot(i);
                            if (stack.getItem() instanceof AxeItem) {
                                axeSlot = i;
                                break;
                            }
                        }
                    }

                    if (axeSlot != -1 && axeSlot != mc.player.inventory.currentItem) {
                        mc.player.inventory.currentItem = axeSlot;
                        mc.player.connection.sendPacket(new CPlayerPacket.RotationPacket(
                                mc.player.rotationYaw,
                                mc.player.rotationPitch,
                                mc.player.isOnGround()
                        ));
                    }

                    if (mc.player.getCooledAttackStrength(0.5f) >= 0.9f) {
                        for (int i = 0; i < 2; i++) {
                            mc.playerController.attackEntity(mc.player, target);
                            mc.player.swingArm(Hand.MAIN_HAND);
                        }
                    }
                }
            }
        }
    }

    private void preSprintReset() {
        if (mc.player == null || mc.player.connection == null) {
            return;
        }
        if (mc.player.serverSprintState) {
            mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.STOP_SPRINTING));
            mc.player.serverSprintState = false;
            mc.player.setSprinting(false);
            wasSprinting = true;
        } else {
            wasSprinting = false;
        }
    }

    private void postSprintReset() {
        if (mc.player == null || mc.player.connection == null) {
            return;
        }
        if (wasSprinting) {
            mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.START_SPRINTING));
            mc.player.serverSprintState = true;
            mc.player.setSprinting(true);
            wasSprinting = false;
        }
    }

    private boolean isValid(LivingEntity entity) {
        if (entity == null) return false;

        float dst = attackRange.getValue().floatValue();

        if (getDistance(entity) > dst) return false;

        if (entity instanceof FakePlayer) return false;
        if (entity.getHealth() <= 0 || !entity.isAlive() || entity.equals(mc.player)) return false;
        if (entity instanceof ArmorStandEntity) return false;
        if (entity instanceof PlayerEntity
                && Pasta.instance.manager != null
                && Pasta.instance.manager.antiBot != null
                && Pasta.instance.manager.antiBot.enable
                && AntiBot.isBot(entity)) {
            return false;
        }
        if (!isAllowedTarget(entity)) return false;

        return entity.isAlive();
    }

    private boolean isAllowedTarget(LivingEntity entity) {
        boolean allowPlayers = targets.get("Players");
        boolean allowNaked = targets.get("Naked");
        boolean allowMobs = targets.get("Mobs");
        boolean allowMonsters = targets.get("Monsters");

        if (entity instanceof PlayerEntity player) {
            if (Pasta.instance.friendManager != null && Pasta.instance.friendManager.isFriend(player) && !attackFriends.get()) {
                return false;
            }
            boolean naked = isNaked(player);
            if (naked) {
                return allowNaked;
            }
            return allowPlayers;
        }

        if (entity instanceof MonsterEntity) {
            return allowMonsters;
        }

        if (entity instanceof AnimalEntity || entity instanceof VillagerEntity) {
            return allowMobs;
        }

        if (entity instanceof MobEntity) {
            return allowMobs || allowMonsters;
        }

        return false;
    }

    private boolean isNaked(PlayerEntity player) {
        for (ItemStack stack : player.inventory.armorInventory) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private double getEntityArmor(PlayerEntity entityPlayer2) {
        double d2 = 0.0;
        for (int i2 = 0; i2 < 4; ++i2) {
            ItemStack is = entityPlayer2.inventory.armorInventory.get(i2);
            if (!(is.getItem() instanceof ArmorItem)) continue;
            d2 += getProtectionLvl(is);
        }
        return d2;
    }

    private double getProtectionLvl(ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem i) {
            double damageReduceAmount = i.getDamageReduceAmount();
            if (stack.isEnchanted()) {
                damageReduceAmount += (double) EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION, stack) * 0.25;
            }
            return damageReduceAmount;
        }
        return 0;
    }

    private double getEntityHealth(LivingEntity ent) {
        if (ent instanceof PlayerEntity player) {
            return (double) (player.getHealth() + player.getAbsorptionAmount()) * (getEntityArmor(player) / 20.0);
        }
        return ent.getHealth() + ent.getAbsorptionAmount();
    }

    public Vector3d getVector(Entity target) {
        if (target.getDistance(target) >= 36) {
            return null;
        } else {
            return target.getPositionVec().add(0, target.getEyeHeight(), 0);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        target = null;
        lastTarget = null;
        snapReturnPending = false;
        if (mc.player != null) {
            FreerotationHelper.freeYaw = mc.player.rotationYaw;
            FreerotationHelper.freePitch = mc.player.rotationPitch;
            FreerotationHelper.active = true;
            rotateVector = new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);
            lastYaw = mc.player.rotationYaw;
            lastPitch = mc.player.rotationPitch;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        processSnapReturn();
        reset();
    }

    private void reset() {
        target = null;
        lastTarget = null;
        wasSprinting = false;
        snapReturnPending = false;
        rotateVector = new Vector2f(FreerotationHelper.freeYaw, FreerotationHelper.freePitch);
        lastYaw = rotateVector.x;
        lastPitch = rotateVector.y;

        if (!enable) {
            if (mc.player != null) {
                mc.player.rotationYaw = FreerotationHelper.freeYaw;
                mc.player.rotationPitch = FreerotationHelper.freePitch;
                mc.player.rotationYawHead = FreerotationHelper.freeYaw;
                mc.player.renderYawOffset = FreerotationHelper.freeYaw;
            }
            RotationService.currentTask = RotationService.RotationTask.IDLE;
            RotationService.currentPriority = 0;
            FreerotationHelper.active = false;
            return;
        }

        RotationService.update(new Rotation(FreerotationHelper.freeYaw, FreerotationHelper.freePitch), 25, 25, 0, 5);
    }

    private boolean isPlayerInCobweb() {
        AxisAlignedBB bb = mc.player.getBoundingBox();
        return mc.world.getBlockState(new BlockPos(bb.minX, bb.minY, bb.minZ)).getBlock() == Blocks.COBWEB
                || mc.world.getBlockState(new BlockPos(bb.maxX, bb.minY, bb.minZ)).getBlock() == Blocks.COBWEB
                || mc.world.getBlockState(new BlockPos(bb.minX, bb.minY, bb.maxZ)).getBlock() == Blocks.COBWEB
                || mc.world.getBlockState(new BlockPos(bb.maxX, bb.minY, bb.maxZ)).getBlock() == Blocks.COBWEB;
    }


    private Vector3d getVector3d(LivingEntity me, LivingEntity to) {

        double wHalf = to.getWidth() / 2;

        float dst = attackRange.getValue().floatValue();

        double yExpand = clamp(me.getPosYEye() - to.getPosY(), 0, to.getHeight() * (mc.player.getDistance(to) / (dst)));

        double xExpand = clamp(mc.player.getPosX() - to.getPosX(), -wHalf, wHalf);
        double zExpand = clamp(mc.player.getPosZ() - to.getPosZ(), -wHalf, wHalf);

        return new Vector3d(
                to.getPosX() - me.getPosX() + xExpand,
                to.getPosY() - me.getPosYEye() + yExpand,
                to.getPosZ() - me.getPosZ() + zExpand
        );
    }

    public Vector2f getRotationVector() {
        return rotateVector;
    }

    private boolean isBestMomentToHit(boolean onCrit) {
        if (!onCrit) return true;

        float adaptiveFallValue = 0.0f, maxFallOff = 0.2f;
        if (getYCapacityOnPlayerPos(2) > .20000004768371582D) {
            adaptiveFallValue = maxFallOff;
        }

        final boolean hasFall = convenientFallOffset() > adaptiveFallValue || getYCapacityOnPlayerPos(2) < 0.1f;

        if (hasFall) return true;

        final boolean badLiquidMoment = !mc.player.movementInput.jump && (mc.player.isInWater() || mc.player.isInLava())
                || mc.player.areEyesInFluid(FluidTags.WATER) || mc.player.areEyesInFluid(FluidTags.LAVA) || isPlayerInCobweb();

        final boolean skipFallCheck =
                badLiquidMoment ||
                        (!mc.player.movementInput.jump && mc.player.ticksOnGround > 6) &&
                                mc.player.isOnLadder() ||
                        mc.player.isElytraFlying() ||
                        mc.player.isRidingHorse() ||
                        mc.player.isPotionActive(Effects.BLINDNESS) || mc.player.isPotionActive(Effects.LEVITATION) || mc.player.isPotionActive(Effects.SLOW_FALLING) ||
                        mc.player.abilities.isFlying ||
                        mc.player.getPosY() < -64.D;

        return skipFallCheck;
    }

    private double convenientFallOffset() {
        double fallOffset = mc.player.fallDistance;
        if (mc.world != null && !mc.player.isOnGround() && mc.player.getMotion().y < -.0784000015258789D && !mc.world.getBlockState(mc.player.getPosition()).getMaterial().isLiquid()
                && !mc.world.getBlockState(mc.player.getPosition().up()).getMaterial().isLiquid()) {
            if (mc.player.fallDistance < -mc.player.getMotion().y && mc.player.ticksOnGround > 6) fallOffset = -mc.player.getMotion().y;
        }
        return fallOffset;
    }

    private double getYCapacityOnPlayerPos(int rangeY) {
        if (mc.world == null) return 1.D;
        Vector3d eyePos = mc.player.getEyePosition(mc.getRenderPartialTicks());
        double minDst = rangeY * 2.D, dst;
        double maxY = 255, minY = -64;
        final float selfWD2 = mc.player.getWidth() / 2.F - 1E-2F;
        RayTraceResult first, second;
        for (final Vector3d vec : Arrays.asList(eyePos.add(-selfWD2, .0D, -selfWD2), eyePos.add(selfWD2, .0D, selfWD2), eyePos.add(selfWD2, .0D, -selfWD2), eyePos.add(-selfWD2, .0D, selfWD2))) {
            first = mc.world.rayTraceBlocks(new RayTraceContext(vec, vec.add(0.D, -rangeY, 0.D), RayTraceContext.BlockMode.VISUAL, RayTraceContext.FluidMode.ANY, mc.player));
            second = mc.world.rayTraceBlocks(new RayTraceContext(vec, vec.add(0.D, rangeY, 0.D), RayTraceContext.BlockMode.VISUAL, RayTraceContext.FluidMode.ANY, mc.player));
            if (maxY > second.getHitVec().y) maxY = second.getHitVec().y;
            if (minY < first.getHitVec().y) minY = first.getHitVec().y;
            dst = maxY - minY;
            if (minDst > dst) minDst = dst;
        }
        return minDst - mc.player.getHeight();
    }

    private boolean cancelSprintTick() {
        if (target != null && shouldAttack(target, false, onlyCrit.get(), -50L)) {
            if (!mc.player.isOnGround() && !mc.player.areEyesInFluid(FluidTags.WATER)) {
                return mc.player.getMotion().y <= .0030162615090425808;
            }
        }
        return false;
    }

    public static long getMsCooldown() {
        long msCooldown;

        double attributeAttackSpeed = mc.player.getAttributeValue(Attributes.ATTACK_SPEED);
        float maxDeviation = .2F;
        msCooldown = (long) ((1.F / attributeAttackSpeed) * 1000.F * (1.F - Math.min(maxDeviation, 1.F)));
        if (attributeAttackSpeed == 4.D || attributeAttackSpeed == 4.4000000059604645 || attributeAttackSpeed == 4.800000011920929)
            msCooldown = 450L;
        msCooldown = Math.max(msCooldown, 450L);

        return msCooldown;
    }

    public boolean msCooldownReached(long msOffset) {
        return cooldownTimer.isreached(getMsCooldown() + msOffset);
    }

    public boolean shouldAttack(LivingEntity livingTarget, boolean distanceCheck, boolean onlyCrit, long cooldownMSOffset) {if (distanceCheck && livingTarget != null)
            return false;

        if (!msCooldownReached(cooldownMSOffset)) return false;

        return isBestMomentToHit(onlyCrit);
    }

    private double getDistance(LivingEntity entity) {
        return getVector3d(mc.player, entity).length();
    }

    public static Block getBlock(double x, double y, double z) {
        return mc.player == null && mc.world == null ? Blocks.AIR : mc.world.getBlockState(mc.player.getPosition().add(x, y, z)).getBlock();
    }
}
