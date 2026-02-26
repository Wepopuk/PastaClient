package fun.pasta.ui.clickGui.settings;

import com.mojang.blaze3d.matrix.MatrixStack;

public interface ISetting {

    void init();

    void draw(MatrixStack stack, int mouseX, int mouseY);

    void mouseClicked(int mouseX, int mouseY, int mouseButton);

    void mouseReleased(int mouseX, int mouseY, int mouseButton);

    void mouseScrolled(int mouseX, int mouseY, double delta);

    void mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY);

    void keyTyped(int keyCode, int scanCode, int modifiers);

    void charTyped(char codePoint, int modifiers);
}