package fun.pasta.module.impl.visual;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.pasta.event.EventRender3D;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.SliderOption;
import fun.pasta.ui.clickGui.theme.ThemeManager;
import fun.pasta.util.color.ColorUtil;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Trails extends Module {

    private final SliderOption trailLength = new SliderOption("Length", 2.0F, 1.0F, 8.0F, 0.5F);
    private final List<Point> points = new ArrayList<>();

    public Trails() {
        super("Trails", Category.VISUAL);
        add(trailLength);
    }

    @Subscribe
    public void onRender3D(EventRender3D event) {
        if (!enable || mc.player == null || mc.world == null) {
            points.clear();
            return;
        }
        if (mc.gameSettings.getPointOfView() == PointOfView.FIRST_PERSON) {
            return;
        }

        long now = System.currentTimeMillis();
        long maxAge = (long) (trailLength.getValue().floatValue() * 1000.0F);
        for (Iterator<Point> it = points.iterator(); it.hasNext(); ) {
            if (now - it.next().time > maxAge) {
                it.remove();
            }
        }

        double x = MathHelper.lerp(event.partialTicks, mc.player.lastTickPosX, mc.player.getPosX());
        double y = MathHelper.lerp(event.partialTicks, mc.player.lastTickPosY, mc.player.getPosY()) + 0.02D;
        double z = MathHelper.lerp(event.partialTicks, mc.player.lastTickPosZ, mc.player.getPosZ());
        points.add(new Point(new Vector3d(x, y, z), now));

        if (points.size() < 2) {
            return;
        }

        int color = ThemeManager.INSTANCE != null ? ThemeManager.INSTANCE.getThemeColor() : ColorUtil.defaultRed;
        int r = ColorUtil.getRed(color);
        int g = ColorUtil.getGreen(color);
        int b = ColorUtil.getBlue(color);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        GL11.glLineWidth(2.0F);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i);
            float alpha = (float) i / (float) points.size();
            int a = MathHelper.clamp((int) (alpha * 255.0F), 15, 255);
            buffer.pos(point.pos.x - event.camX, point.pos.y - event.camY, point.pos.z - event.camZ)
                    .color(r, g, b, a)
                    .endVertex();
        }

        buffer.finishDrawing();
        WorldVertexBufferUploader.draw(buffer);

        GL11.glLineWidth(1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public void onDisable() {
        points.clear();
        super.onDisable();
    }

    private static class Point {
        private final Vector3d pos;
        private final long time;

        private Point(Vector3d pos, long time) {
            this.pos = pos;
            this.time = time;
        }
    }
}

