package com.hbm.blocks.machine.storage;

import com.hbm.lib.ForgeDirection;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.items.ModDataComponents;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.storage.TileEntityBatteryREDD;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
public final class MachineBatteryREDD extends BlockMultiblockMachine implements IEnergyConnectorBlock, WorldlyContainerHolder {

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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntityBatteryREDD battery) {
            battery.power = ModDataComponents.batteryReddPower(stack);
            battery.muffled = ModDataComponents.batteryReddMuffled(stack);
            battery.setChanged();
            battery.networkPackNT();
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int onAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return MultiblockSurface.coreEntity(level, pos) instanceof TileEntityBatteryREDD battery
                ? battery.getComparatorPower() : 0;
    }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityBatteryREDD battery ? battery : null;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockPos pos = BlockPos.containing(params.getParameter(LootContextParams.ORIGIN));
        BlockPos core = MultiblockSurface.coreOfAny(params.getLevel(), pos);
        ItemStack stack = new ItemStack(asItem());
        if (core != null && params.getLevel().getBlockEntity(core) instanceof TileEntityBatteryREDD battery) {
            ModDataComponents.setBatteryReddPower(stack, battery.power);
            ModDataComponents.setBatteryReddMuffled(stack, battery.muffled);
        }
        return List.of(stack);
    }

    @Override
    protected ItemStack onPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
        return cloneStack(level, pos);
    }

    private ItemStack cloneStack(BlockGetter level, BlockPos pos) {
        ItemStack stack = new ItemStack(asItem());
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntityBatteryREDD battery) {
            ModDataComponents.setBatteryReddPower(stack, battery.power);
            ModDataComponents.setBatteryReddMuffled(stack, battery.muffled);
        }
        return stack;
    }

    /** Four cable anchors at the corners of the deck, and one at the middle of each long flank. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction rot = facing.getClockWise();
        visitor.cell(core.relative(facing, 2).relative(rot, 2), MASK_SOUTH);
        visitor.cell(core.relative(facing, 2).relative(rot, -2), MASK_SOUTH);
        visitor.cell(core.relative(facing, -2).relative(rot, 2), MASK_NORTH);
        visitor.cell(core.relative(facing, -2).relative(rot, -2), MASK_NORTH);
        visitor.cell(core.relative(rot, 4), MASK_WEST);
        visitor.cell(core.relative(rot, -4), MASK_EAST);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_ITEMS;
        visitor.passiveCell(core.relative(facing, 2).relative(rot, 2), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, 2).relative(rot, -2), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, -2).relative(rot, 2), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, -2).relative(rot, -2), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot, 4), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot, -4), MASK_ALL, domains);
    }

    @Override
    public int[][] getAllPorts(ForgeDirection dir) {
        return rotatePorts(new int[][] {
                {2, 0, 2, ForgeDirection.SOUTH.ordinal()},
                {-2, 0, 2, ForgeDirection.SOUTH.ordinal()},
                {2, 0, -2, ForgeDirection.NORTH.ordinal()},
                {-2, 0, -2, ForgeDirection.NORTH.ordinal()},
                {4, 0, 0, ForgeDirection.EAST.ordinal()},
                {-4, 0, 0, ForgeDirection.WEST.ordinal()},
        }, dir);
    }
}
