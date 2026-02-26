package fun.pasta.ui.clickGui.theme;

import fun.pasta.util.color.ColorUtil;
import java.util.ArrayList;
import java.util.List;

public class ThemeManager {
    
    public static ThemeManager INSTANCE = new ThemeManager();
    
    // Theme list
    private List<Theme> themes = new ArrayList<>();
    private int currentThemeIndex = 0;
    
    // Preset colors
    public static final int ORANGE = ColorUtil.rgba(255, 140, 50, 255);
    public static final int BLUE = ColorUtil.rgba(66, 165, 245, 255);
    public static final int RED = ColorUtil.rgba(255, 82, 82, 255);
    public static final int GREEN = ColorUtil.rgba(102, 187, 106, 255);
    public static final int PURPLE = ColorUtil.rgba(171, 71, 188, 255);
    public static final int PINK = ColorUtil.rgba(236, 64, 122, 255);
    
    // Blur settings
    private boolean blurEnabled = true;
    private float blurIntensity = 0.7f;
    
    // HUD settings
    private float hudOpacity = 1.0f;
    private boolean hudBlurEnabled = true;
    
    public ThemeManager() {
        // Add default themes
        themes.add(new Theme("Orange", ORANGE, -1, ColorUtil.rgba(200, 200, 200, 255), ColorUtil.rgba(14, 15, 17, 255)));
        themes.add(new Theme("Blue", BLUE, -1, ColorUtil.rgba(200, 200, 200, 255), ColorUtil.rgba(14, 15, 17, 255)));
        themes.add(new Theme("Red", RED, -1, ColorUtil.rgba(200, 200, 200, 255), ColorUtil.rgba(14, 15, 17, 255)));
        themes.add(new Theme("Purple", PURPLE, -1, ColorUtil.rgba(200, 200, 200, 255), ColorUtil.rgba(14, 15, 17, 255)));
        themes.add(new Theme("Green", GREEN, -1, ColorUtil.rgba(200, 200, 200, 255), ColorUtil.rgba(14, 15, 17, 255)));
    }
    
    public Theme getCurrentTheme() {
        if (currentThemeIndex >= 0 && currentThemeIndex < themes.size()) {
            return themes.get(currentThemeIndex);
        }
        return themes.get(0);
    }
    
    public List<Theme> getThemes() {
        return themes;
    }
    
    public int getCurrentThemeIndex() {
        return currentThemeIndex;
    }
    
    public void setCurrentThemeIndex(int index) {
        if (index >= 0 && index < themes.size()) {
            currentThemeIndex = index;
        }
    }
    
    public void selectTheme(String name) {
        for (int i = 0; i < themes.size(); i++) {
            if (themes.get(i).getName().equals(name)) {
                currentThemeIndex = i;
                return;
            }
        }
    }
    
    public Theme createNewTheme(String name) {
        Theme theme = new Theme(name, ORANGE, -1, ColorUtil.rgba(200, 200, 200, 255), ColorUtil.rgba(14, 15, 17, 255));
        themes.add(theme);
        return theme;
    }
    
    public void deleteTheme(int index) {
        if (index > 4 && index < themes.size()) { // Don't delete default themes
            themes.remove(index);
            if (currentThemeIndex >= themes.size()) {
                currentThemeIndex = themes.size() - 1;
            }
        }
    }
    
    // Convenience methods
    public int getThemeColor() {
        return getCurrentTheme().getAccentColor();
    }
    
    public int getTextColor() {
        return getCurrentTheme().getTextColor();
    }
    
    public int getSecondaryTextColor() {
        return getCurrentTheme().getSecondaryTextColor();
    }
    
    public int getBackgroundColor() {
        return getCurrentTheme().getBackgroundColor();
    }
    
    public void setThemeColor(int color) {
        getCurrentTheme().setAccentColor(color);
    }
    
    // For backwards compatibility
    public int getCustomRed() { return ColorUtil.getRed(getThemeColor()); }
    public int getCustomGreen() { return ColorUtil.getGreen(getThemeColor()); }
    public int getCustomBlue() { return ColorUtil.getBlue(getThemeColor()); }
    
    public void setCustomRed(int r) {
        int g = getCustomGreen();
        int b = getCustomBlue();
        getCurrentTheme().setAccentColor(ColorUtil.rgba(r, g, b, 255));
    }
    
    public void setCustomGreen(int g) {
        int r = getCustomRed();
        int b = getCustomBlue();
        getCurrentTheme().setAccentColor(ColorUtil.rgba(r, g, b, 255));
    }
    
    public void setCustomBlue(int b) {
        int r = getCustomRed();
        int g = getCustomGreen();
        getCurrentTheme().setAccentColor(ColorUtil.rgba(r, g, b, 255));
    }
    
    // Blur settings
    public boolean isBlurEnabled() { return blurEnabled; }
    public void setBlurEnabled(boolean enabled) { blurEnabled = enabled; }
    public float getBlurIntensity() { return blurIntensity; }
    public void setBlurIntensity(float intensity) { blurIntensity = Math.max(0, Math.min(1, intensity)); }
    
    // HUD settings
    public float getHudOpacity() { return hudOpacity; }
    public void setHudOpacity(float opacity) { hudOpacity = Math.max(0, Math.min(1, opacity)); }
    public boolean isHudBlurEnabled() { return hudBlurEnabled; }
    public void setHudBlurEnabled(boolean enabled) { hudBlurEnabled = enabled; }
    
    // Theme class
    public static class Theme {
        private String name;
        private int accentColor;      // Main theme color (orange, blue, etc)
        private int textColor;        // Primary text color
        private int secondaryTextColor; // Secondary/dimmed text
        private int backgroundColor;  // Panel background
        
        public Theme(String name, int accentColor, int textColor, int secondaryTextColor, int backgroundColor) {
            this.name = name;
            this.accentColor = accentColor;
            this.textColor = textColor;
            this.secondaryTextColor = secondaryTextColor;
            this.backgroundColor = backgroundColor;
        }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public int getAccentColor() { return accentColor; }
        public void setAccentColor(int color) { this.accentColor = color; }
        
        public int getTextColor() { return textColor; }
        public void setTextColor(int color) { this.textColor = color; }
        
        public int getSecondaryTextColor() { return secondaryTextColor; }
        public void setSecondaryTextColor(int color) { this.secondaryTextColor = color; }
        
        public int getBackgroundColor() { return backgroundColor; }
        public void setBackgroundColor(int color) { this.backgroundColor = color; }
    }
}
