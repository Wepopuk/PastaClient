package fun.pasta.ui.clickGui;

import com.mojang.blaze3d.matrix.MatrixStack;
import fun.pasta.Pasta;
import fun.pasta.fonts.Fonts;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.ui.clickGui.components.ModuleCard;
import fun.pasta.ui.clickGui.components.SidebarItem;
import fun.pasta.ui.clickGui.theme.ThemeManager;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.IMinecraft;
import fun.pasta.util.render.RenderUtil2D;
import fun.pasta.util.render.SmartScissor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CSGui extends Screen implements IMinecraft {

    private final List<SidebarItem> sidebarItems = new ArrayList<>();
    private final List<ModuleCard> moduleCards = new ArrayList<>();
    private Category currentCategory = Category.COMBAT;

    private final float width_gui = 380; // Shrunk even further
    private final float height_gui = 260; // Shrunk even further
    private final float headerHeight = 30;

    private float scrollY = 0;
    private float targetScrollY = 0;
    private float maxScroll = 0;
    private float indicatorX = -1; // Animation field
    
    // Theme manager UI - right bottom corner, always visible
    private int editingColorType = 0; // 0=accent, 1=text, 2=secondary, 3=background
    
    // Color picker dragging
    private boolean draggingRed = false;
    private boolean draggingGreen = false;
    private boolean draggingBlue = false;

    public CSGui() {
        super(ITextComponent.getTextComponentOrEmpty(""));

        for (Category category : Category.values()) {
            sidebarItems.add(new SidebarItem(category, () -> {
                currentCategory = category;
                refreshModules();
                targetScrollY = 0;
            }));
        }
        refreshModules();
    }

    private void refreshModules() {
        moduleCards.clear();
        for (Module module : Pasta.instance.manager.getModulesByCategory(currentCategory)) {
            moduleCards.add(new ModuleCard(module));
        }
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        // Animate scroll
        scrollY = fun.pasta.util.math.MathUtil.lerp(scrollY, targetScrollY, 10);
        
        // Background with optional blur effect
        if (ThemeManager.INSTANCE.isBlurEnabled()) {
            RenderUtil2D.drawRect(0, 0, width, height, ColorUtil.rgba(0, 0, 0, (int)(200 * ThemeManager.INSTANCE.getBlurIntensity())));
        } else {
            RenderUtil2D.drawRect(0, 0, width, height, ColorUtil.rgba(0, 0, 0, 180));
        }

        float x = (width - width_gui) / 2f;
        float y = (height - height_gui) / 2f;

        // Background (Main panel)
        RenderUtil2D.drawRoundRect(x, y, width_gui, height_gui, 10, ColorUtil.rgba(14, 15, 17, 255));

        // Header Rect (Fully Oval categories box)
        float totalNavWidth = Category.values().length * 30;
        float navBoxW = totalNavWidth + 10;
        float navBoxX = x + (width_gui - navBoxW) / 2f;
        float navBoxY = y + 5;
        RenderUtil2D.drawRoundRect(navBoxX, navBoxY, navBoxW, 18, 9, ColorUtil.rgba(20, 21, 23, 255));

        // Separator Line (More visible)
        RenderUtil2D.drawRect(x, y + headerHeight - 1, width_gui, 1, ColorUtil.rgba(255, 255, 255, 25));

        // Navigation Bar (Horizontal Icons)
        float navX = navBoxX + 5;
        float navY = navBoxY + 2;

        float targetIndicatorX = -100;

        for (Category category : Category.values()) {
            boolean active = (category == currentCategory);
            int color = active ? Color.WHITE.getRGB() : ColorUtil.rgba(100, 100, 105, 255);

            // Hover check for icons
            if (mouseX >= navX && mouseX <= navX + 25 && mouseY >= navY && mouseY <= navY + 15) {
                if (!active)
                    color = ColorUtil.rgba(180, 180, 185, 255);
            }

            Fonts.categoryIcon.get(12).drawString(ms, category.icon, navX + 9, navY + 4, color);

            if (active) {
                targetIndicatorX = navX; // Standardized to slot start
            }

            navX += 30;
        }

        // Sliding White Dash (Precisely centered under icon)
        if (indicatorX == -1)
            indicatorX = targetIndicatorX;
        indicatorX = fun.pasta.util.math.MathUtil.lerp(indicatorX, targetIndicatorX, 10);
        RenderUtil2D.drawRoundRect(indicatorX + 6, navBoxY + 14, 14, 1.2f, 0.5f, Color.WHITE.getRGB());

        // Content Area (3-Column Grid)
        float contentX = x + 10;
        float contentY = y + headerHeight + 5;
        float contentWidth = width_gui - 20;
        float contentHeight = height_gui - headerHeight - 15;
        float cardWidth = (contentWidth - 16) / 3f;

        SmartScissor.push();
        SmartScissor.set(contentX, contentY - 5, contentWidth, contentHeight + 10);

        float[] lastYs = new float[] { contentY + scrollY, contentY + scrollY, contentY + scrollY };

        for (int i = 0; i < moduleCards.size(); i++) {
            ModuleCard card = moduleCards.get(i);
            int column = i % 3;
            card.width = cardWidth;
            card.height = card.calculateHeight();
            card.x = contentX + column * (cardWidth + 8);
            card.y = lastYs[column];

            card.draw(ms, mouseX, mouseY);
            lastYs[column] += card.height + 8;
        }

        float contentMaxY = Math.max(lastYs[0], Math.max(lastYs[1], lastYs[2]));
        maxScroll = Math.max(0, contentMaxY - (contentY + scrollY) - contentHeight);

        SmartScissor.unSet();
        SmartScissor.pop();

        // Scrollbar logic
        if (maxScroll > 0) {
            float sbWidth = 2.5f;
            float sbX = x + width_gui - 5;
            float sbY = contentY;
            float sbHeight = contentHeight;

            float thumbHeight = (contentHeight / (contentMaxY - (contentY + scrollY))) * contentHeight;
            float scrollPercent = -scrollY / maxScroll;
            float thumbPos = scrollPercent * (contentHeight - thumbHeight);

            RenderUtil2D.drawRoundRect(sbX, sbY, sbWidth, sbHeight, sbWidth / 2f, ColorUtil.rgba(255, 255, 255, 10)); // Track
            RenderUtil2D.drawRoundRect(sbX, sbY + thumbPos, sbWidth, thumbHeight, sbWidth / 2f,
                    ColorUtil.rgba(255, 255, 255, 50)); // Thumb
        }

        // Color Picker Panel (right bottom corner of screen, always visible)
        renderColorPicker(ms, mouseX, mouseY);

        super.render(ms, mouseX, mouseY, partialTicks);
    }
    
    private void renderColorPicker(MatrixStack ms, int mouseX, int mouseY) {
        float panelW = 170;
        float panelH = 240;
        float panelX = width - panelW - 15;
        float panelY = height - panelH - 15;
        
        // Panel background
        RenderUtil2D.drawRoundRect(panelX - 1, panelY - 1, panelW + 2, panelH + 2, 8, ColorUtil.rgba(40, 40, 40, 255));
        RenderUtil2D.drawRoundRect(panelX, panelY, panelW, panelH, 7, ColorUtil.rgba(18, 18, 20, 255));
        
        // Header
        Fonts.mstr.get(13).drawString(ms, "Color Picker", panelX + 10, panelY + 8, -1);
        RenderUtil2D.drawRect(panelX + 10, panelY + 22, panelW - 20, 1, ColorUtil.rgba(50, 50, 50, 255));
        
        float contentY = panelY + 30;
        
        // Theme list
        Fonts.mstr.get(10).drawString(ms, "Themes", panelX + 10, contentY, ColorUtil.rgba(120, 120, 120, 255));
        contentY += 14;
        
        java.util.List<ThemeManager.Theme> themes = ThemeManager.INSTANCE.getThemes();
        for (int i = 0; i < Math.min(themes.size(), 5); i++) {
            ThemeManager.Theme theme = themes.get(i);
            boolean selected = i == ThemeManager.INSTANCE.getCurrentThemeIndex();
            boolean hover = mouseX >= panelX + 10 && mouseX <= panelX + panelW - 10 && mouseY >= contentY && mouseY <= contentY + 16;
            
            if (selected) {
                RenderUtil2D.drawRoundRect(panelX + 8, contentY - 1, panelW - 16, 16, 3, ColorUtil.rgba(40, 40, 45, 255));
            } else if (hover) {
                RenderUtil2D.drawRoundRect(panelX + 8, contentY - 1, panelW - 16, 16, 3, ColorUtil.rgba(30, 30, 35, 255));
            }
            
            RenderUtil2D.drawRoundRect(panelX + 12, contentY + 2, 10, 10, 2, theme.getAccentColor());
            Fonts.mstr.get(10).drawString(ms, theme.getName(), panelX + 26, contentY + 3, selected ? -1 : ColorUtil.rgba(160, 160, 160, 255));
            contentY += 17;
        }
        
        contentY += 8;
        RenderUtil2D.drawRect(panelX + 10, contentY, panelW - 20, 1, ColorUtil.rgba(50, 50, 50, 255));
        contentY += 8;
        
        // RGB Sliders
        ThemeManager.Theme currentTheme = ThemeManager.INSTANCE.getCurrentTheme();
        int editColor = currentTheme.getAccentColor();
        int r = ColorUtil.getRed(editColor);
        int g = ColorUtil.getGreen(editColor);
        int b = ColorUtil.getBlue(editColor);
        
        float sliderX = panelX + 22;
        float sliderW = panelW - 55;
        
        // Red
        Fonts.mstr.get(10).drawString(ms, "R", panelX + 10, contentY + 2, ColorUtil.rgba(255, 100, 100, 255));
        RenderUtil2D.drawRoundRect(sliderX, contentY, sliderW, 8, 3, ColorUtil.rgba(35, 35, 38, 255));
        RenderUtil2D.drawRoundRect(sliderX, contentY, sliderW * (r / 255f), 8, 3, ColorUtil.rgba(255, 80, 80, 255));
        RenderUtil2D.drawCircle(sliderX + sliderW * (r / 255f), contentY + 4, 5, -1);
        Fonts.mstr.get(9).drawString(ms, String.valueOf(r), panelX + panelW - 25, contentY + 1, ColorUtil.rgba(180, 180, 180, 255));
        contentY += 18;
        
        // Green
        Fonts.mstr.get(10).drawString(ms, "G", panelX + 10, contentY + 2, ColorUtil.rgba(100, 255, 100, 255));
        RenderUtil2D.drawRoundRect(sliderX, contentY, sliderW, 8, 3, ColorUtil.rgba(35, 35, 38, 255));
        RenderUtil2D.drawRoundRect(sliderX, contentY, sliderW * (g / 255f), 8, 3, ColorUtil.rgba(80, 255, 80, 255));
        RenderUtil2D.drawCircle(sliderX + sliderW * (g / 255f), contentY + 4, 5, -1);
        Fonts.mstr.get(9).drawString(ms, String.valueOf(g), panelX + panelW - 25, contentY + 1, ColorUtil.rgba(180, 180, 180, 255));
        contentY += 18;
        
        // Blue
        Fonts.mstr.get(10).drawString(ms, "B", panelX + 10, contentY + 2, ColorUtil.rgba(100, 100, 255, 255));
        RenderUtil2D.drawRoundRect(sliderX, contentY, sliderW, 8, 3, ColorUtil.rgba(35, 35, 38, 255));
        RenderUtil2D.drawRoundRect(sliderX, contentY, sliderW * (b / 255f), 8, 3, ColorUtil.rgba(80, 80, 255, 255));
        RenderUtil2D.drawCircle(sliderX + sliderW * (b / 255f), contentY + 4, 5, -1);
        Fonts.mstr.get(9).drawString(ms, String.valueOf(b), panelX + panelW - 25, contentY + 1, ColorUtil.rgba(180, 180, 180, 255));
        contentY += 20;
        
        // Preview
        Fonts.mstr.get(10).drawString(ms, "Preview", panelX + 10, contentY + 2, ColorUtil.rgba(120, 120, 120, 255));
        RenderUtil2D.drawRoundRect(panelX + 60, contentY - 2, 50, 14, 4, editColor);
        contentY += 22;
        
        // Separator
        RenderUtil2D.drawRect(panelX + 10, contentY, panelW - 20, 1, ColorUtil.rgba(50, 50, 50, 255));
        contentY += 10;
        
        // Blur toggle
        boolean blurEnabled = ThemeManager.INSTANCE.isBlurEnabled();
        Fonts.mstr.get(10).drawString(ms, "Blur", panelX + 10, contentY + 2, ColorUtil.rgba(160, 160, 160, 255));
        int blurBg = blurEnabled ? ThemeManager.INSTANCE.getThemeColor() : ColorUtil.rgba(50, 51, 55, 255);
        RenderUtil2D.drawRoundRect(panelX + panelW - 28, contentY, 18, 10, 5, blurBg);
        float blurKnobX = panelX + panelW - 28 + 1 + (blurEnabled ? 8 : 0);
        RenderUtil2D.drawCircle(blurKnobX + 4, contentY + 5, 3.5f, -1);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        targetScrollY = (float) MathHelper.clamp(targetScrollY + delta * 20, -maxScroll, 0);
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float x = (width - width_gui) / 2f;
        float y = (height - height_gui) / 2f;

        // Color Picker panel interactions (right bottom corner)
        float panelW = 170;
        float panelH = 240;
        float panelX = width - panelW - 15;
        float panelY = height - panelH - 15;
        
        if (mouseX >= panelX && mouseX <= panelX + panelW && mouseY >= panelY && mouseY <= panelY + panelH && button == 0) {
            float contentY = panelY + 44;
            
            // Theme selection
            java.util.List<ThemeManager.Theme> themes = ThemeManager.INSTANCE.getThemes();
            for (int i = 0; i < Math.min(themes.size(), 5); i++) {
                if (mouseX >= panelX + 10 && mouseX <= panelX + panelW - 10 && mouseY >= contentY && mouseY <= contentY + 16) {
                    ThemeManager.INSTANCE.setCurrentThemeIndex(i);
                    return true;
                }
                contentY += 17;
            }
            
            contentY += 16;
            
            // RGB Sliders
            float sliderX = panelX + 22;
            float sliderW = panelW - 55;
            
            // Red slider
            if (mouseX >= sliderX && mouseX <= sliderX + sliderW && mouseY >= contentY - 2 && mouseY <= contentY + 10) {
                draggingRed = true;
                updateColorSlider(mouseX, sliderX, sliderW, 0);
                return true;
            }
            contentY += 18;
            
            // Green slider
            if (mouseX >= sliderX && mouseX <= sliderX + sliderW && mouseY >= contentY - 2 && mouseY <= contentY + 10) {
                draggingGreen = true;
                updateColorSlider(mouseX, sliderX, sliderW, 1);
                return true;
            }
            contentY += 18;
            
            // Blue slider
            if (mouseX >= sliderX && mouseX <= sliderX + sliderW && mouseY >= contentY - 2 && mouseY <= contentY + 10) {
                draggingBlue = true;
                updateColorSlider(mouseX, sliderX, sliderW, 2);
                return true;
            }
            contentY += 52;
            
            // Blur toggle
            if (mouseX >= panelX + panelW - 28 && mouseX <= panelX + panelW - 10 && mouseY >= contentY && mouseY <= contentY + 12) {
                ThemeManager.INSTANCE.setBlurEnabled(!ThemeManager.INSTANCE.isBlurEnabled());
                return true;
            }
            
            return true; // Block clicks inside panel
        }

        float totalNavWidth = Category.values().length * 30;
        float navBoxW = totalNavWidth + 10;
        float navBoxX = x + (width_gui - navBoxW) / 2f;
        float navBoxY = y + 5;
        float navX = navBoxX + 5;
        float navY = navBoxY + 2;

        for (Category category : Category.values()) {
            if (mouseX >= navX && mouseX <= navX + 25 && mouseY >= navY && mouseY <= navY + 16) {
                if (button == 0) {
                    currentCategory = category;
                    refreshModules();
                    scrollY = 0;
                    return true;
                }
            }
            navX += 30;
        }

        for (ModuleCard card : moduleCards) {
            card.mouseClicked(mouseX, mouseY, button);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void updateColorSlider(double mouseX, float sliderX, float sliderW, int component) {
        float percent = (float) MathHelper.clamp((mouseX - sliderX) / sliderW, 0, 1);
        int value = (int) (percent * 255);
        
        ThemeManager.Theme theme = ThemeManager.INSTANCE.getCurrentTheme();
        int currentColor = theme.getAccentColor();
        
        int r = ColorUtil.getRed(currentColor);
        int g = ColorUtil.getGreen(currentColor);
        int b = ColorUtil.getBlue(currentColor);
        
        switch (component) {
            case 0 -> r = value;
            case 1 -> g = value;
            case 2 -> b = value;
        }
        
        theme.setAccentColor(ColorUtil.rgba(r, g, b, 255));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (ModuleCard card : moduleCards) {
            card.keyTyped(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Handle color picker slider dragging
        float panelW = 170;
        float panelX = width - panelW - 15;
        float sliderX = panelX + 22;
        float sliderW = panelW - 55;
        
        if (draggingRed) {
            updateColorSlider(mouseX, sliderX, sliderW, 0);
            return true;
        }
        if (draggingGreen) {
            updateColorSlider(mouseX, sliderX, sliderW, 1);
            return true;
        }
        if (draggingBlue) {
            updateColorSlider(mouseX, sliderX, sliderW, 2);
            return true;
        }
        
        moduleCards.forEach(card -> card.mouseDragged(mouseX, mouseY, button, dragX, dragY));
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingRed = false;
        draggingGreen = false;
        draggingBlue = false;
        
        moduleCards.forEach(card -> card.mouseReleased(mouseX, mouseY, button));
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
