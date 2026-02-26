package fun.pasta.ui.clickGui;

import com.mojang.blaze3d.matrix.MatrixStack;
import fun.pasta.Pasta;
import fun.pasta.fonts.Fonts;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.ui.clickGui.settings.Settings;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.math.MathUtil;
import fun.pasta.util.render.RenderUtil2D;
import fun.pasta.util.render.SmartScissor;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class Panel extends Settings {

    public final Category category;
    private final ModuleSelection selection;
    private final List<ModuleRender> moduleRenders = new ArrayList<>();
    private final List<ModuleRender> visibleRenders = new ArrayList<>();
    private String filter = "";

    public float scroll, scrollOut;
    private float lastHeight = 0;
    private static final float HEADER_HEIGHT = 22f;
    private static final float BODY_GAP = 4f;

    public Panel(Category category, ModuleSelection selection) {
        this.category = category;
        this.selection = selection;
        this.width = 92;
        this.height = 250;

        for (Module module : Pasta.instance.manager.modules) {
            if (module.category == category) {
                moduleRenders.add(new ModuleRender(module, selection));
            }
        }
    }

    @Override
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        scrollOut = MathUtil.lerp(scrollOut, scroll, 10);

        RenderUtil2D.drawShadow(x, y, width, height, 10, ColorUtil.rgba(0, 0, 0, 120));

        RenderUtil2D.drawRoundRect(x, y, width, HEADER_HEIGHT, 8, ColorUtil.rgba(24, 24, 28, 255));
        float bodyY = y + HEADER_HEIGHT + BODY_GAP;
        float bodyH = height - HEADER_HEIGHT - BODY_GAP;
        RenderUtil2D.drawRoundRect(x, bodyY, width, bodyH, 8, ColorUtil.rgba(18, 18, 22, 230));

        Fonts.mstr.get(11).drawString(stack, category.name, x + 8, y + 7, -1);
        Fonts.categoryIcon.get(11).drawString(stack, category.icon, x + width - 14, y + 7, ColorUtil.rgba(190, 190, 190, 255));

        // Modules with Scissor & Scrolling
        SmartScissor.push();
        SmartScissor.set(x, bodyY + 4, width, bodyH - 8);

        visibleRenders.clear();
        float offset = bodyY + 6 + scrollOut;
        for (ModuleRender render : moduleRenders) {
            if (!filter.isEmpty() && !render.module.getName().toLowerCase().contains(filter)) {
                continue;
            }
            render.x = x + 4;
            render.y = offset;
            render.width = width - 8;
            render.draw(stack, mouseX, mouseY);
            offset += 16 + 3;
            visibleRenders.add(render);
        }
        lastHeight = offset - scrollOut - bodyY;

        SmartScissor.unSet();
        SmartScissor.pop();

        // Scrollbar (Strictly confined to track between header and bottom)
        if (lastHeight > bodyH) {
            float trackTop = bodyY + 4;
            float trackBottom = bodyY + bodyH - 4;
            float trackHeight = trackBottom - trackTop;

            float thumbHeight = (bodyH / lastHeight) * trackHeight;
            float maxScroll = lastHeight - bodyH;
            float scrollPercent = -scrollOut / maxScroll;
            float thumbPos = scrollPercent * (trackHeight - thumbHeight);

            RenderUtil2D.drawRoundRect(x + width - 4, trackTop + thumbPos, 2.5f, thumbHeight, 0.5f,
                    ColorUtil.rgba(255, 255, 255, 70));
        }

        // Auto-clamp scroll (removed extra 10px buffer to prevent overflow)
        if (lastHeight > bodyH) {
            scroll = MathHelper.clamp(scroll, -(lastHeight - bodyH), 0);
        } else {
            scroll = 0;
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        float bodyY = y + HEADER_HEIGHT + BODY_GAP;
        float bodyH = height - HEADER_HEIGHT - BODY_GAP;
        if (mouseX > x && mouseX < x + width && mouseY > bodyY && mouseY < bodyY + bodyH) {
            visibleRenders.forEach(render -> render.mouseClicked(mouseX, mouseY, mouseButton));
        }
    }

    @Override
    public void mouseScrolled(int mouseX, int mouseY, double delta) {
        float bodyY = y + HEADER_HEIGHT + BODY_GAP;
        float bodyH = height - HEADER_HEIGHT - BODY_GAP;
        if (mouseX > x && mouseX < x + width && mouseY > bodyY && mouseY < bodyY + bodyH) {
            scroll += (float) (delta * 20);
        }
    }

    @Override
    public void mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
        float bodyY = y + HEADER_HEIGHT + BODY_GAP;
        float bodyH = height - HEADER_HEIGHT - BODY_GAP;
        if (mouseX > x && mouseX < x + width && mouseY > bodyY && mouseY < bodyY + bodyH) {
            visibleRenders.forEach(render -> render.mouseDragged(mouseX, mouseY, mouseButton, deltaX, deltaY));
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
    }

    @Override
    public void keyTyped(int keyCode, int scanCode, int modifiers) {
        moduleRenders.forEach(render -> render.keyTyped(keyCode, scanCode, modifiers));
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
    }

    public void setFilter(String filter) {
        this.filter = filter == null ? "" : filter.trim().toLowerCase();
    }
}
