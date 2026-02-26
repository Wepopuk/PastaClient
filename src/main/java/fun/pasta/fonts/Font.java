package fun.pasta.fonts;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.pasta.Pasta;
import fun.pasta.module.impl.misc.NameProtect;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.text.*;

import java.io.InputStream;
import java.util.Locale;

import static org.lwjgl.opengl.GL11.*;

public class Font {

    private float posX;
    private float posY;
    private final int[] colorCode = new int[32];
    private boolean boldStyle;
    private boolean italicStyle;
    private boolean underlineStyle;
    private boolean strikethroughStyle;

    private final GlyphPage regularGlyphPage;
    private final GlyphPage boldGlyphPage;
    private final GlyphPage italicGlyphPage;
    private final GlyphPage boldItalicGlyphPage;

    public Font(GlyphPage regularGlyphPage, GlyphPage boldGlyphPage, GlyphPage italicGlyphPage,
                GlyphPage boldItalicGlyphPage) {
        this.regularGlyphPage = regularGlyphPage;
        this.boldGlyphPage = boldGlyphPage;
        this.italicGlyphPage = italicGlyphPage;
        this.boldItalicGlyphPage = boldItalicGlyphPage;

        for (int i = 0; i < 32; ++i) {
            int j = (i >> 3 & 1) * 85;
            int k = (i >> 2 & 1) * 170 + j;
            int l = (i >> 1 & 1) * 170 + j;
            int i1 = (i & 1) * 170 + j;

            if (i == 6) {
                k += 85;
            }

            if (i >= 16) {
                k /= 4;
                l /= 4;
                i1 /= 4;
            }

            this.colorCode[i] = (k & 255) << 16 | (l & 255) << 8 | i1 & 255;
        }
    }

    public static Font create(String file, float size, boolean bold, boolean italic, boolean boldItalic) {

        java.awt.Font font = null;

        try {
            InputStream in = Preconditions.checkNotNull(Font.class.getResourceAsStream("/assets/minecraft/pasta/fonts/" + file), "Font resource is null");
            font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, in)
                    .deriveFont(java.awt.Font.PLAIN, size);
        } catch (Exception e) {
            e.printStackTrace();
        }

        GlyphPage regularPage;

        regularPage = new GlyphPage(font, true, true);
        regularPage.generateGlyphPage();
        regularPage.setupTexture();

        GlyphPage boldPage = regularPage;
        GlyphPage italicPage = regularPage;
        GlyphPage boldItalicPage = regularPage;

        try {
            if (bold) {
                InputStream in = Preconditions.checkNotNull(Font.class.getResourceAsStream("/assets/minecraft/pasta/fonts/" + file), "Font resource is null");
                boldPage = new GlyphPage(
                        java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, in)
                                .deriveFont(java.awt.Font.BOLD, size),
                        true, true);

                boldPage.generateGlyphPage();
                boldPage.setupTexture();
            }

            if (italic) {
                InputStream in = Preconditions.checkNotNull(Font.class.getResourceAsStream("/assets/minecraft/pasta/fonts/" + file), "Font resource is null");
                italicPage = new GlyphPage(
                        java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, in)
                                .deriveFont(java.awt.Font.ITALIC, size),
                        true, true);

                italicPage.generateGlyphPage();
                italicPage.setupTexture();
            }

            if (boldItalic) {
                InputStream in = Preconditions.checkNotNull(Font.class.getResourceAsStream("/assets/minecraft/pasta/fonts/" + file), "Font resource is null");

                boldItalicPage = new GlyphPage(
                        java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, in)
                                .deriveFont(java.awt.Font.BOLD | java.awt.Font.ITALIC, size),
                        true, true);

                boldItalicPage.generateGlyphPage();
                boldItalicPage.setupTexture();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new Font(regularPage, boldPage, italicPage, boldItalicPage);
    }

    public int a(int color) {
        return color >> 24 & 0xFF;
    }

    private String protectText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        if (Pasta.instance == null || Pasta.instance.manager == null) {
            return text;
        }
        Object module = Pasta.instance.manager.getModule("NameProtect");
        if (module instanceof NameProtect) {
            return ((NameProtect) module).protect(text);
        }
        return text;
    }

    public int draw(MatrixStack matrices, String text, float x, float y, int color, boolean dropShadow) {
        if (a(color) < 10) return 0;
        text = protectText(text);
        y = y;
        this.resetStyles();
        int i;

        if (dropShadow) {
            i = this.renderString(matrices, text, x + 1.0F, y + 1.0F, color, true);

            i = Math.max(i, this.renderString(matrices, text, x, y, color, false));
        } else {
            i = this.renderString(matrices, text, x, y, color, false);
        }

        return i;
    }

    public int drawString(String text, float x, float y, int color) {
        MatrixStack matrices = new MatrixStack();
        return drawString(matrices, text, x, y, color, false);
    }

    public int drawString(MatrixStack matrices, String text, float x, float y, int color) {
        return drawString(matrices, text, x, y, color, false);
    }

    public int drawString(String text, double x, double y, int color) {
        MatrixStack matrices = new MatrixStack();
        return drawString(matrices, text, (float) x, (float) y, color, false);
    }

    public int drawString(MatrixStack matrices, String text, double x, double y, int color) {
        return drawString(matrices, text, (float) x, (float) y, color, false);
    }
    public int drawCenterXY(String text, double x, double y, int color) {
        MatrixStack matrices = new MatrixStack();
        return drawString(matrices, text, (float) x - getWidth(text) / 2f, (float) y - getHeight() / 2f, color, false);
    }

    public int drawCenterY(String text, double x, double y, int color) {
        MatrixStack matrices = new MatrixStack();
        return drawString(matrices, text, (float) x, (float) y - getHeight() / 2f, color, false);
    }

    public int drawCenterX(String text, double x, double y, int color) {
        MatrixStack matrices = new MatrixStack();
        return drawString(matrices, text, (float) x - getWidth(text) / 2f, (float) y, color, false);
    }
    public int drawCenter(MatrixStack matrixStack, String text, double x, double y, int color) {
        return drawString(matrixStack, text, (float) x - getWidth(text) / 2f, (float) y, color, false);
    }
    public int drawCenteredStringWithShadow(String text, double x, double y, int color) {
        MatrixStack matrices = new MatrixStack();
        return drawString(matrices, text, (float) x - getWidth(text) / 2f, (float) y - getHeight() / 2f, color, true);
    }
    public float getMiddleOfBox(float boxHeight) {
        return boxHeight / 2f - getHeight() / 2f;
    }

    public int drawString(MatrixStack matrices, String text, float x, float y, int color, boolean dropShadow) {
        text = protectText(text);
        y = (long) y;
        this.resetStyles();
        int i;

        if (dropShadow) {
            i = this.renderString(matrices, text, x + 1.0F, y + 1.0F, color, true);
            i = Math.max(i, this.renderString(matrices, text, x, y, color, false));
        } else {
            i = this.renderString(matrices, text, x, y, color, false);
        }

        return i;
    }

    public int drawString(MatrixStack matrices, ITextComponent texComponent, float x, float y, int color, boolean dropShadow) {
        y = (long) y;
        this.resetStyles();
        int i;
        String text = protectText(getPlainText(texComponent));

        if (dropShadow) {
            i = this.renderString(matrices, text, x + 1.0F, y + 1.0F, color, true);
            i = Math.max(i, this.renderString(matrices, text, x, y, color, false));
        } else {
            i = this.renderString(matrices, text, x, y, color, false);
        }

        return i;
    }

    private int renderString(MatrixStack matrices, String text, float x, float y, int color, boolean dropShadow) {
        text = protectText(text);
        if (text == null) {
            return 0;
        } else {

            if ((color & -67108864) == 0) {
                color |= -16777216;
            }

            if (dropShadow) {
                color = (color & 16579836) >> 2 | color & -16777216;
            }
            this.posX = x * 2.0f;
            this.posY = y * 2.0f;
            this.renderStringAtPos(matrices, text, dropShadow, color);
            return (int) (this.posX / 4.0f);
        }
    }

    private void renderStringAtPos(MatrixStack matrices, ITextComponent text, boolean shadow, int color) {
        renderStringAtPos(matrices,getPlainText(text),shadow,color);
    }

    private void renderStringAtPos(MatrixStack matrices, String text, boolean shadow, int color) {
        GlyphPage glyphPage = getCurrentGlyphPage();
        float alpha = (float) (color >> 24 & 255) / 255.0F;
        float g = (float) (color >> 16 & 255) / 255.0F;
        float h = (float) (color >> 8 & 255) / 255.0F;
        float k = (float) (color & 255) / 255.0F;

        matrices.push();
        try {
            matrices.scale(0.5f, 0.5F, 0.5F);

            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.enableTexture();

            glyphPage.bindTexture();
            RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            for (int i = 0; i < text.length(); ++i) {
                char c0 = text.charAt(i);

                if (c0 == 167 && i + 1 < text.length()) {
                    int i1 = "0123456789abcdefklmnor".indexOf(text.toLowerCase(Locale.ENGLISH).charAt(i + 1));

                    if (i1 < 16) {
                        this.boldStyle = false;
                        this.strikethroughStyle = false;
                        this.underlineStyle = false;
                        this.italicStyle = false;

                        if (i1 < 0) {
                            i1 = 15;
                        }

                        if (shadow) {
                            i1 += 16;
                        }

                        int j1 = this.colorCode[i1];

                        g = (float) (j1 >> 16 & 255) / 255.0F;
                        h = (float) (j1 >> 8 & 255) / 255.0F;
                        k = (float) (j1 & 255) / 255.0F;
                    } else if (i1 == 16) {
                    } else if (i1 == 17) {
                        this.boldStyle = true;
                    } else if (i1 == 18) {
                        this.strikethroughStyle = true;
                    } else if (i1 == 19) {
                        this.underlineStyle = true;
                    } else if (i1 == 20) {
                        this.italicStyle = true;
                    } else {
                        this.boldStyle = false;
                        this.strikethroughStyle = false;
                        this.underlineStyle = false;
                        this.italicStyle = false;
                    }

                    ++i;
                } else {
                    glyphPage = getCurrentGlyphPage();
                    glyphPage.bindTexture();
                    float f = glyphPage.drawChar(matrices, c0, posX, posY, g, k, h, alpha);
                    doDraw(matrices, f, glyphPage);
                }
            }
        } finally {
            RenderSystem.bindTexture(0);
            matrices.pop();
        }
    }

    public static String getPlainText(ITextComponent textComponent) {
        StringBuilder builder = new StringBuilder();
        for (ITextComponent component : textComponent.getSiblings()) {
            if (component instanceof StringTextComponent) {
                builder.append(((StringTextComponent) component).getText());
            } else if (component instanceof TranslationTextComponent) {
                builder.append(((TranslationTextComponent) component).getKey());
            } else if (component instanceof ScoreTextComponent) {
                builder.append(((ScoreTextComponent) component).getName());
            } else if (component instanceof KeybindTextComponent) {
                builder.append(((KeybindTextComponent) component).getKeybind());
            } else if (component instanceof NBTTextComponent) {
                builder.append(((NBTTextComponent) component).func_218676_i());
            } else {
                builder.append(component.getString());
            }
        }
        return builder.toString();
    }

    private void doDraw(MatrixStack matrices, float f, GlyphPage glyphPage) {
        if (this.strikethroughStyle) {
            BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
            RenderSystem.disableTexture();
            bufferBuilder.begin(GlStateManager.GL_QUADS, DefaultVertexFormats.POSITION);
            bufferBuilder
                    .pos(this.posX, this.posY + (float) (glyphPage.getMaxFontHeight() / 2), 0.0D)
                    .endVertex();
            bufferBuilder.pos(matrices.getLast().getMatrix(), this.posX + f,
                    this.posY + (float) (glyphPage.getMaxFontHeight() / 2), 0.0F).endVertex();
            bufferBuilder.pos(matrices.getLast().getMatrix(), this.posX + f,
                    this.posY + (float) (glyphPage.getMaxFontHeight() / 2) - 1.0F, 0.0F).endVertex();
            bufferBuilder.pos(matrices.getLast().getMatrix(), this.posX,
                    this.posY + (float) (glyphPage.getMaxFontHeight() / 2) - 1.0F, 0.0F).endVertex();
            bufferBuilder.finishDrawing();
            Tessellator.getInstance().draw();
            RenderSystem.enableTexture();
        }

        if (this.underlineStyle) {
            BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
            RenderSystem.disableTexture();
            bufferBuilder.begin(GlStateManager.GL_QUADS, DefaultVertexFormats.POSITION);
            int l = this.underlineStyle ? -1 : 0;
            bufferBuilder.pos(matrices.getLast().getMatrix(), this.posX + (float) l,
                    this.posY + (float) glyphPage.getMaxFontHeight(), 0.0F).endVertex();
            bufferBuilder
                    .pos(matrices.getLast().getMatrix(), this.posX + f, this.posY + (float) glyphPage.getMaxFontHeight(), 0.0F)
                    .endVertex();
            bufferBuilder.pos(matrices.getLast().getMatrix(), this.posX + f,
                    this.posY + (float) glyphPage.getMaxFontHeight() - 1.0F, 0.0F).endVertex();
            bufferBuilder.pos(matrices.getLast().getMatrix(), this.posX + (float) l,
                    this.posY + (float) glyphPage.getMaxFontHeight() - 1.0F, 0.0F).endVertex();
            bufferBuilder.finishDrawing();
            Tessellator.getInstance().draw();
            RenderSystem.enableTexture();
        }

        this.posX += f;
    }

    private GlyphPage getCurrentGlyphPage() {
        if (boldStyle && italicStyle)
            return boldItalicGlyphPage;
        else if (boldStyle)
            return boldGlyphPage;
        else if (italicStyle)
            return italicGlyphPage;
        else
            return regularGlyphPage;
    }

    private void resetStyles() {
        this.boldStyle = false;
        this.italicStyle = false;
        this.underlineStyle = false;
        this.strikethroughStyle = false;
    }

    public int getHeight() {
        return regularGlyphPage.getMaxFontHeight() / 2;
    }

    public int getWidth(String str) {
        String text = removeColorCodes(protectText(str));
        if (text == null || text.isEmpty()) { // Добавлена проверка на пустую строку
            return 0;
        }
        int width = 0;

        GlyphPage currentPage;

        int size = text.length();

        boolean on = false;

        // Сбросить стили перед расчетом
        boldStyle = false;
        italicStyle = false;

        for (int i = 0; i < size; i++) {
            char character = text.charAt(i);
            if (character == '§') {
                on = true;
                continue; // Переходим к следующей итерации
            }

            if (on) {
                if (character >= '0' && character <= 'r') {
                    int colorIndex = "0123456789abcdefklmnor".indexOf(character);
                    if (colorIndex < 16) {
                        boldStyle = false;
                        italicStyle = false;
                    } else if (colorIndex == 17) {
                        boldStyle = true;
                    } else if (colorIndex == 20) {
                        italicStyle = true;
                    } else if (colorIndex == 21) {
                        boldStyle = false;
                        italicStyle = false;
                    }
                }
                on = false;
                continue;
            }

            currentPage = getCurrentGlyphPage();
            width += currentPage.getWidth(character) - 8;
        }

        return width / 2;
    }

    public String removeColorCodes(String text) {
        if (text == null) {
            return "";
        }
        String str = text;
        String[] colorcodes = new String[]{
                "4", "c", "6", "e", "2", "a", "b", "3", "1", "9", "d",
                "5", "f", "7", "8", "0", "k", "m", "o", "l", "n", "r"};
        for (String c : colorcodes) {
            str = str.replace("§" + c, "");
        }
        return str.trim();
    }

    public String trimStringToWidth(String text, int width) {
        return this.trimStringToWidth(text, width, false);
    }

    public String trimStringToWidth(String str, int maxWidth, boolean reverse) {
        StringBuilder stringbuilder = new StringBuilder();
        boolean on = false;

        String text = removeColorCodes(protectText(str));
        int j = reverse ? text.length() - 1 : 0;
        int k = reverse ? -1 : 1;
        int width = 0;

        GlyphPage currentPage;

        for (int i = j; i >= 0 && i < text.length() && i < maxWidth; i += k) {
            char character = text.charAt(i);

            if (character == '§')
                on = true;
            else if (on && character >= '0' && character <= 'r') {
                int colorIndex = "0123456789abcdefklmnor".indexOf(character);
                if (colorIndex < 16) {
                    boldStyle = false;
                    italicStyle = false;
                } else if (colorIndex == 17) {
                    boldStyle = true;
                } else if (colorIndex == 20) {
                    italicStyle = true;
                } else if (colorIndex == 21) {
                    boldStyle = false;
                    italicStyle = false;
                }
                i++;
                on = false;
            } else {
                if (on)
                    i--;

                character = text.charAt(i);

                currentPage = getCurrentGlyphPage();

                width += (currentPage.getWidth(character) - 8) / 2;
            }

            if (i > width) {
                break;
            }

            if (reverse) {
                stringbuilder.insert(0, character);
            } else {
                stringbuilder.append(character);
            }
        }

        return stringbuilder.toString();
    }

}
