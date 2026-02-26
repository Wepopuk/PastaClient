package fun.pasta.ui.mainMenu;

import com.mojang.blaze3d.matrix.MatrixStack;
import fun.pasta.Pasta;
import fun.pasta.fonts.Fonts;
import fun.pasta.ui.backGround.BackGroundFrontend;
import fun.pasta.util.IMinecraft;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.math.MathUtil;
import net.minecraft.client.gui.screen.*;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.util.text.ITextComponent;
import fun.pasta.util.render.RenderUtil2D;

public class MainScreen extends Screen implements IMinecraft {

    public MainScreen() {
        super(ITextComponent.getTextComponentOrEmpty(""));
    }

    MatrixStack matrixStack;
    int mouseX, mouseY;

    Menu currentMenu = Menu.MAIN;
    float animationCurrentMenu;
    float animWidthCurrentMenu;

    public static boolean swapAnimAlt;
    public static float animSwapAlt;
    public static long timeOnSwap = 0L;

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        /* update animation and set class default */
        setSetting(matrixStack, mouseX, mouseY);
        updateAnim();

        /* render background */
        drawBackGround();

        /* render with main select button */
        if (animationCurrentMenu <= 0.95f) {
            drawButtonMenu();
        }

        /* render with select background */
        if (animationCurrentMenu >= 0.05f) {
            BackGroundFrontend backGroundFrontend = Pasta.instance.backGroundFrontend;
            backGroundFrontend.x = sr.getScaledWidth() + (sr.getScaledWidth() / 2f) - (animationCurrentMenu * sr.getScaledWidth());

            backGroundFrontend.render(matrixStack, mouseX, mouseY, partialTicks);
        }

        /* render decoration */
        drawDecoration();

        /* render select menu */
        drawSelectMenu();

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    private void drawDecoration() {
        /* left */
        RenderUtil2D.drawCircle(-120, sr.getScaledHeight() / 2f, 400, ColorUtil.rgba(255, 54, 64));

        /* right */
        RenderUtil2D.drawCircle(sr.getScaledWidth() + 120, sr.getScaledHeight() / 2f, 400, ColorUtil.rgba(255, 54, 64));

        /* up */
        RenderUtil2D.drawRectHorizontalW(0, 0, sr.getScaledWidth(), 100, ColorUtil.rgba(255, 54, 64, 0), ColorUtil.rgba(255, 54, 64, 120));
    }

    private void drawBackGround() {
        /* background */
        Pasta.instance.drawBackGround();

        /* background alpha */
        RenderUtil2D.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), ColorUtil.rgba(15, 10, 10, 120));

        float x = sr.getScaledWidth() / 2f - (animationCurrentMenu * sr.getScaledWidth());

        Fonts.pastaIcon.get(80).drawCenterX("B", x, sr.getScaledHeight() / 2f - 80, ColorUtil.rgba(255, 54, 64, 255));

        Fonts.mstr.get(12).drawString("Приветствую! " + Pasta.instance.login, 2, sr.getScaledHeight() - 2 - Fonts.mstr.get(12).getHeight(), -1);
    }

    private void drawSelectMenu() {
        float width = 110;
        float height = 28;
        float x = sr.getScaledWidth() / 2f - width / 2f;
        float y = sr.getScaledHeight() / 2f + height + (Button.values().length - 2) * 12;

        RenderUtil2D.drawOutlineRoundRect(x, y, width, height, 4, ColorUtil.rgba(28, 18, 19, 255), ColorUtil.rgba(51, 30, 32, 255));

        /* calculate and render background with text */
        {
            float wAll = 0;
            float wMain = 0;
            for (Menu f : Menu.values()) {

                String text = f.name;
                String icon = f.icon;
                float wText = Fonts.mstr.get(12).getWidth(text);
                float wIcon = Fonts.mainIcon.get(16).getWidth(icon);
                float wTextAndIcon = wText + wIcon + 10;

                if (currentMenu == f) {
                    wAll = wTextAndIcon;
                }

                if (f == Menu.MAIN) {
                    wMain = wTextAndIcon + 5;
                }
            }

            animWidthCurrentMenu = MathUtil.lerp(animWidthCurrentMenu, wAll, 10);

            RenderUtil2D.drawRoundRect(x + 5 + (animationCurrentMenu * wMain), y + 5, wAll, height - 10, 4, ColorUtil.rgba(255, 54, 65, 255));
        }

        /* render menu */
        {
            float xx = 0;
            for (Menu f : Menu.values()) {

                String text = f.name;
                String icon = f.icon;
                float wText = Fonts.mstr.get(12).getWidth(text);
                float wIcon = Fonts.mainIcon.get(16).getWidth(icon);
                float wTextAndIcon = wText + wIcon + 10;

                Fonts.mainIcon.get(14).drawCenterY(icon, x + 8 + xx, y + height / 2, -1);
                Fonts.mstr.get(12).drawCenterY(text, x + 8 + 10 + xx, y + height / 2, -1);

                xx += wTextAndIcon + 5;
            }
        }
    }

    private void drawButtonMenu() {
        float yy = 0;

        float fullWidth = 140;
        float halfWidth = 68;
        float h = 20;
        float x = sr.getScaledWidth() / 2f - fullWidth / 2f - (animationCurrentMenu * sr.getScaledWidth());
        float y = sr.getScaledHeight() / 2f - (Button.values().length * 20) / 2f + 20;

        boolean halfMode = false;

        for (Button f : Button.values()) {
            float currentX;
            float currentWidth;

            if (f.full) {
                currentWidth = fullWidth;
                currentX = x;
            } else {
                if (!halfMode) {
                    currentWidth = halfWidth;
                    currentX = x;
                    halfMode = true;
                } else {
                    currentWidth = halfWidth;
                    currentX = x + halfWidth + 4;
                    halfMode = false;
                }
            }

            float buttonY = y + yy;
            boolean hoverButton = MathUtil.isHover(mouseX, mouseY, currentX, buttonY, currentWidth, h);
            f.animOnHoverImg = MathUtil.lerp(f.animOnHoverImg, hoverButton ? 1.0f : 0.0f, 10);
            int colorHover = ColorUtil.interpolateColor(ColorUtil.rgba(255, 54, 65, 255), ColorUtil.rgba(26, 18, 19, 255), f.animOnHoverImg);

            RenderUtil2D.drawOutlineRoundRect(currentX - 0.5f, buttonY - 0.5f, currentWidth + 1, h + 1, 4, colorHover, ColorUtil.rgba(51, 30, 32, 255));

            Fonts.mstr.get(15).drawCenterXY(f.name, currentX - 1.5f + currentWidth / 2, buttonY + h / 2, -1);

            if (!f.full && halfMode) {
            } else {
                yy += h + 3f;
            }
        }
    }

    private void updateAnim() {
        animationCurrentMenu = MathUtil.lerp(animationCurrentMenu, currentMenu == Menu.MAIN ? 0.0f : 1.0f, 10);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (currentMenu == Menu.MAIN) {
            clickButton(button);
        } else if (currentMenu == Menu.BACK) {
            Pasta.instance.backGroundFrontend.mouseClicked(mouseX, mouseY, button);
        }

        clickMenu(button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Pasta.instance.backGroundFrontend.mouseScroll((float) delta);
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void clickMenu(int button) {
        float width = 110;
        float height = 28;
        float x = sr.getScaledWidth() / 2f - width / 2f;
        float y = sr.getScaledHeight() / 2f + height + (Button.values().length - 2) * 12;

        float xx = 0;
        for (Menu f : Menu.values()) {

            String text = f.name;
            String icon = f.icon;
            float wText = Fonts.mstr.get(12).getWidth(text);
            float wIcon = Fonts.mainIcon.get(16).getWidth(icon);
            float wTextAndIcon = wText + wIcon + 10;

            if (MathUtil.isHover(mouseX, mouseY, x + 5 + xx, y + 5, wTextAndIcon, height - 10) && button == 0) {
                currentMenu = f;
            }

            xx += wTextAndIcon + 5;
        }
    }

    private void clickButton(int button) {
        int yy = 0;
        float fullWidth = 140;
        float halfWidth = 68;
        float h = 20;
        float x = sr.getScaledWidth() / 2f - fullWidth / 2f - (animationCurrentMenu * sr.getScaledWidth());
        float y = sr.getScaledHeight() / 2f - (Button.values().length * 20) / 2f + 20;

        boolean halfMode = false;
        float currentX;
        float currentWidth;

        for (Button f : Button.values()) {
            if (f.full) {
                currentWidth = fullWidth;
                currentX = x;
            } else {
                if (!halfMode) {
                    currentWidth = halfWidth;
                    currentX = x;
                    halfMode = true;
                } else {
                    currentWidth = halfWidth;
                    currentX = x + halfWidth + 4;
                    halfMode = false;
                }
            }

            float buttonY = y + yy;
            boolean hoverButton = MathUtil.isHover(mouseX, mouseY, currentX, buttonY, currentWidth, h);

            if (hoverButton && button == 0 && f.screen != null) {
                mc.displayGuiScreen(f.screen);
            } else if (hoverButton && button == 0 && f.runnable != null) {
                f.runnable.run();
            }

            if (!f.full && halfMode) {
            } else {
                yy += h + 3;
            }
        }
    }

    private void setSetting(MatrixStack matrixStack, int mouseX, int mouseY) {
        this.matrixStack = matrixStack;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    private enum Menu {
        MAIN("A", "Home"),
        BACK("B", "Background");

        public final String icon;
        public final String name;

        Menu(String icon, String name) {
            this.icon = icon;
            this.name = name;
        }
    }

    private enum Button {

        SINGLEPLAYER("Одиночная игра", true, new WorldSelectionScreen(new MainScreen())),
        MULTIPLAYER("Сетевая игра", true, new MultiplayerScreen(new MainScreen())),
        SETTINGS("Настройки", false, new OptionsScreen(new MainScreen(), mc.gameSettings)),
        EXIT("Выход", false, () -> System.exit(-1)),
        ALTMANAGER("Учетная запись", true, Pasta.instance.altManagerScreen);

        public final String name;
        public final Screen screen;
        public final Runnable runnable;
        public float animOnHoverImg;
        public boolean full;

        Button(String name, boolean full, Screen screen) {
            this.name = name;
            this.full = full;
            this.screen = screen;
            this.runnable = null;
        }

        Button(String name, boolean full, Runnable runnable) {
            this.name = name;
            this.full = full;
            this.screen = null;
            this.runnable = runnable;
        }
    }
}