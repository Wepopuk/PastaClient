package fun.pasta.ui.clickGui.components;

import com.mojang.blaze3d.matrix.MatrixStack;
import fun.pasta.fonts.Fonts;
import fun.pasta.module.api.Category;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.render.RenderUtil2D;

import java.awt.*;

public class SidebarItem extends Component {
    public final Category category;
    private final Runnable onClick;
    public boolean active;

    public SidebarItem(Category category, Runnable onClick) {
        super(0, 0, 110, 30);
        this.category = category;
        this.onClick = onClick;
    }

    @Override
    public void draw(MatrixStack ms, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY);

        if (active) {
            RenderUtil2D.drawRect(x, y, 2, height, ColorUtil.rgba(66, 165, 245, 255));
            RenderUtil2D.drawRect(x + 2, y, width - 2, height, ColorUtil.rgba(66, 165, 245, 10));
        } else if (hovered) {
            RenderUtil2D.drawRect(x, y, width, height, ColorUtil.rgba(255, 255, 255, 5));
        }

        int textColor = active ? ColorUtil.rgba(66, 165, 245, 255)
                : (hovered ? Color.WHITE.getRGB() : ColorUtil.rgba(112, 114, 118, 255));

        Fonts.categoryIcon.get(14).drawCenterY(category.icon, x + 15, y + height / 2f, textColor);
        Fonts.mstr.get(12).drawCenterY(category.name, x + 38, y + height / 2f, textColor);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY) && button == 0) {
            onClick.run();
        }
    }
}
