package fun.pasta.ui.clickGui.components;

import com.mojang.blaze3d.matrix.MatrixStack;
import fun.pasta.util.IMinecraft;

public abstract class Component implements IMinecraft {
    public float x, y, width, height;

    public Component(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void draw(MatrixStack matrixStack, int mouseX, int mouseY);

    public void mouseClicked(double mouseX, double mouseY, int button) {
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    public void mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
    }

    public void keyTyped(int keyCode, int scanCode, int modifiers) {
    }

    public void charTyped(char codePoint, int modifiers) {
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
