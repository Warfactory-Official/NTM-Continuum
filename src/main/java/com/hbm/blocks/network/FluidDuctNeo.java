package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityFluidDuctVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The 1.7 {@code FluidDuctStandard} ({@code fluid_duct_neo}), the only universal fluid duct 1.7 registers
 * (the 1.12.2 {@code fluid_duct_mk2} was merged into it): shape, connection rules, identifier typing and
 * look overlay, drawn in one of three skins. 1.7 kept the
 * skin as metadata 0/1/2 ({@code pipe_neo}, {@code pipe_silver}, {@code pipe_colored}); the port
 * flattens them to three blocks, each carrying its {@link #skin} for the renderer.
 */
public class FluidDuctNeo extends BlockFluidDuct {

    /** 1.7 icon index: 0 = the block's own texture, 1 = silver, 2 = colored. */
    public final int skin;

    public FluidDuctNeo(Properties properties, int skin) {
        super(properties);
        this.skin = skin;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFluidDuctVariant(pos, state);
    }
}
