package com.hbm.tileentity;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import dev.golem.core.autocraft.CraftingMachines;
import dev.golem.core.data.CompatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** 1.12.2 {@code TileEntityProxyCombo} for AE2 where it finds machines only through block entities (1.20.1 Forge). */
public class TileEntityProxyCell extends CompatBlockEntity {

    public TileEntityProxyCell(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PROXY_CELL.get(), pos, state);
    }

    public static @Nullable TileEntityProxyCell at(BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof BlockMultiblockMachine) || CraftingMachines.reachesBlocksWithoutBlockEntity()
                || !CraftingMachines.available() || !ModBlockEntities.PROXY_CELL.get().isValid(state)) return null;
        return new TileEntityProxyCell(pos, state);
    }
}
