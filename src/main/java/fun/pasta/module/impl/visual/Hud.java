package fun.pasta.module.impl.visual;

import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.module.settings.SliderOption;

public class Hud extends Module {

    public final BooleanOption watermark = new BooleanOption("Watermark", true);
    // Р-С'Рё Р?Р°С?С'С?Р?Р№РєРё Р?Рч Р?Р?Р+Р°Р?Р>С?РчР? Р? РєР>РёРє Р?С?Рё, Р?Р?Рё С?РїС?Р°Р?Р>С?С?С'С?С? С╪РчС?РчР· РєР?Р?С'РчРєС?С'Р?Р?Рч Р?РчР?С? Р?Р° Р?Р°С'РчС?Р?Р°С?РєРч
    public final BooleanOption showFps = new BooleanOption("Show FPS", true);
    public final BooleanOption showUid = new BooleanOption("Show UID", true);
    public final BooleanOption showBps = new BooleanOption("Show BPS", true);
    public final BooleanOption showPing = new BooleanOption("Show Ping", true);
    
    public final BooleanOption potions = new BooleanOption("Potions", true);
    public final BooleanOption keyBinds = new BooleanOption("Key Binds", true);
    public final BooleanOption staffList = new BooleanOption("Staff List", true);
    public final BooleanOption targetHud = new BooleanOption("TargetHud", true);
    public final BooleanOption musicPlayer = new BooleanOption("MusicPlayer", true);
    public final BooleanOption notifications = new BooleanOption("Notifications", true);
    
    // Blur and opacity settings
    public final BooleanOption hudBlur = new BooleanOption("HUD Blur", true);
    public final SliderOption hudOpacity = new SliderOption("HUD Opacity", 1.0f, 0.1f, 1.0f, 0.05f);

    public Hud() {
        super("HUD", Category.VISUAL);
        // РўР?Р>С?РєР? Р?С?Р?Р?Р?Р?С<Рч Р?Р°С?С'С?Р?Р№РєРё Р? РєР>РёРє Р?С?Рё
        add(watermark, potions, keyBinds, staffList, targetHud, musicPlayer, notifications, hudBlur, hudOpacity);
        this.enable = true; // Enabled by default
    }
}
