package fun.pasta.module.impl.visual.hud.impl;

import fun.pasta.event.EventRender2D;
import fun.pasta.util.IMinecraft;

public interface IRenderHud extends IMinecraft {

    void render(EventRender2D e);
    
    default void mouseClicked(double mouseX, double mouseY, int button) {}
}