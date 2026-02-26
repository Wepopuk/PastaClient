package fun.pasta.module.impl.visual;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.pasta.Pasta;
import fun.pasta.event.EventRender3D;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.module.settings.ModeOption;
import fun.pasta.module.settings.SliderOption;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class TargetESP extends Module {

    private static final ResourceLocation MARKER = new ResourceLocation("pasta/images/icons/marker.png");
    private static final ResourceLocation GHOST_SPRITE = new ResourceLocation("pasta/images/icons/glow.png");
    private static final int HOLD_TICKS_AFTER_LOST = 100;
    private static final int SIDE_SWAP_TICKS = 60;
    private static final int SHATTER_TICKS = 14;
    private static final float MAX_SWING_ANGLE = 30.0F;
    private static final float PICTURE_SIZE_MULT = 1.35F;

    private final SliderOption size = new SliderOption("Size", 42.0F, 14.0F, 100.0F, 1.0F);
    private final BooleanOption throughWalls = new BooleanOption("Through Walls", true);
    private final ModeOption mode = new ModeOption("Mode", "Picture", "Picture", "Ghosts");
    private final SliderOption ghostSize = new SliderOption("Ghost Size", 0.22F, 0.10F, 0.50F, 0.01F);
    private final SliderOption ghostLength = new SliderOption("Ghost Length", 6.0F, 2.0F, 16.0F, 0.5F);
    private final SliderOption ghostSpeed = new SliderOption("Ghost Speed", 12.0F, 2.0F, 24.0F, 0.5F);
    private final SliderOption ghostAmplitude = new SliderOption("Ghost Amplitude", 3.0F, 1.0F, 6.0F, 0.1F);
    private final SliderOption ghostDensity = new SliderOption("Ghost Density", 1.7F, 1.0F, 3.0F, 0.1F);

    private LivingEntity target;
    private float alpha;
    private float ghostAlpha;
    private double ghostPrevX;
    private double ghostPrevY;
    private double ghostPrevZ;
    private double ghostX;
    private double ghostY;
    private double ghostZ;
    private double ghostVelX;
    private double ghostVelY;
    private double ghostVelZ;
    private float ghostWidth = 0.6F;
    private float ghostAge;
    private int ghostHurtTime;
    private int ghostHoldTicks;
    private boolean ghostHasSnapshot;
    private int holdTicks;
    private float swingTick;
    private int shatterTicks;
    private boolean wasVisible;
    private double shatterX;
    private double shatterY;
    private double shatterZ;
    private int shatterSeed;
    private boolean ghostPositionInitialized;

    public TargetESP() {
        super("TargetESP", Category.VISUAL);
        add(size, throughWalls, mode,
                ghostSize.setVisible(() -> mode.get().equalsIgnoreCase("Ghosts")),
                ghostLength.setVisible(() -> mode.get().equalsIgnoreCase("Ghosts")),
                ghostSpeed.setVisible(() -> mode.get().equalsIgnoreCase("Ghosts")),
                ghostAmplitude.setVisible(() -> mode.get().equalsIgnoreCase("Ghosts")),
                ghostDensity.setVisible(() -> mode.get().equalsIgnoreCase("Ghosts")));
    }

    @Override
    public void onDisable() {
        super.onDisable();
        clearTrackingState();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!enable || mc.player == null || mc.world == null) {
            return;
        }
        if (!isKillAuraEnabled()) {
            clearTrackingState();
            return;
        }

        LivingEntity currentTarget = getActiveTarget();
        if (target != null && !target.isAlive()) {
            if (mode.get().equalsIgnoreCase("Ghosts")) {
                snapshotGhost(target);
                ghostHoldTicks = 0;
            } else {
                cacheShatterData(target);
                shatterTicks = SHATTER_TICKS;
            }
            target = null;
            holdTicks = 0;
        }

        if (currentTarget != null) {
            target = currentTarget;
            holdTicks = HOLD_TICKS_AFTER_LOST;
            ghostHoldTicks = HOLD_TICKS_AFTER_LOST;
            ghostHasSnapshot = true;
        } else if (holdTicks > 0 && target != null && target.isAlive()) {
            holdTicks--;
        } else {
            if (target != null && !mode.get().equalsIgnoreCase("Ghosts")) {
                cacheShatterData(target);
                shatterTicks = SHATTER_TICKS;
            }
            target = null;
            holdTicks = 0;
        }

        boolean ghostsMode = mode.get().equalsIgnoreCase("Ghosts");
        boolean visible = target != null && target.isAlive();

        if (!visible && wasVisible && !ghostsMode) {
            shatterTicks = SHATTER_TICKS;
        }
        wasVisible = visible;

        if (visible) {
            shatterTicks = 0;
            cacheShatterData(target);
        } else if (shatterTicks > 0) {
            shatterTicks--;
        }
        if (ghostsMode) {
            shatterTicks = 0;
        }

        alpha = MathHelper.lerp(0.16F, alpha, visible ? 1.0F : 0.0F);
        swingTick += 1.0F;
        if (swingTick >= SIDE_SWAP_TICKS * 2.0F) {
            swingTick -= SIDE_SWAP_TICKS * 2.0F;
        }

        updateGhostState(currentTarget);
    }

    @Subscribe
    public void onRender3D(EventRender3D event) {
        if (!enable || mc.world == null || mc.player == null) {
            return;
        }
        if (!isKillAuraEnabled()) {
            return;
        }

        if (mode.get().equalsIgnoreCase("Ghosts")) {
            if (!ghostHasSnapshot || ghostAlpha <= 0.01F) {
                return;
            }
            renderGhosts(event.matrixStack, event, event.partialTicks);
            return;
        }

        renderPictureMode(event.matrixStack, event, event.partialTicks);
    }

    private void renderPictureMode(MatrixStack matrices, EventRender3D event, float partialTicks) {
        if (target == null && shatterTicks <= 0) {
            return;
        }

        double x;
        double y;
        double z;
        int colorSeed;
        float entityAge;

        if (target != null && target.isAlive()) {
            x = MathHelper.lerp(partialTicks, target.lastTickPosX, target.getPosX());
            y = MathHelper.lerp(partialTicks, target.lastTickPosY, target.getPosY()) + target.getHeight() * 0.5D;
            z = MathHelper.lerp(partialTicks, target.lastTickPosZ, target.getPosZ());
            colorSeed = target.getEntityId();
            entityAge = target.ticksExisted + partialTicks;
        } else {
            x = shatterX;
            y = shatterY;
            z = shatterZ;
            colorSeed = shatterSeed;
            entityAge = mc.player.ticksExisted + partialTicks;
        }

        matrices.push();
        try {
            matrices.translate(x - event.camX, y - event.camY, z - event.camZ);
            matrices.rotate(event.activeRenderInfo.getRotation());

            float phase = (swingTick + partialTicks) / (SIDE_SWAP_TICKS * 2.0F);
            float swing = MathHelper.sin((float) (phase * Math.PI * 2.0D)) * MAX_SWING_ANGLE;
            matrices.rotate(Vector3f.ZP.rotationDegrees(swing));

            float quadScale = (size.getValue().floatValue() / 120.0F) * PICTURE_SIZE_MULT;
            matrices.scale(-quadScale, -quadScale, quadScale);

            int baseAlpha = MathHelper.clamp((int) (alpha * 255.0F), 0, 255);
            float fade = target != null ? 1.0F : shatterTicks / (float) SHATTER_TICKS;
            int finalAlpha = target != null
                    ? baseAlpha
                    : MathHelper.clamp((int) (255.0F * fade), 0, 255);
            if (finalAlpha <= 0) {
                return;
            }

            Color color = new Color(255, 255, 255, finalAlpha);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            if (throughWalls.get()) {
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
            }

            mc.getTextureManager().bindTexture(MARKER);
            Matrix4f model = matrices.getLast().getMatrix();

            if (target == null && shatterTicks > 0) {
                float shatterProgress = 1.0F - (shatterTicks / (float) SHATTER_TICKS);
                drawShatter(model, color.getRed(), color.getGreen(), color.getBlue(), shatterProgress);
            } else {
                drawQuad(model, -1.0F, -1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F,
                        color.getRed(), color.getGreen(), color.getBlue(), finalAlpha);
            }

            if (throughWalls.get()) {
                RenderSystem.depthMask(true);
                RenderSystem.enableDepthTest();
            }
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        } finally {
            matrices.pop();
        }
    }

    private void renderGhosts(MatrixStack matrices, EventRender3D event, float partialTicks) {
        double x = MathHelper.lerp(partialTicks, ghostPrevX, ghostX) - event.camX;
        double y = MathHelper.lerp(partialTicks, ghostPrevY, ghostY) - event.camY;
        double z = MathHelper.lerp(partialTicks, ghostPrevZ, ghostZ) - event.camZ;
        float age = ghostAge + partialTicks;

        float trailLength = ghostLength.getValue().floatValue();
        float rotationFactor = ghostSpeed.getValue().floatValue();
        float amplitude = ghostAmplitude.getValue().floatValue();
        float density = ghostDensity.getValue().floatValue();
        float spriteScale = ghostSize.getValue().floatValue() * 1.45F;
        int particleCount = Math.max(1, (int) (trailLength * density));

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableCull();
        RenderSystem.enableAlphaTest();
        if (throughWalls.get()) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
        }

        mc.getTextureManager().bindTexture(GHOST_SPRITE);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        float[] baseAngles = new float[]{45.0F, 135.0F, 225.0F, 315.0F};
        float time = (System.currentTimeMillis() % 6000L) / 6000.0F;
        float phaseShift = (float) (2.0D * Math.PI / 4.0D);

        for (int arm = 0; arm < 4; arm++) {
            float wave = (float) (Math.sin(2.0D * Math.PI * time + arm * phaseShift) * 0.5D + 0.5D);
            int baseWhite = MathHelper.clamp((int) (220 + 35 * wave), 220, 255);
            int rr = baseWhite;
            int gg = baseWhite;
            int bb = baseWhite;
            if (ghostHurtTime > 0) {
                rr = 255;
                gg = MathHelper.clamp((int) (90 + 40 * wave), 90, 130);
                bb = MathHelper.clamp((int) (90 + 40 * wave), 90, 130);
            }

            for (int i = 0; i < particleCount; i++) {
                float offset = i / (float) particleCount;
                double angle = Math.toRadians((baseAngles[arm] + (offset * trailLength + age) * rotationFactor) % 360.0F);
                double verticalWave = Math.sin(
                        Math.toRadians(age * 2.5F + offset * trailLength * 2.0F + arm * 90.0F) * amplitude
                ) / 1.32D;

                float trailFade = 1.0F - offset * 0.35F;
                float alphaEase = easeInOut(ghostAlpha);
                int a = MathHelper.clamp((int) (255.0F * trailFade * alphaEase), 24, 255);

                double px = x + Math.cos(angle) * ghostWidth;
                double py = y + 1.0D + verticalWave;
                double pz = z + Math.sin(angle) * ghostWidth;
                drawGhostQuad(buffer, matrices, event, px, py, pz, spriteScale, rr, gg, bb, a);
            }
        }

        buffer.finishDrawing();
        WorldVertexBufferUploader.draw(buffer);

        if (throughWalls.get()) {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
        RenderSystem.enableCull();
        RenderSystem.disableAlphaTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void updateGhostState(LivingEntity aimedTarget) {
        if (!mode.get().equalsIgnoreCase("Ghosts")) {
            ghostAlpha = MathHelper.lerp(0.18F, ghostAlpha, 0.0F);
            return;
        }

        boolean updatedFromEntity = false;
        LivingEntity follow = null;

        if (target != null && target.isAlive()) {
            follow = target;
        } else if (aimedTarget != null && aimedTarget.isAlive()) {
            follow = aimedTarget;
        }

        if (follow != null) {
            snapshotGhost(follow);
            updatedFromEntity = true;
        }

        if (ghostHoldTicks > 0) {
            ghostHoldTicks--;
        }

        if (!updatedFromEntity && ghostHasSnapshot && ghostHoldTicks > 0) {
            ghostPrevX = ghostX;
            ghostPrevY = ghostY;
            ghostPrevZ = ghostZ;

            ghostX += ghostVelX;
            ghostY += ghostVelY;
            ghostZ += ghostVelZ;

            ghostVelX *= 0.92D;
            ghostVelZ *= 0.92D;
            ghostVelY = (ghostVelY - 0.015D) * 0.90D;
            ghostAge += 1.0F;
        }

        boolean hasTarget = target != null && target.isAlive();
        boolean shouldShow = hasTarget || ghostHoldTicks > 0;
        ghostAlpha = MathHelper.lerp(0.17F, ghostAlpha, shouldShow ? 1.0F : 0.0F);

        if (!shouldShow && ghostAlpha <= 0.01F) {
            ghostHasSnapshot = false;
            ghostPositionInitialized = false;
        }
    }

    private void snapshotGhost(LivingEntity source) {
        Vector3d velocity = source.getMotion();
        double targetX = source.getPosX() + velocity.x * 0.35D;
        double targetY = source.getPosY() + velocity.y * 0.25D;
        double targetZ = source.getPosZ() + velocity.z * 0.35D;

        if (!ghostPositionInitialized) {
            ghostX = targetX;
            ghostY = targetY;
            ghostZ = targetZ;
            ghostPrevX = ghostX;
            ghostPrevY = ghostY;
            ghostPrevZ = ghostZ;
            ghostPositionInitialized = true;
        } else {
            ghostPrevX = ghostX;
            ghostPrevY = ghostY;
            ghostPrevZ = ghostZ;

            double dx = targetX - ghostX;
            double dy = targetY - ghostY;
            double dz = targetZ - ghostZ;
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq > 25.0D) {
                ghostX = targetX;
                ghostY = targetY;
                ghostZ = targetZ;
            } else {
                ghostX += dx * 0.42D;
                ghostY += dy * 0.42D;
                ghostZ += dz * 0.42D;
            }
        }

        ghostVelX = MathHelper.lerp(0.35F, (float) ghostVelX, (float) velocity.x);
        ghostVelY = MathHelper.lerp(0.35F, (float) ghostVelY, (float) velocity.y);
        ghostVelZ = MathHelper.lerp(0.35F, (float) ghostVelZ, (float) velocity.z);
        ghostWidth = source.getWidth();
        ghostAge = source.ticksExisted;
        ghostHurtTime = source.hurtTime;
        ghostHasSnapshot = true;
    }

    private LivingEntity getActiveTarget() {
        if (!isKillAuraEnabled()) {
            return null;
        }
        LivingEntity auraTarget = Pasta.instance.manager.killAura.target;
        if (isValidTarget(auraTarget)) {
            return auraTarget;
        }
        return null;
    }

    private boolean isKillAuraEnabled() {
        return Pasta.instance != null
                && Pasta.instance.manager != null
                && Pasta.instance.manager.killAura != null
                && Pasta.instance.manager.killAura.enable;
    }

    private void clearTrackingState() {
        target = null;
        holdTicks = 0;
        ghostHoldTicks = 0;
        ghostHasSnapshot = false;
        ghostPositionInitialized = false;
        alpha = 0.0F;
        ghostAlpha = 0.0F;
        shatterTicks = 0;
        wasVisible = false;
    }

    private boolean isValidTarget(LivingEntity entity) {
        return entity != null
                && entity != mc.player
                && entity.isAlive()
                && !entity.removed;
    }

    private void cacheShatterData(LivingEntity entity) {
        shatterX = entity.getPosX();
        shatterY = entity.getPosY() + entity.getHeight() * 0.5D;
        shatterZ = entity.getPosZ();
        shatterSeed = entity.getEntityId();
    }

    private float easeInOut(float t) {
        float x = MathHelper.clamp(t, 0.0F, 1.0F);
        if (x < 0.5F) {
            return 4.0F * x * x * x;
        }
        float p = -2.0F * x + 2.0F;
        return 1.0F - (p * p * p) * 0.5F;
    }

    private void drawGhostQuad(BufferBuilder buffer, MatrixStack matrices, EventRender3D event,
                               double x, double y, double z, float scale,
                               int r, int g, int b, int a) {
        matrices.push();
        try {
            matrices.translate(x, y, z);
            matrices.rotate(event.activeRenderInfo.getRotation());
            matrices.scale(-scale, -scale, scale);

            Matrix4f model = matrices.getLast().getMatrix();
            buffer.pos(model, -1.0F, -1.0F, 0.0F).tex(0.0F, 1.0F).color(r, g, b, a).endVertex();
            buffer.pos(model, 1.0F, -1.0F, 0.0F).tex(1.0F, 1.0F).color(r, g, b, a).endVertex();
            buffer.pos(model, 1.0F, 1.0F, 0.0F).tex(1.0F, 0.0F).color(r, g, b, a).endVertex();
            buffer.pos(model, -1.0F, 1.0F, 0.0F).tex(0.0F, 0.0F).color(r, g, b, a).endVertex();
        } finally {
            matrices.pop();
        }
    }

    private void drawShatter(Matrix4f model, int r, int g, int b, float progress) {
        int alpha = MathHelper.clamp((int) ((1.0F - progress) * 255.0F), 0, 255);
        float dark = 1.0F - progress * 0.92F;
        int rr = MathHelper.clamp((int) (r * dark), 0, 255);
        int gg = MathHelper.clamp((int) (g * dark), 0, 255);
        int bb = MathHelper.clamp((int) (b * dark), 0, 255);
        if (alpha <= 0) {
            return;
        }

        drawShardTriangle(model, 0.50F, 0.50F, 0.00F, 0.00F, 1.00F, 0.00F, 1.35F, 0, rr, gg, bb, alpha, progress);
        drawShardTriangle(model, 0.50F, 0.50F, 1.00F, 0.00F, 1.00F, 1.00F, 1.30F, 1, rr, gg, bb, alpha, progress);
        drawShardTriangle(model, 0.50F, 0.50F, 1.00F, 1.00F, 0.00F, 1.00F, 1.42F, 2, rr, gg, bb, alpha, progress);
        drawShardTriangle(model, 0.50F, 0.50F, 0.00F, 1.00F, 0.00F, 0.00F, 1.26F, 3, rr, gg, bb, alpha, progress);

        drawShardTriangle(model, 0.50F, 0.00F, 0.30F, 0.22F, 0.70F, 0.22F, 0.55F, 4, rr, gg, bb, alpha, progress);
        drawShardTriangle(model, 0.70F, 0.22F, 0.88F, 0.14F, 0.86F, 0.36F, 0.60F, 5, rr, gg, bb, alpha, progress);
        drawShardTriangle(model, 0.86F, 0.36F, 1.00F, 0.50F, 0.78F, 0.52F, 0.52F, 6, rr, gg, bb, alpha, progress);
        drawShardTriangle(model, 0.78F, 0.52F, 0.92F, 0.78F, 0.72F, 0.70F, 0.58F, 7, rr, gg, bb, alpha, progress);
        drawShardTriangle(model, 0.72F, 0.70F, 0.50F, 1.00F, 0.56F, 0.74F, 0.54F, 8, rr, gg, bb, alpha, progress);
        drawShardTriangle(model, 0.56F, 0.74F, 0.34F, 0.90F, 0.44F, 0.62F, 0.50F, 9, rr, gg, bb, alpha, progress);
        drawShardTriangle(model, 0.44F, 0.62F, 0.16F, 0.84F, 0.24F, 0.56F, 0.56F, 10, rr, gg, bb, alpha, progress);
        drawShardTriangle(model, 0.24F, 0.56F, 0.00F, 0.50F, 0.20F, 0.46F, 0.52F, 11, rr, gg, bb, alpha, progress);
        drawShardTriangle(model, 0.20F, 0.46F, 0.10F, 0.22F, 0.30F, 0.30F, 0.59F, 12, rr, gg, bb, alpha, progress);
        drawShardTriangle(model, 0.30F, 0.30F, 0.12F, 0.14F, 0.36F, 0.20F, 0.53F, 13, rr, gg, bb, alpha, progress);
        drawShardTriangle(model, 0.36F, 0.20F, 0.28F, 0.04F, 0.50F, 0.00F, 0.48F, 14, rr, gg, bb, alpha, progress);
        drawShardTriangle(model, 0.50F, 0.00F, 0.64F, 0.18F, 0.36F, 0.20F, 0.46F, 15, rr, gg, bb, alpha, progress);
    }

    private void drawShardTriangle(Matrix4f model,
                                   float u0, float v0,
                                   float u1, float v1,
                                   float u2, float v2,
                                   float force, int seed,
                                   int r, int g, int b, int alpha,
                                   float progress) {
        float cx = ((u0 + u1 + u2) / 3.0F) * 2.0F - 1.0F;
        float cy = ((v0 + v1 + v2) / 3.0F) * 2.0F - 1.0F;
        float len = MathHelper.sqrt(cx * cx + cy * cy);
        float dx = len < 0.001F ? 0.0F : cx / len;
        float dy = len < 0.001F ? 1.0F : cy / len;

        float random = ((seed * 37) % 100) / 100.0F;
        float move = (0.22F + random * 0.25F) * force * progress;
        float drift = (0.02F + random * 0.04F) * progress;

        float ax = uvToX(u0) + dx * move + drift;
        float ay = uvToY(v0) + dy * move;
        float bx = uvToX(u1) + dx * move + drift;
        float by = uvToY(v1) + dy * move;
        float cxp = uvToX(u2) + dx * move + drift;
        float cyp = uvToY(v2) + dy * move;

        drawTriangle(model, ax, ay, bx, by, cxp, cyp, u0, v0, u1, v1, u2, v2, r, g, b, alpha);
    }

    private float uvToX(float u) {
        return -1.0F + 2.0F * u;
    }

    private float uvToY(float v) {
        return -1.0F + 2.0F * v;
    }

    private void drawTriangle(Matrix4f model,
                              float x0, float y0,
                              float x1, float y1,
                              float x2, float y2,
                              float u0, float v0,
                              float u1, float v1,
                              float u2, float v2,
                              int r, int g, int b, int a) {
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.pos(model, x0, y0, 0.0F).tex(u0, 1.0F - v0).color(r, g, b, a).endVertex();
        buffer.pos(model, x1, y1, 0.0F).tex(u1, 1.0F - v1).color(r, g, b, a).endVertex();
        buffer.pos(model, x2, y2, 0.0F).tex(u2, 1.0F - v2).color(r, g, b, a).endVertex();
        buffer.finishDrawing();
        WorldVertexBufferUploader.draw(buffer);
    }

    private void drawQuad(Matrix4f model,
                          float x0, float y0,
                          float x1, float y1,
                          float u0, float v0,
                          float u1, float v1,
                          int r, int g, int b, int a) {
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.pos(model, x0, y0, 0.0F).tex(u0, v1).color(r, g, b, a).endVertex();
        buffer.pos(model, x1, y0, 0.0F).tex(u1, v1).color(r, g, b, a).endVertex();
        buffer.pos(model, x1, y1, 0.0F).tex(u1, v0).color(r, g, b, a).endVertex();
        buffer.pos(model, x0, y1, 0.0F).tex(u0, v0).color(r, g, b, a).endVertex();
        buffer.finishDrawing();
        WorldVertexBufferUploader.draw(buffer);
    }
}
