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
     * 1.7 sets the core {@code SoyuzLauncher.height} above the block the complex grows from. Its
     * {@code checkRequirement}/{@code fillSpace} add that height back before stating the boxes, so every box is
     * relative to the CORE: the apron is the core's own course and the one below it, the legs and trenches the
     * four courses under the apron (§19ec-satellites corrected an earlier reading that stated them from the ground
     * block, which sank the whole footprint four blocks below the rendered pad).
     */
    /** 1.7 {@code onBlockPlacedBy}: {@code ForgeDirection dir = ForgeDirection.EAST}, whichever way the placer looks. */
    @Override
    public Direction getDirModified(Direction direction) {
        return Direction.EAST;
    }

    @Override
    public int getHeightOffset() {
        return HEIGHT;
    }

    /** The apron, the four flame trenches and the two service towers, exactly upstream's boxes. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        for (int[] box : BOXES) MultiblockHandlerXR.visitBox(core, box, facing, visitor);
        ring(core, (pos, mask) -> visitor.cell(pos, mask, ROLE_ALL));
    }

    /**
     * The apron's outer ring, both courses (the core's and the one below: 1.7 {@code getConPos}), each cell open
     * on the face it turns outward. The ring is square and its four sides are stated together, so the set is the
     * same whichever way the pad faces.
     */
    private static void ring(BlockPos ground, java.util.function.ObjIntConsumer<BlockPos> out) {
        for (int i = -6; i <= 6; i++) {
            int corner = i == 6 ? MASK_SOUTH : i == -6 ? MASK_NORTH : MASK_NONE;
            int end = i == 6 ? MASK_EAST : i == -6 ? MASK_WEST : MASK_NONE;
            for (int course = 0; course >= -1; course--) {
                out.accept(ground.offset(6, course, i), MASK_EAST | corner);
                out.accept(ground.offset(-6, course, i), MASK_WEST | corner);
                out.accept(ground.offset(i, course, 6), MASK_SOUTH | end);
                out.accept(ground.offset(i, course, -6), MASK_NORTH | end);
            }
        }
    }

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        for (int[] box : BOXES) {
            if (!MultiblockHandlerXR.checkSpace(level, core, box, clicked, facing)) return false;
        }
        return true;
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        ring(core, (pos, mask) -> visitor.passiveCell(pos, MASK_ALL, domains));
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

    /** 1.7 {@code onBlockActivated}: {@code BossSpawnHandler.markFBI} on every non-sneaking use. */
    @Override
    protected net.minecraft.world.InteractionResult useAtCore(net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.Level level, net.minecraft.core.BlockPos core, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.phys.BlockHitResult hit) {
        if (!level.isClientSide() && !player.isShiftKeyDown()) com.hbm.handler.BossSpawnHandler.markFBI(player);
        return super.useAtCore(state, level, core, player, hit);
    }
}
