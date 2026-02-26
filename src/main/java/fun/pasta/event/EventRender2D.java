package fun.pasta.event;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.MainWindow;
import net.minecraft.client.renderer.ActiveRenderInfo;

public class EventRender2D {
    public MatrixStack matrixStack;
    public ActiveRenderInfo activeRenderInfo;
    public MainWindow mainWindow;
    public float particles;
}