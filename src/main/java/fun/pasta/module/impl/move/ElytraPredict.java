package fun.pasta.module.impl.move;

import com.google.common.eventbus.Subscribe;
import fun.pasta.Pasta;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.SliderOption;
import fun.pasta.util.time.TimerUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Vector3d;

public class ElytraPredict extends Module {

    private final SliderOption elytraDistance = new SliderOption("Overtake Distance", 3.0F, 0.0F, 4.5F, 0.05F);
    private final TimerUtil disabledTimer = new TimerUtil();
    private boolean disabled;

    public ElytraPredict() {
        super("ElytraPredict", Category.MOVE);
        add(elytraDistance);
        disabledTimer.reset();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!enable || mc.player == null || mc.world == null || !mc.player.isElytraFlying()) {
            return;
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        if (!canPredict(target)) {
            return;
        }

        Vector3d predict = target.getPositionVec().add(target.getMotion().scale(getElytraDistance(target)));
        Vector3d to = predict.subtract(mc.player.getPositionVec());
        if (to.lengthSquared() < 1.0E-4D) {
            return;
        }

        Vector3d dir = to.normalize().scale(0.03D);
        mc.player.setMotion(mc.player.getMotion().add(dir));
    }

    public double getElytraDistance(LivingEntity target) {
        return elytraDistance.getValue().floatValue();
    }

    public boolean canPredict(LivingEntity target) {
        if (mc.player.hurtTime > 0) {
            disabled = true;
            disabledTimer.reset();
        }
        if (disabled && disabledTimer.hasReached(500L)) {
            disabled = false;
        }
        return !disabled;
    }

    private LivingEntity getTarget() {
        if (Pasta.instance == null || Pasta.instance.manager == null || Pasta.instance.manager.killAura == null) {
            return null;
        }
        return Pasta.instance.manager.killAura.target;
    }
}

