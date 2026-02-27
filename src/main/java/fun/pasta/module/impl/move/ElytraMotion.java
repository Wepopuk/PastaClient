package fun.pasta.module.impl.move;

import com.google.common.eventbus.Subscribe;
import fun.pasta.Pasta;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.SliderOption;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Vector3d;

public class ElytraMotion extends Module {

    private final SliderOption distance = new SliderOption("Target Distance", 2.0F, 1.25F, 2.5F, 0.05F);

    public ElytraMotion() {
        super("ElytraMotion", Category.MOVE);
        add(distance);
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

        double desired = distance.getValue().floatValue();
        double dist = mc.player.getDistance(target);
        Vector3d to = target.getPositionVec().subtract(mc.player.getPositionVec());
        double len = to.length();
        if (len < 1.0E-4D) {
            return;
        }
        Vector3d dir = to.scale(1.0D / len);

        Vector3d motion = mc.player.getMotion();
        if (dist > desired + 0.25D) {
            mc.player.setMotion(motion.add(dir.scale(0.045D)));
        } else if (dist < desired - 0.25D) {
            mc.player.setMotion(motion.subtract(dir.scale(0.040D)));
        }
    }

    private LivingEntity getTarget() {
        if (Pasta.instance == null || Pasta.instance.manager == null || Pasta.instance.manager.killAura == null) {
            return null;
        }
        return Pasta.instance.manager.killAura.target;
    }
}

