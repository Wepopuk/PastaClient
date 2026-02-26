package fun.pasta.ui.backGround;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.util.text.Color;
import fun.pasta.Pasta;
import fun.pasta.fonts.Fonts;
import fun.pasta.util.IMinecraft;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.math.MathUtil;
import fun.pasta.util.render.RenderUtil2D;
import fun.pasta.util.render.SmartScissor;
import fun.pasta.util.shader.ShaderUtil;

public class BackGroundBackEnd implements IMinecraft {

    public String name;
    public String resourceLocation;

    MatrixStack matrixStack;
    int mouseX, mouseY;

    float x, y, width, height;

    public BackGroundBackEnd(String name, String resourceLocation) {
        this.name = name;
        this.resourceLocation = resourceLocation;
    }

    public void draw() {
        drawBack();
    }

    private void drawBack() {
        setSetting(matrixStack, mouseX, mouseY);

        boolean currentBackground = Pasta.instance.backGroundManager.currentBackResource.equals(resourceLocation);
        int colorCurrentOutline = ColorUtil.interpolateColor(ColorUtil.rgba(255, 53, 61), ColorUtil.rgba(36, 36, 36, 255), currentBackground ? 1.0f : 0.0f);
        RenderUtil2D.drawOutlineRoundRect(x, y, width, height, 5, ColorUtil.rgba(24, 24, 24, 255), colorCurrentOutline);

        float del = 12;
        float w = 1920 / del;
        float h = 1080 / del;

        SmartScissor.push();
        SmartScissor.set(x + 0.5f, y + 0.5f, width - 1, height);

        Vector4f vector4f = new Vector4f(1.5f, 0, 1.5f, 0);
        ShaderUtil.roundedImage.attach();
        ShaderUtil.roundedImage.setUniform("size", w / 2f, h / 2f);
        ShaderUtil.roundedImage.setUniform("round", vector4f.x * 4.5f, vector4f.y * 4.5f, vector4f.z * 4.5f, vector4f.w * 4.5f);
        ShaderUtil.roundedImage.setUniform("smoothness", 0.f, 2.0f);
        ShaderUtil.roundedImage.setUniform("alpha", 1f);
        RenderUtil2D.drawImage(new ResourceLocation(resourceLocation), x, y, w, h, -1);
        ShaderUtil.roundedImage.detach();

        SmartScissor.unSet();
        SmartScissor.pop();

        int colorCurrent = ColorUtil.interpolateColor(ColorUtil.rgba(255, 72, 72, 255), ColorUtil.rgba(94, 94, 94, 255), currentBackground ? 1.0f : 0.0f);

        RenderUtil2D.drawCircle(x + 10, y + height - 10, 10, colorCurrent);
        RenderUtil2D.drawCircle(x + 10, y + height - 10, 8, ColorUtil.rgba(24, 24, 24, 255));

        Fonts.mstr.get(14).drawCenterY(name, x + 5 + 15, y + height - 10, -1);
    }

    public void click(int mouseX, int mouseY, int button) {
        if (MathUtil.isHover(mouseX, mouseY, x, y, width, height) && button == 0) {
            Pasta.instance.backGroundManager.currentBackResource = resourceLocation;
        }
    }

    public void setSetting(MatrixStack matrixStack, int mouseX, int mouseY) {
        this.matrixStack = matrixStack;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    public void setPos(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}