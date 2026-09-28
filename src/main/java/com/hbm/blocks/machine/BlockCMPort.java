package com.hbm.blocks.machine;

import com.hbm.tileentity.machine.TileEntityCMPort;
import com.hbm.tileentity.machine.TileEntityCustomMachine;
import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code BlockCMPort} ({@code cm_port}, one flattened block per casing material): a machine casing carrying a
 * {@code TileEntityProxyCombo().inventory().power().fluid()}. Energy and fluids go through
 * {@link TileEntityCMPort}; items go through {@link #getContainer}, which hoppers consult before the block entity.
 */
public class BlockCMPort extends BlockCM implements EntityBlock, WorldlyContainerHolder {

    public BlockCMPort(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCMPort(pos, state);
    }

    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        if (level instanceof ServerLevel server) LevelNodeGraph.invalidateEndpointsAround(server, pos);
    }

    /** The proxy's {@code inventory()}: the controller's sided inventory, once a structure check has linked it. */
    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityCMPort port)) return null;
        TileEntityCustomMachine core = port.getCore();
        return core == null ? null : new PortContainer(core);
    }

    /** The controller seen through a port: the same slots and rules it answers at its own faces. */
    private static final class PortContainer implements WorldlyContainer {
        private final TileEntityCustomMachine core;

        private PortContainer(TileEntityCustomMachine core) {
            this.core = core;
        }

        @Override public int[] getSlotsForFace(Direction side) { return core.getSlotsForFace(side); }
        @Override public int getContainerSize() { return core.getContainerSize(); }
        @Override public boolean isEmpty() { return core.isEmpty(); }
        @Override public ItemStack getItem(int slot) { return core.getItem(slot); }
        @Override public ItemStack removeItem(int slot, int amount) { return core.removeItem(slot, amount); }
        @Override public ItemStack removeItemNoUpdate(int slot) { return core.removeItemNoUpdate(slot); }
        @Override public void setItem(int slot, ItemStack stack) { core.setItem(slot, stack); }
        @Override public void setChanged() { core.setChanged(); }
        @Override public boolean stillValid(Player player) { return false; }
        @Override public void clearContent() { core.clearContent(); }
        @Override public boolean canPlaceItem(int slot, ItemStack stack) { return core.canPlaceItem(slot, stack); }
        @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return core.canPlaceItemThroughFace(slot, stack, side); }
        @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return core.canTakeItemThroughFace(slot, stack, side); }
    }
}
