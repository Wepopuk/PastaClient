package fun.pasta.ui.clickGui.settings.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import fun.pasta.fonts.Fonts;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.ui.clickGui.ModuleRender;
import fun.pasta.ui.clickGui.settings.Settings;
import fun.pasta.util.IMinecraft;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.math.MathUtil;
import fun.pasta.util.render.RenderUtil2D;

public class BooleanObject extends Settings implements IMinecraft {

    public ModuleRender object;
    public BooleanOption set;
    public float enabledAnimation = 0;

    public BooleanObject(ModuleRender object, BooleanOption set) {
        this.object = object;
        this.set = set;
        setting = set;
    }

    @Override
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        height = 14;

        Fonts.mstr.get(12).drawCenterY(set.name, x + 3, y + height / 2, ColorUtil.rgba(166, 166, 166));

        double max = !set.get() ? 0 : 9;
        this.enabledAnimation = MathUtil.lerp(enabledAnimation, (float) max, 10);

        int color = ColorUtil.interpolateColor(ColorUtil.rgba(35, 165, 90, 255), ColorUtil.rgba(128, 132, 142, 255), enabledAnimation / 9);

        RenderUtil2D.drawRoundRect(x + width - 22 - 5, y + 3.5f, 17, 7, 3, color);
        RenderUtil2D.drawCircle(x + width - 22 - 1 + enabledAnimation, y + 7, 5, -1);

        super.draw(stack, mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0) {
            if (isHovered(mouseX, mouseY)) {
                set.toggle();
            }
        }
        System.out.println("1");
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {

    }

    @Override
    public void keyTyped(int keyCode, int scanCode, int modifiers) {

    }

    @Override
    public void mouseScrolled(int mouseX, int mouseY, double delta) {

    }

    @Override
    public void charTyped(char codePoint, int modifiers) {

    }
}
