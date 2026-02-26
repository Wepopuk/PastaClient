package fun.pasta.ui.clickGui.settings.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import fun.pasta.module.settings.BindOption;
import fun.pasta.ui.clickGui.ModuleRender;
import fun.pasta.ui.clickGui.settings.Settings;
import fun.pasta.util.IMinecraft;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.key.KeyUtil;
import net.minecraft.util.text.TextFormatting;
import fun.pasta.fonts.Fonts;
import fun.pasta.util.render.RenderUtil2D;
import org.lwjgl.glfw.GLFW;

public class BindObject extends Settings implements IMinecraft {

    public ModuleRender object;
    public BindOption set;

    public static BindObject bindingBind;

    public BindObject(ModuleRender object, BindOption set) {
        this.object = object;
        this.set = set;
        setting = set;
    }

    @Override
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        height = 14;

        float fontSize = 12;

        String keyName = bindingBind == this ? "..." : set.key == 0 ? "Пусто" : KeyUtil.getKey(set.key);
        float wKey = Fonts.mstr.get(fontSize).getWidth(keyName) + 4;

        Fonts.mstr.get(12).drawCenterY(set.name, x + 3, y + height / 2, ColorUtil.rgba(166, 166, 166));

        RenderUtil2D.drawRoundRect(x + width - wKey - 10, y, wKey, 10, 2, ColorUtil.rgba(52, 30, 32));

        Fonts.mstr.get(fontSize).drawCenterY(keyName, x + width - wKey - 9, y + height / 2 - 1, -1);

        super.draw(stack, mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY)) {
            if (mouseButton == 0) {
                if (bindingBind == this) {
                    bindingBind = null;
                } else {
                    bindingBind = this;
                }
            } else if (mouseButton > 1 && bindingBind == this) {
                set.key = (-100 + mouseButton);
                bindingBind = null;
            }
        }
    }

    @Override
    public void keyTyped(int keyCode, int scanCode, int modifiers) {
        if (bindingBind == this) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                set.key = (0);
                bindingBind = null;
            } else if (keyCode != GLFW.GLFW_KEY_ENTER) {
                set.key = (keyCode);
                bindingBind = null;
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
    }

    @Override
    public void mouseScrolled(int mouseX, int mouseY, double delta) {
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
    }
}