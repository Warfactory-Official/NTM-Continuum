package com.hbm.blocks.machine.soyuz;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.soyuz.TileEntitySoyuzLauncher;
import net.minecraft.world.level.BlockGetter;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.soyuz.TileEntitySoyuzLauncher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The launch complex. It is never placed by hand: the struct core assembles it once the pad, its legs and
 * the service tower are built, and this block's footprint is the whole thirteen-by-thirteen apron plus the
 * towers, filled in one pass from the same boxes upstream uses.
 */
public class SoyuzLauncher extends BlockMultiblockMachine
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    /** How far above the struct core the launcher's own core sits. */
    public static final int HEIGHT = 4;

    public SoyuzLauncher(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntitySoyuzLauncher(pos, state);
    }

    /** The seven footprint boxes the complex is assembled from, in upstream's order. */
    private static final int[][] BOXES = {
            {0, 1, 6, 6, 6, 6},
            {-2, 4, -3, 6, -3, 6},
            {-2, 4, 6, -3, -3, 6},
            {-2, 4, 6, -3, 6, -3},
            {-2, 4, -3, 6, 6, -3},
            {0, 4, 1, 1, -6, 8},
            {0, 4, 2, 2, 9, -5},
    };

    @Override
    public int[] getDimensions() {
        return BOXES[0];
    }

    @Override
    public int getOffset() {
        return 0;
    }

    /**
     * 1.7 sets the core {@code SoyuzLauncher.height} above the block the complex grows from and fills the
     * footprint around that ground block, so every box below is stated from there rather than from the core.
     */
    @Override
    public int getHeightOffset() {
        return HEIGHT;
    }

    /** The apron, the four flame trenches and the two service towers, exactly upstream's boxes. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        BlockPos ground = core.below(HEIGHT);
        for (int[] box : BOXES) MultiblockHandlerXR.visitBox(ground, box, facing, visitor);
        visitor.cell(ground, MASK_ALL);
        ring(ground, (pos, mask) -> visitor.cell(pos, mask, ROLE_ALL));
    }

    /**
     * The apron's outer ring, both courses, each cell open on the face it turns outward. The ring is
     * square and its four sides are stated together, so the set is the same whichever way the pad faces.
     */
    private static void ring(BlockPos ground, java.util.function.ObjIntConsumer<BlockPos> out) {
        for (int i = -6; i <= 6; i++) {
            for (int course = 0; course >= -1; course--) {
                out.accept(ground.offset(6, course, i), MASK_EAST);
                out.accept(ground.offset(-6, course, i), MASK_WEST);
                out.accept(ground.offset(i, course, 6), MASK_SOUTH);
                out.accept(ground.offset(i, course, -6), MASK_NORTH);
            }
        }
    }

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        BlockPos ground = core.below(HEIGHT);
        for (int[] box : BOXES) {
            if (!MultiblockHandlerXR.checkSpace(level, ground, box, clicked, facing)) return false;
        }
        return true;
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        ring(core.below(HEIGHT), (pos, mask) -> visitor.passiveCell(pos, MASK_ALL, domains));
    }

    /**
     * The pad has no block entity at its cells, so a cable or a duct touching one asks the block, which
     * resolves the core and defers to the same port list the launcher pushes through.
     */
    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntitySoyuzLauncher launcher
                && IEnergyConnectorBlock.isPort(launcher.getConPos(), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntitySoyuzLauncher launcher
                && IEnergyConnectorBlock.isPort(launcher.getConPos(), pos, dir);
    }

    /** The complex cannot be picked up; breaking it gives back the bulk it was built from. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

}
