package fun.pasta.ui.clickGui;

import com.mojang.blaze3d.matrix.MatrixStack;
import fun.pasta.fonts.Fonts;
import fun.pasta.module.api.Module;
import fun.pasta.ui.clickGui.settings.Settings;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.key.KeyUtil;
import fun.pasta.util.math.MathUtil;
import fun.pasta.util.render.RenderUtil2D;
import fun.pasta.util.render.SmartScissor;

public class ModuleRender extends Settings {

    public final Module module;
    private final ModuleSelection selection;
    private boolean binding;

    private float scrollOffset = 0;
    private long lastScrollTime = System.currentTimeMillis();
    private boolean scrollRight = true;
    private static final float SCROLL_SPEED = 0.5f;
    private static final long SCROLL_DELAY = 1500;

    private float hoverAnim = 0;
    private float enableAnim = 0;

    private static final int ACCENT_COLOR = ColorUtil.rgba(235, 75, 65, 255);

    public ModuleRender(Module module, ModuleSelection selection) {
        this.module = module;
        this.selection = selection;
        this.width = 104;
        this.height = 16;
    }

    @Override
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        float baseHeight = 16;
        boolean hovered = isHovered(mouseX, mouseY, width, baseHeight);
        boolean selected = selection.selected == module;

        hoverAnim = MathUtil.lerp(hoverAnim, hovered ? 1f : 0f, 12);
        enableAnim = MathUtil.lerp(enableAnim, module.enable ? 1f : 0f, 12);

        int base = ColorUtil.rgba(22, 22, 26, 200);
        int hover = ColorUtil.rgba(30, 30, 36, 230);
        int active = ColorUtil.rgba(32, 32, 38, 235);
        int bg = ColorUtil.interpolate(base, hover, hoverAnim);
        bg = ColorUtil.interpolate(bg, active, enableAnim);
        if (selected) {
            bg = ColorUtil.interpolate(bg, ColorUtil.rgba(26, 26, 32, 240), 0.6f);
        }

        RenderUtil2D.drawRoundRect(x, y, width, baseHeight, 4, bg);

        if (module.enable) {
            float barW = 1.5f + (0.8f * enableAnim);
            RenderUtil2D.drawRoundRect(x + 1, y + 1, barW, baseHeight - 2, 1.5f, ACCENT_COLOR);
        }

        float nameMaxWidth = width - 28;
        String name = module.getName();
        float nameWidth = Fonts.mstr.get(11).getWidth(name);

        SmartScissor.push();
        SmartScissor.set(x + 6, y, nameMaxWidth, baseHeight);

        if (nameWidth > nameMaxWidth) {
            long now = System.currentTimeMillis();
            if (now - lastScrollTime > SCROLL_DELAY) {
                if (scrollRight) {
                    scrollOffset += SCROLL_SPEED;
                    if (scrollOffset >= nameWidth - nameMaxWidth) {
                        scrollOffset = nameWidth - nameMaxWidth;
                        scrollRight = false;
                        lastScrollTime = now;
                    }
                } else {
                    scrollOffset -= SCROLL_SPEED;
                    if (scrollOffset <= 0) {
                        scrollOffset = 0;
                        scrollRight = true;
                        lastScrollTime = now;
                    }
                }
            }
            Fonts.mstr.get(11).drawCenterY(name, x + 6 - scrollOffset, y + (baseHeight / 2f), -1);
        } else {
            Fonts.mstr.get(11).drawCenterY(name, x + 6, y + (baseHeight / 2f), -1);
        }

        SmartScissor.unSet();
        SmartScissor.pop();

        if (module.key != 0) {
            String key = KeyUtil.getKey(module.key).toUpperCase();
            float keyWidth = Fonts.mstr.get(8).getWidth(key);
            Fonts.mstr.get(8).drawCenterY(key, x + width - 20 - keyWidth, y + baseHeight / 2f, ColorUtil.rgba(140, 140, 140, 255));
        }

        float toggleSize = 8;
        float toggleX = x + width - 12;
        float toggleY = y + 4;
        RenderUtil2D.drawRoundRect(toggleX, toggleY, toggleSize, toggleSize, 2, ColorUtil.rgba(35, 36, 40, 255));
        if (module.enable) {
            RenderUtil2D.drawRoundRect(toggleX + 2, toggleY + 2, toggleSize - 4, toggleSize - 4, 1, ACCENT_COLOR);
        }

        if (binding) {
            Fonts.mstr.get(8).drawCenterY("...", x + width - 26, y + baseHeight / 2f, ColorUtil.rgba(180, 180, 180, 255));
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY, width, 16)) {
            if (mouseButton == 0) {
                module.toggle();
                selection.selected = module;
            } else if (mouseButton == 1) {
                selection.selected = module;
            } else if (mouseButton == 2) {
                binding = !binding;
            }
        }
    }

    @Override
    public void mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
    }

    @Override
    public void mouseScrolled(int mouseX, int mouseY, double delta) {
    }

    @Override
    public void keyTyped(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            int key = (keyCode == 256 || keyCode == 261) ? 0 : keyCode;
            module.key = key;
            binding = false;
        }
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
    }
}
