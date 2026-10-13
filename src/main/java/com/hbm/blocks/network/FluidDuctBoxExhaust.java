package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityPipeExhaust;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * The 1.7 {@code FluidDuctBoxExhaust}: the boxduct in its exhaust skin, carrying all three smoke fluids
 * at once (see {@link TileEntityPipeExhaust}). Five sizes, flattened to {@code fluid_duct_exhaust_<size>}.
 */
public class FluidDuctBoxExhaust extends FluidDuctBox {

    public FluidDuctBoxExhaust(Properties properties, int size) {
        super(properties, -1, size);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPipeExhaust(pos, state);
    }

    /**
     * The parent probes the six neighbours with the duct's carried type, which at placement (no block
     * entity yet) reads as {@code NONE}; an exhaust duct is born carrying {@code SMOKE}, so probe with
     * that instead or a freshly placed exhaust never shows its arm toward the exhaust run beside it.
     */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        BlockPos pos = context.getClickedPos();
        for (Map.Entry<Direction, BooleanProperty> e : PROPERTY_BY_DIRECTION.entrySet()) {
            boolean connects = Library.canConnectFluid(context.getLevel(), pos.relative(e.getKey()),
                    ForgeDirection.getOrientation(e.getKey()), Fluids.SMOKE);
            state = state.setValue(e.getValue(), connects);
        }
        return state;
    }

    /** 1.7 listed all three smokes rather than the carried type. */
    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        List<String> text = new ArrayList<>();
        text.add(Fluids.SMOKE.getDisplayNameString());
        text.add(Fluids.SMOKE_LEADED.getDisplayNameString());
        text.add(Fluids.SMOKE_POISON.getDisplayNameString());
        return new ILookOverlay.Data(this.getName().getString(), 0xffff00, 0x404000, text);
    }
}
