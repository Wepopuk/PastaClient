package fun.pasta.fonts;

import it.unimi.dsi.fastutil.floats.Float2ObjectArrayMap;
import it.unimi.dsi.fastutil.floats.Float2ObjectMap;

public enum Fonts {

    mstr("montserrat.ttf"),
    pastaIcon("pastaIcon.ttf"),
    mainButtonIcon("mainButtonIcon.ttf"),
    categoryIcon("categoryIcon.ttf"),
    mainIcon("mainIcon.ttf"),
    waterIcon("waterIcon.ttf"),
    arial("arial.ttf"),
    sfpro("sfpro.ttf"),
    notifICON("notifICON.ttf");

    public final String file;
    public final Float2ObjectMap<Font> fontFloat2ObjectMap = new Float2ObjectArrayMap<>();

    Fonts(String file) {
        this.file = file;
    }

    public Font get(float size) {
        return fontFloat2ObjectMap.computeIfAbsent(size, font -> {
            try {
                return Font.create(file, size, false, false, false);
            } catch (Exception e) {
                throw new RuntimeException("load font failed: " + this + ", " + e);
            }
        });
    }
}
