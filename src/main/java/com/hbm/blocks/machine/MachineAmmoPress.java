package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineAmmoPress;
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

/** The positional 3-wide x 2-high Ammo Press from the 1.12.2 machine. */
public class MachineAmmoPress extends BlockMultiblockMachine implements WorldlyContainerHolder {

    public MachineAmmoPress(Properties properties) {
        super(properties);
    }

    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineAmmoPress press ? press : null;
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineAmmoPress(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 0, 0, 1, 1};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    /** Item automation reaches the core through any cell; there is no receive domain, so no active face. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        passiveEverywhere(core, facing, visitor, PASSIVE_ITEMS);
    }
}
