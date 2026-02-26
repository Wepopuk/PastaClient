package fun.pasta.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import fun.pasta.Pasta;
import fun.pasta.event.EventPacket;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;
import net.minecraft.network.play.server.SChatPacket;

import java.util.Locale;

public class AutoAccept extends Module {

    private final BooleanOption onlyFriends = new BooleanOption("Only Friends", true);

    public AutoAccept() {
        super("AutoAccept", Category.MISC);
        add(onlyFriends);
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (!enable || mc.player == null || mc.world == null || !event.isreceive()) {
            return;
        }

        if (!(event.packet instanceof SChatPacket packet)) {
            return;
        }

        String raw = packet.getChatComponent().getString();
        if (raw == null || raw.isEmpty()) {
            return;
        }

        String message = raw.toLowerCase(Locale.ROOT);
        boolean isTpRequest =
                message.contains("телепортироваться")
                        || message.contains("has requested teleport")
                        || message.contains("просит к вам телепортироваться");

        if (!isTpRequest) {
            return;
        }

        if (onlyFriends.get()) {
            if (Pasta.instance == null || Pasta.instance.friendManager == null) {
                return;
            }

            boolean friendFound = false;
            for (String friend : Pasta.instance.friendManager.getFriends()) {
                if (friend != null && !friend.isEmpty()
                        && message.contains(friend.toLowerCase(Locale.ROOT))) {
                    friendFound = true;
                    break;
                }
            }

            if (!friendFound) {
                return;
            }
        }

        mc.player.sendChatMessage("/tpaccept");
    }
}
