package fun.pasta.module.impl.visual;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.pasta.Pasta;
import fun.pasta.event.EventRender3D;
import fun.pasta.fonts.Font;
import fun.pasta.fonts.Fonts;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.impl.combat.AntiBot;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.module.settings.ModeOption;
import fun.pasta.module.settings.MultiBoxOption;
import fun.pasta.module.settings.SliderOption;
import fun.pasta.util.HealthUtil;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.render.RenderUtil2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.vector.Vector4f;

public class ESP extends Module {

    private static final int LEFT_ACCENT = ColorUtil.rgba(116, 220, 255, 255);
    private static final int RIGHT_ACCENT = ColorUtil.rgba(255, 86, 86, 255);
    private static final int FRIEND_ACCENT = ColorUtil.rgba(90, 220, 120, 255);
    private static final int OUTLINE = ColorUtil.rgba(15, 15, 15, 180);
    private static final int TAG_BG = ColorUtil.rgba(18, 18, 18, 165);

    public final SliderOption scaleSetting = new SliderOption("Scale", 1.0F, 0.5F, 2.0F, 0.1F);
    public final ModeOption renderType = new ModeOption("Render", "Hybrid", "Box", "Corners", "Hybrid");
    public final MultiBoxOption elements = new MultiBoxOption("Elements",
            new BooleanOption("Players", true),
            new BooleanOption("Items", true),
            new BooleanOption("Health Bar", true),
            new BooleanOption("Health Text", true),
            new BooleanOption("Name", true),
            new BooleanOption("Held Item", true));

    public ESP() {
        super("ESP", Category.VISUAL);
        add(scaleSetting, renderType, elements);
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

        try {
            MatrixStack guiStack = new MatrixStack();
            for (Entity entity : mc.world.getAllEntities()) {
                if (entity instanceof PlayerEntity) {
                    if (!elements.get("Players")) {
                        continue;
                    }
                    renderPlayer(event, guiStack, (PlayerEntity) entity, view, projection);
                    continue;
                }
                if (entity instanceof ItemEntity && elements.get("Items")) {
                    renderItem(event, guiStack, (ItemEntity) entity, view, projection);
                }
            }
        } finally {
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
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

    private void renderPlayer(EventRender3D event, MatrixStack stack, PlayerEntity player, Matrix4f view, Matrix4f projection) {
        if (!player.isAlive() || player.isInvisible()) {
            return;
        }
        if (AntiBot.isBot(player)) {
            return;
        }
        if (player == mc.player && mc.gameSettings.getPointOfView() == PointOfView.FIRST_PERSON) {
            return;
        }

        AxisAlignedBB bb = player.getBoundingBox().grow(0.08D);
        float[] screen = projectBox(bb, view, projection, event.camX, event.camY, event.camZ);
        if (screen == null) {
            return;
        }

        float x = screen[0];
        float y = screen[1];
        float x2 = screen[2];
        float y2 = screen[3];
        float w = x2 - x;
        float h = y2 - y;
        if (w <= 1.0F || h <= 1.0F) {
            return;
        }

        boolean isFriend = Pasta.instance != null
                && Pasta.instance.friendManager != null
                && Pasta.instance.friendManager.isFriend(player);
        int leftColor = isFriend ? FRIEND_ACCENT : LEFT_ACCENT;
        int rightColor = isFriend ? FRIEND_ACCENT : RIGHT_ACCENT;

        if ("Box".equalsIgnoreCase(renderType.get()) || "Hybrid".equalsIgnoreCase(renderType.get())) {
            drawFullBox(stack, x, y, x2, y2, leftColor, rightColor);
        }
        if ("Corners".equalsIgnoreCase(renderType.get()) || "Hybrid".equalsIgnoreCase(renderType.get())) {
            drawCornerBox(stack, x, y, w, h, leftColor, rightColor);
        }

        float hp = HealthUtil.getHealthForEsp(player) + Math.max(player.getAbsorptionAmount(), 0.0F);
        float maxHp = Math.max(player.getMaxHealth(), 20.0F) + Math.max(player.getAbsorptionAmount(), 0.0F);
        float ratio = maxHp <= 0.001F ? 0.0F : MathHelper.clamp(hp / maxHp, 0.0F, 1.0F);

        if (elements.get("Health Bar")) {
            float barX = x - 3.5F;
            RenderUtil2D.drawRect(stack, barX - 0.5F, y - 0.5F, 2.0F, h + 1.0F, OUTLINE);
            RenderUtil2D.drawRect(stack, barX, y, 1.0F, h, ColorUtil.rgba(18, 18, 18, 180));
            float fillY = y2 - h * ratio;
            RenderUtil2D.drawRectVerticalW(barX, fillY, 1.0F, y2 - fillY, rightColor, leftColor);
        }

        Font font = Fonts.mstr.get(8.0F);
        int hpInt = Math.max(0, MathHelper.ceil(hp));

        if (elements.get("Health Text")) {
            String hpText = hpInt + "HP";
            font.drawString(stack, hpText, x - font.getWidth(hpText) - 6.0F, y - 1.0F, 0xFFFFFFFF);
        }

        if (elements.get("Name")) {
            String name = player.getName().getString();
            String suffix = " - " + hpInt;
            float nameW = font.getWidth(name);
            float suffixW = font.getWidth(suffix);
            float totalW = nameW + suffixW + 8.0F;
            float bx = x + (w - totalW) / 2.0F;
            float by = y - 11.0F;

            RenderUtil2D.drawRoundRect(stack, bx, by, totalW, 9.0F, 2.0F, TAG_BG);
            font.drawString(stack, name, bx + 4.0F, by + 1.0F, 0xFFFFFFFF);
            font.drawString(stack, suffix, bx + 4.0F + nameW, by + 1.0F, rightColor);
        }

        if (elements.get("Held Item")) {
            ItemStack item = player.getHeldItemMainhand();
            if (!item.isEmpty()) {
                String itemName = item.getDisplayName().getString();
                float textW = font.getWidth(itemName);
                float tx = x + (w - textW - 6.0F) / 2.0F;
                float ty = y2 + 3.0F;
                RenderUtil2D.drawRoundRect(stack, tx, ty, textW + 6.0F, 9.0F, 2.0F, TAG_BG);
                font.drawString(stack, itemName, tx + 3.0F, ty + 1.0F, 0xFFFFFFFF);
            }
        }
    }

    private void renderItem(EventRender3D event, MatrixStack stack, ItemEntity itemEntity, Matrix4f view, Matrix4f projection) {
        if (itemEntity == null || itemEntity.getItem().isEmpty()) {
            return;
        }
        if (mc.player.getDistanceSq(itemEntity) > 80.0D * 80.0D) {
            return;
        }

        AxisAlignedBB bb = itemEntity.getBoundingBox().grow(0.08D);
        float[] screen = projectBox(bb, view, projection, event.camX, event.camY, event.camZ);
        if (screen == null) {
            return;
        }

        float x = screen[0];
        float y = screen[1];
        float x2 = screen[2];
        float w = x2 - x;

        String text = itemEntity.getItem().getDisplayName().getString() + " x" + itemEntity.getItem().getCount();
        Font font = Fonts.mstr.get(8.0F);
        float textW = font.getWidth(text);
        float bx = x + (w - textW - 6.0F) / 2.0F;
        float by = y - 11.0F;
        RenderUtil2D.drawRoundRect(stack, bx, by, textW + 6.0F, 9.0F, 2.0F, TAG_BG);
        font.drawString(stack, text, bx + 3.0F, by + 1.0F, 0xFFFFFFFF);
    }

    private void drawFullBox(MatrixStack stack, float x, float y, float x2, float y2, int leftColor, int rightColor) {
        float w = x2 - x;
        float h = y2 - y;
        RenderUtil2D.drawRect(stack, x - 0.5F, y - 0.5F, w + 1.0F, h + 1.0F, OUTLINE);
        RenderUtil2D.drawRectHorizontalW(x, y, w, 1.0F, leftColor, rightColor);
        RenderUtil2D.drawRectHorizontalW(x, y2 - 1.0F, w, 1.0F, leftColor, rightColor);
        RenderUtil2D.drawRectVerticalW(x, y, 1.0F, h, leftColor, leftColor);
        RenderUtil2D.drawRectVerticalW(x2 - 1.0F, y, 1.0F, h, rightColor, rightColor);
    }

    private void drawCornerBox(MatrixStack stack, float x, float y, float w, float h, int leftColor, int rightColor) {
        float thickness = 1.2F;
        float len = Math.max(4F, Math.min(w, h) * 0.22F);

        RenderUtil2D.drawRect(stack, x - 1.0F, y - 1.0F, len + 1.0F, thickness + 1.0F, OUTLINE);
        RenderUtil2D.drawRect(stack, x - 1.0F, y - 1.0F, thickness + 1.0F, len + 1.0F, OUTLINE);
        RenderUtil2D.drawRect(stack, x + w - len, y - 1.0F, len + 1.0F, thickness + 1.0F, OUTLINE);
        RenderUtil2D.drawRect(stack, x + w - thickness, y - 1.0F, thickness + 1.0F, len + 1.0F, OUTLINE);
        RenderUtil2D.drawRect(stack, x - 1.0F, y + h - thickness, len + 1.0F, thickness + 1.0F, OUTLINE);
        RenderUtil2D.drawRect(stack, x - 1.0F, y + h - len, thickness + 1.0F, len + 1.0F, OUTLINE);
        RenderUtil2D.drawRect(stack, x + w - len, y + h - thickness, len + 1.0F, thickness + 1.0F, OUTLINE);
        RenderUtil2D.drawRect(stack, x + w - thickness, y + h - len, thickness + 1.0F, len + 1.0F, OUTLINE);

        RenderUtil2D.drawRect(stack, x - 0.5F, y - 0.5F, len, thickness, leftColor);
        RenderUtil2D.drawRect(stack, x - 0.5F, y - 0.5F, thickness, len, leftColor);
        RenderUtil2D.drawRect(stack, x + w - len + 0.5F, y - 0.5F, len, thickness, rightColor);
        RenderUtil2D.drawRect(stack, x + w - thickness + 0.5F, y - 0.5F, thickness, len, rightColor);
        RenderUtil2D.drawRect(stack, x - 0.5F, y + h - thickness + 0.5F, len, thickness, leftColor);
        RenderUtil2D.drawRect(stack, x - 0.5F, y + h - len + 0.5F, thickness, len, leftColor);
        RenderUtil2D.drawRect(stack, x + w - len + 0.5F, y + h - thickness + 0.5F, len, thickness, rightColor);
        RenderUtil2D.drawRect(stack, x + w - thickness + 0.5F, y + h - len + 0.5F, thickness, len, rightColor);
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
