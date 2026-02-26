package fun.pasta.module.impl.visual;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.pasta.Pasta;
import fun.pasta.event.EventRender3D;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.module.settings.SliderOption;
import fun.pasta.ui.clickGui.theme.ThemeManager;
import fun.pasta.util.color.ColorUtil;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import org.lwjgl.opengl.GL11;

public class ChinaHat extends Module {

    private static final int SEGMENTS = 180;

    private final BooleanOption self = new BooleanOption("Self", true);
    private final BooleanOption friends = new BooleanOption("Friends", true);
    private final BooleanOption throughWalls = new BooleanOption("Through Walls", false);
    private final SliderOption radius = new SliderOption("Radius", 1.10F, 0.60F, 2.00F, 0.05F);
    private final SliderOption height = new SliderOption("Height", 0.35F, 0.15F, 0.80F, 0.01F);
    private final SliderOption lineWidth = new SliderOption("Line Width", 2.5F, 1.0F, 5.0F, 0.1F);

    public ChinaHat() {
        super("ChinaHat", Category.VISUAL);
        add(self, friends, throughWalls, radius, height, lineWidth);
    }

    @Subscribe
    public void onRender3D(EventRender3D event) {
        if (!enable || mc.player == null || mc.world == null || event == null || event.matrixStack == null) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.enableAlphaTest();
        RenderSystem.shadeModel(GL11.GL_SMOOTH);

        if (throughWalls.get()) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
        }

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(lineWidth.getValue().floatValue());

        try {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (!shouldRenderPlayer(player)) {
                    continue;
                }
                renderHat(event, player);
            }
        } finally {
            GL11.glLineWidth(1.0F);
            GL11.glDisable(GL11.GL_LINE_SMOOTH);

            if (throughWalls.get()) {
                RenderSystem.depthMask(true);
                RenderSystem.enableDepthTest();
            }

            RenderSystem.shadeModel(GL11.GL_FLAT);
            RenderSystem.enableTexture();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.disableAlphaTest();
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private boolean shouldRenderPlayer(PlayerEntity player) {
        if (player == null || !player.isAlive() || player.isInvisible()) {
            return false;
        }

        if (player == mc.player) {
            return self.get() && mc.gameSettings.getPointOfView() != PointOfView.FIRST_PERSON;
        }

        return friends.get()
                && Pasta.instance != null
                && Pasta.instance.friendManager != null
                && Pasta.instance.friendManager.isFriend(player);
    }

    private void renderHat(EventRender3D event, PlayerEntity player) {
        double x = MathHelper.lerp(event.partialTicks, player.lastTickPosX, player.getPosX());
        double y = MathHelper.lerp(event.partialTicks, player.lastTickPosY, player.getPosY());
        double z = MathHelper.lerp(event.partialTicks, player.lastTickPosZ, player.getPosZ());

        float armorOffset = player.getItemStackFromSlot(EquipmentSlotType.HEAD).isEmpty() ? 0.03F : -0.01F;
        y += player.getHeight() + (player.isSneaking() ? 0.02F : 0.06F) + armorOffset;

        float hatRadius = player.getWidth() * radius.getValue().floatValue();
        float hatHeight = height.getValue().floatValue();

        int themeColor = ThemeManager.INSTANCE != null
                ? ThemeManager.INSTANCE.getThemeColor()
                : ColorUtil.defaultRed;
        int baseR = ColorUtil.getRed(themeColor);
        int baseG = ColorUtil.getGreen(themeColor);
        int baseB = ColorUtil.getBlue(themeColor);

        float animationAngle = ((System.currentTimeMillis() % 4000L) / 4000.0F) * ((float) Math.PI * 8.0F);

        MatrixStack stack = event.matrixStack;
        stack.push();
        try {
            stack.translate(x - event.camX, y - event.camY, z - event.camZ);
            Matrix4f model = stack.getLast().getMatrix();

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();

            buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(model, 0.0F, hatHeight, 0.0F).color(baseR, baseG, baseB, 200).endVertex();
            for (int i = 0; i <= SEGMENTS; i++) {
                float angle = (float) i / (float) SEGMENTS * (float) (Math.PI * 2.0D);
                float animated = angle + animationAngle;
                float fade = (float) (Math.sin(animated) * 0.5D + 0.5D);
                fade = 0.5F + 0.5F * fade;

                int r = MathHelper.clamp((int) (baseR * fade * 0.40F), 0, 255);
                int g = MathHelper.clamp((int) (baseG * fade * 0.40F), 0, 255);
                int b = MathHelper.clamp((int) (baseB * fade * 0.40F), 0, 255);
                int a = MathHelper.clamp((int) (fade * 220.0F), 0, 255);

                float vx = -MathHelper.sin(angle) * hatRadius;
                float vz = MathHelper.cos(angle) * hatRadius;
                buffer.pos(model, vx, 0.0F, vz).color(r, g, b, a).endVertex();
            }
            buffer.finishDrawing();
            WorldVertexBufferUploader.draw(buffer);

            buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i <= SEGMENTS; i++) {
                float angle = (float) i / (float) SEGMENTS * (float) (Math.PI * 2.0D);
                float animated = angle + animationAngle;
                float fade = (float) (Math.sin(animated) * 0.5D + 0.5D);
                fade = 0.5F + 0.5F * fade;

                int r = MathHelper.clamp((int) (baseR * fade * 0.60F), 0, 255);
                int g = MathHelper.clamp((int) (baseG * fade * 0.60F), 0, 255);
                int b = MathHelper.clamp((int) (baseB * fade * 0.60F), 0, 255);
                int a = MathHelper.clamp((int) (fade * 230.0F), 0, 255);

                float vx = -MathHelper.sin(angle) * hatRadius;
                float vz = MathHelper.cos(angle) * hatRadius;
                buffer.pos(model, vx, 0.0F, vz).color(r, g, b, a).endVertex();
            }
            buffer.finishDrawing();
            WorldVertexBufferUploader.draw(buffer);
        } finally {
            stack.pop();
        }
    }
}
