package fun.pasta.commands.impl;

import com.google.common.eventbus.Subscribe;
import fun.pasta.Pasta;
import fun.pasta.commands.Command;
import fun.pasta.event.EventRender2D;
import fun.pasta.util.IMinecraft;
import fun.pasta.util.render.RenderUtil2D;
import net.minecraft.util.ResourceLocation;

public class H0td04ekCommand extends Command implements IMinecraft {

    private static final ResourceLocation IMAGE = new ResourceLocation("pasta/images/icons/daun.png");
    private static final long DISPLAY_MS = 5000L;
    private static volatile long showUntilMs = 0L;
    private static boolean listenerRegistered = false;

    public H0td04ekCommand() {
        super("h0td04ek");
        if (!listenerRegistered && Pasta.instance != null && Pasta.instance.eventBus != null) {
            Pasta.instance.eventBus.register(this);
            listenerRegistered = true;
        }
    }

    @Override
    public void execute(String[] args) {
        showUntilMs = System.currentTimeMillis() + DISPLAY_MS;
    }

    @Subscribe
    public void onRender2D(EventRender2D event) {
        if (System.currentTimeMillis() > showUntilMs) {
            return;
        }

        float sw = sr.getScaledWidth();
        float sh = sr.getScaledHeight();
        float size = Math.min(sw, sh) * 0.35f;
        float x = (sw - size) / 2.0f;
        float y = (sh - size) / 2.0f;

        RenderUtil2D.drawImage(IMAGE, x, y, size, size, -1);
    }
}
