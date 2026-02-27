package fun.pasta.module.impl.move;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.ModeOption;
import fun.pasta.module.settings.SliderOption;

public class DragonFly extends Module {

    private final ModeOption mode = new ModeOption("Mode", "ReallyWorld", "ReallyWorld", "Custom");
    private final SliderOption speedXZ = new SliderOption("Speed XZ", 1.15F, 0.5F, 3.0F, 0.01F)
            .setVisible(() -> "Custom".equalsIgnoreCase(mode.get()));
    private final SliderOption speedY = new SliderOption("Speed Y", 1.2F, 0.1F, 3.0F, 0.01F)
            .setVisible(() -> "Custom".equalsIgnoreCase(mode.get()));

    public DragonFly() {
        super("DragonFly", Category.MOVE);
        add(mode, speedXZ, speedY);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!enable || mc.player == null || mc.world == null) {
            return;
        }
        if (!mc.player.abilities.isFlying) {
            return;
        }

        float xz = "Custom".equalsIgnoreCase(mode.get()) ? speedXZ.getValue().floatValue() : 1.111F;
        float y = "Custom".equalsIgnoreCase(mode.get()) ? speedY.getValue().floatValue() : 1.2F;

        if (mc.gameSettings.keyBindJump.isKeyDown() && !mc.player.isSneaking()) {
            mc.player.setMotion(mc.player.getMotion().x, y, mc.player.getMotion().z);
        } else if (mc.gameSettings.keyBindSneak.isKeyDown()) {
            mc.player.setMotion(mc.player.getMotion().x, -y, mc.player.getMotion().z);
        } else {
            mc.player.setMotion(mc.player.getMotion().x, 0.0D, mc.player.getMotion().z);
        }

        if (isMoving()) {
            setSpeed(xz);
        } else {
            mc.player.setMotion(0.0D, mc.player.getMotion().y, 0.0D);
        }
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

