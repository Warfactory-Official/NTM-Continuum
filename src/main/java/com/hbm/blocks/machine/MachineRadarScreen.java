package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.tileentity.machine.TileEntityMachineRadarNT;
import com.hbm.tileentity.machine.TileEntityMachineRadarScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1.7 {@code MachineRadarScreen}: a 2x2 panel ({@code {1, 0, 0, 0, 1, 0}}, offset 0) that shows what its linked
 * radar sees. A plain click opens the linked radar's map, when there is one; 1.7 then returned {@code false}
 * on the client and {@code true} on the server, so a click never falls through to the held item unless the
 * player sneaks — which is how the radar linker is used on it.
 */
public class MachineRadarScreen extends BlockMultiblockMachine {

    public MachineRadarScreen(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineRadarScreen(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 0, 0, 1, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide() && level.getBlockEntity(core) instanceof TileEntityMachineRadarScreen screen && screen.linked
                && level.getBlockEntity(screen.radarPos) instanceof TileEntityMachineRadarNT radar) {
            MachineRadar.OPEN_SCREEN.accept(radar);
        }
        return InteractionResult.SUCCESS;
    }
}
