package com.hbm.blocks.machine.storage;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.items.ModDataComponents;
import com.hbm.blocks.BlockDummyable;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.storage.TileEntityBatteryREDD;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Current FEnSU: the separate 9x5 footprint, six-port BigInteger storage machine. */
public final class MachineBatteryREDD extends BlockDummyable implements IEnergyConnectorBlock, WorldlyContainerHolder {

    public MachineBatteryREDD(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityBatteryREDD(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{9, 0, 2, 2, 4, 4};
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection direction) {
        super.formStructure(level, core, direction);
        addPortCells(level, core, direction);
    }

    private void addPortCells(Level level, BlockPos core, ForgeDirection direction) {
        ForgeDirection rotation = direction.getRotation(ForgeDirection.UP);
        makeExtra(level, core.getX() + direction.offsetX * 2 + rotation.offsetX * 2,
                core.getY(), core.getZ() + direction.offsetZ * 2 + rotation.offsetZ * 2);
        makeExtra(level, core.getX() + direction.offsetX * 2 - rotation.offsetX * 2,
                core.getY(), core.getZ() + direction.offsetZ * 2 - rotation.offsetZ * 2);
        makeExtra(level, core.getX() - direction.offsetX * 2 + rotation.offsetX * 2,
                core.getY(), core.getZ() - direction.offsetZ * 2 + rotation.offsetZ * 2);
        makeExtra(level, core.getX() - direction.offsetX * 2 - rotation.offsetX * 2,
                core.getY(), core.getZ() - direction.offsetZ * 2 - rotation.offsetZ * 2);
        makeExtra(level, core.getX() + rotation.offsetX * 4,
                core.getY(), core.getZ() + rotation.offsetZ * 4);
        makeExtra(level, core.getX() - rotation.offsetX * 4,
                core.getY(), core.getZ() - rotation.offsetZ * 4);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        BlockPos core = findCore(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntityBatteryREDD battery) {
            battery.power = ModDataComponents.batteryReddPower(stack);
            battery.muffled = ModDataComponents.batteryReddMuffled(stack);
            battery.setChanged();
            battery.networkPackNT();
        }
    }

    @Override
    public boolean canConnect(BlockGetter level, BlockPos pos, ForgeDirection direction) {
        return findCoreTE(level, pos) instanceof TileEntityBatteryREDD battery
                && IEnergyConnectorBlock.isPort(battery.getConPos(), pos, direction);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int onAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return findCoreTE(level, pos) instanceof TileEntityBatteryREDD battery
                ? battery.getComparatorPower() : 0;
    }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityBatteryREDD battery ? battery : null;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                            BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.BATTERY_REDD.get(), TileEntityBatteryREDD::clientTick)
                : createTickerHelper(type, ModBlockEntities.BATTERY_REDD.get(), TileEntityBatteryREDD::serverTick);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockPos pos = BlockPos.containing(params.getParameter(LootContextParams.ORIGIN));
        BlockPos core = findCore(params.getLevel(), pos);
        ItemStack stack = new ItemStack(asItem());
        if (core != null && params.getLevel().getBlockEntity(core) instanceof TileEntityBatteryREDD battery) {
            ModDataComponents.setBatteryReddPower(stack, battery.power);
            ModDataComponents.setBatteryReddMuffled(stack, battery.muffled);
        }
        return List.of(stack);
    }

    public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
        return cloneStack(level, pos);
    }

    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return cloneStack(level, pos);
    }

    private ItemStack cloneStack(BlockGetter level, BlockPos pos) {
        ItemStack stack = new ItemStack(asItem());
        BlockPos core = findCore(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntityBatteryREDD battery) {
            ModDataComponents.setBatteryReddPower(stack, battery.power);
            ModDataComponents.setBatteryReddMuffled(stack, battery.muffled);
        }
        return stack;
    }
}
