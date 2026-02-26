package fun.pasta.util.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.pasta.util.IMinecraft;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class BlurUtil implements IMinecraft {
    
    private static boolean enabled = true;
    private static float intensity = 1.0f;
    
    public static void setEnabled(boolean value) {
        enabled = value;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setIntensity(float value) {
        intensity = Math.max(0, Math.min(1, value));
    }
    
    public static float getIntensity() {
        return intensity;
    }
    
    /**
     * Draws a blurred rectangle area using a simple darkening effect
     * For actual blur, shaders would be needed
     */
    public static void drawBlurRect(float x, float y, float width, float height, int alpha) {
        if (!enabled) return;
        
        int adjustedAlpha = (int) (alpha * intensity);
        RenderUtil2D.drawRect(x, y, width, height, 
            ((adjustedAlpha & 0xFF) << 24) | (0 & 0xFF) << 16 | (0 & 0xFF) << 8 | (0 & 0xFF));
    }
    
    /**
     * Draws a blurred rounded rectangle
     */
    public static void drawBlurRoundRect(float x, float y, float width, float height, float radius, int alpha) {
        if (!enabled) return;
        
        int adjustedAlpha = (int) (alpha * intensity);
        RenderUtil2D.drawRoundRect(x, y, width, height, radius,
            ((adjustedAlpha & 0xFF) << 24) | (0 & 0xFF) << 16 | (0 & 0xFF) << 8 | (0 & 0xFF));
    }
}
