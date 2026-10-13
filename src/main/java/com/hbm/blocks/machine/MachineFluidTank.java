package com.hbm.blocks.machine;

import com.hbm.api.block.IToolable;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.IBlockExplodedVNT;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Fluid Tank: a 5×3×5 dummyable multiblock (the 1.12.2 {@code MachineFluidTank}), dims {2,0,1,1,2,2}
 * with the four corner cells filled. Drawn by the generic OBJ renderer ({@code fluidtank.obj}); the core
 * is {@link TileEntityMachineFluidTank}. Implements {@link IFluidConnectorBlockMK2} so fluid ducts
 * connect to any face of the footprint without a BlockEntity at each cell.
 */
public class MachineFluidTank extends BlockMultiblockMachine implements IFluidConnectorBlockMK2,
        IBlockExplodedVNT, IToolable, ILookOverlay {

    public MachineFluidTank(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineFluidTank(pos, state);
    }

    /**
     * Ducts connect only at the tank's actual fluid ports (the {@code getConPos} positions: two per
     * horizontal side, at the core's Y), NOT every footprint face, so a duct alongside the tank body
     * doesn't sprout a phantom connection. {@code dir} is the tank's outward face toward the duct.
     */
    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos core = MultiblockSurface.coreOfAny(world, new BlockPos(x, y, z));
        return core != null && isPort(core, x, y, z, dir);
    }

    /**
     * Whether tank cell {@code (x,y,z)} exposes a fluid port on face {@code dir}. 1.7.10: the ports face
     * the four inner-corner proxy cells {@code (±1, 0, ±1)} (= each {@code getConPos} entry's
     * {@code pos − dir}); on any facing only the long-side faces of those cells can meet a duct.
     */
    private static boolean isPort(BlockPos c, int x, int y, int z, ForgeDirection dir) {
        int dx = x - c.getX(), dy = y - c.getY(), dz = z - c.getZ();
        if (dy != 0) return false;
        return switch (dir) {
            case EAST -> dx == 1 && (dz == -1 || dz == 1);   // +X (Library.POS_X)
            case WEST -> dx == -1 && (dz == -1 || dz == 1);  // -X (Library.NEG_X)
            case SOUTH -> dz == 1 && (dx == -1 || dx == 1);  // +Z (Library.POS_Z)
            case NORTH -> dz == -1 && (dx == -1 || dx == 1); // -Z (Library.NEG_Z)
            default -> false;
        };
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 1, 1, 2, 2};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    // --- comparator output from the tank fill (works from any cell via findCore) ---

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int onAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return MultiblockSurface.coreEntity(level, pos) instanceof TileEntityMachineFluidTank tank
                ? tank.tank.getRedstoneComparatorPower() : 0;
    }

    @Override
    public int coreMask() {
        return MASK_NONE;
    }

    /**
     * 1.7.10 {@code fillSpace}: the four inner corners (core ± 1, ± 1) are the proxy extras, and the tile's
     * fixed getConPos points at them from two blocks out. Opening their horizontal faces gives exactly
     * 1.7's reachable set for either orientation: the faces toward the tank's own body are covered by
     * dummy cells, so only the long-side faces can meet a duct.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int i = -1; i <= 1; i += 2) {
            for (int j = -1; j <= 1; j += 2) visitor.cell(core.offset(i, 0, j), MASK_HORIZONTAL);
        }
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        for (int i = -1; i <= 1; i += 2) {
            for (int j = -1; j <= 1; j += 2) visitor.passiveCell(core.offset(i, 0, j), MASK_ALL, PASSIVE_FLUID_IN);
        }
    }

    // --- 1.7 explosion damage and blowtorch repair (MachineExplosionDamage) ---

    /** 1.7 {@code onBlockExploded}: vanilla explosions. */
    @Override
    protected boolean onExplosion(BlockState state, ServerLevel level, BlockPos pos,
                                  Explosion explosion,
                                  BiConsumer<ItemStack, BlockPos> onHit) {
        return MachineExplosionDamage.hit(level, pos, explosion, explosion.getDirectSourceEntity())
                || super.onExplosion(state, level, pos, explosion, onHit);
    }

    /** 1.7 {@code onBlockExploded}: HBM's own (VNT) explosions. */
    @Override
    public boolean onVNTExploded(ServerLevel level, BlockPos pos, BlockState state,
                                 ExplosionVNT explosion) {
        return MachineExplosionDamage.hit(level, pos, explosion, explosion.exploder);
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side,
                           Vec3 hit, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.TORCH) return false;
        return MachineExplosionDamage.repair(level, pos, player);
    }

    @Override
    public ILookOverlay.@Nullable Data getLookOverlay(Level level, BlockPos pos,
                                                                        Player player) {
        return MachineExplosionDamage.overlay(level, pos, player, getName().getString());
    }

    /** 1.7: a blown-open machine opens no GUI. */
    @Override
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core,
            Player player, BlockHitResult hit) {
        if (MachineExplosionDamage.blownOpen(level, core)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return super.useAtCore(state, level, core, player, hit);
    }
}
