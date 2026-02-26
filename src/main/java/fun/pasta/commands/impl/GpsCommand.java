package fun.pasta.commands.impl;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.pasta.Pasta;
import fun.pasta.commands.Command;
import fun.pasta.event.EventRender2D;
import fun.pasta.fonts.Font;
import fun.pasta.fonts.Fonts;
import fun.pasta.util.IMinecraft;
import fun.pasta.util.chat.ChatUtil;
import fun.pasta.util.render.RenderUtil2D;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

public class GpsCommand extends Command implements IMinecraft {

    private static final ResourceLocation ARROW_TEXTURE = new ResourceLocation("pasta/images/icons/arrow.png");
    private static final float ORBIT_RADIUS = 42.0F;
    private static final float ARROW_SIZE = 13.0F;
    private static final int ARROW_COLOR = 0xFFFFFFFF;
    private static final int SHADOW_COLOR = 0xAA000000;

    private static volatile boolean active;
    private static volatile double gpsX;
    private static volatile double gpsZ;
    private static boolean listenerRegistered = false;

    public GpsCommand() {
        super("gps");
        if (!listenerRegistered && Pasta.instance != null && Pasta.instance.eventBus != null) {
            Pasta.instance.eventBus.register(this);
            listenerRegistered = true;
        }
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 1 && ("off".equalsIgnoreCase(args[0]) || "clear".equalsIgnoreCase(args[0])
                || "disable".equalsIgnoreCase(args[0]) || "reset".equalsIgnoreCase(args[0]))) {
            active = false;
            ChatUtil.addChatMessage("GPS disabled.");
            return;
        }

        if (args.length < 2) {
            ChatUtil.addChatMessage("Usage: .gps <x> <z>");
            return;
        }

        try {
            gpsX = Double.parseDouble(args[0]);
            gpsZ = Double.parseDouble(args[1]);
            active = true;
            ChatUtil.addChatMessage("GPS set: X " + Math.round(gpsX) + ", Z " + Math.round(gpsZ));
        } catch (NumberFormatException e) {
            ChatUtil.addChatMessage("Usage: .gps <x> <z>");
        }
    }

    @Subscribe
    public void onRender2D(EventRender2D event) {
        if (!active || mc.player == null || mc.world == null || event.mainWindow == null) {
            return;
        }

        float partialTicks = event.particles;
        float centerX = event.mainWindow.getScaledWidth() / 2.0F;
        float centerY = event.mainWindow.getScaledHeight() / 2.0F;
        float playerYaw = MathHelper.lerp(partialTicks, mc.player.prevRotationYaw, mc.player.rotationYaw);
        Font font = Fonts.mstr.get(11.0F);

        double selfX = MathHelper.lerp(partialTicks, mc.player.lastTickPosX, mc.player.getPosX());
        double selfZ = MathHelper.lerp(partialTicks, mc.player.lastTickPosZ, mc.player.getPosZ());
        double diffX = gpsX - selfX;
        double diffZ = gpsZ - selfZ;
        double distance = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float relativeYaw;
        if (distance < 1.0E-4D) {
            relativeYaw = 0.0F;
        } else {
            float yawToTarget = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0D);
            relativeYaw = MathHelper.wrapDegrees(yawToTarget - playerYaw);
        }
        float radians = (float) Math.toRadians(relativeYaw);

        float arrowX = centerX + MathHelper.sin(radians) * ORBIT_RADIUS;
        float arrowY = centerY - MathHelper.cos(radians) * ORBIT_RADIUS;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        drawArrow(arrowX, arrowY, relativeYaw, ARROW_COLOR);

        if (event.matrixStack != null) {
            String distanceText = Math.round(distance) + "m";
            float textX = arrowX - font.getWidth(distanceText) / 2.0F;
            float textY = arrowY + ARROW_SIZE / 2.0F + 2.0F;
            font.drawString(event.matrixStack, distanceText, textX + 0.5F, textY + 0.5F, SHADOW_COLOR);
            font.drawString(event.matrixStack, distanceText, textX, textY, ARROW_COLOR);
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
