package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityPipeExhaust;
import com.hbm.tileentity.network.TileEntityPipeExhaustPaintable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.Fluids;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The 1.7 {@code FluidDuctPaintableBlockExhaust}: the full-cube coated duct for the smoke fluids
 * (painted as {@link FluidDuctPaintable} is; carries all three smokes at once, see
 * {@link TileEntityPipeExhaust}).
 */
public class FluidDuctPaintableBlockExhaust extends FluidDuctPaintable {

    public FluidDuctPaintableBlockExhaust(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPipeExhaustPaintable(pos, state);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        List<String> text = new ArrayList<>();
        text.add(Fluids.SMOKE.getDisplayNameString());
        text.add(Fluids.SMOKE_LEADED.getDisplayNameString());
        text.add(Fluids.SMOKE_POISON.getDisplayNameString());
        return new ILookOverlay.Data(this.getName().getString(), 0xffff00, 0x404000, text);
    }
}
