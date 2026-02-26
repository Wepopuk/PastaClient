package fun.pasta.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import fun.pasta.Pasta;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.event.EventUpdate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;

public class TriggerBot extends Module {

    public TriggerBot() {
        super("TriggerBot", Category.COMBAT);
    }

    long timeOnAttack = 0L;

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        Entity target = getValidEntity();

        if (target != null) {
            onAttack(target);
        }
    }

    private void onAttack(Entity entity) {
        if (isBestMomentCriticals(true) && System.currentTimeMillis() - timeOnAttack > 500) {

            mc.playerController.attackEntity(mc.player, entity);
            mc.player.swingArm(Hand.MAIN_HAND);

            timeOnAttack = System.currentTimeMillis();

        }
    }

    private boolean isBestMomentCriticals(boolean onlyCriticals) {

        boolean cancel = mc.player.isInWater() || mc.player.areEyesInFluid(FluidTags.WATER) ||
                mc.player.isInLava() || mc.player.isOnLadder() || mc.player.isPassenger();

        float attack = mc.player.getCooledAttackStrength(1.0f);

        if (attack < 0.92f) return false;

        if (!cancel && onlyCriticals) {
            return !mc.player.isOnLadder() && mc.player.fallDistance > 0;
        }
        return true;
    }

    private Entity getValidEntity() {
        if (mc.objectMouseOver != null && mc.objectMouseOver.getType() == RayTraceResult.Type.ENTITY) {
            Entity entity = ((EntityRayTraceResult) mc.objectMouseOver).getEntity();
            if (isValidTarget(entity)) {
                return entity;
            }
        }
        return null;
    }

    private boolean isValidTarget(Entity entity) {
        if (entity instanceof PlayerEntity player) {
            if (!player.isAlive() || player.getHealth() <= 0.0F || player == mc.player) {
                return false;
            }

            if (Pasta.instance.manager != null
                    && Pasta.instance.manager.antiBot != null
                    && Pasta.instance.manager.antiBot.enable
                    && AntiBot.isBot(player)) {
                return false;
            }

            return true;
        }

        return false;
    }
}
