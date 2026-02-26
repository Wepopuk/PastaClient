package fun.pasta.event;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.IRenderTypeBuffer;

public class EventRender3D {
    public MatrixStack matrixStack;
    public IRenderTypeBuffer buffer;
    public ActiveRenderInfo activeRenderInfo;
    public double camX;
    public double camY;
    public double camZ;
    public float partialTicks;
}
