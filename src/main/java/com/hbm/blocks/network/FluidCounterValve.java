package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityFluidCounterValve;
import com.hbm.interfaces.ILookOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The 1.7 {@code FluidCounterValve}: a hand valve that counts what flows through its net while open
 * (see {@link TileEntityFluidCounterValve}). The look overlay adds the count.
 */
public class FluidCounterValve extends FluidValve {

    public FluidCounterValve(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFluidCounterValve(pos, state);
    }

    /** 1.7's FluidCounterValve extends FluidDuctBase and has no onBlockActivated: it is opened only via ROR/OC setstate. */
    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityFluidCounterValve valve)) return null;
        List<String> text = new ArrayList<>();
        text.add("&[" + valve.getFluidType().getColor() + "&]" + valve.getFluidType().getDisplayNameString());
        text.add("Counter: " + valve.getCounter());
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
