package fun.pasta.module.impl.visual;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.ModeOption;
import fun.pasta.module.settings.SliderOption;
import net.minecraft.util.math.MathHelper;

public class Aspect extends Module {

    private final ModeOption mode = new ModeOption("Ratio", "Custom", "4:3", "16:9", "1:1", "16:10", "Custom");
    private final SliderOption customRatio = new SliderOption("Value", 1.6F, 0.1F, 5.0F, 0.1F)
            .setVisible(() -> "Custom".equalsIgnoreCase(mode.get()));

    private double oldFov = -1.0D;

    public Aspect() {
        super("Aspect", Category.VISUAL);
        add(mode, customRatio);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc != null && mc.gameSettings != null) {
            oldFov = mc.gameSettings.fov;
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!enable || mc.player == null || mc.world == null || mc.gameSettings == null) {
            return;
        }

        float ratio = resolveRatio();
        float mul = (16.0F / 9.0F) / Math.max(0.1F, ratio);
        double base = oldFov > 0.0D ? oldFov : 70.0D;
        mc.gameSettings.fov = MathHelper.clamp(base * mul, 30.0D, 130.0D);
    }

    private float resolveRatio() {
        switch (mode.get()) {
            case "4:3":
                return 4.0F / 3.0F;
            case "16:9":
                return 16.0F / 9.0F;
            case "1:1":
                return 1.0F;
            case "16:10":
                return 16.0F / 10.0F;
            default:
                return customRatio.getValue().floatValue();
        }
    }

    @Override
    public void onDisable() {
        if (mc != null && mc.gameSettings != null && oldFov > 0.0D) {
            mc.gameSettings.fov = oldFov;
        }
        super.onDisable();
    }
}

