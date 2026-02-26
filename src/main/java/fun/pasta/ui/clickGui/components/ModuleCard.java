package fun.pasta.ui.clickGui.components;

import com.mojang.blaze3d.matrix.MatrixStack;
import fun.pasta.fonts.Fonts;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.*;
import fun.pasta.ui.clickGui.settings.Setting;
import fun.pasta.ui.clickGui.theme.ThemeManager;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.math.MathUtil;
import fun.pasta.util.render.RenderUtil2D;
import fun.pasta.util.render.SmartScissor;
import fun.pasta.util.key.KeyUtil;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class ModuleCard extends Component {
    public final Module module;
    public boolean extended;
    public boolean binding = false;
    public Setting bindingSetting = null;
    public MultiBoxOption expandedMultiBox = null;

    // Theme color (from ThemeManager)
    private int getThemeColor() {
        return ThemeManager.INSTANCE.getThemeColor();
    }
    
    // Animation values
    private float toggleAnimation = 0;
    private java.util.Map<Setting, Float> settingAnimations = new java.util.HashMap<>();
    
    // Scrolling text animation
    private float scrollOffset = 0;
    private long lastScrollTime = 0;
    private boolean scrollDirection = true; // true = right, false = left
    private static final float SCROLL_SPEED = 0.5f;
    private static final long SCROLL_PAUSE = 1500;
    private static final float MAX_NAME_WIDTH = 60f;

    public ModuleCard(Module module) {
        super(0, 0, 145, 30);
        this.module = module;
        this.toggleAnimation = module.enable ? 1 : 0;
    }

    @Override
    public void draw(MatrixStack ms, int mouseX, int mouseY) {
        float dynamicHeight = calculateHeight();

        // Animate toggle
        float targetToggle = module.enable ? 1 : 0;
        toggleAnimation = MathUtil.lerp(toggleAnimation, targetToggle, 8);

        // Card Background (Darker and rounded)
        RenderUtil2D.drawRoundRect(x, y, width, dynamicHeight, 6, ColorUtil.rgba(25, 26, 28, 255));

        // Module Header (with scrolling if too long)
        String name = module.getName();
        float nameWidth = Fonts.mstr.get(14).getWidth(name);

        SmartScissor.push();
        SmartScissor.set(x + 8, y, MAX_NAME_WIDTH, height);

        if (nameWidth > MAX_NAME_WIDTH) {
            long now = System.currentTimeMillis();
            float maxOffset = nameWidth - MAX_NAME_WIDTH;
            
            if (lastScrollTime == 0) lastScrollTime = now;
            
            long elapsed = now - lastScrollTime;
            if (elapsed > SCROLL_PAUSE) {
                scrollOffset += scrollDirection ? SCROLL_SPEED : -SCROLL_SPEED;
                
                if (scrollOffset >= maxOffset) {
                    scrollOffset = maxOffset;
                    scrollDirection = false;
                    lastScrollTime = now;
                } else if (scrollOffset <= 0) {
                    scrollOffset = 0;
                    scrollDirection = true;
                    lastScrollTime = now;
                }
            }
            Fonts.mstr.get(14).drawString(ms, name, x + 8 - scrollOffset, y + 10, -1);
        } else {
            Fonts.mstr.get(14).drawString(ms, name, x + 8, y + 10, -1);
        }
        SmartScissor.unSet();
        SmartScissor.pop();

        // Keybind Button (Standardized)
        String keyText = binding ? "..."
                : (KeyUtil.getKey(module.key).isEmpty() ? "None" : KeyUtil.getKey(module.key).toUpperCase());
        float keyW = Fonts.mstr.get(8).getWidth(keyText) + 6;
        float keyX = x + width - 33 - keyW;
        RenderUtil2D.drawRoundRect(keyX, y + 8, keyW, 11, 2.5f, ColorUtil.rgba(40, 41, 45, 255));
        Fonts.mstr.get(8).drawCenterXY(keyText, keyX + keyW / 2f - 1f, y + 8 + 11 / 2f, -1);

        // Modern Toggle Switch with animation
        float switchX = x + width - 28;
        float switchY = y + 8;
        int switchBg = ColorUtil.interpolateColor(ColorUtil.rgba(50, 51, 55, 255), getThemeColor(), toggleAnimation);
        RenderUtil2D.drawRoundRect(switchX, switchY, 20, 10, 5, switchBg);
        float knobX = switchX + 1 + (10 * toggleAnimation);
        RenderUtil2D.drawCircle(knobX + 4, switchY + 5, 3.5f, -1);

        // Settings (Compact and wide)
        if (!module.settingList.isEmpty()) {
            float settingY = y + 32;
            for (Setting setting : module.settingList) {
                if (!setting.visible())
                    continue;

                Fonts.mstr.get(11).drawString(ms, setting.getName(), x + 8, settingY,
                        ColorUtil.rgba(200, 200, 200, 255));

                if (setting instanceof BooleanOption bool) {
                    // Animate boolean toggle
                    float anim = settingAnimations.getOrDefault(setting, bool.get() ? 1f : 0f);
                    float targetAnim = bool.get() ? 1 : 0;
                    anim = MathUtil.lerp(anim, targetAnim, 8);
                    settingAnimations.put(setting, anim);
                    
                    // Small switch for boolean options
                    float sX = x + width - 18;
                    int sBg = ColorUtil.interpolateColor(ColorUtil.rgba(50, 51, 55, 255), getThemeColor(), anim);
                    RenderUtil2D.drawRoundRect(sX, settingY - 1, 14, 8, 4, sBg);
                    float kX = sX + 1 + (6 * anim);
                    RenderUtil2D.drawCircle(kX + 3, settingY + 3, 2.5f, -1);
                } else if (setting instanceof SliderOption slider) {
                    // Wide modern slider
                    float sliderW = width - 16;
                    float sliderX = x + 8;
                    float percent = (slider.value - slider.min) / (slider.max - slider.min);
                    
                    // Animate slider
                    float anim = settingAnimations.getOrDefault(setting, percent);
                    anim = MathUtil.lerp(anim, percent, 10);
                    settingAnimations.put(setting, anim);

                    String valStr = slider.increment == 1.0f ? String.valueOf((int) slider.value)
                            : String.format("%.2f", slider.value);
                    Fonts.mstr.get(11).drawString(ms, valStr, x + width - 8 - Fonts.mstr.get(11).getWidth(valStr),
                            settingY, getThemeColor());

                    settingY += 12;
                    RenderUtil2D.drawRoundRect(sliderX, settingY, sliderW, 3, 1.5f, ColorUtil.rgba(40, 41, 45, 255));
                    RenderUtil2D.drawRoundRect(sliderX, settingY, sliderW * anim, 3, 1.5f, getThemeColor());
                    RenderUtil2D.drawCircle(sliderX + sliderW * anim, settingY + 1.5f, 5.5f, -1);
                    settingY += 4;
                } else if (setting instanceof ModeOption mode) {
                    // Boxed dropdown look (Clean)
                    settingY += 12;
                    RenderUtil2D.drawRoundRect(x + 8, settingY - 2, width - 16, 12, 3, ColorUtil.rgba(40, 41, 45, 255));
                    Fonts.mstr.get(11).drawString(ms, mode.get(), x + 12, settingY + 1.5f, Color.WHITE.getRGB());
                    Fonts.mstr.get(10).drawString(ms, "v", x + width - 18, settingY + 2, Color.GRAY.getRGB());
                    settingY += 6;
                } else if (setting instanceof MultiBoxOption multi) {
                    // MultiBox header (Clean)
                    settingY += 10;
                    RenderUtil2D.drawRoundRect(x + 8, settingY - 2, width - 16, 12, 3, ColorUtil.rgba(40, 41, 45, 255));
                    
                    // РЎР?Р+РёС?Р°РчР? Р?С<Р+С?Р°Р?Р?С<Рч Р?РїС┼РёРё
                    StringBuilder selected = new StringBuilder();
                    for (BooleanOption opt : multi.options) {
                        if (opt.get()) {
                            if (selected.length() > 0) selected.append(", ");
                            selected.append(opt.getName());
                        }
                    }
                    String displayText = selected.length() > 0 ? selected.toString() : "Р?Рч Р?С<Р+С?Р°Р?Р?";
                    if (displayText.length() > 15) displayText = displayText.substring(0, 12) + "...";
                    
                    Fonts.mstr.get(11).drawString(ms, displayText, x + 12, settingY + 1.5f, Color.WHITE.getRGB());
                    Fonts.mstr.get(10).drawString(ms, "v", x + width - 18, settingY + 2, Color.GRAY.getRGB());

                    if (expandedMultiBox == multi) {
                        for (BooleanOption opt : multi.options) {
                            settingY += 16;
                            Fonts.mstr.get(11).drawString(ms, opt.getName(), x + 12, settingY,
                                    opt.get() ? Color.WHITE.getRGB() : Color.GRAY.getRGB());
                            if (opt.get()) {
                                RenderUtil2D.drawRoundRect(x + width - 18, settingY, 8, 8, 1, getThemeColor());
                            } else {
                                RenderUtil2D.drawRoundRect(x + width - 18, settingY, 8, 8, 1,
                                        ColorUtil.rgba(10, 10, 10, 255));
                            }
                        }
                    }
                    settingY += 6;
                } else if (setting instanceof BindOption bind) {
                    String bText = (binding && bindingSetting == bind) ? "..."
                            : (KeyUtil.getKey(bind.key).isEmpty() ? "None" : KeyUtil.getKey(bind.key).toUpperCase());
                    float bW = Fonts.mstr.get(11).getWidth(bText) + 10;
                    RenderUtil2D.drawRoundRect(x + width - 8 - bW, settingY - 2, bW, 12, 3,
                            ColorUtil.rgba(40, 41, 45, 255));
                    Fonts.mstr.get(11).drawCenterXY(bText, x + width - 8 - bW / 2f, settingY + 4,
                            Color.WHITE.getRGB());
                }
                settingY += 18;
            }
        }
    }

    public float calculateHeight() {
        float h = 32; // Header (Always shown)
        if (!module.settingList.isEmpty()) {
            h += 4; // Margin after header
            for (Setting setting : module.settingList) {
                if (!setting.visible())
                    continue;
                h += 18; // Label + small gap
                if (setting instanceof SliderOption) {
                    h += 16; // Slider bar + padding
                } else if (setting instanceof ModeOption) {
                    h += 16; // Dropdown box
                } else if (setting instanceof MultiBoxOption multi) {
                    h += 16; // Header box
                    if (expandedMultiBox == multi) {
                        h += multi.options.size() * 16;
                    }
                } else if (setting instanceof BindOption) {
                    h += 0; // Bind С?С?Р?Р?Р? С? С'РчРєС?С'Р?Р?, Р?Рч Р?Р?Р+Р°Р?Р>С?РчС' Р?С<С?Р?С'С?
                }
            }
        }
        return h + 4;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (binding && button > 1) {
            int key = -100 + button;
            if (bindingSetting instanceof BindOption bind) {
                bind.key = key;
            } else {
                module.key = key;
            }
            binding = false;
            bindingSetting = null;
            return;
        }

        if (isHovered(mouseX, mouseY)) {
            if (button == 0) {
                // Keybind Button
                String keyText = KeyUtil.getKey(module.key).isEmpty() ? "None"
                        : KeyUtil.getKey(module.key).toUpperCase();
                float keyW = Fonts.mstr.get(8).getWidth(keyText) + 8;
                float keyX = x + width - 35 - keyW;
                if (mouseX >= keyX && mouseX <= keyX + keyW && mouseY >= y + 8 && mouseY <= y + 19) {
                    binding = !binding;
                    bindingSetting = null;
                    return;
                }

                // Main Toggle Switch
                float switchX = x + width - 28;
                if (mouseX >= switchX && mouseX <= switchX + 20 && mouseY >= y + 9 && mouseY <= y + 18) {
                    module.toggle();
                    return;
                }

                // Settings Interaction
                float settingY = y + 32;
                for (Setting setting : module.settingList) {
                    if (!setting.visible())
                        continue;

                    if (setting instanceof BooleanOption bool) {
                        float sX = x + width - 18;
                        if (mouseX >= sX && mouseX <= sX + 14 && mouseY >= settingY - 1 && mouseY <= settingY + 7) {
                            bool.toggle();
                            return;
                        }
                    } else if (setting instanceof SliderOption slider) {
                        if (mouseX >= x && mouseX <= x + width && mouseY >= settingY + 12 && mouseY <= settingY + 16) {
                            updateSlider(slider, mouseX);
                            return;
                        }
                    } else if (setting instanceof ModeOption mode) {
                        if (mouseX >= x + 8 && mouseX <= x + width - 8 && mouseY >= settingY + 8
                                && mouseY <= settingY + 22) {
                            mode.cycle();
                            return;
                        }
                        settingY += 16;
                    } else if (setting instanceof MultiBoxOption multi) {
                        if (mouseX >= x + 8 && mouseX <= x + width - 8 && mouseY >= settingY + 8
                                && mouseY <= settingY + 22) {
                            expandedMultiBox = (expandedMultiBox == multi) ? null : multi;
                            return;
                        }
                        settingY += 16;
                        if (expandedMultiBox == multi) {
                            for (BooleanOption opt : multi.options) {
                                settingY += 16;
                                if (mouseX >= x + 8 && mouseX <= x + width - 8 && mouseY >= settingY - 4
                                        && mouseY <= settingY + 12) {
                                    opt.toggle();
                                    return;
                                }
                            }
                        }
                    } else if (setting instanceof BindOption) {
                        if (mouseX >= x + width - 50 && mouseX <= x + width - 8 && mouseY >= settingY + 6
                                && mouseY <= settingY + 20) {
                            binding = !binding;
                            bindingSetting = setting;
                            return;
                        }
                    }

                    settingY += 18;
                    if (setting instanceof SliderOption)
                        settingY += 16;
                }
            } else if (button == 1) {
                // Keep right-click for something else? Image doesn't show expansion state,
                // cards look fixed
            }
        }
    }

    @Override
    public void mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && isHovered(mouseX, mouseY)) {
            float settingY = y + 32;
            for (Setting setting : module.settingList) {
                if (!setting.visible())
                    continue;
                if (setting instanceof SliderOption slider) {
                    if (mouseY >= settingY + 8 && mouseY <= settingY + 24) {
                        updateSlider(slider, mouseX);
                    }
                }
                settingY += 18;
                if (setting instanceof SliderOption)
                    settingY += 16;
                else if (setting instanceof ModeOption)
                    settingY += 16;
                else if (setting instanceof MultiBoxOption multi) {
                    settingY += 16;
                    if (expandedMultiBox == multi)
                        settingY += multi.options.size() * 16;
                }
            }
        }
    }

    @Override
    public void keyTyped(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            int key = (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) ? 0 : keyCode;
            if (bindingSetting instanceof BindOption bind) {
                bind.key = key;
            } else {
                module.key = key;
            }
            binding = false;
            bindingSetting = null;
        }
    }
    
    public void mouseReleased(double mouseX, double mouseY, int button) {
        // Empty for now, can be used for slider release handling
    }

    private void updateSlider(SliderOption slider, double mouseX) {
        float sliderW = width - 16;
        float sliderX = x + 8;
        float percent = (float) ((mouseX - sliderX) / sliderW);
        percent = Math.max(0, Math.min(1, percent));
        float newValue = slider.min + (slider.max - slider.min) * percent;

        if (slider.increment > 0) {
            newValue = Math.round(newValue / slider.increment) * slider.increment;
        }
        slider.setValue(newValue);
    }
}
