package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityFluidDuctVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The 1.7 {@code FluidDuctBox}: the boxy "boxduct" in three materials (silver, copper, white) and five
 * sizes. 1.7 packed material and size into metadata ({@code meta % 3} = material, {@code meta / 3} =
 * size, 15 sub-blocks); the port flattens them to fifteen blocks, {@code fluid_duct_box_<material>_<size>}.
 * Networking is the plain duct's; the shape follows the 1.7 {@code setBlockBoundsBasedOnState}.
 */
public class FluidDuctBox extends BlockFluidDuct {

    public static final String[] MATERIALS = {"silver", "copper", "white"};

    /** 0 = silver, 1 = copper, 2 = white: the 1.7 {@code rectify(meta)} index. */
    public final int material;
    /** 0..4, the 1.7 {@code meta / 3} (each step shrinks the duct by a pixel per side). */
    public final int size;

    public FluidDuctBox(Properties properties, int material, int size) {
        super(properties);
        this.material = material;
        this.size = size;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFluidDuctVariant(pos, state);
    }

    /** The 1.7 duct half-width: 0.125 plus one pixel per size step. */
    public float lower() {
        return 0.125F + size * 0.0625F;
    }

    /** The 1.7 junction half-width: one pixel wider than the duct. */
    public float junctionLower() {
        return 0.0625F + size * 0.0625F;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean pX = state.getValue(EAST), nX = state.getValue(WEST);
        boolean pY = state.getValue(UP), nY = state.getValue(DOWN);
        boolean pZ = state.getValue(SOUTH), nZ = state.getValue(NORTH);
        int mask = (pX ? 32 : 0) | (nX ? 16 : 0) | (pY ? 8 : 0) | (nY ? 4 : 0) | (pZ ? 2 : 0) | (nZ ? 1 : 0);
        int count = Integer.bitCount(mask);
        float lower = lower(), upper = 1F - lower;
        float jLower = junctionLower(), jUpper = 1F - jLower;

        if (mask == 0) return box(jLower, jLower, jLower, jUpper, jUpper, jUpper);
        if (mask == 0b100000 || mask == 0b010000 || mask == 0b110000) return box(0F, lower, lower, 1F, upper, upper);
        if (mask == 0b001000 || mask == 0b000100 || mask == 0b001100) return box(lower, 0F, lower, upper, 1F, upper);
        if (mask == 0b000010 || mask == 0b000001 || mask == 0b000011) return box(lower, lower, 0F, upper, upper, 1F);

        float c = count != 2 ? jLower : lower;
        float cU = 1F - c;
        VoxelShape shape = box(c, c, c, cU, cU, cU);
        if (pX) shape = Shapes.or(shape, box(cU, lower, lower, 1F, upper, upper));
        if (nX) shape = Shapes.or(shape, box(0F, lower, lower, c, upper, upper));
        if (pY) shape = Shapes.or(shape, box(lower, cU, lower, upper, 1F, upper));
        if (nY) shape = Shapes.or(shape, box(lower, 0F, lower, upper, c, upper));
        if (pZ) shape = Shapes.or(shape, box(lower, lower, cU, upper, upper, 1F));
        if (nZ) shape = Shapes.or(shape, box(lower, lower, 0F, upper, upper, c));
        return shape;
    }

    private static VoxelShape box(float x0, float y0, float z0, float x1, float y1, float z1) {
        return Shapes.box(x0, y0, z0, x1, y1, z1);
    }

    /** The six connection flags as the 1.7 pX/nX/pY/nY/pZ/nZ mask. */
    public static int connectionMask(BlockState state) {
        return (state.getValue(EAST) ? 32 : 0) | (state.getValue(WEST) ? 16 : 0)
                | (state.getValue(UP) ? 8 : 0) | (state.getValue(DOWN) ? 4 : 0)
                | (state.getValue(SOUTH) ? 2 : 0) | (state.getValue(NORTH) ? 1 : 0);
    }

    public static boolean connected(BlockState state, Direction dir) {
        return state.getValue(PROPERTY_BY_DIRECTION.get(dir));
    }
}
