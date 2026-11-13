package com.hbm.tileentity;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.turret.BlockTurretEmplacement;
import com.hbm.blocks.turret.BlockTurretPad;
import dev.golem.core.autocraft.CraftingMachines;
import dev.golem.core.data.CompatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A folded multiblock cell's block entity on 1.20.1 Forge, where capabilities hang only on block entities: other mods'
 * pipes, storage buses and AE2 then reach the machine through its cells, as through 1.7's {@code TileEntityProxyCombo}.
 * Its item and fluid capabilities forward to the core. Elsewhere cells carry none.
 */
public class TileEntityProxyCell extends CompatBlockEntity {

    public TileEntityProxyCell(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PROXY_CELL.get(), pos, state);
    }

    public static @Nullable TileEntityProxyCell at(BlockPos pos, BlockState state) {
        if (!proxies(state.getBlock()) || CraftingMachines.reachesBlocksWithoutBlockEntity()) return null;
        return new TileEntityProxyCell(pos, state);
    }

    /** The cell blocks that carry one: folded machines, and the turrets' own pad and emplacement cells. */
    public static boolean proxies(Block block) {
        return block instanceof BlockMultiblockMachine machine && machine.proxiesCells()
                || block instanceof BlockTurretPad || block instanceof BlockTurretEmplacement;
    }
}
