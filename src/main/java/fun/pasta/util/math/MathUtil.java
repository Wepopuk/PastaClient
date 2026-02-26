package fun.pasta.util.math;

import fun.pasta.util.IMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathUtil implements IMinecraft {

    public static boolean isHover(int mouseX, int mouseY, float x, float y, float w, float h) {
        return mouseX > x && mouseX < x + w && mouseY > y && mouseY < y + h;
    }

    public static double fpsTime() {
        return mc.debugFPS > 0f ? (1f / mc.debugFPS) : 1f;
    }

    public static float lerp(float end, float start, float multiple) {
        return (float) (end + (start - end) * MathHelper.clamp(fpsTime() * multiple, 0, 1));
    }

    public static double round(double num, double increment) {
        double v = (double) Math.round(num / increment) * increment;
        BigDecimal bd = new BigDecimal(v);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}

