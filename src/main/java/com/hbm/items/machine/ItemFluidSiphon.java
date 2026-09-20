package com.hbm.items.machine;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
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
 * empty fluid containers in your inventory — one tank per click, as many whole containers as the tank
 * can fill. 1.7 matched containers through its {@code FluidContainerRegistry}; here a container is any
 * {@link IFillableItem} that accepts the tank's fluid and is filled to the brim by it.
 *
 * <p>Not ported: the pipette top-up (there is no pipette item) and the {@code FT_Unsiphonable} trait
 * (no fluid carries it in the port).
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
            boolean hasDrainedTank = false;

            for (int j = 0; j < player.getInventory().getContainerSize(); j++) {
                ItemStack inventoryStack = player.getInventory().getItem(j);
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
            if (hasDrainedTank) {
                te.setChanged();
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
