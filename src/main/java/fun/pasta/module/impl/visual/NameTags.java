package fun.pasta.module.impl.visual;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import fun.pasta.event.EventRender3D;
import fun.pasta.fonts.Font;
import fun.pasta.fonts.Fonts;
import fun.pasta.Pasta;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.util.render.RenderUtil2D;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.optifine.reflect.Reflector;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.Optional;

public class NameTags extends Module {

    public NameTags() {
        super("NameTags", Category.VISUAL);
    }

    @Subscribe
    public void onRender3D(EventRender3D event) {
        if (!enable || mc.world == null || mc.player == null) {
            return;
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) {
                continue;
            }
            try {
                renderNameTag(player, event.matrixStack, event.camX, event.camY, event.camZ, event.partialTicks);
            } catch (Throwable ignored) {
            }
        }
    }

    private void renderNameTag(PlayerEntity player, MatrixStack matrixStack, double camX, double camY, double camZ, float partialTicks) {
        double distSq = mc.getRenderManager().squareDistanceTo(player);
        boolean render = !(distSq > 4096.0D);

        if (Reflector.ForgeHooksClient_isNameplateInRenderDistance.exists()) {
            render = Reflector.ForgeHooksClient_isNameplateInRenderDistance.callBoolean(player, distSq);
        }

        if (!render) {
            return;
        }

        double x = MathHelper.lerp(partialTicks, player.lastTickPosX, player.getPosX()) - camX;
        double y = MathHelper.lerp(partialTicks, player.lastTickPosY, player.getPosY()) - camY;
        double z = MathHelper.lerp(partialTicks, player.lastTickPosZ, player.getPosZ()) - camZ;

        float yOffset = player.getHeight() + 0.5F;
        int baseY = "deadmau5".equals(player.getName().getString()) ? -10 : 0;
        boolean seeThrough = !player.isDiscrete();
        int alphaMask = seeThrough ? 0xFF000000 : 0x20000000;
        boolean isFriend = Pasta.instance.friendManager != null && Pasta.instance.friendManager.isFriend(player);
        int textColor = isFriend ? (alphaMask | 0x55FF55) : (seeThrough ? -1 : 553648127);

        // Render names with a higher glyph resolution and compensate by lower world-scale.
        final float baseFontSize = 13.0F;
        final float renderFontSize = 30.0F;
        final float scaleMul = baseFontSize / renderFontSize;

        Font font = Fonts.mstr.get(renderFontSize);
        float textHeight = font.getHeight();
        float paddingX = 2.0F;
        float paddingY = 1.0F;
        float extraWidth = 6.0F;

        matrixStack.push();
        try {
            matrixStack.translate(x, y + yOffset, z);
            matrixStack.rotate(mc.getRenderManager().getCameraOrientation());
            matrixStack.scale(-0.025F * scaleMul, -0.025F * scaleMul, 0.025F * scaleMul);

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();

            try {
                Scoreboard scoreboard = player.getWorldScoreboard();
                ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(2);

                if (objective != null && distSq < 100.0D) {
                    Score score = scoreboard.getOrCreateScore(player.getScoreboardName(), objective);
                    ITextComponent scoreText = new StringTextComponent(Integer.toString(score.getScorePoints()))
                            .appendString(" ")
                            .append(objective.getDisplayName());
                    float scoreWidth = measureComponent(font, scoreText);
                    float scoreY = baseY - textHeight - 2.0F;

                    drawTagBackground(matrixStack, scoreWidth + extraWidth, textHeight, scoreY, paddingX, paddingY);
                    drawComponent(font, matrixStack, scoreText, -scoreWidth / 2.0F, scoreY, textColor, alphaMask);
                }

                Team team = player.getTeam();
                ScorePlayerTeam scoreTeam = team instanceof ScorePlayerTeam ? (ScorePlayerTeam) team : null;

                ITextComponent prefixComponent = scoreTeam != null ? scoreTeam.getPrefix() : StringTextComponent.EMPTY;
                ITextComponent suffixComponent = scoreTeam != null ? scoreTeam.getSuffix() : StringTextComponent.EMPTY;

                IFormattableTextComponent nameComponent = player.getName().deepCopy();
                if (!isFriend && team != null && team.getColor() != TextFormatting.RESET) {
                    nameComponent.mergeStyle(team.getColor());
                }

                int healthValue = MathHelper.ceil(player.getHealth());
                String hpText = " [" + healthValue + "]";

                float prefixWidth = measureComponent(font, prefixComponent);
                float suffixWidth = measureComponent(font, suffixComponent);
                float nameWidth = measureComponent(font, nameComponent);
                float hpWidth = font.getWidth(hpText);
                float totalWidth = prefixWidth + nameWidth + suffixWidth + hpWidth;

                drawTagBackground(matrixStack, totalWidth + extraWidth, textHeight, baseY, paddingX, paddingY);

                float currentX = -totalWidth / 2.0F;
                currentX += drawComponent(font, matrixStack, prefixComponent, currentX, baseY, textColor, alphaMask);
                currentX += drawComponent(font, matrixStack, nameComponent, currentX, baseY, textColor, alphaMask);
                currentX += drawComponent(font, matrixStack, suffixComponent, currentX, baseY, textColor, alphaMask);

                int hpColor = alphaMask | getHealthColor(player.getHealth(), player.getMaxHealth());
                font.drawString(matrixStack, hpText, currentX, baseY, hpColor);
            } finally {
                RenderSystem.enableCull();
                RenderSystem.disableBlend();
                RenderSystem.depthMask(true);
                RenderSystem.enableDepthTest();
            }
        } finally {
            matrixStack.pop();
        }
    }

    private void drawTagBackground(MatrixStack matrixStack, float width, float height, float y, float paddingX, float paddingY) {
        float rectX = -width / 2.0F - paddingX;
        float rectY = y - paddingY;
        float rectW = width + paddingX * 2.0F;
        float rectH = height + paddingY * 2.0F;
        RenderUtil2D.drawRoundRect(matrixStack, rectX, rectY, rectW, rectH, 8.0F, 0x90000000);
    }

    private float measureComponent(Font font, ITextComponent component) {
        if (component == null) {
            return 0.0F;
        }

        float[] width = new float[] {0.0F};
        component.getComponentWithStyle((style, text) -> {
            if (text != null && !text.isEmpty()) {
                String formatted = getFormatCodes(style) + text;
                width[0] += font.getWidth(formatted);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return width[0];
    }

    private float drawComponent(Font font, MatrixStack matrixStack, ITextComponent component, float x, float y,
                                int defaultColor, int alphaMask) {
        if (component == null) {
            return 0.0F;
        }

        float[] width = new float[] {0.0F};
        component.getComponentWithStyle((style, text) -> {
            if (text == null || text.isEmpty()) {
                return Optional.empty();
            }

            int color = defaultColor;
            if (style.getColor() != null) {
                color = alphaMask | style.getColor().getColor();
            }

            String formatted = getFormatCodes(style) + text;
            font.drawString(matrixStack, formatted, x + width[0], y, color);
            width[0] += font.getWidth(formatted);
            return Optional.empty();
        }, Style.EMPTY);
        return width[0];
    }

    private String getFormatCodes(Style style) {
        if (style == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        if (style.getObfuscated()) {
            sb.append(TextFormatting.OBFUSCATED);
        }
        if (style.getBold()) {
            sb.append(TextFormatting.BOLD);
        }
        if (style.getStrikethrough()) {
            sb.append(TextFormatting.STRIKETHROUGH);
        }
        if (style.getUnderlined()) {
            sb.append(TextFormatting.UNDERLINE);
        }
        if (style.getItalic()) {
            sb.append(TextFormatting.ITALIC);
        }
        return sb.toString();
    }

    private int getHealthColor(float health, float maxHealth) {
        if (maxHealth <= 0.0F) {
            return 0xFFFFFF;
        }

        float percentage = MathHelper.clamp(health / maxHealth, 0.0F, 1.0F);
        if (percentage > 0.75f) {
            return 0x64FF64;
        }
        if (percentage > 0.5f) {
            return 0xB4FF64;
        }
        if (percentage > 0.25f) {
            return 0xFFFF64;
        }
        if (percentage > 0.1f) {
            return 0xFFB432;
        }
        return 0xFF6464;
    }
}
