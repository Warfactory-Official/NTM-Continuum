package com.hbm.items.machine;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FluidTraitSimple;
import com.hbm.items.ModUtilityToolItems;
import com.hbm.items.tool.ItemPipette;
import dev.golem.core.world.CompatItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The 1.7 {@code ItemFluidSiphon}: right-click a machine and its receiving tanks are drained into the
 * empty fluid containers in your inventory. One tank per click, as many whole containers as the tank
 * can fill. 1.7 matched containers through its {@code FluidContainerRegistry}; here a container is any
 * {@link IFillableItem} that accepts the tank's fluid and is filled to the brim by it.
 *
 * <p>A tank's remainder under 1,000 mB tops up the first pipette in the inventory that would not fizzle on
 * it (laboratory pipettes are skipped, as in 1.7); pipettes are never filled as whole containers. Tanks of
 * an {@code FT_Unsiphonable} fluid (water and the four steams) are skipped, as in 1.7.
 */
public class ItemFluidSiphon extends CompatItem {

    public ItemFluidSiphon(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        BlockPos pos = context.getClickedPos();
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        BlockEntity te = level.getBlockEntity(core != null ? core : pos);
        if (!(te instanceof IFluidStandardReceiverMK2 receiver)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        // We need to iterate through the inventory for _each_ siphonable
        // tank, so we can handle fluids that can only go into certain containers
        // After we successfully siphon any fluid from a tank, we stop
        // further processing, multiple fluid types require multiple clicks
        for (FluidTankNTM tank : receiver.getReceivingTanks()) {
            if (tank.getFill() <= 0) continue;
            FluidType tankType = tank.getTankType();
            if (tankType == Fluids.NONE) continue;
            if (tankType.hasTrait(FluidTraitSimple.FT_Unsiphonable.class)) continue;
            boolean hasDrainedTank = false;
            ItemStack availablePipette = null;

            for (int j = 0; j < player.getInventory().getContainerSize(); j++) {
                ItemStack inventoryStack = player.getInventory().getItem(j);
                if (availablePipette == null && inventoryStack.getItem() instanceof ItemPipette pipette) {
                    if (!pipette.willFizzle(tankType)
                            && pipette != ModUtilityToolItems.pipette_laboratory.get()) { // Ignoring laboratory pipettes for now
                        availablePipette = inventoryStack;
                    }
                }
                // 1.7 matched containers through FluidContainerRegistry, which never held a pipette.
                if (inventoryStack.getItem() instanceof ItemPipette) continue;
                if (inventoryStack.isEmpty() || !(inventoryStack.getItem() instanceof IFillableItem fillable)) continue;
                if (fillable.getFill(inventoryStack) != 0 || !fillable.acceptsFluid(tankType, inventoryStack)) continue;

                while (inventoryStack.getCount() > 0 && tank.getFill() > 0) {
                    ItemStack filled = inventoryStack.copy();
                    filled.setCount(1);
                    int remainder = fillable.tryFill(tankType, tank.getFill(), filled);
                    int content = tank.getFill() - remainder;
                    // 1.7 only filled whole containers: the tank must hold at least the container's content.
                    if (content <= 0 || fillable.tryFill(tankType, 1, filled.copy()) != 1) break;
                    hasDrainedTank = true;
                    inventoryStack.shrink(1);
                    if (inventoryStack.isEmpty()) player.getInventory().setItem(j, ItemStack.EMPTY);
                    tank.setFill(remainder);
                    if (!player.getInventory().add(filled)) player.drop(filled, false);
                }
            }
            // If the remainder of the tank can only fit into a pipette, fill a pipette with the remainder.
            // Will not auto-fill fizzlable pipettes, there is no feedback for the fizzle in this case.
            if (availablePipette != null && tank.getFill() < 1000) {
                ItemPipette pipette = (ItemPipette) availablePipette.getItem();
                if (pipette.acceptsFluid(tankType, availablePipette)) {
                    hasDrainedTank = true;
                    tank.setFill(pipette.tryFill(tankType, tank.getFill(), availablePipette));
                }
            }
            if (hasDrainedTank) {
                te.setChanged();
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
