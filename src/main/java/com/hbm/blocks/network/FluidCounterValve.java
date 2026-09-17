package com.hbm.blocks.network;

import com.hbm.interfaces.ILookOverlay;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.network.TileEntityFluidCounterValve;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A valve that also totals what has gone through it. Ported from 1.12.2's {@code FluidCounterValve}.
 *
 * <p>Everything a plain {@link FluidValve} does it does too — the gate, the fluid typing, the
 * shift-click spread — so only the readout and the block entity differ.
 */
public class FluidCounterValve extends FluidValve {

    public FluidCounterValve(Properties properties) {
        super(properties);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFluidCounterValve(pos, state);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityFluidCounterValve valve)) return null;
        List<String> text = new ArrayList<>();
        text.add(state(level, pos) ? "§aOpen" : "§cClosed");
        text.add("&[" + valve.getFluidType().getColor() + "&]" + valve.getFluidType().getDisplayNameString());
        text.add(String.format(Locale.US, "%,d", valve.getCounter()) + " mB");
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    private static boolean state(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.hasProperty(OPEN) && state.getValue(OPEN);
    }
}
