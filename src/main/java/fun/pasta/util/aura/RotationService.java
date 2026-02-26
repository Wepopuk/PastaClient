package fun.pasta.util.aura;

import com.google.common.eventbus.Subscribe;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import fun.pasta.Pasta;
import fun.pasta.event.EventInput;
import fun.pasta.event.EventUpdate;
import fun.pasta.util.IMinecraft;
import fun.pasta.util.math.MoveUtils;
import fun.pasta.util.math.SensitivityUtil;


import static net.minecraft.util.math.MathHelper.clamp;
import static net.minecraft.util.math.MathHelper.wrapDegrees;

public class RotationService implements IMinecraft {

    public RotationService() {
        Pasta.instance.eventBus.register(this);
    }

    public static RotationTask currentTask = RotationTask.IDLE;
    public static float currentTurnSpeed;
    public static float currentReturnSpeed;
    public static int currentPriority;
    public static int currentTimeout;

    public static int idleTicks;
    public static int ticksHoldFocusMovement = 0;

    @Subscribe
    public void onEvent(EventInput event) {
        if (Pasta.instance.manager.killAura.enable && ticksHoldFocusMovement <= 0) {
            float falseYaw = MathHelper.wrapDegrees(mc.gameRenderer.getActiveRenderInfo().getYaw()) + (mc.gameSettings.getPointOfView().thirdPersonFront ? 180 : 0);

            MoveUtils.fixMovement2(event, falseYaw);
        }
    }

    @Subscribe
    public void tick(EventUpdate event) {
        idleTicks++;

        if (currentTask == RotationTask.AIM && idleTicks > currentTimeout) {
            currentTask = RotationTask.RESET;
        }

        if (currentTask == RotationTask.RESET) {
            Rotation targetRotation = new Rotation(FreerotationHelper.freeYaw, FreerotationHelper.freePitch);

            if (updateRotation(targetRotation, currentReturnSpeed)) {
                currentTask = RotationTask.IDLE;
                currentPriority = 0;
                FreerotationHelper.active = (false);
            }
        }
    }

    public static void update(Rotation rotation, float turnSpeed, float returnSpeed, int timeout, int priority) {
        if (currentPriority > priority) {
            return;
        }

        if (currentTask == RotationTask.IDLE) {
            FreerotationHelper.active = (true);
        }

        currentTurnSpeed = turnSpeed;
        currentReturnSpeed = returnSpeed;
        currentTimeout = timeout;
        currentPriority = priority;

        currentTask = RotationTask.AIM;

        updateRotation(rotation, turnSpeed);
    }

    public static void update(Rotation target, boolean enableFeature, float yawSpeed, float pitchSpeed, float yawReturnSpeed, float pitchReturnSpeed, int timeout, int priority, boolean clientRotation) {
        if (currentPriority > priority) {
            return;
        }

        if (currentTask.equals(RotationTask.IDLE)) {
            FreerotationHelper.active = (true);
        }

        currentTurnSpeed = yawSpeed; // Using yawSpeed as primary turn speed
        currentReturnSpeed = yawReturnSpeed; // Using yawReturnSpeed as primary return speed
        currentTimeout = timeout;
        currentPriority = priority;
        currentTask = RotationTask.AIM;

        updateRotation(target, yawSpeed, pitchSpeed);
    }

//    public static void update(Rotation targetRotation, float turnSpeed, float returnSpeed, int timeout, int priority) {
//        update(targetRotation, true, turnSpeed, turnSpeed, returnSpeed, returnSpeed, timeout, priority, false);
//    }

    public static Vector2f applySensitivityPatch(Vector2f rotation, Vector2f previousrotation) {
        double sens = mc.gameSettings.mouseSensitivity;
        double gcd = Math.pow(sens * (double) 0.6F + (double) 0.2F, 3.0D) * 8.0D;

        double prevYaw = previousrotation.x;
        double prevPitch = previousrotation.y;

        double currentYaw = rotation.x;
        double currentPitch = rotation.y;

        double yaw = (Math.ceil(((currentYaw - prevYaw) / gcd) / 0.15F) * gcd) * 0.15F;
        double pitch = (Math.ceil(((currentPitch - prevPitch) / gcd) / 0.15F) * gcd) * 0.15F;

        return new Vector2f((float) (prevYaw + yaw), (float) (prevPitch + pitch));
    }

    private static boolean updateRotation(Rotation rotation, float turnSpeed) {
        Rotation currentRotation = new Rotation(mc.player.rotationYaw, mc.player.rotationPitch);

        float yawDelta = wrapDegrees(rotation.yaw - currentRotation.yaw);
        float pitchDelta = rotation.pitch - currentRotation.pitch;

        float totalDelta = Math.abs(yawDelta) + Math.abs(pitchDelta);

        float yawSpeed = (totalDelta == 0) ? 0 : Math.abs(yawDelta / totalDelta) * turnSpeed;
        float pitchSpeed = (totalDelta == 0) ? 0 : Math.abs(pitchDelta / totalDelta) * turnSpeed;

        mc.player.rotationYaw += SensitivityUtil.getSensitivity(clamp(yawDelta, -yawSpeed, yawSpeed));
        mc.player.rotationPitch = MathHelper.clamp(mc.player.rotationPitch + SensitivityUtil.getSensitivity(clamp(pitchDelta, -pitchSpeed, pitchSpeed)), -90F, 90F);

        Rotation finalRotation = new Rotation(mc.player.rotationYaw, mc.player.rotationPitch);

        idleTicks = 0;
        return new Rotation(mc.player.rotationYaw, mc.player.rotationPitch).getDelta(rotation) < 1F;
    }

    private static boolean updateRotation(Rotation rotation, float yawSpeed, float pitchSpeed) {
        Rotation currentRotation = new Rotation(mc.player.rotationYaw, mc.player.rotationPitch);

        float yawDelta = wrapDegrees(rotation.yaw - currentRotation.yaw);
        float pitchDelta = rotation.pitch - currentRotation.pitch;

        mc.player.rotationYaw += SensitivityUtil.getSensitivity(clamp(yawDelta, -yawSpeed, yawSpeed));
        mc.player.rotationPitch = MathHelper.clamp(mc.player.rotationPitch + SensitivityUtil.getSensitivity(clamp(pitchDelta, -pitchSpeed, pitchSpeed)), -90F, 90F);

        Rotation finalRotation = new Rotation(mc.player.rotationYaw, mc.player.rotationPitch);

        idleTicks = 0;

        return finalRotation.getDelta(rotation) < (currentTask.equals(RotationTask.RESET) ? currentReturnSpeed : currentTurnSpeed);
    }

    public enum RotationTask {
        AIM,
        RESET,
        IDLE
    }
}