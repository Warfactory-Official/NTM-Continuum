package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineArcWelder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/** The 3x1x3 Arc Welder multiblock and its ten external power/fluid port cells. */
public class MachineArcWelder extends BlockDummyable
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder {

    public MachineArcWelder(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineArcWelder(pos, state);
    }

    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineArcWelder welder)) {
            return null;
        }
        // The old proxy passed its own position into getAccessibleSlotsFromSide. Keep that information
        // instead of exposing all three input slots from every dummy cell.
        return new SidedContainer(welder, pos);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityMachineArcWelder welder
                && IEnergyConnectorBlock.isPort(welder.getConPos(), pos, dir);
    }

    @Override
    public boolean canConnect(com.hbm.inventory.fluid.FluidType type, BlockGetter world, int x, int y, int z,
                              ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        return findCoreTE(world, pos) instanceof TileEntityMachineArcWelder welder
                && IEnergyConnectorBlock.isPort(welder.getConPos(), pos, dir);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 1, 0, 1, 1};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    /** A vanilla WorldlyContainer adapter that preserves the old proxy-relative hopper routing. */
    private static final class SidedContainer implements WorldlyContainer {
        private final TileEntityMachineArcWelder machine;
        private final int[] slots;

        private SidedContainer(TileEntityMachineArcWelder machine, BlockPos accessor) {
            this.machine = machine;
            this.slots = machine.getAccessibleSlotsFromSide(accessor);
        }

        @Override public int[] getSlotsForFace(Direction side) { return slots; }
        @Override public int getContainerSize() { return machine.getContainerSize(); }
        @Override public boolean isEmpty() { return machine.isEmpty(); }
        @Override public ItemStack getItem(int slot) { return machine.getItem(slot); }
        @Override public ItemStack removeItem(int slot, int amount) { return machine.removeItem(slot, amount); }
        @Override public ItemStack removeItemNoUpdate(int slot) { return machine.removeItemNoUpdate(slot); }
        @Override public void setItem(int slot, ItemStack stack) { machine.setItem(slot, stack); }
        @Override public void setChanged() { machine.setChanged(); }
        @Override public boolean stillValid(Player player) { return machine.stillValid(player); }
        @Override public void clearContent() { machine.clearContent(); }
        @Override public boolean canPlaceItem(int slot, ItemStack stack) { return machine.canPlaceItem(slot, stack); }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
            return contains(slot) && machine.canInsertItem(slot, stack);
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
            return contains(slot) && machine.canExtractItem(slot, stack, 1);
        }

        private boolean contains(int slot) {
            for (int allowed : slots) if (allowed == slot) return true;
            return false;
        }
    }
}
