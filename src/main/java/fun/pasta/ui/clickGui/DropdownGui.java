package fun.pasta.ui.clickGui;

import com.mojang.blaze3d.matrix.MatrixStack;
import fun.pasta.fonts.Fonts;
import fun.pasta.util.IMinecraft;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.render.RenderUtil2D;
import net.minecraft.client.gui.screen.Screen;
import fun.pasta.module.api.Category;
import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.List;

public class DropdownGui extends Screen implements IMinecraft {

    private final List<Panel> panels = new ArrayList<>();
    private final ModuleSelection selection = new ModuleSelection();
    private final SettingsPanel settingsPanel = new SettingsPanel(selection);
    private final ModuleSelection popupSelection = new ModuleSelection();
    private final SettingsPanel popupPanel = new SettingsPanel(popupSelection);
    private final float popupWidth = 120;
    private final float popupHeight = 170;

    private final float settingsWidth = 110;
    private final float panelHeight = 200;
    private final float spacing = 3;
    private final float settingsGap = 8;
    private final float panelShift = -80;
    private final float settingsShift = 10;

    private String searchQuery = "";
    private boolean searchFocused = false;
    private final float searchWidth = 96;
    private final float searchHeight = 12;

    private float panelsStartX;
    private float panelsWidth;
    private float panelsTop;
    private float popupRenderX;
    private float popupRenderY;

    public DropdownGui() {
        super(ITextComponent.getTextComponentOrEmpty(""));
        for (Category category : Category.values()) {
            panels.add(new Panel(category, selection));
        }
    }

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        RenderUtil2D.drawRect(0, 0, width, height, ColorUtil.rgba(0, 0, 0, 120));

        float totalPanelsWidth = 0;
        for (Panel panel : panels) {
            totalPanelsWidth += panel.width;
        }
        totalPanelsWidth += spacing * (panels.size() - 1);

        float totalWidth = totalPanelsWidth + settingsGap + settingsWidth;
        float baseStartX = (width - totalWidth) / 2f;
        float startY = (height - panelHeight) / 2f;

        float panelStartX = baseStartX + panelShift;
        float settingsX = baseStartX + totalPanelsWidth + settingsGap + settingsShift;

        float minLeft = 6;
        if (panelStartX < minLeft) {
            float delta = minLeft - panelStartX;
            panelStartX += delta;
            settingsX += delta;
        }

        float maxRight = width - 6;
        float rightEdge = settingsX + settingsWidth;
        if (rightEdge > maxRight) {
            float delta = rightEdge - maxRight;
            panelStartX -= delta;
            settingsX -= delta;
        }

        panelsStartX = panelStartX;
        panelsWidth = totalPanelsWidth;
        panelsTop = startY;

        float offset = panelStartX;
        for (Panel panel : panels) {
            panel.x = offset;
            panel.y = startY;
            panel.height = panelHeight;
            panel.setFilter(searchQuery);
            panel.draw(stack, mouseX, mouseY);
            offset += panel.width + spacing;
        }

        settingsPanel.setBounds(settingsX, startY, settingsWidth, panelHeight);
        settingsPanel.draw(stack, mouseX, mouseY);

        renderPopup(stack, mouseX, mouseY);
        renderSearch(stack, mouseX, mouseY);

        super.render(stack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton == 0 && isInSearch(mouseX, mouseY)) {
            searchFocused = true;
        } else if (mouseButton == 0) {
            searchFocused = false;
        }
        if (mouseButton == 0 && selection.popupOpen && !isInPopup(mouseX, mouseY)) {
            selection.popupOpen = false;
        }
        panels.forEach(panel -> panel.mouseClicked((int) mouseX, (int) mouseY, mouseButton));
        settingsPanel.mouseClicked(mouseX, mouseY, mouseButton);
        if (selection.popupOpen) {
            popupPanel.mouseClicked(mouseX, mouseY, mouseButton);
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        panels.forEach(panel -> panel.mouseReleased((int) mouseX, (int) mouseY, mouseButton));
        settingsPanel.mouseReleased(mouseX, mouseY, mouseButton);
        if (selection.popupOpen) {
            popupPanel.mouseReleased(mouseX, mouseY, mouseButton);
        }
        return super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
        panels.forEach(panel -> panel.mouseDragged(mouseX, mouseY, mouseButton, deltaX, deltaY));
        settingsPanel.mouseDragged(mouseX, mouseY, mouseButton);
        if (selection.popupOpen) {
            popupPanel.mouseDragged(mouseX, mouseY, mouseButton);
        }
        return super.mouseDragged(mouseX, mouseY, mouseButton, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        panels.forEach(panel -> panel.mouseScrolled((int) mouseX, (int) mouseY, delta));
        settingsPanel.mouseScrolled(mouseX, mouseY, delta);
        if (selection.popupOpen) {
            popupPanel.mouseScrolled(mouseX, mouseY, delta);
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocused && keyCode == 259 && !searchQuery.isEmpty()) {
            searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            return true;
        }
        if (searchFocused && keyCode == 256) {
            searchFocused = false;
            return true;
        }
        panels.forEach(panel -> panel.keyTyped(keyCode, scanCode, modifiers));
        settingsPanel.keyTyped(keyCode);
        if (selection.popupOpen) {
            popupPanel.keyTyped(keyCode);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchFocused && codePoint >= 32 && codePoint != 127) {
            searchQuery += codePoint;
            return true;
        }
        panels.forEach(panel -> panel.charTyped(codePoint, modifiers));
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderSearch(MatrixStack stack, int mouseX, int mouseY) {
        float x = getSearchX();
        float y = getSearchY();
        boolean hovered = isInSearch(mouseX, mouseY);

        int bg = hovered || searchFocused ? ColorUtil.rgba(28, 28, 34, 230) : ColorUtil.rgba(20, 20, 26, 210);
        RenderUtil2D.drawRoundRect(x, y, searchWidth, searchHeight, 8, bg);
        RenderUtil2D.drawRoundRect(x + 1, y + 1, searchWidth - 2, searchHeight - 2, 7, ColorUtil.rgba(16, 16, 20, 200));

        float iconX = x + 6;
        float iconY = y + 5;
        RenderUtil2D.drawCircle(iconX + 3, iconY + 3, 6, ColorUtil.rgba(120, 120, 120, 255));
        RenderUtil2D.drawRect(iconX + 6, iconY + 6, 4, 1, ColorUtil.rgba(120, 120, 120, 255));

        String text = searchQuery.isEmpty() ? "\u041f\u043e\u0438\u0441\u043a \u043c\u043e\u0434\u0443\u043b\u0435\u0439..." : searchQuery;
        int color = searchQuery.isEmpty() ? ColorUtil.rgba(120, 120, 120, 255) : ColorUtil.rgba(220, 220, 220, 255);
        Fonts.mstr.get(10).drawString(stack, text, x + 20, y + 4, color);
    }

    private boolean isInSearch(double mouseX, double mouseY) {
        float x = getSearchX();
        float y = getSearchY();
        return mouseX >= x && mouseX <= x + searchWidth && mouseY >= y && mouseY <= y + searchHeight;
    }

    private float getSearchX() {
        return panelsStartX + (panelsWidth / 2f) - (searchWidth / 2f);
    }

    private float getSearchY() {
        float y = panelsTop + panelHeight + 16;
        float maxY = height - searchHeight - 6;
        return Math.min(y, maxY);
    }

    private void renderPopup(MatrixStack stack, int mouseX, int mouseY) {
        if (!selection.popupOpen || selection.popupModule == null) {
            return;
        }
        popupSelection.selected = selection.popupModule;
        float px = selection.popupX;
        float py = selection.popupY;

        float minX = 6;
        float maxX = width - popupWidth - 6;
        float minY = 6;
        float maxY = height - popupHeight - 6;

        px = Math.max(minX, Math.min(px, maxX));
        py = Math.max(minY, Math.min(py, maxY));

        popupRenderX = px;
        popupRenderY = py;
        popupPanel.setBounds(px, py, popupWidth, popupHeight);
        popupPanel.draw(stack, mouseX, mouseY);
    }

    private boolean isInPopup(double mouseX, double mouseY) {
        if (!selection.popupOpen) {
            return false;
        }
        return mouseX >= popupRenderX && mouseX <= popupRenderX + popupWidth
                && mouseY >= popupRenderY && mouseY <= popupRenderY + popupHeight;
    }
}
