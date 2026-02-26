package fun.pasta.event;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.vector.Vector3d;

public class EventFireworkMotion extends Event {

    private final LivingEntity entity;
    private final FireworkRocketEntity firework;
    private Vector3d vector;

    public EventFireworkMotion(LivingEntity entity, FireworkRocketEntity firework, Vector3d vector) {
        this.entity = entity;
        this.firework = firework;
        this.vector = vector;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public FireworkRocketEntity getFirework() {
        return firework;
    }

    public Vector3d getVector() {
        return vector;
    }

    public void setVector(Vector3d vector) {
        this.vector = vector;
    }
}
