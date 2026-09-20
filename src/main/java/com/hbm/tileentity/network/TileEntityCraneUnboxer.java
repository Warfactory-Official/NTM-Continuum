package com.hbm.tileentity.network;

import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.entity.item.EntityMovingItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.hbm.inventory.container.ContainerCraneUnboxer;
import com.hbm.items.ModConveyorItems;
import com.hbm.tileentity.ModConveyorBlockEntities;
import com.hbm.util.ConveyorInventories;

/**
 * 1.7 {@code tileentity/network/TileEntityCraneUnboxer}: receives packages at its output side (switcheroo) and
 * deals their stacks back onto the belt at its input side, one at a time.
 */
public class TileEntityCraneUnboxer extends TileEntityCraneBase {

    public static final int BUFFER = 21;
    private static final int[] ACCESS = ConveyorInventories.range(BUFFER);

    public TileEntityCraneUnboxer(BlockPos pos, BlockState state) {
        super(ModConveyorBlockEntities.CRANE_UNBOXER.get(), pos, state, 23);
    }

    @Override
    public String getDefaultName() {
        return "container.craneUnboxer";
    }

    @Override
    protected int[] getUpgradeSlotRange() {
        return new int[]{21, 22};
    }

    public void serverTick() {
        if (level == null) return;

        int delay = ModConveyorItems.ejectorDelay(getItem(22));

        if (level.getGameTime() % delay == 0 && !level.hasNeighborSignal(worldPosition)) {
            int amount = ModConveyorItems.stackAmount(getItem(21));

            Direction outputSide = getInputSide(); // note the switcheroo!
            BlockPos target = worldPosition.relative(outputSide);
            Block b = level.getBlockState(target).getBlock();

            if (b instanceof IConveyorBelt belt) {

                for (int i = 0; i < BUFFER; i++) {
                    ItemStack stack = getItem(i);

                    if (!stack.isEmpty()) {
                        stack = stack.copy();
                        int toSend = Math.min(amount, stack.getCount());
                        removeItem(i, toSend);
                        stack.setCount(toSend);

                        EntityMovingItem moving = new EntityMovingItem(level);
                        Vec3 pos = new Vec3(worldPosition.getX() + 0.5 + outputSide.getStepX() * 0.55,
                                worldPosition.getY() + 0.5 + outputSide.getStepY() * 0.55,
                                worldPosition.getZ() + 0.5 + outputSide.getStepZ() * 0.55);
                        Vec3 snap = belt.getClosestSnappingPosition(level, target, pos);
                        moving.snapTo(snap.x, snap.y, snap.z, 0F, 0F);
                        moving.setItemStack(stack);
                        level.addFreshEntity(moving);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return ACCESS;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerCraneUnboxer(containerId, inventory, this);
    }
}
