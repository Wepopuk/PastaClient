package fun.pasta.event;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

public class PlaceObsidianEvent {

    private final Block block;
    private final BlockPos pos;

    public PlaceObsidianEvent(Block block, BlockPos pos) {
        this.block = block;
        this.pos = pos;
    }

    public Block getBlock() {
        return block;
    }

    public BlockPos getPos() {
        return pos;
    }
}
