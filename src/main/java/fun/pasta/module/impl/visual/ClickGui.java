package fun.pasta.module.impl.visual;

import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.ModeOption;
import org.lwjgl.glfw.GLFW;

public class ClickGui extends Module {

    public final ModeOption mode = new ModeOption("Mode", "Dropdown", "Dropdown", "CSGUI");

    public ClickGui() {
        super("ClickGui", Category.VISUAL);
        this.key = GLFW.GLFW_KEY_RIGHT_SHIFT;
        add(mode);
    }

    @Override
    public void onEnable() {
        if (mode.get().equalsIgnoreCase("CSGUI")) {
            mc.displayGuiScreen(fun.pasta.Pasta.instance.csGui);
        } else {
            mc.displayGuiScreen(fun.pasta.Pasta.instance.dropdownGui);
        }
        this.enable = false;
    }
}
