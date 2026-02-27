package fun.pasta.module.impl.visual;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.pasta.event.EventRender3D;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.module.settings.MultiBoxOption;
import fun.pasta.ui.clickGui.theme.ThemeManager;
import fun.pasta.util.color.ColorUtil;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.item.BowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import org.lwjgl.opengl.GL11;

public class Trajectories extends Module {

    private final MultiBoxOption projectileSettings = new MultiBoxOption("Projectiles",
            new BooleanOption("Bow", true),
            new BooleanOption("EnderPearl", true),
            new BooleanOption("Snowball", true),
            new BooleanOption("Egg", true),
            new BooleanOption("Potion", true));

    public Trajectories() {
        super("Trajectories", Category.VISUAL);
        add(projectileSettings);
    }

    @Subscribe
    public void onRender3D(EventRender3D event) {
        if (!enable || mc.player == null || mc.world == null) {
            return;
        }

        ItemStack stack = mc.player.getHeldItemMainhand();
        if (stack.isEmpty()) {
            stack = mc.player.getHeldItemOffhand();
            if (stack.isEmpty()) {
                return;
            }
        }

        ProjectileType type = resolveType(stack.getItem());
        if (type == null || !isTypeEnabled(type)) {
            return;
        }

        Vector3d start = mc.player.getEyePosition(event.partialTicks);
        Vector3d look = mc.player.getLook(event.partialTicks);

        double velocity = type.baseVelocity;
        if (type == ProjectileType.BOW) {
            int useTicks = mc.player.getItemInUseMaxCount();
            float power = BowItem.getArrowVelocity(useTicks);
            if (power <= 0.1F) {
                power = 1.0F;
            }
            velocity = 3.0D * power;
        }

        Vector3d motion = look.scale(velocity);
        simulateAndRender(event, start, motion, type);
    }

    private void simulateAndRender(EventRender3D event, Vector3d start, Vector3d startMotion, ProjectileType type) {
        int color = ThemeManager.INSTANCE != null ? ThemeManager.INSTANCE.getThemeColor() : ColorUtil.defaultRed;
        int r = ColorUtil.getRed(color);
        int g = ColorUtil.getGreen(color);
        int b = ColorUtil.getBlue(color);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        GL11.glLineWidth(1.8F);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        Vector3d pos = start;
        Vector3d motion = startMotion;

        buffer.pos(pos.x - event.camX, pos.y - event.camY, pos.z - event.camZ).color(r, g, b, 255).endVertex();

        for (int i = 0; i < 160; i++) {
            Vector3d prev = pos;
            pos = pos.add(motion.scale(0.05D));
            motion = motion.scale(0.99D).add(0.0D, -type.gravity * 0.05D, 0.0D);

            RayTraceResult hit = mc.world.rayTraceBlocks(new RayTraceContext(
                    prev, pos,
                    RayTraceContext.BlockMode.COLLIDER,
                    RayTraceContext.FluidMode.ANY,
                    mc.player
            ));

            int alpha = Math.max(30, 255 - i);
            buffer.pos(pos.x - event.camX, pos.y - event.camY, pos.z - event.camZ).color(r, g, b, alpha).endVertex();

            if (hit.getType() != RayTraceResult.Type.MISS) {
                break;
            }

            Entity entityHit = findEntityHit(prev, pos);
            if (entityHit != null) {
                break;
            }
        }

        buffer.finishDrawing();
        WorldVertexBufferUploader.draw(buffer);

        GL11.glLineWidth(1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private Entity findEntityHit(Vector3d from, Vector3d to) {
        for (Entity entity : mc.world.getAllEntities()) {
            if (entity == mc.player || !entity.isAlive() || entity.isSpectator()) {
                continue;
            }
            if (entity.getBoundingBox().grow(0.2D).rayTrace(from, to).isPresent()) {
                return entity;
            }
        }
        return null;
    }

    private ProjectileType resolveType(Item item) {
        if (item instanceof BowItem) return ProjectileType.BOW;
        if (item instanceof EnderPearlItem) return ProjectileType.ENDER_PEARL;
        if (item instanceof SnowballItem) return ProjectileType.SNOWBALL;
        if (item instanceof EggItem) return ProjectileType.EGG;
        if (item instanceof PotionItem) return ProjectileType.POTION;
        return null;
    }

    private boolean isTypeEnabled(ProjectileType type) {
        switch (type) {
            case BOW:
                return projectileSettings.get("Bow");
            case ENDER_PEARL:
                return projectileSettings.get("EnderPearl");
            case SNOWBALL:
                return projectileSettings.get("Snowball");
            case EGG:
                return projectileSettings.get("Egg");
            case POTION:
                return projectileSettings.get("Potion");
            default:
                return false;
        }
    }

    private enum ProjectileType {
        BOW(3.0D, 0.05D),
        ENDER_PEARL(1.5D, 0.03D),
        SNOWBALL(1.5D, 0.03D),
        EGG(1.5D, 0.03D),
        POTION(0.9D, 0.05D);

        private final double baseVelocity;
        private final double gravity;

        ProjectileType(double baseVelocity, double gravity) {
            this.baseVelocity = baseVelocity;
            this.gravity = gravity;
        }
    }
}

