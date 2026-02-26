package fun.pasta.module.impl.visual;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventPacket;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.client.CUseEntityPacket;
import net.minecraft.network.play.server.SEntityStatusPacket;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DeathEffect extends Module {

    private static final int ATTACK_TRACK_TICKS = 160;

    private final Map<UUID, AttackInfo> attackedPlayers = new HashMap<>();
    private final Map<UUID, Boolean> aliveState = new HashMap<>();
    private int nextLocalEntityId = -2_000_000;

    public DeathEffect() {
        super("DeathEffect", Category.VISUAL);
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (!enable || mc.player == null || mc.world == null) {
            return;
        }

        int nowTick = mc.player.ticksExisted;

        if (event.isSend()
                && event.packet instanceof CUseEntityPacket packet
                && packet.getAction() == CUseEntityPacket.Action.ATTACK) {
            Entity entity = packet.getEntityFromWorld(mc.world);
            if (entity instanceof PlayerEntity player
                    && player != mc.player
                    && mc.player.connection != null
                    && mc.player.connection.getPlayerInfo(player.getUniqueID()) != null) {
                UUID id = player.getUniqueID();
                AttackInfo info = attackedPlayers.computeIfAbsent(id, k -> new AttackInfo());
                info.entityId = player.getEntityId();
                info.lastAttackTick = nowTick;
                info.lastX = player.getPosX();
                info.lastY = player.getPosY();
                info.lastZ = player.getPosZ();
            }
            return;
        }

        if (event.isreceive()
                && event.packet instanceof SEntityStatusPacket packet
                && packet.getOpCode() == 3) {
            Entity entity = packet.getEntity(mc.world);
            if (entity == mc.player) {
                attackedPlayers.clear();
                aliveState.clear();
                return;
            }
            if (entity instanceof PlayerEntity player && player != mc.player) {
                UUID id = player.getUniqueID();
                AttackInfo info = attackedPlayers.get(id);
                if (info != null && nowTick - info.lastAttackTick <= ATTACK_TRACK_TICKS) {
                    spawnLightning(player.getPosX(), player.getPosY(), player.getPosZ());
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
        if (!mc.player.isAlive() || mc.player.getHealth() <= 0.0F) {
            attackedPlayers.clear();
            aliveState.clear();
            return;
        }

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
                info.entityId = player.getEntityId();
                info.lastX = player.getPosX();
                info.lastY = player.getPosY();
                info.lastZ = player.getPosZ();
            }

            if (info != null && wasAlive && !aliveNow && nowTick - info.lastAttackTick <= ATTACK_TRACK_TICKS) {
                spawnLightning(player.getPosX(), player.getPosY(), player.getPosZ());
                attackedPlayers.remove(id);
            }

            aliveState.put(id, aliveNow);
        }

        Iterator<Map.Entry<UUID, AttackInfo>> iterator = attackedPlayers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, AttackInfo> entry = iterator.next();
            UUID id = entry.getKey();
            AttackInfo info = entry.getValue();

            if (nowTick - info.lastAttackTick > ATTACK_TRACK_TICKS) {
                iterator.remove();
                continue;
            }
            boolean stillOnline = mc.player.connection != null && mc.player.connection.getPlayerInfo(id) != null;
            if (!stillOnline) {
                iterator.remove();
            }
        }

        aliveState.keySet().retainAll(seen);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        attackedPlayers.clear();
        aliveState.clear();
    }

    private void spawnLightning(double x, double y, double z) {
        if (mc.world == null) {
            return;
        }

        LightningBoltEntity lightning = EntityType.LIGHTNING_BOLT.create(mc.world);
        if (lightning == null) {
            return;
        }

        lightning.moveForced(x, y, z);
        lightning.setEffectOnly(true);
        mc.world.addEntity(nextLocalEntityId--, lightning);
    }

    private static final class AttackInfo {
        private int entityId = -1;
        private int lastAttackTick;
        private double lastX;
        private double lastY;
        private double lastZ;
    }
}
