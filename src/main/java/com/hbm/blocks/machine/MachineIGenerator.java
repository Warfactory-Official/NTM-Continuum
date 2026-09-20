package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.interfaces.ILookOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import com.hbm.tileentity.machine.TileEntityMachineIGenerator;

/**
 * 1.7 {@code MachineIGenerator}, the "Industrial Generator Memorial": a {@code BlockDummyable} whose footprint
 * is {@code {0,0,0,0,0,0}} at offset 0 (one block, facing kept), whose right-click is swallowed without a
 * menu, and whose look overlay reads "In memory of all that we have lost".
 */
public class MachineIGenerator extends BlockMultiblockMachine implements ILookOverlay {

    public MachineIGenerator(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineIGenerator(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 0, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    /** 1.7: client true; server true unless sneaking (the openGui call is commented out). */
    @Override
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        return player.isShiftKeyDown() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.SUCCESS;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        return new ILookOverlay.Data("Industrial Generator Memorial", 0xff8000, 0x804000,
                List.of("In memory of all that we have lost"));
    }
}
