package fun.pasta.module.impl.player;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventPacket;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.module.settings.MultiBoxOption;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.TrapDoorBlock;
import net.minecraft.network.play.client.CPlayerTryUseItemOnBlockPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;

public class NoInteract extends Module {

    private final BooleanOption allBlocks = new BooleanOption("All Blocks", false);
    private final MultiBoxOption ignoreInteract = new MultiBoxOption("Ignore",
            new BooleanOption("Chests", true),
            new BooleanOption("Doors", true),
            new BooleanOption("Buttons/Levers", true),
            new BooleanOption("Hoppers", true),
            new BooleanOption("Dispensers", true),
            new BooleanOption("Crafting", true),
            new BooleanOption("Trapdoors", true),
            new BooleanOption("Furnaces", true),
            new BooleanOption("Anvils", true),
            new BooleanOption("NoteBlocks", true))
            .setVisible(() -> !allBlocks.get());

    public NoInteract() {
        super("NoInteract", Category.PLAYER);
        add(allBlocks, ignoreInteract);
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (!enable || mc.player == null || mc.world == null) {
            return;
        }
        if (!event.isSend()) {
            return;
        }
        if (!(event.packet instanceof CPlayerTryUseItemOnBlockPacket)) {
            return;
        }

        if (allBlocks.get()) {
            event.cancel();
            return;
        }

        CPlayerTryUseItemOnBlockPacket packet = (CPlayerTryUseItemOnBlockPacket) event.packet;
        BlockRayTraceResult hit = packet.func_218794_c();
        if (hit == null) {
            return;
        }

        BlockPos pos = hit.getPos();
        Block block = mc.world.getBlockState(pos).getBlock();
        if (shouldCancel(block)) {
            event.cancel();
        }
    }

    private boolean shouldCancel(Block block) {
        if (ignoreInteract.get("Chests") && block instanceof ChestBlock) return true;
        if (ignoreInteract.get("Doors") && block instanceof DoorBlock) return true;
        if (ignoreInteract.get("Buttons/Levers") && block instanceof LeverBlock) return true;
        if (ignoreInteract.get("Hoppers") && block instanceof HopperBlock) return true;
        if (ignoreInteract.get("Dispensers") && block instanceof DispenserBlock) return true;
        if (ignoreInteract.get("Crafting") && block instanceof CraftingTableBlock) return true;
        if (ignoreInteract.get("Trapdoors") && block instanceof TrapDoorBlock) return true;
        if (ignoreInteract.get("Furnaces") && block instanceof AbstractFurnaceBlock) return true;
        if (ignoreInteract.get("Anvils") && block instanceof AnvilBlock) return true;
        if (ignoreInteract.get("NoteBlocks") && block instanceof NoteBlock) return true;
        return false;
    }
}

