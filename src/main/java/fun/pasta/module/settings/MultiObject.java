package fun.pasta.module.settings;

import com.mojang.blaze3d.matrix.MatrixStack;
import fun.pasta.fonts.Fonts;
import fun.pasta.ui.clickGui.ModuleRender;
import fun.pasta.ui.clickGui.settings.Settings;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.math.MathUtil;
import fun.pasta.util.render.RenderUtil2D;
import fun.pasta.util.render.SmartScissor;

import java.awt.*;

public class MultiObject extends Settings {

    public MultiBoxOption set;
    public ModuleRender object;

    public MultiObject(ModuleRender object, MultiBoxOption set) {
        this.object = object;
        this.set = set;
        setting = set;
    }

    boolean open = false;
    float animHeight;

    @Override
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        super.draw(stack, mouseX, mouseY);

        float fontSize = 12;

        RenderUtil2D.drawRoundRect(x + 3, y + 2, width - 12, animHeight - 6, 4, ColorUtil.rgba(51, 30, 32));

        drawCurrentSelect(fontSize);

        drawList(fontSize);
    }

    private void drawList(float fontSize) {
        float h = 0;
        int i = 0;

        SmartScissor.push();
        SmartScissor.set(x, y + 14, width, animHeight - 18);

        for (BooleanOption mode : set.options) {
            Fonts.mstr.get(fontSize).drawCenterY(mode.name, x + 3 + 4, y + 1 + 18 + h, set.get(i) ? ColorUtil.rgba(181, 160, 162) : ColorUtil.rgba(81, 60, 62));

            h += 10;
            i++;
        }

        if (animHeight > 19) {
            RenderUtil2D.drawRect(x + 3 + 10, y + 2 + 12, width - 12 - 20, 0.5f, ColorUtil.rgba(81, 60, 62));
        }

        SmartScissor.unSet();
        SmartScissor.pop();

        animHeight = MathUtil.lerp(animHeight, open ? 18 + h : 18, 15);

        if (open) {
            height += h;
        } else {
            height = 18;
        }
    }

    private void drawCurrentSelect(float fontSize) {
        float xx = 0;
        int index = 0, i = 0;
        int totalOptions = set.options.size();

        SmartScissor.push();
        SmartScissor.set(x, y, width, animHeight);

        for (BooleanOption mode : set.options) {
            if (set.get(index)) {
                String displayText = "";

                if (mode.name.length() >= 2) {
                    displayText = mode.name.substring(0, 2);
                } else if (mode.name.length() == 1) {
                    displayText = mode.name;
                }

                if (!displayText.isEmpty()) {
                    if (index < totalOptions - 1) {
                        displayText += ",";
                    }

                    Fonts.mstr.get(fontSize).drawCenterY(displayText, x + 3 + 4 + xx, y + 3 + (height - 6) / 2f, -1);

                    xx += Fonts.mstr.get(fontSize).getWidth(displayText);
                }
            }
            if (set.get(index)) {
                i++;
            }
            index++;
        }

        SmartScissor.unSet();
        SmartScissor.pop();

        if (i == 0) {
            Fonts.mstr.get(fontSize).drawCenterY("Пусто", x + 3 + 4, y + 3 + (height - 6) / 2f, -1);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        float fontSize = 14;

        float h = 0;
        int i = 0;
        for (BooleanOption mode : set.options) {
            if (MathUtil.isHover(mouseX, mouseY, x + 3, y + 14 + h, width - 12, 10) && mouseButton == 0) {
                set.set(i, !set.get(i));
            }

            h += 10;
            i++;
        }

        clickOpen(mouseX, mouseY, mouseButton);
    }

    private void clickOpen(int mouseX, int mouseY, int mouseButton) {
        if (MathUtil.isHover(mouseX, mouseY, x + 3, y + 2, width - 12, 18 - 6) && (mouseButton == 0 || mouseButton == 1)) {
            open = !open;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {

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