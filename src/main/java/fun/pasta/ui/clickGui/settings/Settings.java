package fun.pasta.ui.clickGui.settings;

import com.mojang.blaze3d.matrix.MatrixStack;
import fun.pasta.ui.clickGui.Panel;
import fun.pasta.util.IMinecraft;

public abstract class Settings implements ISetting, IMinecraft {

    public float x, y, width, height;

    public Panel panel;

    public Setting setting;

    public boolean isHovered(int mouseX, int mouseY, float width, float height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    public boolean isHovered(int mouseX, int mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height;
    }

    public boolean isHovered(int mouseX, int mouseY, float height) {
        return mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height;
    }

    @Override
    public void init() {
    }

    @Override
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
    }

    @Override
    public abstract void mouseClicked(int mouseX, int mouseY, int mouseButton);

    @Override
    public abstract void mouseScrolled(int mouseX, int mouseY, double delta);

    @Override
    public abstract void mouseReleased(int mouseX, int mouseY, int mouseButton);

    @Override
    public void mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
    }

    @Override
    public abstract void keyTyped(int keyCode, int scanCode, int modifiers);

    @Override
    public abstract void charTyped(char codePoint, int modifiers);

}