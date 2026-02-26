package fun.pasta.module.impl.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.EditSignScreen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import fun.pasta.event.EventUpdate;
import fun.pasta.event.EventPacket;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.util.time.StopWatch;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CClickWindowPacket;

import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Queue;

public class InventoryMove extends Module {

    private final Queue<IPacket<?>> queuedPackets = new ArrayDeque<>();
    private final StopWatch wait = new StopWatch();
    private long flushAtMs = -1L;
    private boolean wasInventoryOpen;
    private boolean flushingPackets;

    public InventoryMove() {
        super("InventoryMove", Category.PLAYER);
        wait.setTime(0L);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) {
            return;
        }

        final KeyBinding[] moveKeys = {
                mc.gameSettings.keyBindForward,
                mc.gameSettings.keyBindBack,
                mc.gameSettings.keyBindLeft,
                mc.gameSettings.keyBindRight,
                mc.gameSettings.keyBindJump,
                mc.gameSettings.keyBindSprint
        };

        boolean funtime = isConnectedToServer("funtime");
        boolean inventoryOpen = mc.currentScreen instanceof InventoryScreen;

        if (funtime && wasInventoryOpen && !inventoryOpen && !queuedPackets.isEmpty()) {
            flushAtMs = System.currentTimeMillis() + 300L;
            wait.reset();
        }
        wasInventoryOpen = inventoryOpen;

        if (!funtime) {
            queuedPackets.clear();
            flushAtMs = -1L;
        } else if (flushAtMs > 0L && System.currentTimeMillis() >= flushAtMs) {
            flushQueuedPackets();
            flushAtMs = -1L;
        }

        if (funtime && !wait.isreached(400L)) {
            for (KeyBinding keyBinding : moveKeys) {
                keyBinding.setPressed(false);
            }
            return;
        }

        if (mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof EditSignScreen) {
            return;
        }

        updateKeyBindingState(moveKeys);
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.world == null || flushingPackets) {
            return;
        }

        if (!isConnectedToServer("funtime")) {
            return;
        }

        if (!event.isSend()) {
            return;
        }

        if (event.packet instanceof CClickWindowPacket
                && mc.currentScreen instanceof InventoryScreen
                && isMoving()) {
            queuedPackets.add(event.packet);
            event.cancel();
        }
    }

    @Override
    public void onDisable() {
        KeyBinding[] moveKeys = {
                mc.gameSettings.keyBindForward,
                mc.gameSettings.keyBindBack,
                mc.gameSettings.keyBindLeft,
                mc.gameSettings.keyBindRight,
                mc.gameSettings.keyBindJump,
                mc.gameSettings.keyBindSprint
        };

        for (KeyBinding key : moveKeys) {
            key.setPressed(false);
        }

        queuedPackets.clear();
        flushAtMs = -1L;
        wasInventoryOpen = false;
        flushingPackets = false;
        wait.setTime(0L);
        super.onDisable();
    }

    private boolean isMoving() {
        return Math.abs(mc.player.moveForward) > 1.0E-4F || Math.abs(mc.player.moveStrafing) > 1.0E-4F;
    }

    private boolean isConnectedToServer(String serverKey) {
        if (serverKey == null || serverKey.isEmpty() || mc.isSingleplayer() || mc.getCurrentServerData() == null) {
            return false;
        }

        String ip = mc.getCurrentServerData().serverIP;
        return ip != null && ip.toLowerCase(Locale.ROOT).contains(serverKey.toLowerCase(Locale.ROOT));
    }

    private void flushQueuedPackets() {
        if (mc.player == null || mc.player.connection == null || queuedPackets.isEmpty()) {
            queuedPackets.clear();
            return;
        }

        flushingPackets = true;
        try {
            IPacket<?> packet;
            while ((packet = queuedPackets.poll()) != null) {
                mc.player.connection.sendPacket(packet);
            }
        } finally {
            flushingPackets = false;
        }
    }

    private void updateKeyBindingState(KeyBinding[] keyBindings) {
        long handle = mc.getMainWindow().getHandle();
        for (KeyBinding keyBinding : keyBindings) {
            boolean pressed = InputMappings.isKeyDown(handle, keyBinding.getDefault().getKeyCode());
            keyBinding.setPressed(pressed);
        }
    }
}
