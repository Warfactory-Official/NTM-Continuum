package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.lib.ForgeDirection;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineCyclotron;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** The standard 5×1×5 cyclotron multiblock. */
public class MachineCyclotron extends BlockDummyable
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder {

    public MachineCyclotron(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineCyclotron(pos, state);
    }

    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineCyclotron cyclotron)) return null;
        return new SidedContainer(cyclotron, pos);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityMachineCyclotron cyclotron
                && IEnergyConnectorBlock.isPort(cyclotron.getConPos(), pos, dir);
    }

    @Override
    public boolean canConnect(com.hbm.inventory.fluid.FluidType type, BlockGetter world, int x, int y, int z,
                              ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        return findCoreTE(world, pos) instanceof TileEntityMachineCyclotron cyclotron
                && IEnergyConnectorBlock.isPort(cyclotron.getConPos(), pos, dir);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        BlockPos core = findCore(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntityMachineCyclotron cyclotron) {
            for (int i = 0; i < 4; i++) {
                net.minecraft.world.item.Item plug = TileEntityMachineCyclotron.getItemForPlug(i);
                if (plug != null && stack.getItem() == plug && !cyclotron.getPlug(i)) {
                    if (!level.isClientSide()) {
                        stack.shrink(1);
                        cyclotron.setPlug(i);
                        level.playSound(null, pos, ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return super.onUseItem(state, level, pos, player, hand, stack, hit);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                  BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return super.onUseWithoutItem(state, level, pos, player, hit);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.MACHINE_CYCLOTRON.get(), TileEntityMachineCyclotron::clientTick)
                : createTickerHelper(type, ModBlockEntities.MACHINE_CYCLOTRON.get(), TileEntityMachineCyclotron::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override public int[] getDimensions() { return new int[]{2, 0, 2, 2, 2, 2}; }
    @Override public int getOffset() { return 2; }

    @Override
    protected void fillSpace(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        super.fillSpace(level, x, y, z, dir, offset);
        addExtraPerimeter(level, new BlockPos(x + dir.offsetX * offset, y, z + dir.offsetZ * offset));
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        super.formStructure(level, core, dir);
        addExtraPerimeter(level, core);
    }

    private void addExtraPerimeter(Level level, BlockPos core) {
        for (int i = -1; i <= 1; i++) {
            makeExtra(level, core.getX() + 2, core.getY(), core.getZ() + i);
            makeExtra(level, core.getX() - 2, core.getY(), core.getZ() + i);
            makeExtra(level, core.getX() + i, core.getY(), core.getZ() + 2);
            makeExtra(level, core.getX() + i, core.getY(), core.getZ() - 2);
        }
    }

    /** Carries the queried footprint cell into the old proxy-relative sided-inventory contract. */
    private static final class SidedContainer implements WorldlyContainer {
        private final TileEntityMachineCyclotron machine;
        private final int[] slots;

        private SidedContainer(TileEntityMachineCyclotron machine, BlockPos accessor) {
            this.machine = machine;
            this.slots = machine.getAccessibleSlotsFromSide(accessor);
        }

        @Override public int[] getSlotsForFace(net.minecraft.core.Direction side) { return slots; }
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
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
            return contains(slot) && machine.isItemValidForSlot(slot, stack);
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
            return contains(slot) && machine.canExtractItem(slot, stack, 1);
        }

        private boolean contains(int slot) {
            for (int allowed : slots) if (allowed == slot) return true;
            return false;
        }
    }
}
