package fun.pasta.module.impl.move;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventPacket;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.module.settings.SliderOption;
import net.minecraft.block.AirBlock;
import net.minecraft.block.SoulSandBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.server.SEntityStatusPacket;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.BlockPos;

public class Strafe extends Module {

    private final BooleanOption damageBoost = new BooleanOption("Damage Boost", false);
    private final SliderOption boostSpeed = new SliderOption("Boost Speed", 0.4f, 0.1F, 5.0f, 0.1F)
            .setVisible(damageBoost::get);

    private double oldSpeed;
    private long lastDamageTime;

    public Strafe() {
        super("Strafe", Category.MOVE);
        add(damageBoost, boostSpeed);
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.world == null || !event.isreceive()) {
            return;
        }

        if (event.packet instanceof SPlayerPositionLookPacket) {
            oldSpeed = 0.0D;
        }

        if (!damageBoost.get()) {
            return;
        }

        if (event.packet instanceof SEntityStatusPacket packet) {
            Entity entity = packet.getEntity(mc.world);
            if (entity == mc.player && packet.getOpCode() == 2) {
                lastDamageTime = System.currentTimeMillis();
            }
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (mc.player.hurtTime > 0) {
            lastDamageTime = System.currentTimeMillis();
        }

        if (!canStrafe()) {
            oldSpeed = 0.0D;
            return;
        }

        if (!isMoving()) {
            oldSpeed = 0.0D;
            return;
        }

        double base = getBaseSpeed();
        double speed = calculateSpeed(base);

        if (damageBoost.get() && System.currentTimeMillis() - lastDamageTime <= 700L) {
            speed += boostSpeed.getValue().floatValue() / 16.0F;
        }

        if (mc.player.isOnGround()) {
            mc.player.jump();
        }

        setSpeed(speed);
        oldSpeed = Math.max(base, speed);
    }

    private double calculateSpeed(double base) {
        if (mc.player.isOnGround()) {
            return Math.max(base, oldSpeed * 1.32D);
        }

        double decayed = oldSpeed * 0.96D;
        return Math.max(base, decayed);
    }

    private double getBaseSpeed() {
        double base = 0.235D;
        if (mc.player.isPotionActive(Effects.SPEED)) {
            int amp = mc.player.getActivePotionEffect(Effects.SPEED).getAmplifier();
            base *= 1.0D + 0.2D * (amp + 1);
        }
        if (mc.player.isPotionActive(Effects.SLOWNESS)) {
            int amp = mc.player.getActivePotionEffect(Effects.SLOWNESS).getAmplifier();
            base *= Math.max(0.0D, 1.0D - 0.15D * (amp + 1));
        }
        return base;
    }

    private boolean canStrafe() {
        if (mc.player.isSneaking() || mc.player.isElytraFlying()) {
            return false;
        }

        if (mc.player.isInWater() || mc.player.isInLava()) {
            boolean blockAboveNotAir = !(mc.world.getBlockState(mc.player.getPosition().add(0, 1, 0)).getBlock() instanceof AirBlock);
            if (mc.gameSettings.keyBindJump.isKeyDown() && !mc.player.isSneaking() && blockAboveNotAir) {
                return false;
            }
        }

        BlockPos feet = new BlockPos(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ());
        BlockPos underFeet = new BlockPos(mc.player.getPosX(), mc.player.getPosY() - 0.01D, mc.player.getPosZ());
        if (mc.world.getBlockState(feet).getMaterial() == Material.WEB
                || mc.world.getBlockState(underFeet).getBlock() instanceof SoulSandBlock) {
            return false;
        }

        if (mc.player.abilities.isFlying || mc.player.isPotionActive(Effects.LEVITATION)) {
            return false;
        }

        return !mc.player.areEyesInFluid(FluidTags.WATER) && !mc.player.areEyesInFluid(FluidTags.LAVA);
    }

    private boolean isMoving() {
        return Math.abs(mc.player.moveForward) > 1.0E-4F || Math.abs(mc.player.moveStrafing) > 1.0E-4F;
    }

    private void setSpeed(double speed) {
        float forward = mc.player.movementInput.moveForward;
        float strafe = mc.player.movementInput.moveStrafe;
        float yaw = mc.player.rotationYaw;

        if (forward == 0.0F && strafe == 0.0F) {
            return;
        }

        if (forward != 0.0F) {
            if (strafe > 0.0F) {
                yaw += forward > 0.0F ? -45.0F : 45.0F;
            } else if (strafe < 0.0F) {
                yaw += forward > 0.0F ? 45.0F : -45.0F;
            }
            strafe = 0.0F;
            forward = forward > 0.0F ? 1.0F : -1.0F;
        }

        double rad = Math.toRadians(yaw + 90.0F);
        double motionX = forward * speed * Math.cos(rad) + strafe * speed * Math.sin(rad);
        double motionZ = forward * speed * Math.sin(rad) - strafe * speed * Math.cos(rad);
        mc.player.setMotion(motionX, mc.player.getMotion().y, motionZ);
    }

    @Override
    public void onEnable() {
        oldSpeed = 0.0D;
        lastDamageTime = 0L;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            mc.player.setMotion(mc.player.getMotion().x * 0.7D, mc.player.getMotion().y, mc.player.getMotion().z * 0.7D);
        }
        oldSpeed = 0.0D;
        lastDamageTime = 0L;
        super.onDisable();
    }
}
