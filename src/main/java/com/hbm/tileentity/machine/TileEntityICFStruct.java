package com.hbm.tileentity.machine;

import com.hbm.blocks.ModIcfBlocks;
import com.hbm.blocks.machine.BlockICFStruct;
import com.hbm.blocks.machine.struct.StructPattern;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModIcfBlockEntities;
import com.hbm.tileentity.machine.struct.TileEntityStructBase;
import com.hbm.util.TickPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.function.Supplier;

/**
 * 1.7 {@code TileEntityICFStruct}: once a second, checks for the whole vessel around the seed and, when it is all
 * there, becomes the ICF facing the seed's own direction.
 *
 * <p>1.7's {@code cbarp(block, meta, widthwise, y, lengthwise, dir)} ladder is the {@link StructPattern} here,
 * one per facing: lengthwise runs along {@code dir.getRotation(UP)} from -8 to 8, widthwise along {@code dir}. Per
 * slice: three stability elements (meta 0) across the ground row (the seed itself is the gap in the middle row),
 * a welded vessel wall three up the middle, and the five-row hull from y 1 to 5 around it: welded vessel walls
 * (meta 2) within two slices of the middle, riveted casing (meta 4) outside that. The ghost preview reads the
 * same pattern ({@code RenderICFMultiblock} hard-coded the same ladder).
 */
public class TileEntityICFStruct extends TileEntityStructBase {

    private static final EnumMap<Direction, StructPattern> PATTERNS = new EnumMap<>(Direction.class);

    public TileEntityICFStruct(BlockPos pos, BlockState state) {
        super(ModIcfBlockEntities.ICF_STRUCT.get(), pos, state);
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockICFStruct.FACING) ? state.getValue(BlockICFStruct.FACING) : Direction.NORTH;
    }

    @Override
    public StructPattern pattern() {
        return pattern(facing());
    }

    public static synchronized StructPattern pattern(Direction facing) {
        return PATTERNS.computeIfAbsent(facing, TileEntityICFStruct::build);
    }

    private static StructPattern build(Direction facing) {
        ForgeDirection dir = ForgeDirection.getOrientation(facing);
        StructPattern.Builder b = StructPattern.builder();
        for (int i = -8; i <= 8; i++) {
            cbarp(b, dir, 0, 1, 0, i);
            if (i != 0) cbarp(b, dir, 0, 0, 0, i);
            cbarp(b, dir, 0, -1, 0, i);
            cbarp(b, dir, 2, 0, 3, i);

            int hull = Math.abs(i) <= 2 ? 2 : 4;
            for (int j = -1; j <= 1; j++) cbarp(b, dir, hull, j, 1, i);
            for (int j = -2; j <= 2; j++) cbarp(b, dir, hull, j, 2, i);
            for (int j = -2; j <= 2; j++) if (j != 0) cbarp(b, dir, hull, j, 3, i);
            for (int j = -2; j <= 2; j++) cbarp(b, dir, hull, j, 4, i);
            for (int j = -1; j <= 1; j++) cbarp(b, dir, hull, j, 5, i);
        }
        return b.build();
    }

    /** 1.7 {@code cbarp}: "check block at relative position", here "add". */
    private static void cbarp(StructPattern.Builder b, ForgeDirection dir, int meta, int widthwiseOffset, int y,
                              int lengthwiseOffset) {
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int ix = rot.offsetX * lengthwiseOffset + dir.offsetX * widthwiseOffset;
        int iz = rot.offsetZ * lengthwiseOffset + dir.offsetZ * widthwiseOffset;
        b.add(ix, y, iz, component(meta), texture(meta));
    }

    private static Supplier<Block> component(int meta) {
        return switch (meta) {
            case 0 -> () -> ModIcfBlocks.icf_component.get();
            case 2 -> () -> ModIcfBlocks.icf_component_vessel_welded.get();
            default -> () -> ModIcfBlocks.icf_component_structure_bolted.get();
        };
    }

    private static String texture(int meta) {
        return switch (meta) {
            case 0 -> "icf_component";
            case 2 -> "icf_component.vessel_welded";
            default -> "icf_component.structure_bolted";
        };
    }

    @Override
    protected BlockMultiblockCore target() {
        return ModIcfBlocks.icf.get();
    }

    /**
     * 1.7: the seed becomes the ICF core facing the seed's direction and fills the reactor's space from it, over
     * the components just checked.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityICFStruct te) {
        if (!TickPhase.every(level, pos, 20)) return;
        if (!te.pattern().matches(level, pos)) return;

        Direction facing = te.facing();
        BlockMultiblockCore machine = te.target();
        BlockMultiblockCore.withoutTeardown(() -> MultiblockHandlerXR.placeAssembled(level, pos, machine, facing));
        // `te` is gone: the setBlock above destroyed this BlockEntity.
    }
}
