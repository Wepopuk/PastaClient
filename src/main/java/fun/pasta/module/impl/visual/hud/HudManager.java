package fun.pasta.module.impl.visual.hud;

import com.google.common.eventbus.Subscribe;
import fun.pasta.Pasta;
import fun.pasta.drag.Dragging;
import fun.pasta.event.EventRender2D;
import fun.pasta.module.api.Module;
import fun.pasta.module.impl.visual.hud.impl.IRenderHud;

import java.util.ArrayList;
import java.util.List;

public class HudManager {
    
    private final List<IRenderHud> hudElements = new ArrayList<>();
    
    public HudManager() {
        Module hudModule = Pasta.instance.manager.getModule("HUD");
        
        // Создаем dragging для каждого элемента
        Dragging watermarkDrag = Pasta.instance.createDrag(hudModule, "watermark", 5, 5);
        Dragging potionsDrag = Pasta.instance.createDrag(hudModule, "potions", 5, 50);
        Dragging keybindsDrag = Pasta.instance.createDrag(hudModule, "keybinds", 5, 100);
        Dragging staffListDrag = Pasta.instance.createDrag(hudModule, "stafflist", 5, 140);
        Dragging targetHudDrag = Pasta.instance.createDrag(hudModule, "targethud", 100, 100);
        Dragging musicPlayerDrag = Pasta.instance.createDrag(hudModule, "musicplayer", 5, 180);
        
        hudElements.add(new WaterMarkRender(watermarkDrag));
        hudElements.add(new PotionsRender(potionsDrag));
        hudElements.add(new KeyBindsRender(keybindsDrag));
        hudElements.add(new StaffListRender(staffListDrag));
        hudElements.add(new TargetHudRender(targetHudDrag));
        hudElements.add(new MusicPlayerRender(musicPlayerDrag));
        hudElements.add(new NotificationRender());
        
        Pasta.instance.eventBus.register(this);
    }
    
    @Subscribe
    public void onRender2D(EventRender2D event) {
        fun.pasta.module.impl.visual.Hud hudModule = (fun.pasta.module.impl.visual.Hud) Pasta.instance.manager
                .getModule("HUD");
        if (hudModule == null || !hudModule.enable) {
            return;
        }

        for (IRenderHud hudElement : hudElements) {
            hudElement.render(event);
        }
    }
    
    public void mouseClicked(double mouseX, double mouseY, int button) {
        for (IRenderHud hud : hudElements) {
            hud.mouseClicked(mouseX, mouseY, button);
        }
    }
}
