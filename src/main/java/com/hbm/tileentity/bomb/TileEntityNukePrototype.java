package com.hbm.tileentity.bomb;

import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.special.ItemCell;
import com.hbm.items.ModNuclearMiscItems;
import com.hbm.items.machine.ItemBreedingRod.BreedingRodType;
import com.hbm.items.machine.ItemBreedingRod.RodSize;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.inventory.container.ContainerNukePrototype;
import com.hbm.tileentity.ModBombs2BlockEntities;

/**
 * The Prototype's 14-slot core: SAS3 cells on the ends (0, 1, 12, 13) and, in pairs, uranium / lead /
 * neptunium / lead / uranium quad breeding rods (slots 2-11).
 */
public class TileEntityNukePrototype extends TileEntityNukeBomb {

    public TileEntityNukePrototype(BlockPos pos, BlockState state) {
        super(ModBombs2BlockEntities.NUKE_PROTOTYPE.get(), pos, state, 14);
    }

    public boolean hasPart(int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (stack.isEmpty()) return false;
        return switch (slot) {
            case 0, 1, 12, 13 -> ItemCell.isFullCell(stack, Fluids.SAS3);
            case 2, 3, 10, 11 -> stack.getItem() == ModNuclearMiscItems.rod(RodSize.QUAD, BreedingRodType.URANIUM);
            case 4, 5, 8, 9 -> stack.getItem() == ModNuclearMiscItems.rod(RodSize.QUAD, BreedingRodType.LEAD);
            case 6, 7 -> stack.getItem() == ModNuclearMiscItems.rod(RodSize.QUAD, BreedingRodType.NP237);
            default -> false;
        };
    }

    @Override
    public boolean isReady() {
        for (int i = 0; i < 14; i++) {
            if (!hasPart(i)) return false;
        }
        return true;
    }

    @Override
    public String getDefaultName() {
        return "container.nukePrototype";
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerNukePrototype(containerId, inventory, this);
    }
}
