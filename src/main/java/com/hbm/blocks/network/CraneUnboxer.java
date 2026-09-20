package com.hbm.blocks.network;

import com.hbm.api.conveyor.IConveyorItem;
import com.hbm.api.conveyor.IConveyorPackage;
import com.hbm.api.conveyor.IEnterableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.network.TileEntityCraneUnboxer;
import com.hbm.util.ConveyorInventories;

/** 1.7 {@code blocks/network/CraneUnboxer}: takes packages at its OUTPUT side and never loose items. */
public class CraneUnboxer extends BlockCraneBase implements IEnterableBlock {

    public CraneUnboxer(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCraneUnboxer(pos, state);
    }

    @Override
    public boolean canItemEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorItem entity) {
        return false;
    }

    @Override
    public void onItemEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorItem entity) {}

    @Override
    public boolean canPackageEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorPackage entity) {
        return getOutputSide(level, pos) == dir;
    }

    @Override
    public void onPackageEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorPackage entity) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityCraneUnboxer unboxer)) return;
        Direction accessedSide = getOutputSide(level, pos).getOpposite();

        for (ItemStack stack : entity.getItemStacks()) {
            ItemStack remainder = ConveyorInventories.addToInventory(unboxer, unboxer.getSlotsForFace(accessedSide), stack, accessedSide);
            if (!remainder.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, remainder.copy()));
            }
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int onAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof Container c ? AbstractContainerMenu.getRedstoneSignalFromContainer(c) : 0;
    }
}
