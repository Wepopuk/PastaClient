package fun.pasta.module.impl.move;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.module.settings.ModeOption;
import fun.pasta.module.settings.SliderOption;

public class ElytraBooster extends Module {

    private final ModeOption mode = new ModeOption("Mode", "Custom", "Custom", "Bravo", "ReallyWorld");
    private final BooleanOption byPitch = new BooleanOption("By Pitch", false).setVisible(() -> "Custom".equalsIgnoreCase(mode.get()));
    private final SliderOption speedXZ = new SliderOption("Speed XZ", 1.65F, 1.5F, 2.5F, 0.01F)
            .setVisible(() -> "Custom".equalsIgnoreCase(mode.get()) && !byPitch.get());
    private final SliderOption speedY = new SliderOption("Speed Y", 1.59F, 1.5F, 2.5F, 0.01F)
            .setVisible(() -> "Custom".equalsIgnoreCase(mode.get()) && !byPitch.get());

    public ElytraBooster() {
        super("ElytraBooster", Category.MOVE);
        add(mode, byPitch, speedXZ, speedY);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!enable || mc.player == null || mc.world == null) {
            return;
        }
        if (!mc.player.isElytraFlying()) {
            return;
        }

        float xz;
        float y;
        if ("Bravo".equalsIgnoreCase(mode.get())) {
            xz = 1.72F;
            y = 1.66F;
        } else if ("ReallyWorld".equalsIgnoreCase(mode.get())) {
            xz = 1.60F;
            y = 1.58F;
        } else {
            xz = speedXZ.getValue().floatValue();
            y = speedY.getValue().floatValue();
            if (byPitch.get()) {
                float pitch = Math.abs(mc.player.rotationPitch);
                float mul = 1.45F + Math.min(0.65F, pitch / 60.0F);
                xz = mul;
                y = mul;
            }
        }

        if (isMoving()) {
            setSpeed(xz);
        }

        if (mc.gameSettings.keyBindJump.isKeyDown()) {
            mc.player.setMotion(mc.player.getMotion().x, mc.player.getMotion().y + 0.01D * y, mc.player.getMotion().z);
        } else if (mc.gameSettings.keyBindSneak.isKeyDown()) {
            mc.player.setMotion(mc.player.getMotion().x, mc.player.getMotion().y - 0.01D * y, mc.player.getMotion().z);
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

