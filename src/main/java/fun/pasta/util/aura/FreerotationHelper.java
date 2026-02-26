package fun.pasta.util.aura;

import com.google.common.eventbus.Subscribe;
import net.minecraft.util.math.MathHelper;
import fun.pasta.Pasta;
import fun.pasta.event.EventRotate;
import fun.pasta.event.EventRotation;
import fun.pasta.util.IMinecraft;

public class FreerotationHelper implements IMinecraft {

    public FreerotationHelper() {
        Pasta.instance.eventBus.register(this);
    }

    public static boolean active;
    public static float freeYaw, freePitch;

    @Subscribe
    public void onEvent(EventRotate event) {
        if (active) {
            rotateTowards(event.yaw, event.pitch);
            event.cancel();
        }
    }

    @Subscribe
    public void onEvent(EventRotation event) {
        if (active) {
            event.yaw = (freeYaw);
            event.pitch = (freePitch);
        } else {
            freeYaw = event.yaw;
            freePitch = event.pitch;
        }
    }

    private void rotateTowards(double targetYaw, double targetPitch) {
        freePitch = MathHelper.clamp((float) (freePitch + targetPitch * 0.15D), -90.0F, 90.0F);
        freeYaw = (float) (freeYaw + targetYaw * 0.15D);
    }
}