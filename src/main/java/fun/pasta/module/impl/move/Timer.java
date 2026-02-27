package fun.pasta.module.impl.move;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.module.settings.SliderOption;
import fun.pasta.util.time.TimerUtil;
import net.minecraft.util.math.MathHelper;

public class Timer extends Module {

    private final SliderOption timerAmount = new SliderOption("Speed", 2.0F, 1.0F, 10.0F, 0.01F);
    private final BooleanOption smart = new BooleanOption("Smart", true);
    private final SliderOption upValue = new SliderOption("Add While Moving", 0.02F, 0.01F, 0.5F, 0.01F)
            .setVisible(smart::get);
    private final SliderOption decay = new SliderOption("Decay", 1.0F, 0.15F, 3.0F, 0.1F)
            .setVisible(smart::get);

    private final TimerUtil resetTimer = new TimerUtil();
    private float violation;

    public Timer() {
        super("Timer", Category.MOVE);
        add(timerAmount, smart, upValue, decay);
        resetTimer.reset();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!enable || mc.player == null || mc.world == null) {
            return;
        }

        if (resetTimer.hasReached(25000L)) {
            violation = 0.0F;
            resetTimer.reset();
        }

        float desired = timerAmount.getValue().floatValue();
        if (smart.get()) {
            if (canCharge()) {
                violation += upValue.getValue().floatValue();
            } else {
                violation -= decay.getValue().floatValue() * 0.05F;
            }
            violation = MathHelper.clamp(violation, 0.0F, 2.0F);
            desired = MathHelper.clamp(1.0F + violation, 1.0F, timerAmount.getValue().floatValue());
        }

        mc.timer.timerSpeed = desired;
    }

    private boolean canCharge() {
        if (mc.player.isInWater() || mc.player.isInLava() || mc.player.isElytraFlying()) {
            return false;
        }
        return Math.abs(mc.player.moveForward) > 1.0E-4F || Math.abs(mc.player.moveStrafing) > 1.0E-4F;
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            mc.timer.timerSpeed = 1.0F;
        }
        violation = 0.0F;
        super.onDisable();
    }
}

