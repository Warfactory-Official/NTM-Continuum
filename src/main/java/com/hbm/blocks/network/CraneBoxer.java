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
import com.hbm.tileentity.network.TileEntityCraneBoxer;
import com.hbm.util.ConveyorInventories;

/** 1.7 {@code blocks/network/CraneBoxer}: accepts items at its input side and packages at all; the tile packs. */
public class CraneBoxer extends BlockCraneBase implements IEnterableBlock {

    public CraneBoxer(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCraneBoxer(pos, state);
    }

    @Override
    public boolean canItemEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorItem entity) {
        return getInputSide(level, pos) == dir;
    }

    @Override
    public void onItemEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorItem entity) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityCraneBoxer boxer) || dir == null) return;

        ItemStack remainder = ConveyorInventories.addToInventory(boxer, boxer.getSlotsForFace(dir), entity.getItemStack().copy(), dir);
        if (!remainder.isEmpty()) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, remainder.copy()));
        }
    }

    @Override
    public boolean canPackageEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorPackage entity) {
        return true;
    }

    @Override
    public void onPackageEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorPackage entity) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityCraneBoxer boxer)) return;
        Direction accessedSide = getOutputSide(level, pos).getOpposite();

        for (ItemStack stack : entity.getItemStacks()) {
            ItemStack remainder = ConveyorInventories.addToInventory(boxer, boxer.getSlotsForFace(accessedSide), stack, accessedSide);
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
