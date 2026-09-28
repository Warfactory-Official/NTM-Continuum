package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityCableBox;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The 1.7 {@code PowerCableBox} ("Red Copper Boxcable"): the box duct's power twin. 1.7 kept the thickness in
 * metadata 0..4 (each step one pixel thinner per side) behind one {@code red_cable_box} item; the port flattens
 * it the way {@link FluidDuctBox} was flattened, to {@code red_cable_box_<size>}.
 *
 * <p>Conduction, the connection flags and the graph cell are {@link BlockCable}'s, exactly like 1.7's
 * {@code TileEntityCableBaseNT} + {@code Library.canConnect}. Unlike the fluid box there is no wider junction:
 * 1.7's {@code RenderBoxDuct} moved the power junction bounds with the duct ({@code jLower = lower}).
 */
public class PowerCableBox extends BlockCable {

    /** 0..4, the 1.7 metadata. */
    public final int size;

    public PowerCableBox(Properties properties, int size) {
        super(properties);
        this.size = size;
    }

    /** The 1.7 {@code lower}: 0.125 plus one pixel for every metadata step. */
    public float lower() {
        return 0.125F + size * 0.0625F;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCableBox(pos, state);
    }

    /** 1.7 {@code addCollisionBoxesToList}: one long box for a straight run, otherwise the core plus an arm per side. */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = connectionMask(state);
        float lower = lower(), upper = 1F - lower;
        if (mask == 0) return Shapes.box(lower, lower, lower, upper, upper, upper);
        if (mask == 0b100000 || mask == 0b010000 || mask == 0b110000) return Shapes.box(0, lower, lower, 1, upper, upper);
        if (mask == 0b001000 || mask == 0b000100 || mask == 0b001100) return Shapes.box(lower, 0, lower, upper, 1, upper);
        if (mask == 0b000010 || mask == 0b000001 || mask == 0b000011) return Shapes.box(lower, lower, 0, upper, upper, 1);

        VoxelShape shape = Shapes.box(lower, lower, lower, upper, upper, upper);
        if (state.getValue(EAST)) shape = Shapes.or(shape, Shapes.box(upper, lower, lower, 1, upper, upper));
        if (state.getValue(WEST)) shape = Shapes.or(shape, Shapes.box(0, lower, lower, lower, upper, upper));
        if (state.getValue(UP)) shape = Shapes.or(shape, Shapes.box(lower, upper, lower, upper, 1, upper));
        if (state.getValue(DOWN)) shape = Shapes.or(shape, Shapes.box(lower, 0, lower, upper, lower, upper));
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, Shapes.box(lower, lower, upper, upper, upper, 1));
        if (state.getValue(NORTH)) shape = Shapes.or(shape, Shapes.box(lower, lower, 0, upper, upper, lower));
        return shape;
    }

    /** The six connection flags as the 1.7 pX/nX/pY/nY/pZ/nZ mask. */
    public static int connectionMask(BlockState state) {
        return (state.getValue(EAST) ? 32 : 0) | (state.getValue(WEST) ? 16 : 0)
                | (state.getValue(UP) ? 8 : 0) | (state.getValue(DOWN) ? 4 : 0)
                | (state.getValue(SOUTH) ? 2 : 0) | (state.getValue(NORTH) ? 1 : 0);
    }
}
