package fun.pasta.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import com.mojang.authlib.GameProfile;
import fun.pasta.Pasta;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AntiBot extends Module {

    private final Set<UUID> detectedBots = ConcurrentHashMap.newKeySet();

    public AntiBot() {
        super("AntiBot", Category.COMBAT);
    }

    @Subscribe
    private void onUpdate(EventUpdate e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.world == null || mc.player == null || mc.player.connection == null) {
            detectedBots.clear();
            return;
        }

        Set<UUID> currentPlayers = new HashSet<>();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) {
                continue;
            }

            UUID uuid = player.getUniqueID();
            currentPlayers.add(uuid);

            if (isBotByHeuristics(player, mc)) {
                detectedBots.add(uuid);
            } else {
                detectedBots.remove(uuid);
            }
        }

        detectedBots.retainAll(currentPlayers);
    }

    private boolean isBotByHeuristics(PlayerEntity player, Minecraft mc) {
        if (player.removed || player.isSpectator()) {
            return true;
        }

        NetworkPlayerInfo infoByUuid = mc.player.connection.getPlayerInfo(player.getUniqueID());

        // Strict UUID check: if entity UUID is absent in tab list, treat it as bot.
        if (infoByUuid == null) {
            return true;
        }

        int ping = infoByUuid.getResponseTime();
        if (ping < 0 || ping > 1000) {
            return true;
        }

        GameProfile profile = infoByUuid.getGameProfile();
        if (profile == null || profile.getId() == null) {
            return true;
        }

        return !player.getUniqueID().equals(profile.getId());
    }

    public static boolean isBot(Entity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        }

        AntiBot module = getInstance();
        if (module == null || !module.enable) {
            return false;
        }

        UUID uuid = entity.getUniqueID();
        if (module.detectedBots.contains(uuid)) {
            return true;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) {
            return false;
        }

        return mc.player.connection.getPlayerInfo(uuid) == null;
    }

    public static boolean isBotU(Entity entity) {
        return isBot(entity);
    }

    private static AntiBot getInstance() {
        if (Pasta.instance == null || Pasta.instance.manager == null) {
            return null;
        }
        return Pasta.instance.manager.antiBot;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        detectedBots.clear();
    }
}
