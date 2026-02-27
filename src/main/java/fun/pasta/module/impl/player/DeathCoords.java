package fun.pasta.module.impl.player;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.util.chat.ChatUtil;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.util.math.BlockPos;

public class DeathCoords extends Module {

    private boolean printed;

    public DeathCoords() {
        super("DeathCoords", Category.PLAYER);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!enable || mc.player == null || mc.world == null) {
            return;
        }

        boolean deadScreen = mc.currentScreen instanceof DeathScreen && mc.player.getHealth() <= 0.0F;
        if (deadScreen && !printed) {
            BlockPos pos = mc.player.getPosition();
            ChatUtil.addChatMessage("Death coords: X " + pos.getX() + " Y " + pos.getY() + " Z " + pos.getZ());
            printed = true;
            return;
        }

        if (!deadScreen && mc.player.getHealth() > 0.0F) {
            printed = false;
        }
    }
}

