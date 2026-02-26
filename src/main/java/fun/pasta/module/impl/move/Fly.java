package fun.pasta.module.impl.move;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventTick;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.settings.ModeSetting;
import fun.pasta.settings.NumberSetting;
import fun.pasta.util.IMinecraft;

public class Fly extends Module {

    public Fly() {
        super("Fly", Category.MOVE);
    }

    @Subscribe
    public void onTick(EventTick e) {
        if (mc.player == null) return;

        mc.player.abilities.isFlying = true;
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;

        mc.player.abilities.isFlying = false;
        super.onDisable();
    }
}