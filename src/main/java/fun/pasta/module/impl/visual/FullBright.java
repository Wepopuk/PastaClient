package fun.pasta.module.impl.visual;

import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.ModeOption;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;

public class FullBright extends Module {

    public final ModeOption mode = new ModeOption("Mode", "Gamma", "Potion", "Gamma");

    private final Minecraft mc = Minecraft.getInstance();
    private double oldGamma = 1.0;
    private String lastMode = "";

    public FullBright() {
        super("FullBright", Category.VISUAL);
        add(mode);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        lastMode = "";
        switchMode(mode.get());
        lastMode = mode.get();
    }

    @Override
    public void onDisable() {
        super.onDisable();

        if (mc != null && mc.gameSettings != null)
            mc.gameSettings.gamma = oldGamma;

        if (mc != null && mc.player != null)
            mc.player.removePotionEffect(Effects.NIGHT_VISION);
    }

    public void onTick() {
        if (!enable || mc == null) return;

        String current = mode.get();

        if (!current.equals(lastMode)) {
            switchMode(current);
            lastMode = current;
        }

        if (current.equals("Potion") && mc.player != null) {
            mc.player.addPotionEffect(
                    new EffectInstance(Effects.NIGHT_VISION, 220, 0, false, false)
            );
        }
    }

    private void switchMode(String modeName) {
        if (mc.gameSettings != null)
            mc.gameSettings.gamma = oldGamma;

        if (mc.player != null)
            mc.player.removePotionEffect(Effects.NIGHT_VISION);

        if (modeName.equals("Gamma") && mc.gameSettings != null) {
            oldGamma = mc.gameSettings.gamma;
            mc.gameSettings.gamma = 15.0;
        }
    }
}
