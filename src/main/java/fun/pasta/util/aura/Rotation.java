package fun.pasta.util.aura;

import fun.pasta.util.IMinecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;

public class Rotation implements IMinecraft {
    public float yaw, pitch;

    public Rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public float getDelta(Rotation target) {
        float yawDelta = MathHelper.wrapDegrees(target.pitch - this.yaw);
        float pitchDelta = target.yaw - this.pitch;
        return (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
    }

    public double getDeltaDouble(Rotation target) {
        double yawDelta = MathHelper.wrapDegrees(target.yaw - yaw);
        double pitchDelta = MathHelper.wrapDegrees(target.pitch - pitch);
        return Math.hypot(yawDelta, pitchDelta);
    }

    public static Vector2f camera() {
        return new Vector2f(cameraYaw(), cameraPitch());
    }

    public static float cameraYaw() {
        return MathHelper.wrapDegrees(mc.gameRenderer.getActiveRenderInfo().getYaw() + (mc.gameRenderer.getActiveRenderInfo().thirdPersonReverse ? 180 : 0));
    }

    public static float cameraPitch() {
        return (mc.gameRenderer.getActiveRenderInfo().thirdPersonReverse ? -1 : 1) * mc.gameRenderer.getActiveRenderInfo().getPitch();
    }
}