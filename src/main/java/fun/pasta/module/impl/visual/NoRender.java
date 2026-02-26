package fun.pasta.module.impl.visual;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventCancelOverlay;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.module.settings.MultiBoxOption;
import net.minecraft.potion.Effects;

public class NoRender extends Module {

    public MultiBoxOption element = new MultiBoxOption("Elements",
            new BooleanOption("Fire", true),
            new BooleanOption("BossBar", true),
            new BooleanOption("Scoreboard", true),
            new BooleanOption("Titles", true),
            new BooleanOption("Totem", true),
            new BooleanOption("Fog", true),
            new BooleanOption("HurtCam", true),
            new BooleanOption("Rain", true),
            new BooleanOption("Blindness", true));

    public NoRender() {
        super("NoRender", Category.VISUAL);
        add(element);
    }

    @Subscribe
    private void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        boolean israining = mc.world.isRaining() && element.get("Rain");
        boolean hasEffects = (mc.player.isPotionActive(Effects.BLINDNESS)
                || mc.player.isPotionActive(Effects.NAUSEA)) && element.get("Blindness");

        if (israining) {
            mc.world.setRainStrength(0);
            mc.world.setThunderStrength(0);
        }

        if (hasEffects) {
            mc.player.removePotionEffect(Effects.NAUSEA);
            mc.player.removePotionEffect(Effects.BLINDNESS);
        }
    }

    @Subscribe
    private void onEventCancelOverlay(EventCancelOverlay eventCancelOverlay) {
        boolean cancelOverlay = switch (eventCancelOverlay.overlayType) {
            case FIRE_OVERLAY -> element.get("Fire");
            case BOSS_LINE -> element.get("BossBar");
            case SCOREBOARD -> element.get("Scoreboard");
            case TITLES -> element.get("Titles");
            case TOTEM -> element.get("Totem");
            case FOG -> element.get("Fog");
            case HURT -> element.get("HurtCam");
        };

        if (cancelOverlay) {
            eventCancelOverlay.cancel();
        }
    }
}
