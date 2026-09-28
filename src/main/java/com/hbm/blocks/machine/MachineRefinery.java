package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.oil.TileEntityMachineRefinery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Refinery block — 1:1 port of the 1.12.2 {@code MachineRefinery}: a 3×3×9 the folded core
 * (dims {@code {8,0,1,1,1,1}}, offset 1) with the 4 base corners marked as extras (the fluid ports).
 */
public class MachineRefinery extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder,
        com.hbm.explosion.vanillant.IBlockExplodedVNT, com.hbm.api.block.IToolable, com.hbm.interfaces.ILookOverlay {

    public MachineRefinery(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineRefinery(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{8, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineRefinery te ? te : null;
    }

    /** The four base corners are the port ring. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                visitor.cell(core.offset(dx, 0, dz), outwardMask(dx, dz, 1));
            }
        }
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                visitor.passiveCell(core.offset(dx, 0, dz), MASK_ALL, domains);
            }
        }
    }

    // --- 1.7 explosion damage and blowtorch repair (MachineExplosionDamage) ---

    /** 1.7 {@code onBlockExploded}: vanilla explosions. */
    @Override
    protected boolean onExplosion(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos,
                                  net.minecraft.world.level.Explosion explosion,
                                  java.util.function.BiConsumer<net.minecraft.world.item.ItemStack, BlockPos> onHit) {
        return MachineExplosionDamage.hit(level, pos, explosion, explosion.getDirectSourceEntity())
                || super.onExplosion(state, level, pos, explosion, onHit);
    }

    /** 1.7 {@code onBlockExploded}: HBM's own (VNT) explosions. */
    @Override
    public boolean onVNTExploded(net.minecraft.server.level.ServerLevel level, BlockPos pos, BlockState state,
                                 com.hbm.explosion.vanillant.ExplosionVNT explosion) {
        return MachineExplosionDamage.hit(level, pos, explosion, explosion.exploder);
    }

    @Override
    public boolean onScrew(Level level, net.minecraft.world.entity.player.Player player, BlockPos pos, Direction side,
                           net.minecraft.world.phys.Vec3 hit, net.minecraft.world.InteractionHand hand, ToolType tool) {
        if (tool != ToolType.TORCH) return false;
        return MachineExplosionDamage.repair(level, pos, player);
    }

    @Override
    public com.hbm.interfaces.ILookOverlay.@Nullable Data getLookOverlay(Level level, BlockPos pos,
                                                                        net.minecraft.world.entity.player.Player player) {
        return MachineExplosionDamage.overlay(level, pos, player, getName().getString());
    }

    /** 1.7: a blown-open machine opens no GUI. */
    @Override
    protected net.minecraft.world.InteractionResult useAtCore(BlockState state, Level level, BlockPos core,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (MachineExplosionDamage.blownOpen(level, core)) return net.minecraft.world.InteractionResult.TRY_WITH_EMPTY_HAND;
        return super.useAtCore(state, level, core, player, hit);
    }
}
