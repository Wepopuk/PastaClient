package fun.pasta.module.impl.move;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.module.settings.ModeOption;
import fun.pasta.module.settings.SliderOption;

public class NoSlow extends Module {

    private final ModeOption mode = new ModeOption("Mode", "Grim", "Matrix", "Grim", "FunTime", "ReallyWorld", "Spooky");
    private final SliderOption vanillaSpeed = new SliderOption("Speed", 0.60F, 0.1F, 1.0F, 0.05F)
            .setVisible(() -> "Vanilla".equalsIgnoreCase(mode.get()));
    private final BooleanOption onlyGround = new BooleanOption("Only Ground", false)
            .setVisible(() -> !"FunTime".equalsIgnoreCase(mode.get()));

    public NoSlow() {
        super("NoSlow", Category.MOVE);
        add(mode, vanillaSpeed, onlyGround);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!enable || mc.player == null || mc.world == null) {
            return;
        }
        if (mc.player.isElytraFlying()) {
            return;
        }
        if (!mc.player.isHandActive()) {
            return;
        }
        if (onlyGround.get() && !mc.player.isOnGround()) {
            return;
        }
        if (!isMoving()) {
            return;
        }

        float mul = resolveMultiplier();
        double base = Math.max(horizontalSpeed(), 0.13D);
        setSpeed((float) (base * mul));
    }

    private float resolveMultiplier() {
        switch (mode.get()) {
            case "Matrix":
                return 1.08F;
            case "FunTime":
                return 1.16F;
            case "ReallyWorld":
                return 1.14F;
            case "Spooky":
                return 1.10F;
            case "Vanilla":
                return 1.0F + vanillaSpeed.getValue().floatValue();
            case "Grim":
            default:
                return 1.12F;
        }
    }

    private boolean isMoving() {
        return Math.abs(mc.player.moveForward) > 1.0E-4F || Math.abs(mc.player.moveStrafing) > 1.0E-4F;
    }

    private double horizontalSpeed() {
        return Math.hypot(mc.player.getMotion().x, mc.player.getMotion().z);
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
}

