package com.hbm.blocks.machine.struct;

import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.TickingBlock;
import com.hbm.tileentity.machine.struct.TileEntityStructBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The seed block of a hand-built multiblock: place it, build the machine's shape around it out of
 * component blocks, and its {@link TileEntityStructBase} scans once a second until the shape matches —
 * at which point it replaces itself with the finished {@link com.hbm.blocks.BlockDummyable} core and
 * fills in the dummy shell. Port of 1.12.2's {@code BlockStruct}.
 *
 * <p>Upstream's comment still applies: <b>this is not direction sensitive</b>. Assembly always forms
 * the structure facing {@link com.hbm.lib.ForgeDirection#NORTH}, so where you stand when you place the
 * seed makes no difference to the finished machine's facing.
 *
 * <p>It renders as an ordinary block model (it has a real texture — the player needs to see where the
 * seed is), unlike the invisible dummyable cells it eventually becomes.
 */
public class BlockStruct extends dev.golem.core.world.CompatBlock implements TickingBlock {

    private final BlockEntities.Factory<?> factory;
    private final java.util.function.Supplier<BlockEntityType<?>> type;

    public BlockStruct(Properties properties, BlockEntities.Factory<?> factory,
                       java.util.function.Supplier<BlockEntityType<?>> type) {
        super(properties);
        this.factory = factory;
        this.type = type;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.create(pos, state);
    }

}
