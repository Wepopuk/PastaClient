package fun.pasta.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.module.settings.MultiBoxOption;
import net.minecraft.block.Block;
import net.minecraft.block.CropsBlock;
import net.minecraft.block.DeadBushBlock;
import net.minecraft.block.DoublePlantBlock;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.MushroomBlock;
import net.minecraft.block.NetherSproutsBlock;
import net.minecraft.block.TallGrassBlock;
import net.minecraft.client.settings.ParticleStatus;

public class Optimization extends Module {

    private final MultiBoxOption optimize = new MultiBoxOption("Optimize",
            new BooleanOption("Plants", true),
            new BooleanOption("Particles", true),
            new BooleanOption("Clouds", true),
            new BooleanOption("Sky", true),
            new BooleanOption("Entities", true));

    private boolean oldSky;
    private boolean oldCustomSky;
    private boolean oldEntityShadows;
    private ParticleStatus oldParticles;
    private boolean captured;

    public Optimization() {
        super("Optimization", Category.MISC);
        add(optimize);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        captureOld();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!enable || mc == null || mc.gameSettings == null) {
            return;
        }

        if (!captured) {
            captureOld();
        }

        if (optimize.get("Clouds")) {
            mc.gameSettings.ofSky = false;
        }
        if (optimize.get("Sky")) {
            mc.gameSettings.ofCustomSky = false;
        }
        if (optimize.get("Entities")) {
            mc.gameSettings.entityShadows = false;
        }
        if (optimize.get("Particles")) {
            mc.gameSettings.particles = ParticleStatus.MINIMAL;
        }
    }

    public boolean canRenderBlock(Block block) {
        if (!optimize.get("Plants")) {
            return true;
        }
        return !(block instanceof TallGrassBlock)
                && !(block instanceof FlowerBlock)
                && !(block instanceof DoublePlantBlock)
                && !(block instanceof DeadBushBlock)
                && !(block instanceof MushroomBlock)
                && !(block instanceof CropsBlock)
                && !(block instanceof NetherSproutsBlock);
    }

    @Override
    public void onDisable() {
        if (mc != null && mc.gameSettings != null && captured) {
            mc.gameSettings.ofSky = oldSky;
            mc.gameSettings.ofCustomSky = oldCustomSky;
            mc.gameSettings.entityShadows = oldEntityShadows;
            mc.gameSettings.particles = oldParticles;
        }
        super.onDisable();
    }

    private void captureOld() {
        if (mc == null || mc.gameSettings == null) {
            return;
        }
        oldSky = mc.gameSettings.ofSky;
        oldCustomSky = mc.gameSettings.ofCustomSky;
        oldEntityShadows = mc.gameSettings.entityShadows;
        oldParticles = mc.gameSettings.particles;
        captured = true;
    }
}
