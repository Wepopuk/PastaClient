package fun.pasta.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventPacket;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.client.CUseEntityPacket;
import net.minecraft.network.play.server.SEntityStatusPacket;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AutoMessage extends Module {

    private static final int ATTACK_TRACK_TICKS = 160;
    private final Map<UUID, Boolean> aliveState = new HashMap<>();
    private final Map<UUID, AttackInfo> attackedPlayers = new ConcurrentHashMap<>();
    private final Queue<String> pendingMessages = new ConcurrentLinkedQueue<>();
    private boolean suppressUntilNextAttack;

    public AutoMessage() {
        super("AutoMessage", Category.MISC);
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (!enable || mc.player == null || mc.world == null) {
            return;
        }

        int nowTick = mc.player.ticksExisted;

        if (event.isreceive()
                && event.packet instanceof SEntityStatusPacket packet
                && packet.getOpCode() == 3) {
            Entity entity = packet.getEntity(mc.world);
            if (entity == mc.player) {
                suppressUntilNextAttack = true;
                clearCombatTracking();
                return;
            }
        }

        if (event.isSend()
                && event.packet instanceof CUseEntityPacket packet
                && packet.getAction() == CUseEntityPacket.Action.ATTACK) {
            if (suppressUntilNextAttack) {
                clearCombatTracking();
                suppressUntilNextAttack = false;
            }

            Entity entity = packet.getEntityFromWorld(mc.world);
            if (entity instanceof PlayerEntity player
                    && player != mc.player
                    && mc.player.connection != null
                    && mc.player.connection.getPlayerInfo(player.getUniqueID()) != null) {
                UUID id = player.getUniqueID();
                AttackInfo info = attackedPlayers.computeIfAbsent(id, k -> new AttackInfo());
                info.name = player.getName().getString();
                info.entityId = player.getEntityId();
                info.lastAttackTick = nowTick;
            }
            return;
        }

        if (suppressUntilNextAttack || !isSelfAlive()) {
            return;
        }

        if (event.isreceive()
                && event.packet instanceof SEntityStatusPacket packet
                && packet.getOpCode() == 3) {
            Entity entity = packet.getEntity(mc.world);
            if (entity instanceof PlayerEntity player && player != mc.player) {
                UUID id = player.getUniqueID();
                AttackInfo info = attackedPlayers.get(id);
                if (info != null && nowTick - info.lastAttackTick <= ATTACK_TRACK_TICKS) {
                    queueKillMessage(info.name != null ? info.name : player.getName().getString());
                    attackedPlayers.remove(id);
                }
            }
            return;
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!enable || mc.player == null || mc.world == null) {
            return;
        }
        if (suppressUntilNextAttack || !isSelfAlive()) {
            clearCombatTracking();
            return;
        }

        flushPendingMessages();

        int nowTick = mc.player.ticksExisted;

        Set<UUID> seen = new HashSet<>();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) {
                continue;
            }

            UUID id = player.getUniqueID();
            seen.add(id);

            boolean aliveNow = player.isAlive() && player.getHealth() > 0.0F;
            boolean wasAlive = aliveState.getOrDefault(id, aliveNow);
            AttackInfo info = attackedPlayers.get(id);
            if (info != null) {
                info.name = player.getName().getString();
                info.entityId = player.getEntityId();
            }

            if (info != null && wasAlive && !aliveNow && nowTick - info.lastAttackTick <= ATTACK_TRACK_TICKS) {
                queueKillMessage(info.name != null ? info.name : player.getName().getString());
                attackedPlayers.remove(id);
            }

            aliveState.put(id, aliveNow);
        }

        for (Map.Entry<UUID, AttackInfo> entry : attackedPlayers.entrySet()) {
            UUID id = entry.getKey();
            AttackInfo info = entry.getValue();

            if (nowTick - info.lastAttackTick > ATTACK_TRACK_TICKS) {
                attackedPlayers.remove(id);
                continue;
            }
            boolean stillOnline = mc.player.connection != null && mc.player.connection.getPlayerInfo(id) != null;
            if (!stillOnline) {
                attackedPlayers.remove(id);
            }
        }

        aliveState.keySet().retainAll(seen);
        flushPendingMessages();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        suppressUntilNextAttack = false;
        clearCombatTracking();
    }

    private void queueKillMessage(String name) {
        if (name != null && !name.isEmpty()) {
            pendingMessages.add(name);
        }
    }

    private void flushPendingMessages() {
        String name;
        while ((name = pendingMessages.poll()) != null) {
            sendKillMessage(name);
        }
    }

    private void sendKillMessage(String name) {
        if (suppressUntilNextAttack || !isSelfAlive() || mc.player.connection == null) {
            return;
        }
        String message = "! -" + name + " \u043D\u0430\u043F\u0438\u0448\u0438 \u043C\u043D\u0435 \u0432 \u0434\u0441 h0td04ek \u044F \u0442\u0435 \u0434\u0430\u043C PastaClient";
        mc.player.sendChatMessage(message);
    }

    private boolean isSelfAlive() {
        return mc.player != null && mc.player.isAlive() && mc.player.getHealth() > 0.0F;
    }

    private void clearCombatTracking() {
        aliveState.clear();
        attackedPlayers.clear();
        pendingMessages.clear();
    }

    private static final class AttackInfo {
        private String name;
        private int entityId = -1;
        private int lastAttackTick;
    }
}
