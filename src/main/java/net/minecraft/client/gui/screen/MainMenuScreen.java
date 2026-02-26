package net.minecraft.client.gui.screen;

import com.mojang.blaze3d.matrix.MatrixStack;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import net.minecraft.client.renderer.RenderSkyboxCube;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import fun.pasta.Pasta;
import fun.pasta.fonts.Fonts;
import fun.pasta.ui.mainMenu.MainScreen;
import fun.pasta.util.IMinecraft;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.math.MathUtil;
import fun.pasta.util.render.RenderUtil2D;

public class MainMenuScreen extends Screen implements IMinecraft {

    public MainMenuScreen(boolean b)
    {
        super(new TranslationTextComponent("narrator.screen.title"));
    }

    protected void init() {

    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {

        RenderUtil2D.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), ColorUtil.rgba(16, 16, 16, 255));

        drawButtonMenu(matrixStack, mouseX, mouseY);

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    private void drawButtonMenu(MatrixStack matrixStack, int mouseX, int mouseY) {

        int yy = 0;

        float w = 100;
        float h = 20;
        float x = sr.getScaledWidth() / 2f - w / 2f;
        float y = sr.getScaledHeight() / 2f - (Button.values().length * 20) / 2f;

        for (Button f : Button.values()) {

            boolean hoverButton = MathUtil.isHover(mouseX, mouseY, x, y + yy, w, h);
            int colorHover = hoverButton ? ColorUtil.rgba(66, 66, 66, 255) : ColorUtil.rgba(36, 36, 36, 255);
            int colorHoverText = hoverButton ? ColorUtil.rgba(255, 255, 255, 255) : ColorUtil.rgba(144, 144, 144, 255);

            RenderUtil2D.drawRoundRect(x - 0.5f, y + yy - 0.5f, w + 1, h + 1, 4, colorHover);
            RenderUtil2D.drawRoundRect(x, y + yy, w, h, 4, ColorUtil.rgba(24, 24, 24, 255));

            Fonts.mstr.get(16).drawCenterXY(f.name, x + w / 2, y + h / 2 + yy, colorHoverText);

            yy += 20 + 3;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        int yy = 0;

        float w = 100;
        float h = 20;
        float x = sr.getScaledWidth() / 2f - w / 2f;
        float y = sr.getScaledHeight() / 2f - (Button.values().length * 20) / 2f;

        for (Button f : Button.values()) {

            boolean hoverButton = MathUtil.isHover((int) mouseX, (int) mouseY, x, y + yy, w, h);

            if (hoverButton && button == 0) {
                mc.displayGuiScreen(f.screen);
            }

            yy += 20 + 3;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean isPauseScreen()
    {
        return false;
    }

    public boolean shouldCloseOnEsc()
    {
        return false;
    }

    private static final ResourceLocation MINECRAFT_TITLE_TEXTURES = new ResourceLocation("textures/gui/title/minecraft.png");
    private static final ResourceLocation MINECRAFT_TITLE_EDITION = new ResourceLocation("textures/gui/title/edition.png");
    public static final RenderSkyboxCube PANORAMA_RESOURCES = new RenderSkyboxCube(new ResourceLocation("textures/gui/title/background/panorama"));
    private static final ResourceLocation PANORAMA_OVERLAY_TEXTURES = new ResourceLocation("textures/gui/title/background/panorama_overlay.png");

    public static CompletableFuture<Void> loadAsync(TextureManager texMngr, Executor backgroundExecutor) {
        return CompletableFuture.allOf(texMngr.loadAsync(MINECRAFT_TITLE_TEXTURES, backgroundExecutor), texMngr.loadAsync(MINECRAFT_TITLE_EDITION, backgroundExecutor),
                texMngr.loadAsync(PANORAMA_OVERLAY_TEXTURES, backgroundExecutor), PANORAMA_RESOURCES.loadAsync(texMngr, backgroundExecutor));
    }

    private enum Button {

        SINGLEPLAYER("Одиночная игра", new WorldSelectionScreen(new MainScreen())),
        MULTIPLAYER("Сетевая игра", new MultiplayerScreen(new MainScreen())),
        ALTMANAGER("Учетная запись", Pasta.instance.altManagerScreen),
        SETTINGS("Настройки", new OptionsScreen(new MainScreen(), mc.gameSettings));

        public final String name;
        public final Screen screen;

        Button(String name, Screen screen) {
            this.name = name;
            this.screen = screen;
        }
    }

}
