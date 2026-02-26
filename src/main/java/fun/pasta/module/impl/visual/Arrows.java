package fun.pasta.module.impl.visual;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.pasta.Pasta;
import fun.pasta.event.EventRender2D;
import fun.pasta.fonts.Font;
import fun.pasta.fonts.Fonts;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.impl.combat.AntiBot;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.util.render.RenderUtil2D;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

public class Arrows extends Module {

    private static final ResourceLocation ARROW_TEXTURE = new ResourceLocation("pasta/images/icons/arrow.png");
    private static final float ORBIT_RADIUS = 42.0F;
    private static final float ARROW_SIZE = 13.0F;
    private static final int DEFAULT_COLOR = 0xFFFFFFFF;
    private static final int FRIEND_COLOR = 0xFF55FF55;
    private static final int SHADOW_COLOR = 0xAA000000;

    public final BooleanOption showMeters = new BooleanOption("Show Meters", true);

    public Arrows() {
        super("Arrows", Category.VISUAL);
        add(showMeters);
    }

    @Subscribe
    public void onRender2D(EventRender2D event) {
        if (!enable || mc.world == null || mc.player == null || event.mainWindow == null) {
            return;
        }

        float partialTicks = event.particles;
        float centerX = event.mainWindow.getScaledWidth() / 2.0F;
        float centerY = event.mainWindow.getScaledHeight() / 2.0F;
        float playerYaw = MathHelper.lerp(partialTicks, mc.player.prevRotationYaw, mc.player.rotationYaw);
        Font font = Fonts.mstr.get(11.0F);

        double selfX = MathHelper.lerp(partialTicks, mc.player.lastTickPosX, mc.player.getPosX());
        double selfZ = MathHelper.lerp(partialTicks, mc.player.lastTickPosZ, mc.player.getPosZ());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive() || player.isSpectator()) {
                continue;
            }

            if (AntiBot.isBot(player)) {
                continue;
            }

            double targetX = MathHelper.lerp(partialTicks, player.lastTickPosX, player.getPosX());
            double targetZ = MathHelper.lerp(partialTicks, player.lastTickPosZ, player.getPosZ());
            double diffX = targetX - selfX;
            double diffZ = targetZ - selfZ;

            if (diffX * diffX + diffZ * diffZ < 1.0E-4D) {
                continue;
            }

            float yawToTarget = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0D);
            float relativeYaw = MathHelper.wrapDegrees(yawToTarget - playerYaw);
            float radians = (float) Math.toRadians(relativeYaw);

            // relativeYaw = 0 means "in front of camera".
            // On 2D screen that should be above crosshair.
            float arrowX = centerX + MathHelper.sin(radians) * ORBIT_RADIUS;
            float arrowY = centerY - MathHelper.cos(radians) * ORBIT_RADIUS;

            boolean isFriend = Pasta.instance != null
                    && Pasta.instance.friendManager != null
                    && Pasta.instance.friendManager.isFriend(player);
            int arrowColor = isFriend ? FRIEND_COLOR : DEFAULT_COLOR;

            drawArrow(arrowX, arrowY, relativeYaw, arrowColor);

            if (showMeters.get() && event.matrixStack != null) {
                int meters = Math.round((float) mc.player.getDistance(player));
                String distanceText = meters + "m";
                float textX = arrowX - font.getWidth(distanceText) / 2.0F;
                float textY = arrowY + ARROW_SIZE / 2.0F + 2.0F;

                font.drawString(event.matrixStack, distanceText, textX + 0.5F, textY + 0.5F, SHADOW_COLOR);
                font.drawString(event.matrixStack, distanceText, textX, textY, arrowColor);
            }
        }

        RenderSystem.disableBlend();
    }

    private void drawArrow(float x, float y, float angle, int color) {
        RenderSystem.pushMatrix();
        RenderSystem.translatef(x, y, 0.0F);
        RenderSystem.rotatef(angle, 0.0F, 0.0F, 1.0F);
        RenderUtil2D.drawImage(ARROW_TEXTURE, -ARROW_SIZE / 2.0F, -ARROW_SIZE / 2.0F, ARROW_SIZE, ARROW_SIZE, color);
        RenderSystem.popMatrix();
    }
}
