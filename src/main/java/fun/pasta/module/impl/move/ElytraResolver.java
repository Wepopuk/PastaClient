package fun.pasta.module.impl.move;

import com.google.common.eventbus.Subscribe;
import fun.pasta.Pasta;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.module.settings.MultiBoxOption;
import fun.pasta.module.settings.SliderOption;
import fun.pasta.util.inventory.InventoryUtil;
import fun.pasta.util.time.TimerUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.vector.Vector3d;

public class ElytraResolver extends Module {

    private final MultiBoxOption vector = new MultiBoxOption("Vectors",
            new BooleanOption("Up", true),
            new BooleanOption("Down", false),
            new BooleanOption("East", true),
            new BooleanOption("West", true),
            new BooleanOption("South", true),
            new BooleanOption("North", true));
    private final SliderOption distance = new SliderOption("Distance", 4.5F, 3.0F, 8.0F, 0.5F);
    private final BooleanOption skipCollision = new BooleanOption("Skip Collision", true);
    private final BooleanOption autoFirework = new BooleanOption("Auto Firework", true);
    private final BooleanOption freezeNearTarget = new BooleanOption("Freeze Near Target", true);

    private final TimerUtil fireworkTimer = new TimerUtil();

    public ElytraResolver() {
        super("ElytraResolver", Category.MOVE);
        add(vector, distance, skipCollision, autoFirework, freezeNearTarget);
        fireworkTimer.reset();
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

        double maxDist = distance.getValue().floatValue();
        double dist = mc.player.getDistance(target);
        if (dist > maxDist) {
            return;
        }

        Vector3d motion = mc.player.getMotion();
        double shift = 0.018D;

        if (vector.get("Up")) motion = motion.add(0.0D, shift, 0.0D);
        if (vector.get("Down")) motion = motion.add(0.0D, -shift, 0.0D);
        if (vector.get("East")) motion = motion.add(shift, 0.0D, 0.0D);
        if (vector.get("West")) motion = motion.add(-shift, 0.0D, 0.0D);
        if (vector.get("South")) motion = motion.add(0.0D, 0.0D, shift);
        if (vector.get("North")) motion = motion.add(0.0D, 0.0D, -shift);

        if (skipCollision.get()) {
            motion = motion.scale(1.01D);
        }

        if (freezeNearTarget.get() && dist < 2.0D) {
            motion = motion.scale(0.75D);
        }

        mc.player.setMotion(motion);

        if (autoFirework.get() && fireworkTimer.hasReached(900L)) {
            if (InventoryUtil.findItem(Items.FIREWORK_ROCKET) != -1) {
                InventoryUtil.useItem(Items.FIREWORK_ROCKET);
            }
            fireworkTimer.reset();
        }
    }

    private LivingEntity getTarget() {
        if (Pasta.instance == null || Pasta.instance.manager == null || Pasta.instance.manager.killAura == null) {
            return null;
        }
        return Pasta.instance.manager.killAura.target;
    }
}

