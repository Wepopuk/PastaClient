package fun.pasta.module.impl.player;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventPacket;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.SliderOption;
import fun.pasta.util.time.TimerUtil;
import net.minecraft.client.gui.screen.inventory.ChestScreen;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.network.play.server.SJoinGamePacket;
import net.minecraft.util.Hand;

public class ServerJoiner extends Module {

    private final SliderOption griefSelection = new SliderOption("Server Slot", 1.0F, 1.0F, 54.0F, 1.0F);
    private final SliderOption speed = new SliderOption("Speed", 3.0F, 1.0F, 10.0F, 1.0F);
    private final TimerUtil timer = new TimerUtil();

    public ServerJoiner() {
        super("ServerJoiner", Category.PLAYER);
        add(griefSelection, speed);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        timer.reset();
        if (mc.player != null && mc.player.connection != null) {
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!enable || mc.player == null || mc.world == null || mc.playerController == null) {
            return;
        }
        if (!(mc.currentScreen instanceof ChestScreen)) {
            return;
        }

        long delay = Math.max(40L, (long) (260L / Math.max(1.0F, speed.getValue().floatValue())));
        if (!timer.hasReached(delay)) {
            return;
        }

        int slotIndex = (int) griefSelection.getValue().floatValue() - 1;
        if (slotIndex < 0 || slotIndex >= mc.player.openContainer.inventorySlots.size()) {
            return;
        }

        Slot slot = mc.player.openContainer.inventorySlots.get(slotIndex);
        if (slot == null) {
            return;
        }

        mc.playerController.windowClick(mc.player.openContainer.windowId, slot.slotNumber, 0, ClickType.PICKUP, mc.player);
        mc.playerController.windowClick(mc.player.openContainer.windowId, slot.slotNumber, 0, ClickType.PICKUP, mc.player);
        timer.reset();
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (!event.isreceive()) {
            return;
        }
        if (event.packet instanceof SJoinGamePacket) {
            timer.reset();
        }
    }
}

