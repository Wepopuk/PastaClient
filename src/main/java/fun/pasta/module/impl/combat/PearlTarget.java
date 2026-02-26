package fun.pasta.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import fun.pasta.Pasta;
import fun.pasta.event.EventPacket;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.module.settings.SliderOption;
import fun.pasta.util.inventory.InventoryUtil;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.server.SSpawnObjectPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;

public class PearlTarget extends Module {

    private final SliderOption detectDistance = new SliderOption("Detect Dist", 4.5f, 1.5f, 6.0f, 0.1f);
    private final SliderOption followCooldown = new SliderOption("Cooldown", 450.0f, 0.0f, 2000.0f, 50.0f);
    private final BooleanOption silentRotate = new BooleanOption("Silent Rotate", true);

    private boolean pendingThrow;
    private float pendingYaw;
    private float pendingPitch;
    private long lastThrowAt;
    private long lastOwnThrowAt;
    private int lastPearlEntityId = Integer.MIN_VALUE;

    public PearlTarget() {
        super("PearlTarget", Category.COMBAT);
        add(detectDistance, followCooldown, silentRotate);
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.world == null) return;
        if (!event.isreceive()) return;
        if (!(event.packet instanceof SSpawnObjectPacket packet)) return;
        if (packet.getType() != EntityType.ENDER_PEARL) return;
        if (packet.getEntityID() == lastPearlEntityId) return;
        if (System.currentTimeMillis() - lastThrowAt < followCooldown.getValue().floatValue()) return;

        LivingEntity auraTarget = getAuraTarget();
        if (auraTarget == null || !auraTarget.isAlive()) return;
        if (!isPearlFromTarget(packet, auraTarget)) return;

        Vector3d spawn = new Vector3d(packet.getX(), packet.getY(), packet.getZ());
        Vector3d motion = new Vector3d(packet.func_218693_g(), packet.func_218695_h(), packet.func_218692_i());
        if (motion.lengthSquared() < 1.0E-5D) return;
        Vector3d targetImpact = predictPearlImpact(spawn, motion);
        float[] bestRotation = solveRotationToPoint(targetImpact);
        if (bestRotation == null) return;

        pendingYaw = bestRotation[0];
        pendingPitch = bestRotation[1];
        pendingThrow = true;
        lastPearlEntityId = packet.getEntityID();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!pendingThrow) return;
        if (mc.player == null || mc.world == null || mc.player.connection == null) {
            pendingThrow = false;
            return;
        }
        if (getAuraTarget() == null) {
            pendingThrow = false;
            return;
        }

        if (mc.player.getCooldownTracker().hasCooldown(Items.ENDER_PEARL)) return;

        if (silentRotate.get()) {
            mc.player.connection.sendPacket(new CPlayerPacket.RotationPacket(
                    pendingYaw,
                    pendingPitch,
                    mc.player.isOnGround()
            ));
        } else {
            mc.player.rotationYaw = pendingYaw;
            mc.player.rotationPitch = pendingPitch;
        }

        if (InventoryUtil.useItem(Items.ENDER_PEARL)) {
            lastThrowAt = System.currentTimeMillis();
            lastOwnThrowAt = lastThrowAt;
        }
        pendingThrow = false;
    }

    private boolean isPearlFromTarget(SSpawnObjectPacket packet, LivingEntity target) {
        Vector3d spawnPos = new Vector3d(packet.getX(), packet.getY(), packet.getZ());
        Vector3d selfEye = mc.player.getEyePosition(1.0f);
        if (System.currentTimeMillis() - lastOwnThrowAt < 300L && spawnPos.squareDistanceTo(selfEye) < 4.0D) {
            return false;
        }

        float maxDist = detectDistance.getValue().floatValue();
        double maxDistSq = maxDist * maxDist;

        Vector3d eyePos = target.getEyePosition(1.0f);
        if (spawnPos.squareDistanceTo(eyePos) > maxDistSq) {
            Vector3d bodyPos = target.getPositionVec().add(0.0, target.getHeight() * 0.85, 0.0);
            if (spawnPos.squareDistanceTo(bodyPos) > maxDistSq) {
                return false;
            }
        }

        Vector3d motion = new Vector3d(packet.func_218693_g(), packet.func_218695_h(), packet.func_218692_i());
        if (motion.lengthSquared() < 1.0E-5D) {
            return false;
        }

        return true;
    }

    private Vector3d predictPearlImpact(Vector3d startPos, Vector3d startMotion) {
        Vector3d pos = startPos;
        Vector3d motion = startMotion;

        for (int i = 0; i < 160; i++) {
            Vector3d nextPos = pos.add(motion);
            RayTraceResult ray = mc.world.rayTraceBlocks(new RayTraceContext(
                    pos,
                    nextPos,
                    RayTraceContext.BlockMode.COLLIDER,
                    RayTraceContext.FluidMode.NONE,
                    mc.player
            ));
            if (ray.getType() != RayTraceResult.Type.MISS) {
                return ray.getHitVec();
            }

            pos = nextPos;
            motion = motion.scale(0.99D).add(0.0D, -0.03D, 0.0D);

            if (pos.y < -64.0D) {
                break;
            }
        }

        return pos;
    }

    private float[] solveRotationToPoint(Vector3d point) {
        Vector3d start = getPearlStartPos();
        double dx = point.x - start.x;
        double dz = point.z - start.z;
        if (dx * dx + dz * dz < 1.0E-6D) {
            return null;
        }

        float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float bestPitch = 0.0F;
        double bestDistanceSq = Double.MAX_VALUE;

        for (float pitch = -89.0F; pitch <= 89.0F; pitch += 0.25F) {
            Vector3d velocity = createLaunchVelocity(yaw, pitch);
            Vector3d impact = predictPearlImpact(start, velocity);
            double distSq = impact.squareDistanceTo(point);

            if (distSq < bestDistanceSq) {
                bestDistanceSq = distSq;
                bestPitch = pitch;
            }
        }

        return new float[]{yaw, bestPitch};
    }

    private Vector3d createLaunchVelocity(float yaw, float pitch) {
        float yawRad = yaw * ((float) Math.PI / 180.0F);
        float pitchRad = pitch * ((float) Math.PI / 180.0F);

        double x = -MathHelper.sin(yawRad) * MathHelper.cos(pitchRad);
        double y = -MathHelper.sin(pitchRad);
        double z = MathHelper.cos(yawRad) * MathHelper.cos(pitchRad);

        Vector3d motion = new Vector3d(x, y, z).normalize().scale(1.5D);
        Vector3d playerMotion = mc.player.getMotion();
        return motion.add(playerMotion.x, mc.player.isOnGround() ? 0.0D : playerMotion.y, playerMotion.z);
    }

    private Vector3d getPearlStartPos() {
        return new Vector3d(
                mc.player.getPosX(),
                mc.player.getPosYEye() - 0.1D,
                mc.player.getPosZ()
        );
    }

    private LivingEntity getAuraTarget() {
        if (Pasta.instance == null || Pasta.instance.manager == null || Pasta.instance.manager.killAura == null) {
            return null;
        }
        if (!Pasta.instance.manager.killAura.enable) {
            return null;
        }
        return Pasta.instance.manager.killAura.target;
    }

    @Override
    public void onDisable() {
        pendingThrow = false;
        lastPearlEntityId = Integer.MIN_VALUE;
        lastOwnThrowAt = 0L;
        super.onDisable();
    }
}
