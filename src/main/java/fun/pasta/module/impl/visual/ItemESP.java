package fun.pasta.module.impl.visual;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.pasta.event.EventRender3D;
import fun.pasta.fonts.Font;
import fun.pasta.fonts.Fonts;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.util.render.RenderUtil2D;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

public class ItemESP extends Module {

    private static final double MAX_DISTANCE_SQ = 80.0D * 80.0D;

    public ItemESP() {
        super("ItemESP", Category.VISUAL);
    }

    @Subscribe
    public void onRender3D(EventRender3D event) {
        if (!enable || mc.world == null || mc.player == null) {
            return;
        }

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        try {
            for (Entity entity : mc.world.getAllEntities()) {
                if (!(entity instanceof ItemEntity itemEntity)) {
                    continue;
                }

                if (mc.player.getDistanceSq(itemEntity) > MAX_DISTANCE_SQ) {
                    continue;
                }

                ItemStack stack = itemEntity.getItem();
                if (stack.isEmpty()) {
                    continue;
                }

                try {
                    renderItemTag(event, itemEntity, stack);
                } catch (Throwable ignored) {
                }
            }
        } finally {
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    }

    private void renderItemTag(EventRender3D event, ItemEntity itemEntity, ItemStack stack) {
        MatrixStack matrixStack = event.matrixStack;
        final float baseFontSize = 10.0F;
        final float renderFontSize = 30.0F;
        final float scaleMul = baseFontSize / renderFontSize;
        Font font = Fonts.mstr.get(renderFontSize);

        double x = MathHelper.lerp(event.partialTicks, itemEntity.lastTickPosX, itemEntity.getPosX()) - event.camX;
        double y = MathHelper.lerp(event.partialTicks, itemEntity.lastTickPosY, itemEntity.getPosY())
                - event.camY + itemEntity.getHeight() + 0.25D;
        double z = MathHelper.lerp(event.partialTicks, itemEntity.lastTickPosZ, itemEntity.getPosZ()) - event.camZ;

        String text = stack.getDisplayName().getString() + " x" + stack.getCount();
        int textWidth = font.getWidth(text);
        if (textWidth <= 0) {
            return;
        }
        int textHeight = font.getHeight();

        float distance = mc.player.getDistance(itemEntity);
        float scale = MathHelper.clamp(0.016F + distance * 0.0012F, 0.016F, 0.042F);

        matrixStack.push();
        try {
            matrixStack.translate(x, y, z);
            matrixStack.rotate(event.activeRenderInfo.getRotation());
            matrixStack.scale(-scale * scaleMul, -scale * scaleMul, scale * scaleMul);

            float textX = -textWidth / 2.0F;
            float textY = -1.0F;
            RenderUtil2D.drawRect(
                    matrixStack,
                    textX - 2.0F,
                    textY - 1.0F,
                    textWidth + 4.0F,
                    textHeight + 2.0F,
                    0x90000000
            );
            font.drawString(matrixStack, text, textX, textY, 0xFFFFFFFF);
        } finally {
            matrixStack.pop();
        }
    }
}
