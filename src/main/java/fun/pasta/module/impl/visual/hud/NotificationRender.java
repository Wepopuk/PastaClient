package fun.pasta.module.impl.visual.hud;

import fun.pasta.Pasta;
import fun.pasta.event.EventRender2D;
import fun.pasta.fonts.Fonts;
import fun.pasta.module.impl.visual.hud.impl.IRenderHud;
import fun.pasta.util.Notification;
import fun.pasta.util.NotificationManager;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.math.MathUtil;
import fun.pasta.util.render.RenderUtil2D;
import net.minecraft.util.math.MathHelper;

import java.util.Iterator;
import java.util.List;

public class NotificationRender implements IRenderHud {

    private static final long DISPLAY_MS = 2500L;
    private static final float ROW_HEIGHT = 16f;
    private static final float RADIUS = 5f;
    private static final float ICON_SIZE = 22f;
    private static final float GAP = 6f;
    private static final float STACK_GAP = 3f;
    private static final float TEXT_SIZE = 14f;
    private static final float ICON_TEXT_SIZE = 20f;
    private static final float TEXT_PADDING_X = 6f;
    private static final float SCREEN_MARGIN = 4f;

    private static final int BG_COLOR = ColorUtil.rgba(64, 59, 55, 179);
    private static final int TEXT_COLOR = ColorUtil.rgba(255, 255, 255, 255);

    @Override
    public void render(EventRender2D e) {
        fun.pasta.module.impl.visual.Hud hud = (fun.pasta.module.impl.visual.Hud) Pasta.instance.manager
                .getModule("HUD");
        if (hud == null || !hud.enable || !hud.notifications.get()) {
            return;
        }

        List<Notification> notifications = NotificationManager.getNotifications();
        if (notifications.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        float screenWidth = sr.getScaledWidth();

        int index = 0;
        Iterator<Notification> iterator = notifications.iterator();
        float stackHeight = notifications.size() * ROW_HEIGHT + Math.max(0, notifications.size() - 1) * STACK_GAP;
        float baseY = sr.getScaledHeight() / 2f - stackHeight / 2f;
        if (baseY < SCREEN_MARGIN) {
            baseY = SCREEN_MARGIN;
        }
        while (iterator.hasNext()) {
            Notification notification = iterator.next();
            long age = now - notification.getStartTime();
            float target = age < notification.getDuration() ? 1f : 0f;
            notification.anim = MathUtil.lerp(notification.anim, target, 10);

            if (target == 0f && notification.anim <= 0.05f) {
                iterator.remove();
                continue;
            }

            float targetY = baseY + index * (ROW_HEIGHT + STACK_GAP);
            if (!notification.yInit) {
                notification.y = targetY;
                notification.yInit = true;
            } else {
                notification.y = MathUtil.lerp(notification.y, targetY, 10);
            }

            String text = notification.getText();
            float textWidth = Fonts.sfpro.get(TEXT_SIZE).getWidth(text);
            float textBoxWidth = textWidth + TEXT_PADDING_X * 2f;
            float totalWidth = textBoxWidth + GAP + ICON_SIZE;
            float maxTotalWidth = screenWidth - SCREEN_MARGIN * 2f;
            if (totalWidth > maxTotalWidth) {
                float overflow = totalWidth - maxTotalWidth;
                textBoxWidth = Math.max(ICON_SIZE, textBoxWidth - overflow);
                totalWidth = textBoxWidth + GAP + ICON_SIZE;
            }

            float baseX = screenWidth / 2f - totalWidth / 2f;
            float slideX = (1f - notification.anim) * (totalWidth + 10f);
            float x = baseX + slideX;

            float alpha = clamp(notification.anim);
            int bgColor = applyAlpha(BG_COLOR, alpha);
            int textColor = applyAlpha(TEXT_COLOR, alpha);

            RenderUtil2D.drawRoundRect(x, notification.y, textBoxWidth, ROW_HEIGHT, RADIUS, bgColor);
            RenderUtil2D.drawRoundRect(x + textBoxWidth + GAP, notification.y, ICON_SIZE, ROW_HEIGHT, RADIUS, bgColor);

            String displayText = trimToWidth(text, textBoxWidth - TEXT_PADDING_X * 2f, Fonts.sfpro.get(TEXT_SIZE));
            Fonts.sfpro.get(TEXT_SIZE).drawCenterXY(displayText, x + textBoxWidth / 2f,
                    notification.y + ROW_HEIGHT / 2f, textColor);

            String iconChar = notification.isEnabled() ? "A" : "B";
            float iconWidth = Fonts.notifICON.get(ICON_TEXT_SIZE).getWidth(iconChar);
            float iconHeight = Fonts.notifICON.get(ICON_TEXT_SIZE).getHeight();
            float iconX = x + textBoxWidth + GAP + (ICON_SIZE - iconWidth) / 2f - 0.6f;
            float iconY = notification.y + (ROW_HEIGHT - iconHeight) / 2f;
            Fonts.notifICON.get(ICON_TEXT_SIZE).drawString(iconChar, iconX, iconY, textColor);

            index++;
        }
    }

    private int applyAlpha(int color, float alpha) {
        int a = (int) (ColorUtil.getAlpha(color) * clamp(alpha));
        return ColorUtil.rgba(ColorUtil.getRed(color), ColorUtil.getGreen(color), ColorUtil.getBlue(color), a);
    }

    private float clamp(float value) {
        return MathHelper.clamp(value, 0f, 1f);
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
