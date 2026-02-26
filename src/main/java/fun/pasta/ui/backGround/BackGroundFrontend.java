package fun.pasta.ui.backGround;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.optifine.SmartAnimations;
import fun.pasta.Pasta;
import fun.pasta.fonts.Fonts;
import fun.pasta.util.IMinecraft;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.math.MathUtil;
import fun.pasta.util.render.RenderUtil2D;
import fun.pasta.util.render.SmartScissor;

import java.util.ArrayList;
import java.util.List;


public class BackGroundFrontend implements IMinecraft {

    public BackGroundFrontend() {
        for (BackGround f : Pasta.instance.backGroundManager.backGrounds) {
            backGroundBackEnds.add(new BackGroundBackEnd(f.name, f.resourceLocation));
        }
    }

    List<BackGroundBackEnd> backGroundBackEnds = new ArrayList<>();

    MatrixStack matrixStack;
    int mouseX, mouseY;

    public float x;

    float scroll, scrllOut;

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        setSetting(matrixStack, mouseX, mouseY);

        drawBackGround();

        drawAllBackGround();
    }

    private void drawAllBackGround() {
        float width = 420;
        float height = 120;
        float x = this.x - width / 2;
        float y = sr.getScaledHeight() / 2f - height / 2 - 10;

        RenderUtil2D.drawOutlineRoundRect(x, y, width, height + 6, 6, ColorUtil.rgba(24, 24, 24, 255), ColorUtil.rgba(36, 36, 36, 255));

        float xx = 0;

        SmartScissor.push();
        SmartScissor.set(x, y, width, height);

        for (BackGroundBackEnd f : backGroundBackEnds) {
            float del = 12;
            f.setPos(x + xx + 5 + scrllOut, y + 5, 1920 / del, height - 10);

            f.draw();

            xx += 1920 / del + 5;
        }

        SmartScissor.unSet();
        SmartScissor.pop();

        float max = xx;
        float size = width - 5;

        if (max < size) {
            scroll = 0;
        } else {
            scroll = MathHelper.clamp(scroll, -(max - size), 0);
        }

        scrllOut = MathUtil.lerp(scrllOut, scroll, 10);

        float del = backGroundBackEnds.size() / 4.0f;
        RenderUtil2D.drawRoundRect(x + 5, y + height - 1, width - 10, 3, 1, ColorUtil.rgba(44, 44, 44, 255));
        RenderUtil2D.drawRoundRect(x + 5 - scrllOut / del, y + height - 1, width - 10 + (-(max - size) / del), 3, 1, ColorUtil.rgba(144, 144, 144, 255));
    }

    private void drawBackGround() {
        Fonts.mstr.get(25).drawCenterX("Выбор фона", this.x, sr.getScaledHeight() / 2f - 90, -1);
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        clickBack(button);
    }

    public void mouseScroll(float delta) {
        scroll += (delta * 50);
    }

    private void clickBack(int button) {
        float width = 420;
        float height = 120;
        float x = this.x - width / 2;
        float y = sr.getScaledHeight() / 2f - height / 2 - 10;

        if (MathUtil.isHover(mouseX, mouseY, x, y, width, height)) {
            for (BackGroundBackEnd f : backGroundBackEnds) {
                f.click(mouseX, mouseY, button);
            }
        }
    }

    private void setSetting(MatrixStack matrixStack, int mouseX, int mouseY) {
        this.matrixStack = matrixStack;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }
}