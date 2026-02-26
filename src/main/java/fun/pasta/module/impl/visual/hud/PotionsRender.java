package fun.pasta.module.impl.visual.hud;

import fun.pasta.Pasta;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.MathHelper;
import fun.pasta.drag.Dragging;
import fun.pasta.event.EventRender2D;
import fun.pasta.fonts.Fonts;
import fun.pasta.module.impl.visual.hud.impl.IRenderHud;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.math.MathUtil;
import fun.pasta.util.render.RenderUtil2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PotionsRender implements IRenderHud {

    private static final float BASE_WIDTH = 85f;
    private static final float PILL_HEIGHT = 14f;
    private static final float PILL_RADIUS = 7f;
    private static final float GAP = 1f;
    private static final float HEADER_MIN_WIDTH = 46f;
    private static final float HEADER_PADDING_X = 10f;
    private static final float PILL_PADDING_X = 5f;
    private static final float TEXT_SIZE = 13f;

    private static final int HEADER_COLOR = ColorUtil.rgba(64, 59, 55, 200);
    private static final int ITEM_COLOR = ColorUtil.rgba(64, 59, 55, 150);
    private static final int TEXT_COLOR = ColorUtil.rgba(255, 255, 255, 255);

    float x, y, width, height;

    final Dragging dragging;
    private final Map<String, Float> animatedPositions = new HashMap<>();

    public PotionsRender(Dragging dragging) {
        this.dragging = dragging;
    }

    @Override
    public void render(EventRender2D e) {
        fun.pasta.module.impl.visual.Hud hud = (fun.pasta.module.impl.visual.Hud) Pasta.instance.manager
                .getModule("HUD");
        if (hud == null || !hud.enable || !hud.potions.get())
            return;

        setPosition();

        List<EffectInstance> effects = new ArrayList<>(mc.player.getActivePotionEffects());

        float totalHeight = PILL_HEIGHT;
        if (!effects.isEmpty()) {
            totalHeight += GAP + effects.size() * PILL_HEIGHT + (effects.size() - 1) * GAP;
        }

        this.width = BASE_WIDTH;
        this.height = totalHeight;
        dragging.setWidth(width);
        dragging.setHeight(height);

        List<PotionLine> lines = new ArrayList<>();
        for (EffectInstance f : effects) {
            String namePotion = I18n.format(f.getEffectName());
            int lvl = f.getAmplifier() + 1;
            String lvlText = lvl > 1 ? " " + lvl : "";
            String textDuration = getDuration(f, 1);
            String line = namePotion + lvlText + " (" + textDuration + ")";
            float nameWidth = Fonts.sfpro.get(TEXT_SIZE).getWidth(namePotion + lvlText);
            String key = f.getEffectName() + ":" + f.getAmplifier();
            lines.add(new PotionLine(key, line, nameWidth));
        }

        lines.sort((a, b) -> Float.compare(b.nameWidth, a.nameWidth));

        float headerTextWidth = Fonts.sfpro.get(TEXT_SIZE).getWidth("Potions");
        float headerWidth = Math.min(width, Math.max(HEADER_MIN_WIDTH, headerTextWidth + HEADER_PADDING_X * 2f));
        float maxPillWidth = headerWidth;
        for (PotionLine entry : lines) {
            float textWidth = Fonts.sfpro.get(TEXT_SIZE).getWidth(entry.line);
            float pillWidth = Math.min(width, textWidth + PILL_PADDING_X * 2f);
            if (pillWidth > maxPillWidth) {
                maxPillWidth = pillWidth;
            }
        }

        float effectsX = x + (width - maxPillWidth) / 2f;
        float headerX = effectsX + (maxPillWidth - headerWidth) / 2f;

        drawPill(headerX, y, headerWidth, HEADER_COLOR, "Potions");

        float currentYBase = y + PILL_HEIGHT + (lines.isEmpty() ? 0f : GAP);
        Set<String> activeKeys = new HashSet<>();
        int index = 0;
        for (PotionLine entry : lines) {
            float textWidth = Fonts.sfpro.get(TEXT_SIZE).getWidth(entry.line);
            float pillWidth = Math.min(width, textWidth + PILL_PADDING_X * 2f);
            float pillX = effectsX;
            float targetY = currentYBase + index * (PILL_HEIGHT + GAP);
            float currentY = animatedPositions.getOrDefault(entry.key, targetY);
            currentY = MathUtil.lerp(currentY, targetY, 10);
            animatedPositions.put(entry.key, currentY);
            activeKeys.add(entry.key);

            String display = trimToWidth(entry.line, pillWidth - PILL_PADDING_X * 2f, Fonts.sfpro.get(TEXT_SIZE));
            drawPill(pillX, currentY, pillWidth, ITEM_COLOR, display);

            index++;
        }

        if (!animatedPositions.isEmpty()) {
            animatedPositions.keySet().removeIf(key -> !activeKeys.contains(key));
        }
    }

    private String getDuration(EffectInstance f, float duration) {
        if (f.getIsPotionDurationMax()) {
            return "**:**";
        } else {
            int i = MathHelper.floor(f.getDuration() * duration);
            return getTime(i);
        }
    }

    private String getTime(int ticks) {
        int i = ticks / 20;
        int j = i / 60;
        i = i % 60;
        return i < 10 ? j + ":0" + i : j + ":" + i;
    }

    private String getLvl(int amp) {
        String lvlString;

        if (amp == 0) {
            lvlString = "1";
        } else if (amp == 1) {
            lvlString = "2";
        } else if (amp == 2) {
            lvlString = "3";
        } else if (amp == 3) {
            lvlString = "4";
        } else if (amp == 4) {
            lvlString = "5";
        } else {
            lvlString = "+++";
        }

        return lvlString;
    }

    private void setPosition() {
        x = dragging.getX();
        y = dragging.getY();
    }

    private void drawPill(float px, float py, float pillWidth, int color, String text) {
        RenderUtil2D.drawRoundRect(px, py, pillWidth, PILL_HEIGHT, PILL_RADIUS, color);
        Fonts.sfpro.get(TEXT_SIZE).drawCenterXY(text, px + pillWidth / 2f, py + PILL_HEIGHT / 2f, TEXT_COLOR);
    }

    private static class PotionLine {
        private final String key;
        private final String line;
        private final float nameWidth;

        private PotionLine(String key, String line, float nameWidth) {
            this.key = key;
            this.line = line;
            this.nameWidth = nameWidth;
        }
    }

    private String trimToWidth(String text, float maxWidth, fun.pasta.fonts.Font font) {
        if (maxWidth <= 0f || text.isEmpty()) {
            return "";
        }
        if (font.getWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        float ellipsisWidth = font.getWidth(ellipsis);
        if (ellipsisWidth > maxWidth) {
            return "";
        }
        int end = text.length();
        while (end > 0 && font.getWidth(text.substring(0, end)) + ellipsisWidth > maxWidth) {
            end--;
        }
        return end > 0 ? text.substring(0, end) + ellipsis : "";
    }
}
