package fun.pasta.module.impl.move;

import com.google.common.eventbus.Subscribe;
import fun.pasta.Pasta;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.SliderOption;

public class AntiTarget extends Module {

    private final SliderOption pitch = new SliderOption("Pitch", 40.0F, 30.0F, 50.0F, 1.0F);
    private final SliderOption speed = new SliderOption("Speed", 1.96F, 1.90F, 2.70F, 0.01F);

    public AntiTarget() {
        super("AntiTarget", Category.MOVE);
        add(pitch, speed);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!enable || mc.player == null || mc.world == null) {
            return;
        }
        if (hasAuraTarget()) {
            return;
        }
        if (!mc.player.isElytraFlying()) {
            return;
        }

        mc.player.rotationPitch = -pitch.getValue().floatValue();
        if (isMoving()) {
            setSpeed(speed.getValue().floatValue());
        }
    }

    private boolean hasAuraTarget() {
        return Pasta.instance != null
                && Pasta.instance.manager != null
                && Pasta.instance.manager.killAura != null
                && Pasta.instance.manager.killAura.target != null;
    }

    private boolean isMoving() {
        return Math.abs(mc.player.moveForward) > 1.0E-4F || Math.abs(mc.player.moveStrafing) > 1.0E-4F;
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

