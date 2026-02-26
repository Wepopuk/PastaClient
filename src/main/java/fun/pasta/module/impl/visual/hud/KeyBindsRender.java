package fun.pasta.module.impl.visual.hud;

import fun.pasta.Pasta;
import fun.pasta.drag.Dragging;
import fun.pasta.event.EventRender2D;
import fun.pasta.fonts.Fonts;
import fun.pasta.module.api.Module;
import fun.pasta.module.impl.visual.hud.impl.IRenderHud;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.key.KeyUtil;
import fun.pasta.util.math.MathUtil;
import fun.pasta.util.render.RenderUtil2D;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class KeyBindsRender implements IRenderHud {

    private static final float PILL_HEIGHT = 14f;
    private static final float PILL_RADIUS = 7f;
    private static final float GAP = 1f;
    private static final float TEXT_SIZE = 13f;
    private static final float HEADER_MIN_WIDTH = 46f;
    private static final float HEADER_PADDING_X = 10f;
    private static final float ITEM_PADDING_X = 5f;
    private static final float HEADER_OFFSET_X = 6f;

    private static final int HEADER_COLOR = ColorUtil.rgba(64, 59, 55, 200);
    private static final int ITEM_COLOR = ColorUtil.rgba(64, 59, 55, 150);
    private static final int TEXT_COLOR = ColorUtil.rgba(255, 255, 255, 255);

    private final Dragging dragging;
    private float x;
    private float y;
    private float renderX;
    private float renderY;
    private float width;
    private float height;
    private boolean positionInitialized;

    public KeyBindsRender(Dragging dragging) {
        this.dragging = dragging;
    }

    @Override
    public void render(EventRender2D e) {
        fun.pasta.module.impl.visual.Hud hud = (fun.pasta.module.impl.visual.Hud) Pasta.instance.manager
                .getModule("HUD");
        if (hud == null || !hud.enable || !hud.keyBinds.get()) {
            return;
        }

        setPosition();

        List<BindLine> lines = new ArrayList<>();
        for (Module module : Pasta.instance.manager.modules) {
            module.animAlpha = MathUtil.lerp(module.animAlpha, module.enable ? 1.0f : 0.0f, 10);
            if (module.key == 0 || module.animAlpha <= 0.05f) {
                continue;
            }
            String keyName = KeyUtil.getKey(module.key);
            String line = module.name + " (" + keyName + ")";
            float textWidth = Fonts.sfpro.get(TEXT_SIZE).getWidth(line);
            lines.add(new BindLine(line, textWidth, module.animAlpha));
        }

        lines.sort((a, b) -> Float.compare(b.textWidth, a.textWidth));

        float headerTextWidth = Fonts.sfpro.get(TEXT_SIZE).getWidth("Hotkeys");
        float headerWidth = Math.max(HEADER_MIN_WIDTH, headerTextWidth + HEADER_PADDING_X * 2f);

        float maxItemWidth = 0f;
        for (BindLine line : lines) {
            float pillWidth = line.textWidth + ITEM_PADDING_X * 2f;
            if (pillWidth > maxItemWidth) {
                maxItemWidth = pillWidth;
            }
        }

        width = Math.max(maxItemWidth, headerWidth + HEADER_OFFSET_X);
        height = PILL_HEIGHT;
        if (!lines.isEmpty()) {
            height += GAP + lines.size() * PILL_HEIGHT + (lines.size() - 1) * GAP;
        }

        dragging.setWidth(width);
        dragging.setHeight(height);

        drawPill(renderX + HEADER_OFFSET_X, renderY, headerWidth, HEADER_COLOR, "Hotkeys", 1.0f);

        float startY = renderY + PILL_HEIGHT + (lines.isEmpty() ? 0f : GAP);
        for (int i = 0; i < lines.size(); i++) {
            BindLine line = lines.get(i);
            float pillWidth = line.textWidth + ITEM_PADDING_X * 2f;
            float pillY = startY + i * (PILL_HEIGHT + GAP);
            drawPill(renderX, pillY, pillWidth, ITEM_COLOR, line.text, line.alpha);
        }
    }

    private void setPosition() {
        x = dragging.getX();
        y = dragging.getY();
        if (!positionInitialized) {
            renderX = x;
            renderY = y;
            positionInitialized = true;
        } else {
            renderX = MathUtil.lerp(renderX, x, 10);
            renderY = MathUtil.lerp(renderY, y, 10);
        }
    }

    private void drawPill(float px, float py, float pillWidth, int color, String text, float alpha) {
        int drawColor = applyAlpha(color, alpha);
        RenderUtil2D.drawRoundRect(px, py, pillWidth, PILL_HEIGHT, PILL_RADIUS, drawColor);
        int textColor = ColorUtil.rgba(255, 255, 255, (int) (255 * clamp(alpha)));
        Fonts.sfpro.get(TEXT_SIZE).drawCenterXY(text, px + pillWidth / 2f, py + PILL_HEIGHT / 2f, textColor);
    }

    private int applyAlpha(int color, float alpha) {
        int a = (int) (ColorUtil.getAlpha(color) * clamp(alpha));
        return ColorUtil.rgba(ColorUtil.getRed(color), ColorUtil.getGreen(color), ColorUtil.getBlue(color), a);
    }

    private float clamp(float value) {
        return MathHelper.clamp(value, 0f, 1f);
    }

    private static class BindLine {
        private final String text;
        private final float textWidth;
        private final float alpha;

        private BindLine(String text, float textWidth, float alpha) {
            this.text = text;
            this.textWidth = textWidth;
            this.alpha = alpha;
        }
    }
}
