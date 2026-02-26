package fun.pasta.module.impl.visual.hud;

import fun.pasta.Pasta;
import fun.pasta.drag.Dragging;
import fun.pasta.event.EventRender2D;
import fun.pasta.fonts.Font;
import fun.pasta.fonts.Fonts;
import fun.pasta.module.impl.misc.NameProtect;
import fun.pasta.module.impl.visual.hud.impl.IRenderHud;
import fun.pasta.util.HealthUtil;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.math.Vector4i;
import fun.pasta.util.render.RenderUtil2D;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.util.text.ITextComponent;

import static fun.pasta.util.IMinecraft.mc;

public class TargetHudRender implements IRenderHud {

    private static final float SCALE = 2.0f;

    private static final float HUD_WIDTH = 265f / SCALE;
    private static final float HUD_HEIGHT = 80f / SCALE;
    private static final float HUD_RADIUS = 16f / SCALE;
    private static final float PADDING_LEFT = 8f / SCALE;
    private static final float PADDING_RIGHT = 12f / SCALE;
    private static final float GAP = 9f / SCALE;

    private static final float HEAD_SIZE = 60f / SCALE;
    private static final float HEAD_RADIUS = 9f / SCALE;

    private static final float NAME_SIZE = 18f;
    private static final float HP_SIZE = 16f;
    private static final float BAR_WIDTH = 167f / SCALE;
    private static final float BAR_HEIGHT = 18f / SCALE;

    private static final int BACKGROUND_COLOR = ColorUtil.rgba(64, 59, 55, 179);
    private static final int TEXT_COLOR = ColorUtil.rgba(255, 255, 255, 255);
    private static final int BAR_BG_COLOR = ColorUtil.rgba(50, 50, 50, 200);
    private static final int BAR_GRADIENT_START = ColorUtil.rgba(206, 105, 17, 255);
    private static final int BAR_GRADIENT_END = ColorUtil.rgba(104, 53, 8, 255);

    private final Dragging dragging;
    private float x;
    private float y;
    private float width;
    private float height;

    private AbstractClientPlayerEntity chatPlayer = null;
    private long chatPlayerTime = 0;

    public TargetHudRender(Dragging dragging) {
        this.dragging = dragging;
    }

    @Override
    public void render(EventRender2D e) {
        setPosition();
        fun.pasta.module.impl.visual.Hud hud = (fun.pasta.module.impl.visual.Hud) Pasta.instance.manager
                .getModule("HUD");
        if (hud == null || !hud.enable || !hud.targetHud.get()) {
            return;
        }

        LivingEntity target = Pasta.instance.manager != null && Pasta.instance.manager.killAura != null
                ? Pasta.instance.manager.killAura.target
                : null;

        AbstractClientPlayerEntity displayPlayer = null;
        if (mc.currentScreen instanceof ChatScreen && mc.player instanceof AbstractClientPlayerEntity) {
            if (target instanceof AbstractClientPlayerEntity) {
                displayPlayer = (AbstractClientPlayerEntity) target;
            } else {
                displayPlayer = (AbstractClientPlayerEntity) mc.player;
            }
        } else if (target instanceof AbstractClientPlayerEntity) {
            displayPlayer = (AbstractClientPlayerEntity) target;
        } else if (chatPlayer != null && System.currentTimeMillis() - chatPlayerTime < 5000) {
            displayPlayer = chatPlayer;
        }

        if (displayPlayer == null) {
            return;
        }

        width = HUD_WIDTH;
        height = HUD_HEIGHT;
        dragging.setWidth(width);
        dragging.setHeight(height);

        String name = displayPlayer.getName().getString();
        Object module = Pasta.instance.manager.getModule("NameProtect");
        if (module instanceof NameProtect) {
            name = ((NameProtect) module).protect(name);
        }
        float currentHealth = HealthUtil.getHealthForTargetHud(displayPlayer);
        String healthText = String.format("HP : %.1f", currentHealth);

        drawBackground();
        drawHead(displayPlayer);
        drawInfo(name, healthText);
        drawHealthBar(displayPlayer);
    }

    private void drawBackground() {
        RenderUtil2D.drawRoundRect(x, y, width, height, HUD_RADIUS, BACKGROUND_COLOR);
    }

    private void drawHead(AbstractClientPlayerEntity player) {
        float headX = x + PADDING_LEFT;
        float headY = y + (height - HEAD_SIZE) / 2f;
        ResourceLocation skin = player.getLocationSkin();

        RenderUtil2D.drawRoundedHead(
                skin,
                headX,
                headY,
                HEAD_SIZE,
                HEAD_SIZE,
                HEAD_RADIUS,
                1.0f,
                0.0f
        );
    }

    private void drawInfo(String name, String healthText) {
        Font nameFont = getFont(Fonts.sfpro, Fonts.mstr, NAME_SIZE);
        Font hpFont = getFont(Fonts.sfpro, Fonts.mstr, HP_SIZE);

        float contentX = x + PADDING_LEFT + HEAD_SIZE + GAP;
        float contentY = y + (height - HEAD_SIZE) / 2f;

        float contentRight = x + width - PADDING_RIGHT;
        float contentWidth = Math.max(0f, contentRight - contentX);

        float nameHeight = nameFont.getHeight();
        float hpHeight = hpFont.getHeight();
        float gapY = Math.max(0f, (HEAD_SIZE - (nameHeight + hpHeight + BAR_HEIGHT)) / 2f);

        float nameY = contentY;
        float hpY = nameY + nameHeight + gapY;

        String displayName = trimToWidth(name, contentWidth, nameFont);
        String displayHp = trimToWidth(healthText, contentWidth, hpFont);

        nameFont.drawString(displayName, contentX, nameY, TEXT_COLOR);
        hpFont.drawString(displayHp, contentX, hpY, TEXT_COLOR);
    }

    private void drawHealthBar(AbstractClientPlayerEntity player) {
        float barX = x + PADDING_LEFT + HEAD_SIZE + GAP;
        float contentY = y + (height - HEAD_SIZE) / 2f;

        // правая граница контента (чтобы не было бага с отрицательной шириной)
        float contentRight = x + width - PADDING_RIGHT;
        float barWidth = Math.min(BAR_WIDTH, Math.max(0f, contentRight - barX));

        // вычисляем Y так же, как в drawInfo
        Font nameFont = getFont(Fonts.sfpro, Fonts.mstr, NAME_SIZE);
        Font hpFont = getFont(Fonts.sfpro, Fonts.mstr, HP_SIZE);

        float nameHeight = nameFont.getHeight();
        float hpHeight = hpFont.getHeight();
        float gapY = Math.max(0f, (HEAD_SIZE - (nameHeight + hpHeight + BAR_HEIGHT)) / 2f);

        float barY = contentY + nameHeight + gapY + hpHeight + gapY;

        // хп %
        float maxHealth = player.getMaxHealth();
        float currentHealth = MathHelper.clamp(HealthUtil.getHealthForTargetHud(player), 0f, maxHealth);
        float healthPercentage = (maxHealth > 0f) ? (currentHealth / maxHealth) : 0f;

        float healthWidth = MathHelper.clamp(barWidth * healthPercentage, 0f, barWidth);

        // радиус скругления бара (обычно половина высоты выглядит лучше всего)
        float barRadius = Math.min(BAR_HEIGHT / 2f, 6f / SCALE + 3f);

        // фон бара
        RenderUtil2D.drawRoundRect(
                barX, barY,
                barWidth, BAR_HEIGHT,
                barRadius,
                BAR_BG_COLOR
        );

        // заливка (скругляем тоже; если ширина маленькая — ограничиваем радиус)
        if (healthWidth > 0.001f) {
            float fillRadius = Math.min(barRadius, healthWidth / 2f);
            RenderUtil2D.drawRoundedRect(
                    barX,
                    barY,
                    healthWidth,
                    BAR_HEIGHT,
                    new Vector4f(fillRadius, fillRadius, fillRadius, fillRadius),
                    new Vector4i(
                            BAR_GRADIENT_START,
                            BAR_GRADIENT_START,
                            BAR_GRADIENT_END,
                            BAR_GRADIENT_END
                    )
            );
        }
    }



    public void processChatMessage(ITextComponent message) {
        String text = message.getString().toLowerCase();

        if (mc.world != null && mc.player != null) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player instanceof AbstractClientPlayerEntity
                        && !player.equals(mc.player)
                        && text.contains(player.getName().getString().toLowerCase())) {
                    chatPlayer = (AbstractClientPlayerEntity) player;
                    chatPlayerTime = System.currentTimeMillis();
                    break;
                }
            }
        }
    }

    private String trimToWidth(String text, float maxWidth, Font font) {
        if (text == null || text.isEmpty() || maxWidth <= 0f) {
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

    private Font getFont(Fonts primary, Fonts fallback, float size) {
        try {
            return primary.get(size);
        } catch (RuntimeException ex) {
            return fallback.get(size);
        }
    }

    private void setPosition() {
        x = dragging.getX();
        y = dragging.getY();
    }
}
