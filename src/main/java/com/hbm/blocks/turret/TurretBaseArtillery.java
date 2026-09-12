package com.hbm.blocks.turret;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.turret.TileEntityTurretBaseNT;
import dev.golem.core.gui.Menus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The 4×4 emplacement the two big guns stand on, two blocks tall.
 *
 * <p>Same folding as {@link TurretBaseNT}: 1.12.2 gives all thirty-two cells a block entity through
 * {@code BlockDummyable}, here thirty-one of them are {@link BlockTurretEmplacement} and only the core
 * carries one. The cells are full cubes, not the gun turrets' half-slab pad.
 */
public abstract class TurretBaseArtillery extends BlockMultiblockCore implements IEnergyConnectorBlock {

    private static final int[] DIMENSIONS = {1, 0, 2, 1, 2, 1};

    private static final int MIN_X = -2;
    private static final int MAX_X = 1;
    private static final int MIN_Z = -2;
    private static final int MAX_Z = 1;
    private static final int MAX_Y = 1;

    protected TurretBaseArtillery(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 1;
    }

    /**
     * The footprint, with the outer ring's faces open so a cable may touch any side of the emplacement.
     *
     * <p>1.12.2's {@code updateConnections} rotates the ring's <em>positions</em> with the placement but
     * passes world-constant faces, which only line up at one facing; the mask here is geometric at all
     * four. The consequence is that a face-gated conductor connects on sides 1.12.2 refused — the mask
     * cannot express "subscribe here, but ask that neighbour about a different face".
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        for (int ly = 0; ly <= MAX_Y; ly++) {
            for (int lz = MIN_Z; lz <= MAX_Z; lz++) {
                for (int lx = MIN_X; lx <= MAX_X; lx++) {
                    if (lx == 0 && ly == 0 && lz == 0) continue;

                    int mask = MASK_NONE;
                    if (lx == MIN_X) mask |= MASK_WEST;
                    if (lx == MAX_X) mask |= MASK_EAST;
                    if (lz == MIN_Z) mask |= MASK_NORTH;
                    if (lz == MAX_Z) mask |= MASK_SOUTH;

                    visitor.cell(core.offset(MultiblockMaskTable.worldX(lx, ly, lz, facing), ly,
                            MultiblockMaskTable.worldZ(lx, ly, lz, facing)), mask);
                }
            }
        }
    }

    @Override
    public int coreMask() {
        return MASK_NONE;
    }

    @Override
    protected BlockState cellStateFor(int localX, int localY, int localZ, Direction facing, int shapeId) {
        return ModTurretBlocks.turret_emplacement.get().defaultBlockState();
    }

    /**
     * 1.12.2's {@code standardOpenBehavior} swallows a sneak-click instead of opening the menu.
     *
     * <p>The designator gets out of the way first. A block's {@code useWithoutItem} runs <em>before</em>
     * the held item's {@code useOn}, so opening the menu here would eat the click and the designator
     * could never bind to the gun it was aimed at.
     */
    @Override
    protected InteractionResult useAtCore(BlockState coreState, Level level, BlockPos core, Player player,
                                          BlockHitResult hit) {
        if (player.getMainHandItem().getItem() instanceof com.hbm.items.tool.ItemDesignatorArtyRange) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof MenuProvider menu && player instanceof ServerPlayer serverPlayer) {
            Menus.openWithPos(serverPlayer, menu, core);
        }
        return InteractionResult.SUCCESS;
    }

    /** @see TurretBaseNT#padConnects */
    static boolean emplacementConnects(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        if (!(world instanceof Level level) || dir == ForgeDirection.UNKNOWN) return false;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityTurretBaseNT turret
                && IEnergyConnectorBlock.isPort(turret.getConPos(), pos, dir);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return emplacementConnects(world, pos, dir);
    }

    @Override
    protected void removedAt(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (!movedByPiston && level.getBlockEntity(pos) instanceof TileEntityMachineBase machine) {
            Containers.dropContents(level, pos, machine);
        }
        super.removedAt(state, level, pos, movedByPiston);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
