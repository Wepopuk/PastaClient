package fun.pasta.ui.clickGui;

import com.mojang.blaze3d.matrix.MatrixStack;
import fun.pasta.fonts.Fonts;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.*;
import fun.pasta.ui.clickGui.settings.Setting;
import fun.pasta.util.IMinecraft;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.key.KeyUtil;
import fun.pasta.util.math.MathUtil;
import fun.pasta.util.render.RenderUtil2D;
import fun.pasta.util.render.SmartScissor;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;

public class SettingsPanel implements IMinecraft {

    private final ModuleSelection selection;
    private Module lastModule;

    public float x;
    public float y;
    public float width;
    public float height;

    private float scroll;
    private float scrollOut;
    private float maxScroll;

    private MultiBoxOption expandedMulti;
    private ModeOption expandedMode;
    private ModeOption popupMode;
    private MultiBoxOption popupMulti;
    private float popupX;
    private float popupY;
    private float popupW;
    private float popupH;
    private Setting bindingSetting;
    private boolean binding;
    private SliderOption draggingSlider;

    private final Map<Setting, Float> hoverAnims = new HashMap<>();
    private final Map<Setting, Float> toggleAnims = new HashMap<>();
    private final Map<Setting, Float> sliderAnims = new HashMap<>();

    private static final float HEADER_HEIGHT = 18f;
    private static final float BODY_GAP = 4f;
    private static final float ROW_HEIGHT = 18f;
    private static final float SLIDER_HEIGHT = 26f;
    private static final float ROW_GAP = 4f;
    private static final float PADDING = 6f;
    private static final float CONTROL_HEIGHT = 12f;
    private static final float DROPDOWN_ITEM_HEIGHT = 12f;
    private static final float DROPDOWN_GAP = 2f;
    private static final float DROPDOWN_OFFSET = 2f;
    private static final float ROW_RADIUS = 5f;
    private static final float ICON_SIZE = 10f;
    private static final float ICON_PAD = 6f;
    private static final float ICON_GAP = 5f;
    private static final float ROW_RIGHT_PAD = 6f;
    private static final float TEXT_OFFSET = ICON_PAD + ICON_SIZE + ICON_GAP;

    private static final int BG_COLOR = ColorUtil.rgba(18, 18, 22, 230);
    private static final int HEADER_COLOR = ColorUtil.rgba(24, 24, 28, 255);
    private static final int TEXT_COLOR = ColorUtil.rgba(210, 210, 210, 255);
    private static final int SUB_TEXT_COLOR = ColorUtil.rgba(140, 140, 140, 255);
    private static final int ACCENT_COLOR = ColorUtil.rgba(235, 75, 65, 255);
    private static final int ROW_BG = ColorUtil.rgba(22, 22, 26, 210);
    private static final int ROW_HOVER_BG = ColorUtil.rgba(30, 30, 36, 235);
    private static final int ICON_COLOR = ColorUtil.rgba(185, 185, 185, 255);

    public SettingsPanel(ModuleSelection selection) {
        this.selection = selection;
    }

    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        scrollOut = MathUtil.lerp(scrollOut, scroll, 10);
        popupMode = null;
        popupMulti = null;

        RenderUtil2D.drawShadow(x, y, width, height, 10, ColorUtil.rgba(0, 0, 0, 120));
        RenderUtil2D.drawRoundRect(x, y, width, HEADER_HEIGHT, 8, HEADER_COLOR);
        float bodyY = y + HEADER_HEIGHT + BODY_GAP;
        float bodyH = height - HEADER_HEIGHT - BODY_GAP;
        RenderUtil2D.drawRoundRect(x, bodyY, width, bodyH, 8, BG_COLOR);

        Module module = selection.selected;
        if (module == null) {
            Fonts.mstr.get(11).drawString(stack, "Select a module", x + PADDING, y + 7, SUB_TEXT_COLOR);
            return;
        }

        if (module != lastModule) {
            expandedMulti = null;
            expandedMode = null;
            binding = false;
            bindingSetting = null;
            draggingSlider = null;
            scroll = 0;
            scrollOut = 0;
            hoverAnims.clear();
            toggleAnims.clear();
            sliderAnims.clear();
            lastModule = module;
        }

        float headerIconX = x + PADDING;
        float headerIconY = y + 5;
        Fonts.mainButtonIcon.get(12).drawString(stack, "D", headerIconX, headerIconY, ICON_COLOR);
        Fonts.mstr.get(12).drawString(stack, module.getName(), headerIconX + 12, y + 6, -1);

        if (module.settingList.isEmpty()) {
            Fonts.mstr.get(10).drawString(stack, "No settings", x + PADDING, bodyY + PADDING + 2, SUB_TEXT_COLOR);
            return;
        }

        float contentX = x + PADDING;
        float contentY = bodyY + PADDING + scrollOut;
        float contentW = width - PADDING * 2;

        SmartScissor.push();
        SmartScissor.set(contentX, bodyY + PADDING, contentW, bodyH - PADDING * 2);

        float startY = contentY;
        float totalHeight = 0f;

        for (Setting setting : module.settingList) {
            if (!setting.visible()) {
                continue;
            }

            float rowHeight = getRowHeight(setting);
            float settingHeight = getSettingHeight(setting);
            drawSetting(stack, setting, contentX, contentY, contentW, rowHeight, mouseX, mouseY);
            contentY += settingHeight + ROW_GAP;
            totalHeight = (contentY - ROW_GAP) - startY;
        }

        SmartScissor.unSet();
        SmartScissor.pop();

        if (popupMode != null) {
            drawModePopup(stack);
        }
        if (popupMulti != null) {
            drawMultiPopup(stack);
        }

        float viewHeight = bodyH - PADDING * 2;
        maxScroll = Math.max(0, totalHeight - viewHeight);
        scroll = MathHelper.clamp(scroll, -maxScroll, 0);

        if (maxScroll > 0) {
            float sbWidth = 2.5f;
            float sbX = x + width - 4;
            float sbY = bodyY + 3;
            float sbHeight = bodyH - 6;

            float thumbHeight = (viewHeight / totalHeight) * sbHeight;
            float scrollPercent = -scrollOut / maxScroll;
            float thumbPos = scrollPercent * (sbHeight - thumbHeight);

            RenderUtil2D.drawRoundRect(sbX, sbY, sbWidth, sbHeight, sbWidth / 2f, ColorUtil.rgba(255, 255, 255, 10));
            RenderUtil2D.drawRoundRect(sbX, sbY + thumbPos, sbWidth, thumbHeight, sbWidth / 2f, ColorUtil.rgba(255, 255, 255, 60));
        }
    }

    private float getRowHeight(Setting setting) {
        if (setting instanceof SliderOption) {
            return SLIDER_HEIGHT;
        }
        return ROW_HEIGHT;
    }

    private float getSettingHeight(Setting setting) {
        float rowHeight = getRowHeight(setting);
        return rowHeight;
    }

    private void drawSetting(MatrixStack stack, Setting setting, float x, float y, float width, float rowHeight, int mouseX, int mouseY) {
        boolean hovered = MathUtil.isHover(mouseX, mouseY, x, y, width, rowHeight);
        float hoverAnim = updateAnim(hoverAnims, setting, hovered ? 1f : 0f, 12);
        int rowBg = ColorUtil.interpolate(ROW_BG, ROW_HOVER_BG, hoverAnim);
        RenderUtil2D.drawRoundRect(x, y, width, rowHeight, ROW_RADIUS, rowBg);

        float iconX = x + ICON_PAD;
        float iconY = y + (rowHeight - ICON_SIZE) / 2f;
        Fonts.mainButtonIcon.get(10).drawString(stack, "D", iconX, iconY, ICON_COLOR);

        float textX = x + TEXT_OFFSET;
        float textY = y + 4;

        if (setting instanceof BooleanOption bool) {
            Fonts.mstr.get(9).drawString(stack, setting.getName(), textX, textY, TEXT_COLOR);

            float toggleAnim = updateAnim(toggleAnims, setting, bool.get() ? 1f : 0f, 12);
            float toggleW = 18f;
            float toggleH = 8f;
            float toggleX = x + width - ROW_RIGHT_PAD - toggleW;
            float toggleY = y + (rowHeight - toggleH) / 2f;

            int toggleBg = ColorUtil.interpolate(ColorUtil.rgba(50, 50, 55, 255), ACCENT_COLOR, toggleAnim);
            RenderUtil2D.drawRoundRect(toggleX, toggleY, toggleW, toggleH, toggleH / 2f, toggleBg);
            float knobX = toggleX + 1 + (toggleW - toggleH) * toggleAnim;
            RenderUtil2D.drawCircle(knobX + (toggleH - 2) / 2f, toggleY + toggleH / 2f, toggleH - 2, -1);
            return;
        }

        if (setting instanceof SliderOption slider) {
            Fonts.mstr.get(9).drawString(stack, setting.getName(), textX, textY, TEXT_COLOR);

            float targetPercent = (slider.value - slider.min) / (slider.max - slider.min);
            float percent = updateAnim(sliderAnims, setting, targetPercent, 12);

            String valStr = slider.increment == 1.0f ? String.valueOf((int) slider.value) : String.format("%.1f", slider.value);
            float valW = Fonts.mstr.get(9).getWidth(valStr);
            float valX = x + width - ROW_RIGHT_PAD - valW;
            Fonts.mstr.get(9).drawString(stack, valStr, valX, textY, SUB_TEXT_COLOR);

            float sliderX = x + TEXT_OFFSET;
            float sliderW = getSliderWidth(width);
            float sliderY = y + rowHeight - 7;

            RenderUtil2D.drawRoundRect(sliderX, sliderY, sliderW, 3, 1.5f, ColorUtil.rgba(50, 50, 55, 255));
            RenderUtil2D.drawRoundRect(sliderX, sliderY, sliderW * percent, 3, 1.5f, ACCENT_COLOR);
            RenderUtil2D.drawCircle(sliderX + sliderW * percent, sliderY + 1.5f, 4, -1);
            return;
        }

        if (setting instanceof ModeOption mode) {
            Fonts.mstr.get(9).drawString(stack, setting.getName(), textX, textY, TEXT_COLOR);

            float controlW = getControlWidth(width);
            float controlX = x + width - ROW_RIGHT_PAD - controlW;
            float controlY = y + (rowHeight - CONTROL_HEIGHT) / 2f;

            int controlBg = expandedMode == mode ? ColorUtil.rgba(34, 34, 40, 255) : ColorUtil.rgba(26, 26, 32, 255);
            RenderUtil2D.drawRoundRect(controlX, controlY, controlW, CONTROL_HEIGHT, 4, controlBg);

            String value = trimToWidth(mode.get(), controlW - 12);
            Fonts.mstr.get(9).drawString(stack, value, controlX + 5, controlY + 2, SUB_TEXT_COLOR);
            Fonts.mstr.get(9).drawString(stack, "v", controlX + controlW - 7, controlY + 2, SUB_TEXT_COLOR);

            if (expandedMode == mode) {
                captureModePopup(mode, y);
            }
            return;
        }

        if (setting instanceof MultiBoxOption multi) {
            Fonts.mstr.get(9).drawString(stack, setting.getName(), textX, textY, TEXT_COLOR);

            float controlW = getControlWidth(width);
            float controlX = x + width - ROW_RIGHT_PAD - controlW;
            float controlY = y + (rowHeight - CONTROL_HEIGHT) / 2f;
            int count = 0;
            for (BooleanOption opt : multi.options) {
                if (opt.get()) {
                    count++;
                }
            }
            String value = formatMultiValue(count);

            int controlBg = expandedMulti == multi ? ColorUtil.rgba(34, 34, 40, 255) : ColorUtil.rgba(26, 26, 32, 255);
            RenderUtil2D.drawRoundRect(controlX, controlY, controlW, CONTROL_HEIGHT, 4, controlBg);
            Fonts.mstr.get(9).drawString(stack, trimToWidth(value, controlW - 12), controlX + 5, controlY + 2, SUB_TEXT_COLOR);
            Fonts.mstr.get(9).drawString(stack, "v", controlX + controlW - 7, controlY + 2, SUB_TEXT_COLOR);

            if (expandedMulti == multi) {
                captureMultiPopup(multi, y);
            }
            return;
        }

        if (setting instanceof BindOption bind) {
            Fonts.mstr.get(9).drawString(stack, setting.getName(), textX, textY, TEXT_COLOR);
            String bText = (binding && bindingSetting == bind) ? "..." :
                    (KeyUtil.getKey(bind.key).isEmpty() ? "None" : KeyUtil.getKey(bind.key).toUpperCase());
            float controlW = getControlWidth(width);
            float controlX = x + width - ROW_RIGHT_PAD - controlW;
            float controlY = y + (rowHeight - CONTROL_HEIGHT) / 2f;
            RenderUtil2D.drawRoundRect(controlX, controlY, controlW, CONTROL_HEIGHT, 4, ColorUtil.rgba(26, 26, 32, 255));
            Fonts.mstr.get(9).drawCenterX(bText, controlX + controlW / 2f, controlY + 2, SUB_TEXT_COLOR);
        }
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (binding && bindingSetting instanceof BindOption bind && button > 1) {
            bind.key = -100 + button;
            binding = false;
            bindingSetting = null;
            return;
        }

        if (button != 0) {
            return;
        }

        Module module = selection.selected;
        if (module == null) {
            return;
        }

        float bodyY = y + HEADER_HEIGHT + BODY_GAP;
        float bodyH = height - HEADER_HEIGHT - BODY_GAP;
        boolean inPanel = isInRect(mouseX, mouseY, x, bodyY, width, bodyH);
        float contentX = x + PADDING;
        float contentY = bodyY + PADDING + scrollOut;
        float contentW = width - PADDING * 2;
        boolean keepDropdown = false;

        for (Setting setting : module.settingList) {
            if (!setting.visible()) {
                continue;
            }

            float rowHeight = getRowHeight(setting);
            float settingHeight = getSettingHeight(setting);
            if (setting instanceof ModeOption mode && expandedMode == mode) {
                float dropY = contentY;
                float dropH = getDropdownHeight(mode.modes.size());
                float dropW = getModePopupWidth();
                float dropX = getModePopupX(dropW);
                if (isInRect(mouseX, mouseY, dropX, dropY, dropW, dropH)) {
                    int index = (int) ((mouseY - (dropY + 2)) / (DROPDOWN_ITEM_HEIGHT + DROPDOWN_GAP));
                    if (index >= 0 && index < mode.modes.size()) {
                        mode.set(mode.modes.get(index));
                    }
                    keepDropdown = true;
                    break;
                }
            }
            if (setting instanceof MultiBoxOption multi && expandedMulti == multi) {
                float dropY = contentY;
                float dropH = getDropdownHeight(multi.options.size());
                float dropW = getModePopupWidth();
                float dropX = getModePopupX(dropW);
                if (isInRect(mouseX, mouseY, dropX, dropY, dropW, dropH)) {
                    int index = (int) ((mouseY - (dropY + 2)) / (DROPDOWN_ITEM_HEIGHT + DROPDOWN_GAP));
                    if (index >= 0 && index < multi.options.size()) {
                        multi.options.get(index).toggle();
                    }
                    keepDropdown = true;
                    break;
                }
            }
            if (mouseY >= contentY && mouseY <= contentY + rowHeight) {
                if (setting instanceof BooleanOption bool) {
                    if (inPanel) {
                        bool.toggle();
                    }
                } else if (setting instanceof SliderOption slider) {
                    float sliderX = contentX + TEXT_OFFSET;
                    float sliderW = getSliderWidth(contentW);
                    float sliderY = contentY + rowHeight - 7;
                    if (inPanel && isInRect(mouseX, mouseY, sliderX, sliderY - 4, sliderW, 9)) {
                        draggingSlider = slider;
                        updateSlider(slider, (float) mouseX, sliderX, sliderW);
                    }
                } else if (setting instanceof ModeOption mode) {
                    float controlW = getControlWidth(contentW);
                    float controlX = contentX + contentW - ROW_RIGHT_PAD - controlW;
                    float controlY = contentY + (rowHeight - CONTROL_HEIGHT) / 2f;

                    if (inPanel && isInRect(mouseX, mouseY, controlX, controlY, controlW, CONTROL_HEIGHT)) {
                        expandedMode = expandedMode == mode ? null : mode;
                        expandedMulti = null;
                        keepDropdown = true;
                    }
                } else if (setting instanceof MultiBoxOption multi) {
                    float controlW = getControlWidth(contentW);
                    float controlX = contentX + contentW - ROW_RIGHT_PAD - controlW;
                    float controlY = contentY + (rowHeight - CONTROL_HEIGHT) / 2f;

                    if (inPanel && isInRect(mouseX, mouseY, controlX, controlY, controlW, CONTROL_HEIGHT)) {
                        expandedMulti = expandedMulti == multi ? null : multi;
                        expandedMode = null;
                        keepDropdown = true;
                    }
                } else if (setting instanceof BindOption) {
                    if (inPanel) {
                        binding = true;
                        bindingSetting = setting;
                    }
                }
                break;
            }

            contentY += settingHeight + ROW_GAP;
        }

        if (!keepDropdown && !inPanel) {
            expandedMode = null;
            expandedMulti = null;
            return;
        }

        if (!keepDropdown) {
            expandedMode = null;
            expandedMulti = null;
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        draggingSlider = null;
    }

    public void mouseDragged(double mouseX, double mouseY, int button) {
        if (draggingSlider != null) {
            float contentW = width - PADDING * 2;
            float sliderW = getSliderWidth(contentW);
            float sliderX = x + PADDING + TEXT_OFFSET;
            updateSlider(draggingSlider, (float) mouseX, sliderX, sliderW);
        }
    }

    public void mouseScrolled(double mouseX, double mouseY, double delta) {
        float bodyY = y + HEADER_HEIGHT + BODY_GAP;
        float bodyH = height - HEADER_HEIGHT - BODY_GAP;
        if (isInRect(mouseX, mouseY, x, bodyY, width, bodyH)) {
            scroll += (float) (delta * 18);
        }
    }

    public void keyTyped(int keyCode) {
        if (binding && bindingSetting instanceof BindOption bind) {
            int key = (keyCode == 256 || keyCode == 261) ? 0 : keyCode;
            bind.key = key;
            binding = false;
            bindingSetting = null;
        }
    }

    private boolean isInside(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private boolean isInRect(double mouseX, double mouseY, float x, float y, float w, float h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private float getControlWidth(float contentW) {
        float available = contentW - TEXT_OFFSET - ROW_RIGHT_PAD;
        return Math.min(72f, Math.max(48f, available));
    }

    private float getSliderWidth(float contentW) {
        float available = contentW - TEXT_OFFSET - ROW_RIGHT_PAD;
        return Math.max(40f, available);
    }

    private float getDropdownHeight(int count) {
        if (count <= 0) {
            return 0f;
        }
        return (count * DROPDOWN_ITEM_HEIGHT) + ((count - 1) * DROPDOWN_GAP) + 4f;
    }

    private float getModePopupWidth() {
        return Math.min(88f, width * 0.6f);
    }

    private float getModePopupX(float dropW) {
        float dropX = x - dropW - 6;
        if (dropX < 4) {
            dropX = x + width + 6;
        }
        return dropX;
    }

    private void captureModePopup(ModeOption mode, float rowY) {
        popupMode = mode;
        popupW = getModePopupWidth();
        popupX = getModePopupX(popupW);
        popupY = rowY;
        popupH = getDropdownHeight(mode.modes.size());
    }

    private void captureMultiPopup(MultiBoxOption multi, float rowY) {
        popupMulti = multi;
        popupW = getModePopupWidth();
        popupX = getModePopupX(popupW);
        popupY = rowY;
        popupH = getDropdownHeight(multi.options.size());
    }

    private void drawModePopup(MatrixStack stack) {
        if (popupMode == null) {
            return;
        }

        RenderUtil2D.drawShadow(popupX, popupY, popupW, popupH, 8, ColorUtil.rgba(0, 0, 0, 120));
        RenderUtil2D.drawRoundRect(popupX, popupY, popupW, popupH, 6, ColorUtil.rgba(20, 20, 24, 255));
        float itemY = popupY + 2;
        for (String option : popupMode.modes) {
            boolean active = option.equalsIgnoreCase(popupMode.get());
            int itemBg = active ? ACCENT_COLOR : ColorUtil.rgba(30, 30, 36, 255);
            RenderUtil2D.drawRoundRect(popupX + 2, itemY, popupW - 4, DROPDOWN_ITEM_HEIGHT, 3, itemBg);
            Fonts.mstr.get(9).drawString(stack, trimToWidth(option, popupW - 8), popupX + 6, itemY + 2,
                    active ? -1 : SUB_TEXT_COLOR);
            itemY += DROPDOWN_ITEM_HEIGHT + DROPDOWN_GAP;
        }
    }

    private void drawMultiPopup(MatrixStack stack) {
        if (popupMulti == null) {
            return;
        }

        RenderUtil2D.drawShadow(popupX, popupY, popupW, popupH, 8, ColorUtil.rgba(0, 0, 0, 120));
        RenderUtil2D.drawRoundRect(popupX, popupY, popupW, popupH, 6, ColorUtil.rgba(20, 20, 24, 255));
        float itemY = popupY + 2;
        for (BooleanOption opt : popupMulti.options) {
            int itemBg = opt.get() ? ColorUtil.rgba(34, 34, 40, 255) : ColorUtil.rgba(30, 30, 36, 255);
            RenderUtil2D.drawRoundRect(popupX + 2, itemY, popupW - 4, DROPDOWN_ITEM_HEIGHT, 3, itemBg);
            Fonts.mstr.get(9).drawString(stack, trimToWidth(opt.getName(), popupW - 16), popupX + 6, itemY + 2, SUB_TEXT_COLOR);
            float boxX = popupX + popupW - 12;
            RenderUtil2D.drawRoundRect(boxX, itemY + 2, 8, 8, 2, ColorUtil.rgba(35, 36, 38, 255));
            if (opt.get()) {
                RenderUtil2D.drawRoundRect(boxX + 2, itemY + 4, 4, 4, 1, ACCENT_COLOR);
            }
            itemY += DROPDOWN_ITEM_HEIGHT + DROPDOWN_GAP;
        }
    }

    private String trimToWidth(String text, float maxWidth) {
        if (Fonts.mstr.get(9).getWidth(text) <= maxWidth) {
            return text;
        }
        String trimmed = text;
        while (trimmed.length() > 1 && Fonts.mstr.get(9).getWidth(trimmed + "..") > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + "..";
    }

    private void updateSlider(SliderOption slider, float mouseX, float sliderX, float sliderW) {
        float percent = (mouseX - sliderX) / sliderW;
        percent = Math.max(0, Math.min(1, percent));
        float newValue = slider.min + (slider.max - slider.min) * percent;
        if (slider.increment > 0) {
            newValue = Math.round(newValue / slider.increment) * slider.increment;
        }
        slider.setValue(newValue);
    }

    private String formatMultiValue(int count) {
        if (count == 0) {
            return "None";
        }
        return count + " sel";
    }

    private float updateAnim(Map<Setting, Float> map, Setting setting, float target, float speed) {
        float value = map.getOrDefault(setting, target);
        value = MathUtil.lerp(value, target, speed);
        map.put(setting, value);
        return value;
    }
}
