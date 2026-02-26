package fun.pasta.module.impl.visual;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import fun.pasta.event.EventRender3D;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.SliderOption;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Particles extends Module {

    private static final ResourceLocation[] PARTICLE_TEXTURES = new ResourceLocation[] {
            new ResourceLocation("pasta/images/icons/dollar.png"),
            new ResourceLocation("pasta/images/icons/snow.png"),
            new ResourceLocation("pasta/images/icons/star.png")
    };

    private static final float SPAWN_RADIUS_MIN = 5.0F;
    private static final float SPAWN_RADIUS_MAX = 16.0F;
    private static final float RESET_DISTANCE = 24.0F;

    public final SliderOption strength = new SliderOption("Strength", 120.0F, 20.0F, 400.0F, 1.0F);
    public final SliderOption speed = new SliderOption("Speed", 1.0F, 0.2F, 3.0F, 0.05F);

    private final List<WorldParticle> particles = new ArrayList<>();

    public Particles() {
        super("Particles", Category.VISUAL);
        add(strength, speed);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!enable || mc.player == null || mc.world == null) {
            particles.clear();
            return;
        }

        int desiredCount = Math.max(1, Math.round(strength.getValue().floatValue()));
        while (particles.size() < desiredCount) {
            particles.add(createParticle());
        }
        while (particles.size() > desiredCount) {
            particles.remove(particles.size() - 1);
        }

        float speedMul = speed.getValue().floatValue();
        for (WorldParticle particle : particles) {
            tickParticle(particle, speedMul);
        }
    }

    @Subscribe
    public void onRender3D(EventRender3D event) {
        if (!enable || mc.player == null || mc.world == null || particles.isEmpty()) {
            return;
        }

        MatrixStack matrixStack = event.matrixStack;
        float partialTicks = event.partialTicks;

        for (WorldParticle particle : particles) {
            double x = MathHelper.lerp(partialTicks, particle.prevX, particle.x) - event.camX;
            double y = MathHelper.lerp(partialTicks, particle.prevY, particle.y) - event.camY;
            double z = MathHelper.lerp(partialTicks, particle.prevZ, particle.z) - event.camZ;
            float roll = MathHelper.lerp(partialTicks, particle.prevRoll, particle.roll);

            matrixStack.push();
            try {
                matrixStack.translate(x, y, z);
                matrixStack.rotate(event.activeRenderInfo.getRotation());
                matrixStack.rotate(Vector3f.ZP.rotationDegrees(roll));
                matrixStack.scale(particle.size, particle.size, particle.size);

                IVertexBuilder buffer = event.buffer.getBuffer(RenderType.getEntityTranslucent(particle.texture));
                MatrixStack.Entry entry = matrixStack.getLast();
                int alpha = MathHelper.clamp((int) (particle.alpha * 255.0F), 30, 255);

                addVertex(entry, buffer, -0.5F, -0.5F, 0.0F, 0.0F, 1.0F, alpha);
                addVertex(entry, buffer, 0.5F, -0.5F, 0.0F, 1.0F, 1.0F, alpha);
                addVertex(entry, buffer, 0.5F, 0.5F, 0.0F, 1.0F, 0.0F, alpha);
                addVertex(entry, buffer, -0.5F, 0.5F, 0.0F, 0.0F, 0.0F, alpha);
            } finally {
                matrixStack.pop();
            }
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        particles.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        particles.clear();
    }

    private WorldParticle createParticle() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vector3d playerPos = mc.player.getPositionVec();

        float radius = SPAWN_RADIUS_MIN + random.nextFloat() * (SPAWN_RADIUS_MAX - SPAWN_RADIUS_MIN);
        float angle = random.nextFloat() * (float) (Math.PI * 2.0);

        double px = playerPos.x + Math.cos(angle) * radius + random.nextDouble(-1.5D, 1.5D);
        double py = playerPos.y + random.nextDouble(0.2D, 6.5D);
        double pz = playerPos.z + Math.sin(angle) * radius + random.nextDouble(-1.5D, 1.5D);

        double motionX = random.nextDouble(-0.018D, 0.018D);
        double motionY = random.nextDouble(-0.010D, 0.006D);
        double motionZ = random.nextDouble(-0.018D, 0.018D);

        float size = 0.22F + random.nextFloat() * (0.50F - 0.22F);
        float alpha = 0.45F + random.nextFloat() * (0.95F - 0.45F);
        float roll = random.nextFloat() * 360.0F;
        float rollSpeed = -2.8F + random.nextFloat() * 5.6F;
        ResourceLocation texture = PARTICLE_TEXTURES[random.nextInt(PARTICLE_TEXTURES.length)];

        return new WorldParticle(px, py, pz, motionX, motionY, motionZ, size, alpha, roll, rollSpeed, texture);
    }

    private void tickParticle(WorldParticle particle, float speedMul) {
        particle.prevX = particle.x;
        particle.prevY = particle.y;
        particle.prevZ = particle.z;
        particle.prevRoll = particle.roll;

        particle.x += particle.motionX * speedMul;
        particle.y += particle.motionY * speedMul;
        particle.z += particle.motionZ * speedMul;
        particle.roll += particle.rollSpeed * speedMul;

        Vector3d playerPos = mc.player.getPositionVec();
        double dx = particle.x - playerPos.x;
        double dy = particle.y - playerPos.y;
        double dz = particle.z - playerPos.z;
        double distSq = dx * dx + dy * dy + dz * dz;

        boolean outOfRange = distSq > (RESET_DISTANCE * RESET_DISTANCE);
        boolean outOfHeight = particle.y < playerPos.y - 6.0D || particle.y > playerPos.y + 10.0D;
        if (outOfRange || outOfHeight) {
            resetParticle(particle);
        }
    }

    private void resetParticle(WorldParticle particle) {
        WorldParticle fresh = createParticle();
        particle.prevX = fresh.x;
        particle.prevY = fresh.y;
        particle.prevZ = fresh.z;
        particle.x = fresh.x;
        particle.y = fresh.y;
        particle.z = fresh.z;
        particle.motionX = fresh.motionX;
        particle.motionY = fresh.motionY;
        particle.motionZ = fresh.motionZ;
        particle.size = fresh.size;
        particle.alpha = fresh.alpha;
        particle.prevRoll = fresh.roll;
        particle.roll = fresh.roll;
        particle.rollSpeed = fresh.rollSpeed;
        particle.texture = fresh.texture;
    }

    private void addVertex(MatrixStack.Entry entry, IVertexBuilder buffer,
                           float x, float y, float z, float u, float v, int alpha) {
        buffer.pos(entry.getMatrix(), x, y, z)
                .color(255, 255, 255, alpha)
                .tex(u, v)
                .overlay(OverlayTexture.NO_OVERLAY)
                .lightmap(15728880)
                .normal(entry.getNormal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    private static final class WorldParticle {
        private double prevX;
        private double prevY;
        private double prevZ;
        private double x;
        private double y;
        private double z;
        private double motionX;
        private double motionY;
        private double motionZ;
        private float size;
        private float alpha;
        private float prevRoll;
        private float roll;
        private float rollSpeed;
        private ResourceLocation texture;

        private WorldParticle(double x, double y, double z,
                              double motionX, double motionY, double motionZ,
                              float size, float alpha, float roll, float rollSpeed,
                              ResourceLocation texture) {
            this.prevX = x;
            this.prevY = y;
            this.prevZ = z;
            this.x = x;
            this.y = y;
            this.z = z;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;
            this.size = size;
            this.alpha = alpha;
            this.prevRoll = roll;
            this.roll = roll;
            this.rollSpeed = rollSpeed;
            this.texture = texture;
        }
    }
}
