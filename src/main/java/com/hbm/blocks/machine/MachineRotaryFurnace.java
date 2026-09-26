package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import com.hbm.tileentity.machine.TileEntityMachineRotaryFurnace;

/**
 * The rotary furnace: a 3 (along) x 5 (across) x 5 (high) folded footprint, core one block behind the
 * placed position. 1.7 stamps {@code inventory().fluid()} proxies on the five back cells, the side
 * fluid cell, the exhaust top and the solid-fuel cell; its explicit steam/fluid/smoke connection points
 * become the open faces here.
 */
public final class MachineRotaryFurnace extends BlockMultiblockMachine
        implements IFluidConnectorBlockMK2, WorldlyContainerHolder, ILookOverlay {

    public MachineRotaryFurnace(Properties properties) { super(properties); }

    @Override protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineRotaryFurnace(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{4, 0, 1, 1, 2, 2}; }
    @Override public int getOffset() { return 1; }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineRotaryFurnace furnace)) return null;
        return new SidedContainer(furnace, pos);
    }

    /**
     * Steam in/out at the two back cells beside the centre line (facing backwards), the recipe fluid at
     * the two side cells (facing outwards), smoke out of the exhaust top.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction rot = facing.getCounterClockWise(); // 1.7 dir.getRotation(DOWN)
        BlockPos back = core.relative(facing.getOpposite());

        visitor.cell(back.relative(rot, -2), MASK_NORTH);
        visitor.cell(back.relative(rot, -1), MASK_NORTH);
        // +rot is the counter-clockwise flank, which is local EAST in the mask frame.
        visitor.cell(core.relative(facing).relative(rot, 2), MASK_EAST);
        visitor.cell(back.relative(rot, 2), MASK_EAST);
        visitor.cell(core.relative(rot).above(4), MASK_UP);
    }

    /** 1.7 {@code makeExtra}: the eight proxy cells accept fluid and items on every face. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getCounterClockWise(); // 1.7 dir.getRotation(DOWN)
        BlockPos back = core.relative(facing.getOpposite());
        int domains = PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        for (int i = -2; i <= 2; i++) visitor.passiveCell(back.relative(rot, i), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing).relative(rot, 2), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot).above(4), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing).relative(rot), MASK_ALL, domains);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineRotaryFurnace furnace)) return null;

        Direction facing = level.getBlockState(core).getValue(FACING);
        Direction rot = facing.getCounterClockWise(); // 1.7 dir.getRotation(DOWN)
        BlockPos back = core.relative(facing.getOpposite());
        List<String> text = new ArrayList<>();

        // steam
        if (pos.equals(back.relative(rot, -1)) || pos.equals(back.relative(rot, -2))) {
            text.add("§a-> §r" + furnace.tanks[1].getTankType().getDisplayNameString());
            text.add("§c<- §r" + furnace.tanks[2].getTankType().getDisplayNameString());
        }
        // fluids
        if (pos.equals(core.relative(facing).relative(rot, 2)) || pos.equals(back.relative(rot, 2))) {
            text.add("§a-> §r" + furnace.tanks[0].getTankType().getDisplayNameString());
        }
        if (pos.equals(core.relative(facing).relative(rot))) {
            text.add("§e-> §rFuel");
        }

        if (text.isEmpty()) return null;
        return new ILookOverlay.Data(Component.translatable("block.hbm.machine_rotary_furnace").getString(),
                0xffff00, 0x404000, text);
    }

    /** The old proxy passed its own position and side into getAccessibleSlotsFromSide. */
    private static final class SidedContainer implements WorldlyContainer {
        private final TileEntityMachineRotaryFurnace machine;
        private final BlockPos cell;

        private SidedContainer(TileEntityMachineRotaryFurnace machine, BlockPos cell) {
            this.machine = machine;
            this.cell = cell;
        }

        @Override public int[] getSlotsForFace(Direction side) { return machine.getAccessibleSlotsFromSide(cell, side); }
        @Override public int getContainerSize() { return machine.getContainerSize(); }
        @Override public boolean isEmpty() { return machine.isEmpty(); }
        @Override public ItemStack getItem(int slot) { return machine.getItem(slot); }
        @Override public ItemStack removeItem(int slot, int amount) { return machine.removeItem(slot, amount); }
        @Override public ItemStack removeItemNoUpdate(int slot) { return machine.removeItemNoUpdate(slot); }
        @Override public void setItem(int slot, ItemStack stack) { machine.setItem(slot, stack); }
        @Override public void setChanged() { machine.setChanged(); }
        @Override public boolean stillValid(Player player) { return machine.stillValid(player); }
        @Override public void clearContent() { machine.clearContent(); }
        @Override public boolean canPlaceItem(int slot, ItemStack stack) { return machine.isItemValidForSlot(slot, stack); }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
            for (int allowed : getSlotsForFace(side)) if (allowed == slot) return machine.isItemValidForSlot(slot, stack);
            return false;
        }

        @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    }
}
