package com.hbm.blocks.machine;

import dev.golem.core.graph.LevelNodeGraph;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.machine.TileEntityBlockPWR;
import com.hbm.tileentity.machine.TileEntityPWRController;

/**
 * The block every reactor part becomes once the controller assembles the structure (1.7 {@code BlockPWR}).
 * Metadata 1 marked a converted access port; that is the {@link #PORT} property here, and a port cell is
 * the only face automation can reach the controller's tanks and slots through.
 *
 * <p>1.7 drew the shell with connected textures ({@code IBlockCT}); the port has no CT renderer, so the
 * cells draw the plain {@code pwr_block} / {@code pwr_casing_port} tiles. The block drops nothing and has
 * no item: breaking a cell puts the assembled-from block back instead (see
 * {@link TileEntityBlockPWR#preRemoveSideEffects}).
 */
public class BlockPWR extends CompatBlock implements TickingBlock, WorldlyContainerHolder {

    public static final BooleanProperty PORT = BooleanProperty.create("port");

    public BlockPWR(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(PORT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PORT);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityBlockPWR(pos, state);
    }

    /**
     * 1.7 {@code TileEntityBlockPWR.updateEntity}'s self-break: removing the cell runs the entity's removal
     * hook, which re-sets the original block. A cell that lost its record simply goes away.
     */
    public static void revert(Level level, BlockPos pos) {
        level.removeBlock(pos, false);
    }

    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        if (level instanceof ServerLevel server) LevelNodeGraph.invalidateEndpointsAround(server, pos);
    }

    /** 1.7 {@code ISidedInventory} on the port cell: hoppers and tubes reach the controller's slots. */
    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        if (!state.getValue(PORT)) return null;
        if (!(level.getBlockEntity(pos) instanceof TileEntityBlockPWR cell) || cell.block == null) return null;
        TileEntityPWRController controller = cell.getCore();
        return controller == null ? null : new PortContainer(controller);
    }

    /** The controller's inventory as seen through a port: slots 0 and 1, fuel in, hot rods out. */
    private static final class PortContainer implements WorldlyContainer {
        private final TileEntityPWRController controller;

        private PortContainer(TileEntityPWRController controller) {
            this.controller = controller;
        }

        @Override public int[] getSlotsForFace(Direction side) { return controller.getAccessibleSlotsFromSide(side.ordinal()); }
        @Override public int getContainerSize() { return controller.getContainerSize(); }
        @Override public boolean isEmpty() { return controller.isEmpty(); }
        @Override public ItemStack getItem(int slot) { return controller.getItem(slot); }
        @Override public ItemStack removeItem(int slot, int amount) { return controller.removeItem(slot, amount); }
        @Override public ItemStack removeItemNoUpdate(int slot) { return controller.removeItemNoUpdate(slot); }
        @Override public void setItem(int slot, ItemStack stack) { controller.setItem(slot, stack); }
        @Override public void setChanged() { controller.setChanged(); }
        @Override public boolean stillValid(Player player) { return false; }
        @Override public void clearContent() { controller.clearContent(); }
        @Override public boolean canPlaceItem(int slot, ItemStack stack) { return controller.canPlaceItem(slot, stack); }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
            return controller.isItemValidForSlot(slot, stack);
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
            return controller.canExtractItem(slot, stack, side.ordinal());
        }
    }
}
