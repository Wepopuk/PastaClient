package fun.pasta.util.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.matrix.MatrixStack;
import fun.pasta.util.IMinecraft;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.math.Vector4i;
import fun.pasta.util.shader.ShaderUtil;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.jhlabs.image.GaussianFilter;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector4f;
import net.optifine.util.TextureUtils;
import org.lwjgl.opengl.GL11;
import fun.pasta.util.IMinecraft;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Objects;

import static net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR_TEX;
import static net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX_COLOR;

public class RenderUtil2D implements IMinecraft {

    public static void drawRect(float x, float y, float w, float h, int color) {

        w = x + w;
        h = y + h;

        float a = (float) (color >> 24 & 255) / 255;
        float r = (float) (color >> 16 & 255) / 255;
        float g = (float) (color >> 8 & 255) / 255;
        float b = (float) (color & 255) / 255;

        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();

        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();

        bufferBuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);

        bufferBuilder.pos(x, h, 0.0f).color(r, g, b, a).endVertex();
        bufferBuilder.pos(w, h, 0.0f).color(r, g, b, a).endVertex();
        bufferBuilder.pos(w, y, 0.0f).color(r, g, b, a).endVertex();
        bufferBuilder.pos(x, y, 0.0f).color(r, g, b, a).endVertex();

        bufferBuilder.finishDrawing();
        WorldVertexBufferUploader.draw(bufferBuilder);


        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
    }

    public static void drawQuads(float x, float y, float w, float h, int glsl) {

        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();

        bufferBuilder.begin(glsl, DefaultVertexFormats.POSITION_TEX);

        bufferBuilder.pos(x, y, 0.0f).tex(0, 0).endVertex();
        bufferBuilder.pos(x, y + h, 0.0f).tex(0, 1).endVertex();
        bufferBuilder.pos(x + w, y + h, 0.0f).tex(1, 1).endVertex();
        bufferBuilder.pos(x + w, y, 0.0f).tex(1, 0).endVertex();

        bufferBuilder.finishDrawing();

        WorldVertexBufferUploader.draw(bufferBuilder);

    }

    public static void quads(float x, float y, float width, float height, int glQuads, int color) {
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        Tessellator tessellator = Tessellator.getInstance();

        buffer.begin(glQuads, POSITION_TEX_COLOR);
        {
            buffer.pos(x, y, 0).tex(0, 0).color(color).endVertex();
            buffer.pos(x, y + height, 0).tex(0, 1).color(color).endVertex();
            buffer.pos(x + width, y + height, 0).tex(1, 1).color(color).endVertex();
            buffer.pos(x + width, y, 0).tex(1, 0).color(color).endVertex();
        }
        tessellator.draw();
    }

    public static void quads(float x, float y, float width, float height, int glQuads, int color, int color2, int color3, int color4) {
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        Tessellator tessellator = Tessellator.getInstance();

        buffer.begin(glQuads, POSITION_TEX_COLOR);
        {
            buffer.pos(x, y, 0).tex(0, 0).color(color).endVertex();
            buffer.pos(x, y + height, 0).tex(0, 1).color(color2).endVertex();
            buffer.pos(x + width, y + height, 0).tex(1, 1).color(color3).endVertex();
            buffer.pos(x + width, y, 0).tex(1, 0).color(color4).endVertex();
        }
        tessellator.draw();
    }

    public static void drawOutlineRoundRect(float x, float y, float w, float h, float round, int color, int color2) {
        drawRoundRect(x - 0.5f, y - 0.5f, w + 1, h + 1, round, color2);
        drawRoundRect(x, y, w, h, round, color);
    }

    public static void drawImage(ResourceLocation resourceLocation, float x, float y, float width, float height, int color) {
        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.shadeModel(7425);
        mc.getTextureManager().bindTexture(resourceLocation);
        quads(x, y, width, height, 7, color);
        RenderSystem.shadeModel(7424);
        RenderSystem.color4f(1, 1, 1, 1);
        RenderSystem.popMatrix();
    }

    public static void drawImage(ResourceLocation resourceLocation, float x, float y, float width, float height,
                                 int color, int color2, int color3, int color4) {
        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.shadeModel(7425);
        mc.getTextureManager().bindTexture(resourceLocation);
        quads(x, y, width, height, 7, color, color2, color3, color4);
        RenderSystem.shadeModel(7424);
        RenderSystem.color4f(1, 1, 1, 1);
        RenderSystem.popMatrix();
    }

    public static void drawCircle(float x, float y, float radius, int color) {
        drawRoundRect(x - radius / 2f, y - radius / 2f, radius, radius,
                radius / 2f, color);
    }

    public static void drawRoundRect(float x, float y, float w, float h, float round, int color) {

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();

        ShaderUtil.smooth.attach();

        float scaleFactor = (float) sr.getGuiScaleFactor();

        ShaderUtil.smooth.setUniform("location", x * scaleFactor, h * scaleFactor);

        ShaderUtil.smooth.setUniform("rectSize", w * scaleFactor, h * scaleFactor);

        ShaderUtil.smooth.setUniform("radius", round * scaleFactor);

        ShaderUtil.smooth.setUniform("blur", 0);

        ShaderUtil.smooth.setUniform("color", ColorUtil.rgba(color));

        drawQuads(x, y, w, h, 7);

        ShaderUtil.smooth.detach();

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

    }

    public static void drawRect(MatrixStack matrixStack, float x, float y, float w, float h, int color) {
        float x2 = x + w;
        float y2 = y + h;

        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        Matrix4f matrix = matrixStack.getLast().getMatrix();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();

        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(matrix, x, y2, 0.0F).color(r, g, b, a).endVertex();
        buffer.pos(matrix, x2, y2, 0.0F).color(r, g, b, a).endVertex();
        buffer.pos(matrix, x2, y, 0.0F).color(r, g, b, a).endVertex();
        buffer.pos(matrix, x, y, 0.0F).color(r, g, b, a).endVertex();
        buffer.finishDrawing();
        WorldVertexBufferUploader.draw(buffer);

        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    public static void drawRoundRect(MatrixStack matrixStack, float x, float y, float w, float h, float radius, int color) {
        float r = Math.min(radius, Math.min(w / 2.0F, h / 2.0F));

        if (r <= 0.0F) {
            drawRect(matrixStack, x, y, w, h, color);
            return;
        }

        drawRect(matrixStack, x + r, y, w - 2.0F * r, h, color);
        drawRect(matrixStack, x, y + r, w, h - 2.0F * r, color);

        drawCorner(matrixStack, x + r, y + r, r, 180, 270, color);
        drawCorner(matrixStack, x + w - r, y + r, r, 270, 360, color);
        drawCorner(matrixStack, x + w - r, y + h - r, r, 0, 90, color);
        drawCorner(matrixStack, x + r, y + h - r, r, 90, 180, color);
    }

    private static void drawCorner(MatrixStack matrixStack, float cx, float cy, float radius, int startAngle, int endAngle, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        Matrix4f matrix = matrixStack.getLast().getMatrix();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();

        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();

        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(matrix, cx, cy, 0.0F).color(r, g, b, a).endVertex();

        int segments = 20;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) Math.toRadians(startAngle + (endAngle - startAngle) * (i / (float) segments));
            float x = cx + MathHelper.cos(angle) * radius;
            float y = cy + MathHelper.sin(angle) * radius;
            buffer.pos(matrix, x, y, 0.0F).color(r, g, b, a).endVertex();
        }

        buffer.finishDrawing();
        WorldVertexBufferUploader.draw(buffer);

        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    public static void drawRoundedRect(float x,
                                       float y,
                                       float width,
                                       float height,
                                       Vector4f vector4f,
                                       Vector4i color) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        ShaderUtil.rounded.attach();

        ShaderUtil.rounded.setUniform("size", width * 2, height * 2);
        ShaderUtil.rounded.setUniform("round", vector4f.x * 2, vector4f.y * 2, vector4f.z * 2, vector4f.w * 2);

        ShaderUtil.rounded.setUniform("smoothness", 0.f, 1.5f);
        ShaderUtil.rounded.setUniform("color1",
                ColorUtil.rgba(color.x));
        ShaderUtil.rounded.setUniform("color2",
                ColorUtil.rgba(color.y));
        ShaderUtil.rounded.setUniform("color3",
                ColorUtil.rgba(color.z));
        ShaderUtil.rounded.setUniform("color4",
                ColorUtil.rgba(color.w));
        drawQuads(x, y, width, height, 7);

        ShaderUtil.rounded.detach();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public static void drawRoundedHead(ResourceLocation skin, float x, float y, float w, float h, float radius, float alpha, float hurt) {
        ShaderUtil shaderUtil = ShaderUtil.roundedHead;

        mc.getTextureManager().bindTexture(skin);

        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();

        shaderUtil.attach();

        shaderUtil.setUniform("size", w, h);
        shaderUtil.setUniform("radius", radius);
        shaderUtil.setUniform("hurt_time", hurt);
        shaderUtil.setUniform("alpha", alpha);

        shaderUtil.setUniform("startX", 4f);
        shaderUtil.setUniform("startY", 4f);

        shaderUtil.setUniform("endX", 8f);
        shaderUtil.setUniform("endY", 8f);

        shaderUtil.setUniform("texXSize", 32f);
        shaderUtil.setUniform("texYSize", 32f);

        drawQuads(x, y, w, h, 7);

        shaderUtil.detach();

        RenderSystem.disableBlend();
        RenderSystem.popMatrix();
    }

    public static void drawRectVerticalW(
            double x,
            double y,
            double w,
            double h,
            int color,
            int color1) {

        w = x + w;
        h = y + h;

        if (x < w) {
            double i = x;
            x = w;
            w = i;
        }

        if (y < h) {
            double j = y;
            y = h;
            h = j;
        }

        float[] colorOne = ColorUtil.rgba(color);
        float[] colorTwo = ColorUtil.rgba(color1);
        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.shadeModel(7425);
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(x, h, 0.0F).color(colorOne[0], colorOne[1], colorOne[2], colorOne[3]).endVertex();
        bufferbuilder.pos(w, h, 0.0F).color(colorTwo[0], colorTwo[1], colorTwo[2], colorTwo[3]).endVertex();
        bufferbuilder.pos(w, y, 0.0F).color(colorTwo[0], colorTwo[1], colorTwo[2], colorTwo[3]).endVertex();
        bufferbuilder.pos(x, y, 0.0F).color(colorOne[0], colorOne[1], colorOne[2], colorOne[3]).endVertex();
        bufferbuilder.finishDrawing();
        WorldVertexBufferUploader.draw(bufferbuilder);
        RenderSystem.enableTexture();
        RenderSystem.shadeModel(7424);
        RenderSystem.disableBlend();
    }

    public static void drawRectHorizontalW(
            double x,
            double y,
            double w,
            double h,
            int color,
            int color1) {

        w = x + w;
        h = y + h;

        if (x < w) {
            double i = x;
            x = w;
            w = i;
        }

        if (y < h) {
            double j = y;
            y = h;
            h = j;
        }

        float[] colorOne = ColorUtil.rgba(color);
        float[] colorTwo = ColorUtil.rgba(color1);
        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.shadeModel(7425);
        RenderSystem.defaultBlendFunc();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(x, h, 0.0F).color(colorTwo[0], colorTwo[1], colorTwo[2], colorTwo[3]).endVertex();
        bufferbuilder.pos(w, h, 0.0F).color(colorTwo[0], colorTwo[1], colorTwo[2], colorTwo[3]).endVertex();
        bufferbuilder.pos(w, y, 0.0F).color(colorOne[0], colorOne[1], colorOne[2], colorOne[3]).endVertex();
        bufferbuilder.pos(x, y, 0.0F).color(colorOne[0], colorOne[1], colorOne[2], colorOne[3]).endVertex();
        bufferbuilder.finishDrawing();
        WorldVertexBufferUploader.draw(bufferbuilder);
        RenderSystem.shadeModel(7424);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    private static final HashMap<Integer, Integer> shadowCache = new HashMap();

    public static void drawShadow(float x, float y, float width, float height, int radius, int color) {
        if (width <= 1 || height <= 1) {
            return;
        }

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.01f);
        GlStateManager.disableAlphaTest();

        x -= radius;
        y -= radius;
        width = width + radius * 2;
        height = height + radius * 2;
        x -= 0.25f;
        y += 0.25f;

        int identifier = Objects.hash(width, height, radius);
        int textureId;

        if (shadowCache.containsKey(identifier)) {
            textureId = shadowCache.get(identifier);
            GlStateManager.bindTexture(textureId);
        } else {
            if (width <= 0) {
                width = 1;
            }

            if (height <= 0) {
                height = 1;
            }

            BufferedImage originalImage = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D graphics = originalImage.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(radius, radius, (int) (width - radius * 2), (int) (height - radius * 2));
            graphics.dispose();

            GaussianFilter filter = new GaussianFilter(radius);
            BufferedImage blurredImage = filter.filter(originalImage, null);
            DynamicTexture texture = new DynamicTexture(TextureUtils.toNativeImage(blurredImage));
            texture.setBlurMipmap(true, true);
            try {
                textureId = texture.getGlTextureId();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            shadowCache.put(identifier, textureId);
        }

        float[] startColorComponents = ColorUtil.rgba(color);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();

        buffer.begin(GL11.GL_QUADS, POSITION_COLOR_TEX);
        buffer.pos(x, y, 0.0f)
                .color(startColorComponents[0], startColorComponents[1], startColorComponents[2],
                        startColorComponents[3])
                .tex(0.0f, 0.0f)
                .endVertex();

        buffer.pos(x, y + (float) ((int) height), 0.0f)
                .color(startColorComponents[0], startColorComponents[1], startColorComponents[2],
                        startColorComponents[3])
                .tex(0.0f, 1.0f)
                .endVertex();

        buffer.pos(x + (float) ((int) width), y + (float) ((int) height), 0.0f)
                .color(startColorComponents[0], startColorComponents[1], startColorComponents[2],
                        startColorComponents[3])
                .tex(1.0f, 1.0f)
                .endVertex();

        buffer.pos(x + (float) ((int) width), y, 0.0f)
                .color(startColorComponents[0], startColorComponents[1], startColorComponents[2],
                        startColorComponents[3])
                .tex(1.0f, 0.0f)
                .endVertex();

        Tessellator tessellator = Tessellator.getInstance();

        tessellator.draw();
        GlStateManager.enableAlphaTest();
        GlStateManager.bindTexture(0);
        GlStateManager.disableBlend();
    }

    public static void drawShadowCircle(float x, float y, float radius, int color) {
        drawShadow(x - radius / 2f, y - radius / 2f, radius, radius,
                (int) radius, color);
    }
}
