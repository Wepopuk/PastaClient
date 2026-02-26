package fun.pasta.ui.clickGui.settings.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import fun.pasta.fonts.Fonts;
import fun.pasta.module.settings.SliderOption;
import fun.pasta.ui.clickGui.ModuleRender;
import fun.pasta.ui.clickGui.settings.Settings;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.math.MathUtil;
import fun.pasta.util.render.RenderUtil2D;


public class SliderObject extends Settings {

    public ModuleRender object;
    public SliderOption set;
    public boolean sliding;
    public String value;
    public float animatedVal;

    public SliderObject(ModuleRender object, SliderOption set) {
        this.object = object;
        this.set = set;
        setting = set;
    }

    @Override
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        height = 20;

        float wValue = width;

        if (sliding) {
            float value2 = (mouseX - x - 5) / (width - 15) * (set.max - set.min) + set.min;
            value2 = (float) MathUtil.round(value2, set.increment);
            set.setValue(value2);
        }

        Fonts.mstr.get(12).drawCenterY(set.name, x + 3, y + 6, ColorUtil.rgba(166, 166, 166));

        float sliderWidth = ((set.getValue().floatValue() - set.min) / (set.max - set.min)) * (width - 15);
        animatedVal = MathUtil.lerp(animatedVal, sliderWidth, 20);

        RenderUtil2D.drawRoundRect(x + 5, y + height / 2 + 1, width - 10 - 5, 5, 2, ColorUtil.defaultRed(120));
        RenderUtil2D.drawRoundRect(x + 5 + 1.5f, y + height / 2 + 2, animatedVal, 3.1f, 2, ColorUtil.defaultRed);
        RenderUtil2D.drawCircle(x + 5 + animatedVal, y + height / 2 + 3.5f, 5, -1);

        String value = String.format("%.1f", set.getValue().floatValue());
        Fonts.mstr.get(12).drawCenterY(value, x + width - Fonts.mstr.get(14).getWidth(value) - 10, y + 6, -1);

        super.draw(stack, mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY, x, y, width, height) && mouseButton == 0) {
            sliding = true;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        sliding = false;
    }

    @Override
    public void mouseScrolled(int mouseX, int mouseY, double delta) {
    }

    @Override
    public void keyTyped(int keyCode, int scanCode, int modifiers) {
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
    }
}