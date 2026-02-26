package fun.pasta.altManager;

import com.mojang.blaze3d.matrix.MatrixStack;
import fun.pasta.Pasta;
import fun.pasta.fonts.Fonts;
import fun.pasta.util.IMinecraft;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.math.MathUtil;
import fun.pasta.util.render.RenderUtil2D;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.RandomStringUtils;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class AltManagerScreen extends Screen implements IMinecraft {

    public AltManagerScreen() {
        super(ITextComponent.getTextComponentOrEmpty(""));
    }

    public CopyOnWriteArrayList<Account> accounts = new CopyOnWriteArrayList<>();

    @Override
    protected void init() {
        super.init();
        accounts.clear();
        accounts.addAll(Pasta.instance.accountsConfig.loadAccounts());
    }

    float w = 220;
    float h = 200;
    float x = sr.getScaledWidth() / 2f - w / 2f;
    float y = sr.getScaledHeight() / 2f - h / 2f;

    boolean typing = false;
    String nameAdded = "";

    MatrixStack matrixStack;
    int mouseX, mouseY;

    float scroll, scrollOut;

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        setSetting(matrixStack, mouseX, mouseY);

        drawBackGround();
        drawDecoration();

        drawAccountList();

        drawAddedAccount();

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    private void drawBackGround() {
        /* background */
        Pasta.instance.drawBackGround();

        /* background alpha */
        RenderUtil2D.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), ColorUtil.rgba(15, 10, 10, 120));

        /* render logo */
        Fonts.pastaIcon.get(80).drawCenterX("A", sr.getScaledWidth() / 2f, sr.getScaledHeight() / 2f - 110, ColorUtil.rgba(255, 54, 64, 255));
        Fonts.mstr.get(25).drawCenterXY("AltManager", sr.getScaledWidth() / 2f, sr.getScaledHeight() / 2f - 60, ColorUtil.rgba(255, 54, 64, 255));
    }

    private void drawDecoration() {
        /* left */
        RenderUtil2D.drawCircle(-120, sr.getScaledHeight() / 2f, 400, ColorUtil.rgba(255, 54, 64));

        /* right */
        RenderUtil2D.drawCircle(sr.getScaledWidth() + 120, sr.getScaledHeight() / 2f, 400, ColorUtil.rgba(255, 54, 64));

        /* up */
        RenderUtil2D.drawRectHorizontalW(0, 0, sr.getScaledWidth(), 100, ColorUtil.rgba(255, 54, 64, 0), ColorUtil.rgba(255, 54, 64, 120));
    }

    private void drawAddedAccount() {
        /* render with typing name */
        RenderUtil2D.drawOutlineRoundRect(x + 10, y + 55, 130, 22, 4, ColorUtil.rgba(26, 18, 19), ColorUtil.rgba(51, 30, 32));

        int colorText = typing ? -1 : ColorUtil.rgba(144, 144, 144, 255);

        Fonts.mstr.get(14).drawCenterY(nameAdded.isEmpty() && !typing ? "Введите никнейм" : typing ? nameAdded + (System.currentTimeMillis() % 1000 < 500 ? "_" : "") : nameAdded,
                x + 10 + 3, y + 55 + (22 / 2f), colorText);

        /* render with button "add" */
        boolean hoverAdd = MathUtil.isHover(mouseX, mouseY, x + 140 + 2, y + 55, 50, 22);
        int colorHoverAdd = ColorUtil.interpolateColor(ColorUtil.rgba(255, 54, 65), ColorUtil.rgba(26, 18, 19), hoverAdd ? 1.0f : 0.0f);
        RenderUtil2D.drawOutlineRoundRect(x + 140 + 2, y + 55, 50, 22, 4, colorHoverAdd, ColorUtil.rgba(51, 30, 32));

        Fonts.mstr.get(12).drawCenterXY(nameAdded.isEmpty() ? "Генерация" : "Добавить", x + 140 + 2 + (50 / 2f), y + 55 + (22 / 2f), -1);
    }

    private void drawAccountList() {

        float yy = 0;

        float w = 220;
        float h = 200;
        float x = sr.getScaledWidth() / 2f - (w / 2f) + 10;
        float y = sr.getScaledHeight() / 2f - (h / 2f) + 10;

        for (Account f : accounts) {

            f.draw(matrixStack, mouseX, mouseY, x, y + scrollOut + yy + 70, 182, 22);

            yy += 22 + 3;
        }

        float max = yy;
        float size = 130;

        if (max < size) {
            scroll = 0;
        } else {
            scroll = MathHelper.clamp(scroll, -(max - size), 0);
        }

        scrollOut = MathUtil.lerp(scrollOut, scroll, 10);
    }

    String[] nameRandom = {"Pasta"};

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        /* click on account list */
        {
            float yy = 0;

            float w = 220;
            float h = 200;
            float x = sr.getScaledWidth() / 2f - (w / 2f) + 10;
            float y = sr.getScaledHeight() / 2f - (h / 2f) + 10;

            for (Account f : accounts) {

                f.click((int) mouseX, (int) mouseY, button, x, y + yy + 70, 182, 22);

                yy += 22 + 3;
            }
        }

        /* click typing set (false-true) and random or added */
        if (MathUtil.isHover((int) mouseX, (int) mouseY, x + 10, y + 55, 130, 22) && button == 0) {
            typing = !typing;
        } else if (MathUtil.isHover((int) mouseX, (int) mouseY, x + 140 + 2, y + 55, 50, 22) && button == 0) {
            if (nameAdded.isEmpty()) {
                String randomName = nameRandom[new Random().nextInt(nameRandom.length)] + "_" + RandomStringUtils.randomNumeric(4);
                accounts.add(new Account(randomName));
                Pasta.instance.accountsConfig.saveAccounts(accounts);

                typing = false;
                nameAdded = "";
            } else if (nameAdded.length() > 2 && nameAdded.length() < 18) {
                accounts.add(new Account(nameAdded));
                Pasta.instance.accountsConfig.saveAccounts(accounts);


                typing = false;
                nameAdded = "";
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

        if (typing) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !nameAdded.isEmpty()) {
                nameAdded = nameAdded.substring(0, nameAdded.length() - 1);
            } else if (keyCode == GLFW.GLFW_KEY_ENTER && !nameAdded.isEmpty()) {
                accounts.add(new Account(nameAdded));
                Pasta.instance.accountsConfig.saveAccounts(accounts);
                nameAdded = "";
                typing = false;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {

        if (typing) {
            nameAdded += codePoint;
        }

        return super.charTyped(codePoint, modifiers);
    }

    private void setSetting(MatrixStack matrixStack, int mouseX, int mouseY) {
        this.matrixStack = matrixStack;
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        w = 220;
        h = 200;
        x = sr.getScaledWidth() / 2f - w / 2f;
        y = sr.getScaledHeight() / 2f - h / 2f;
    }
}
