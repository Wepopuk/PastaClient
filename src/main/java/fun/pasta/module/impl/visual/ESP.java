package fun.pasta.module.impl.visual;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.pasta.event.EventRender3D;
import fun.pasta.fonts.Font;
import fun.pasta.fonts.Fonts;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.module.settings.MultiBoxOption;
import fun.pasta.module.settings.SliderOption;
import fun.pasta.util.HealthUtil;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.render.RenderUtil2D;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.vector.Vector4f;

public class ESP extends Module {

    public final SliderOption scaleSetting = new SliderOption("Scale", 1.0F, 0.5F, 2.0F, 0.1F);
    public final MultiBoxOption elements = new MultiBoxOption("Elements",
            new BooleanOption("NameTags", true),
            new BooleanOption("HealthBar", true),
            new BooleanOption("Item", true),
            new BooleanOption("Box", true));

    public ESP() {
        super("ESP", Category.VISUAL);
        add(scaleSetting, elements);
    }

    @Subscribe
    public void onRender3D(EventRender3D event) {
        if (!enable || mc.world == null || mc.player == null) {
            return;
        }

        Matrix4f projection = Minecraft.getInstance().gameRenderer
                .getProjectionMatrix(event.activeRenderInfo, event.partialTicks, true);
        Matrix4f view = new Matrix4f(event.matrixStack.getLast().getMatrix());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive() || player.isInvisible()) {
                continue;
            }

            AxisAlignedBB bb = player.getBoundingBox().grow(0.1D);
            float[] screen = projectBox(bb, view, projection, event.camX, event.camY, event.camZ);
            if (screen == null) {
                continue;
            }

            float minX = screen[0];
            float minY = screen[1];
            float maxX = screen[2];
            float maxY = screen[3];

            float width = maxX - minX;
            float height = maxY - minY;
            if (width <= 1.0F || height <= 1.0F) {
                continue;
            }

            double dx = player.getPosX() - event.camX;
            double dy = player.getPosY() - event.camY;
            double dz = player.getPosZ() - event.camZ;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            float distScale = (float) MathHelper.clamp(1.0 - (dist - 15.0F) * 0.006F, 0.45F, 1.0F);
            float finalScale = scaleSetting.getValue().floatValue() * distScale;

            MatrixStack guiStack = new MatrixStack();

            if (elements.get("Box")) {
                drawCornerBox(guiStack, minX, minY, width, height, ColorUtil.rgba(54, 54, 55, 200));
            }

            if (elements.get("HealthBar")) {
                float hp = HealthUtil.getHealthForEsp(player) + Math.max(player.getAbsorptionAmount(), 0.0F);
                float maxHp = Math.max(player.getMaxHealth(), 20.0F) + Math.max(player.getAbsorptionAmount(), 0.0F);
                float ratio = (maxHp <= 0.001F) ? 0.0F : MathHelper.clamp(hp / maxHp, 0.0F, 1.0F);

                float barX = minX - 5.0F;
                float barW = 2.5F;
                float barH = height;

                RenderUtil2D.drawRect(guiStack, barX, minY, barW, barH, ColorUtil.rgba(33, 35, 39, 255));

                int hpCol = hp > 12 ? ColorUtil.rgba(67, 200, 96, 255)
                        : (hp > 6 ? ColorUtil.rgba(255, 190, 64, 255)
                        : ColorUtil.rgba(235, 87, 87, 255));

                RenderUtil2D.drawRect(guiStack, barX, maxY - (barH * ratio), barW, barH * ratio, hpCol);
            }

            if (elements.get("NameTags")) {
                float hp = HealthUtil.getHealthForEsp(player) + Math.max(player.getAbsorptionAmount(), 0.0F);
                int hpInt = MathHelper.ceil(hp);
                String tag = player.getName().getString() + " [" + hpInt + "]";

                Font font = Fonts.mstr.get(9.0F);
                float textW = font.getWidth(tag);
                float textH = font.getHeight();
                float rectW = textW + 8.0F;
                float rectH = textH + 4.0F;
                float rx = minX + (width - rectW) / 2.0F;
                float ty = minY - rectH - 6.0F;

                float centerX = rx + rectW / 2.0F;
                float centerY = ty + rectH / 2.0F;
                guiStack.push();
                guiStack.translate(centerX, centerY, 0);
                guiStack.scale(finalScale, finalScale, 1.0F);
                guiStack.translate(-centerX, -centerY, 0);

                RenderUtil2D.drawRoundRect(guiStack, rx, ty, rectW, rectH, 4.0F, ColorUtil.rgba(31, 31, 31, 205));
                font.drawString(guiStack, tag, rx + 4.0F, ty + 2.0F, 0xFFFFFFFF);

                guiStack.pop();
            }

            if (elements.get("Item")) {
                ItemStack item = player.getHeldItemMainhand();
                if (!item.isEmpty()) {
                    String itemName = item.getDisplayName().getString();
                    Font font = Fonts.mstr.get(8.0F);
                    float textW = font.getWidth(itemName);
                    float textH = font.getHeight();
                    float rectW = textW + 8.0F;
                    float rectH = textH + 4.0F;
                    float rx = minX + (width - rectW) / 2.0F;
                    float ty = maxY + 6.0F;

                    float centerX = rx + rectW / 2.0F;
                    float centerY = ty + rectH / 2.0F;
                    guiStack.push();
                    guiStack.translate(centerX, centerY, 0);
                    guiStack.scale(finalScale, finalScale, 1.0F);
                    guiStack.translate(-centerX, -centerY, 0);

                    RenderUtil2D.drawRoundRect(guiStack, rx, ty, rectW, rectH, 4.0F, ColorUtil.rgba(30, 30, 30, 220));
                    font.drawString(guiStack, itemName, rx + 4.0F, ty + 2.0F, 0xFFFFFFFF);

                    guiStack.pop();
                }
            }
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private float[] projectBox(AxisAlignedBB box, Matrix4f view, Matrix4f proj,
                               double camX, double camY, double camZ) {
        float minX = (float) (box.minX - camX);
        float minY = (float) (box.minY - camY);
        float minZ = (float) (box.minZ - camZ);
        float maxX = (float) (box.maxX - camX);
        float maxY = (float) (box.maxY - camY);
        float maxZ = (float) (box.maxZ - camZ);

        Vector3f[] corners = new Vector3f[]{
                new Vector3f(minX, minY, minZ),
                new Vector3f(minX, minY, maxZ),
                new Vector3f(minX, maxY, minZ),
                new Vector3f(minX, maxY, maxZ),
                new Vector3f(maxX, minY, minZ),
                new Vector3f(maxX, minY, maxZ),
                new Vector3f(maxX, maxY, minZ),
                new Vector3f(maxX, maxY, maxZ)
        };

        Matrix4f viewNoTrans = new Matrix4f(view);
        viewNoTrans.m03 = 0.0F;
        viewNoTrans.m13 = 0.0F;
        viewNoTrans.m23 = 0.0F;

        Matrix4f vp = new Matrix4f(proj);
        vp.mul(viewNoTrans);

        float sw = Minecraft.getInstance().getMainWindow().getScaledWidth();
        float sh = Minecraft.getInstance().getMainWindow().getScaledHeight();

        boolean anyOnScreen = false;
        float outMinX = Float.POSITIVE_INFINITY;
        float outMinY = Float.POSITIVE_INFINITY;
        float outMaxX = Float.NEGATIVE_INFINITY;
        float outMaxY = Float.NEGATIVE_INFINITY;

        for (Vector3f c : corners) {
            Vector4f v = new Vector4f(c.getX(), c.getY(), c.getZ(), 1.0F);
            v.transform(vp);

            if (v.w <= 0.0F) {
                continue;
            }

            float ndcX = v.x / v.w;
            float ndcY = v.y / v.w;

            float sx = (ndcX * 0.5F + 0.5F) * sw;
            float sy = (0.5F - ndcY * 0.5F) * sh;

            anyOnScreen = true;
            if (sx < outMinX) outMinX = sx;
            if (sy < outMinY) outMinY = sy;
            if (sx > outMaxX) outMaxX = sx;
            if (sy > outMaxY) outMaxY = sy;
        }

        if (!anyOnScreen) {
            return null;
        }

        if (outMinX > sw || outMaxX < 0 || outMinY > sh || outMaxY < 0) {
            return null;
        }

        return new float[]{outMinX, outMinY, outMaxX, outMaxY};
    }

    private void drawCornerBox(MatrixStack stack, float x, float y, float w, float h, int color) {
        float thickness = 1.2F;
        float len = Math.max(4F, Math.min(w, h) * 0.22F);

        RenderUtil2D.drawRect(stack, x - 0.5F, y - 0.5F, len, thickness, color);
        RenderUtil2D.drawRect(stack, x - 0.5F, y - 0.5F, thickness, len, color);

        RenderUtil2D.drawRect(stack, x + w - len + 0.5F, y - 0.5F, len, thickness, color);
        RenderUtil2D.drawRect(stack, x + w - thickness + 0.5F, y - 0.5F, thickness, len, color);

        RenderUtil2D.drawRect(stack, x - 0.5F, y + h - thickness + 0.5F, len, thickness, color);
        RenderUtil2D.drawRect(stack, x - 0.5F, y + h - len + 0.5F, thickness, len, color);

        RenderUtil2D.drawRect(stack, x + w - len + 0.5F, y + h - thickness + 0.5F, len, thickness, color);
        RenderUtil2D.drawRect(stack, x + w - thickness + 0.5F, y + h - len + 0.5F, thickness, len, color);
    }
}
