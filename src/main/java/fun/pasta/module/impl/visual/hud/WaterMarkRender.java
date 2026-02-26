package fun.pasta.module.impl.visual.hud;

import fun.pasta.Pasta;
import fun.pasta.drag.Dragging;
import fun.pasta.event.EventRender2D;
import fun.pasta.fonts.Fonts;
import fun.pasta.module.impl.misc.NameProtect;
import fun.pasta.module.impl.visual.hud.impl.IRenderHud;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.render.RenderUtil2D;

import static fun.pasta.util.IMinecraft.mc;

public class WaterMarkRender implements IRenderHud {

    private static final float SCALE = 2.2f;
    private static final float BOX_SIZE = 42f / SCALE;
    private static final float BOX_RADIUS = 9f / SCALE;
    private static final float PILL_HEIGHT = BOX_SIZE;
    private static final float PILL_RADIUS = 9f / SCALE;
    private static final float GAP = 6f / SCALE;
    private static final float PILL_PADDING_X = 7f / SCALE;
    private static final float PILL_ICON_GAP = 6f / SCALE;
    private static final float PILL_EXTRA_WIDTH = 10f / SCALE;
    private static final float USER_ICON_SHIFT_X = -2f / SCALE;
    private static final float TEXT_SIZE = 16f;
    private static final float LOGO_ICON_SIZE = 22f;
    private static final float USER_ICON_SIZE = 17f;
    private static final float SCREEN_MARGIN = 4f;

    private static final String LOGO_ICON = "B";
    private static final String USER_ICON = "A";

    private static final int BG_COLOR = ColorUtil.rgba(64, 59, 55, 179);
    private static final int TEXT_COLOR = ColorUtil.rgba(255, 255, 255, 255);

    private final Dragging dragging;
    private float x;
    private float y;
    private float width;
    private float height;

    public WaterMarkRender(Dragging dragging) {
        this.dragging = dragging;
    }

    @Override
    public void render(EventRender2D e) {
        fun.pasta.module.impl.visual.Hud hud = (fun.pasta.module.impl.visual.Hud) Pasta.instance.manager
                .getModule("HUD");
        if (hud == null || !hud.enable || !hud.watermark.get()) {
            return;
        }

        String name = mc.player != null ? mc.player.getName().getString() : "Player";
        Object module = Pasta.instance.manager.getModule("NameProtect");
        if (module instanceof NameProtect) {
            name = ((NameProtect) module).protect(name);
        }
        float textWidth = Fonts.sfpro.get(TEXT_SIZE).getWidth(name);
        float iconWidth = Fonts.waterIcon.get(USER_ICON_SIZE).getWidth(USER_ICON);

        float pillWidth = textWidth + PILL_PADDING_X * 2f + iconWidth + PILL_ICON_GAP + PILL_EXTRA_WIDTH;
        float totalWidth = BOX_SIZE + GAP + pillWidth;
        float maxWidth = sr.getScaledWidth() - SCREEN_MARGIN * 2f;
        if (totalWidth > maxWidth) {
            pillWidth = Math.max(0f, maxWidth - BOX_SIZE - GAP);
            totalWidth = BOX_SIZE + GAP + pillWidth;
        }

        setPosition();
        setWH(totalWidth, BOX_SIZE);

        float boxX = x;
        float boxY = y;
        float pillX = x + BOX_SIZE + GAP;
        float pillY = y + (BOX_SIZE - PILL_HEIGHT) / 2f;

        RenderUtil2D.drawRoundRect(boxX, boxY, BOX_SIZE, BOX_SIZE, BOX_RADIUS, BG_COLOR);
        RenderUtil2D.drawRoundRect(pillX, pillY, pillWidth, PILL_HEIGHT, PILL_RADIUS, BG_COLOR);

        float logoWidth = Fonts.pastaIcon.get(LOGO_ICON_SIZE).getWidth(LOGO_ICON);
        float logoHeight = Fonts.pastaIcon.get(LOGO_ICON_SIZE).getHeight();
        float logoX = boxX + (BOX_SIZE - logoWidth) / 2f - 1f;
        float logoY = boxY + (BOX_SIZE - logoHeight) / 2f;
        Fonts.pastaIcon.get(LOGO_ICON_SIZE).drawString(LOGO_ICON, logoX, logoY, TEXT_COLOR);

        float iconX = pillX + PILL_PADDING_X + USER_ICON_SHIFT_X;
        float textX = iconX + iconWidth + PILL_ICON_GAP;
        float textMaxWidth = Math.max(0f, pillWidth - (PILL_PADDING_X * 2f + iconWidth + PILL_ICON_GAP + PILL_EXTRA_WIDTH));
        String displayName = trimToWidth(name, textMaxWidth, Fonts.sfpro.get(TEXT_SIZE));

        float userIconWidth = Fonts.waterIcon.get(USER_ICON_SIZE).getWidth(USER_ICON);
        float userIconHeight = Fonts.waterIcon.get(USER_ICON_SIZE).getHeight();
        float userIconY = pillY + (PILL_HEIGHT - userIconHeight) / 2f;
        Fonts.waterIcon.get(USER_ICON_SIZE).drawString(USER_ICON, iconX, userIconY, TEXT_COLOR);

        float textHeight = Fonts.sfpro.get(TEXT_SIZE).getHeight();
        float textY = pillY + (PILL_HEIGHT - textHeight) / 2f;
        Fonts.sfpro.get(TEXT_SIZE).drawString(displayName, textX + 2.5f, textY + 0.2f, TEXT_COLOR);
    }

    private void setPosition() {
        x = dragging.getX();
        y = dragging.getY();
    }

    private void setWH(float width, float height) {
        this.width = width;
        this.height = height;
        dragging.setWidth(width);
        dragging.setHeight(height);
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
