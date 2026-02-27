package fun.pasta.module.impl.move;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventKey;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BindOption;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.module.settings.ModeOption;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;

public class WaterSpeed extends Module {

    private static final float S20 = 0.7015F;
    private static final float S0 = 0.595F;
    private static final float S15 = 0.6499F;
    private static final float S25 = 0.749F;

    private final ModeOption mode = new ModeOption("Mode", "MetaHvH", "MetaHvH", "Grim");
    private final BooleanOption miniJump = new BooleanOption("Mini Jump", false)
            .setVisible(() -> "Grim".equalsIgnoreCase(mode.get()));
    private final BindOption boostKey = new BindOption("BoostKey", -1)
            .setVisible(() -> "MetaHvH".equalsIgnoreCase(mode.get()));

    private long boostEndTime;
    private boolean boosting;
    private boolean boostPressed;

    public WaterSpeed() {
        super("WaterSpeed", Category.MOVE);
        add(mode, boostKey, miniJump);
    }

    @Subscribe
    public void onKey(EventKey event) {
        if (!enable || mc.player == null || mc.world == null) {
            return;
        }
        if (event.key == boostKey.key) {
            boostPressed = true;
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!enable || mc.player == null || mc.world == null) {
            return;
        }

        if (boostPressed) {
            boosting = true;
            boostEndTime = System.currentTimeMillis() + 900L;
            boostPressed = false;
        }
        if (boosting && System.currentTimeMillis() > boostEndTime) {
            boosting = false;
        }

        if ("Grim".equalsIgnoreCase(mode.get())) {
            handleGrim();
            return;
        }
        handleMetaHvh();
    }

    private void handleGrim() {
        if (!mc.player.isInWater() && !mc.player.areEyesInFluid(FluidTags.WATER)) {
            return;
        }

        if (miniJump.get() && mc.player.isOnGround()) {
            mc.player.jump();
        }
        setSpeed(0.38F * (boosting ? 1.15F : 1.0F));
    }

    private void handleMetaHvh() {
        boolean inLiquid = mc.player.isInWater()
                || mc.player.isInLava()
                || mc.player.areEyesInFluid(FluidTags.WATER)
                || mc.player.areEyesInFluid(FluidTags.LAVA);
        if (!inLiquid) {
            return;
        }

        ItemStack off = mc.player.getHeldItemOffhand();
        String itemName = off.getDisplayName().getString();
        EffectInstance speed = mc.player.getActivePotionEffect(Effects.SPEED);

        float applied = resolveBase(itemName);
        if (speed != null) {
            if (speed.getAmplifier() >= 2) {
                applied *= 1.63F;
            } else if (speed.getAmplifier() == 1) {
                applied *= 1.423F;
            }
        }
        if (boosting) {
            applied *= 1.12F;
        }
        setSpeed(applied);
    }

    private float resolveBase(String itemName) {
        if (hasOrb(itemName)) return S20;
        if (itemName.contains("Талисман Венома")) return S25;
        if (itemName.contains("Талисман Картеля")) return S15;
        return S0;
    }

    private boolean hasOrb(String itemName) {
        return itemName.contains("Шар Геракла 2")
                || itemName.contains("Шар CHAMPION")
                || itemName.contains("Шар Аида 2")
                || itemName.contains("Шар GOD")
                || itemName.contains("КУБИК-РУБИК")
                || itemName.contains("Шар BUNNY");
    }

    private void setSpeed(float speedValue) {
        float forward = mc.player.movementInput.moveForward;
        float strafe = mc.player.movementInput.moveStrafe;
        float yaw = mc.player.rotationYaw;

        if (forward == 0.0F && strafe == 0.0F) {
            return;
        }

        if (forward != 0.0F) {
            if (strafe > 0.0F) yaw += forward > 0.0F ? -45.0F : 45.0F;
            else if (strafe < 0.0F) yaw += forward > 0.0F ? 45.0F : -45.0F;
            strafe = 0.0F;
            forward = forward > 0.0F ? 1.0F : -1.0F;
        }

        double rad = Math.toRadians(yaw + 90.0F);
        double mx = forward * speedValue * Math.cos(rad) + strafe * speedValue * Math.sin(rad);
        double mz = forward * speedValue * Math.sin(rad) - strafe * speedValue * Math.cos(rad);
        mc.player.setMotion(mx, mc.player.getMotion().y, mz);
    }

    @Override
    public void onDisable() {
        boosting = false;
        boostPressed = false;
        super.onDisable();
    }
}

