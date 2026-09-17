package com.hbm.blocks.machine.pwr;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.pwr.TileEntityBlockPWR;
import com.hbm.tileentity.machine.pwr.TileEntityPWRController;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The block an assembled reactor is made of. Every part the controller flood-filled becomes one of
 * these; {@link #IO_ENABLED} marks the cells that were ports, which are the only ones fluid moves
 * through.
 */
public class BlockPWR extends CompatBlock implements EntityBlock, IFluidConnectorBlockMK2, ILookOverlay {

    public static final BooleanProperty IO_ENABLED = BooleanProperty.create("io");

    public BlockPWR(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(IO_ENABLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(IO_ENABLED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityBlockPWR(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.PWR_BLOCK.get(), TileEntityBlockPWR::serverTick);
    }

    /**
     * The cell gives back the part it was built from. Upstream puts that block straight back at the
     * position from {@code breakBlock}, which 26.1 cannot do — the block entity is already gone by the
     * time the removal hook runs — so the cell that was broken hands its part over as an item and the
     * rest of the structure restores itself in place.
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof TileEntityBlockPWR cell && cell.originalBlockState != null) {
            ItemStack stack = new ItemStack(cell.originalBlockState.getBlock());
            if (!stack.isEmpty()) return List.of(stack);
        }
        return List.of();
    }

    /**
     * Tears the reactor down. The block entity that knew where the core was is already removed, so the
     * core is reached through a neighbouring cell instead — every cell of an assembly touches another
     * one or the controller itself, and going through a survivor covers explosions and piston moves as
     * well as a player with a pickaxe.
     */
    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        if (level.isClientSide()) return;
        TileEntityPWRController controller = findController(level, pos);
        if (controller != null) controller.assembled = false;
    }

    private static @Nullable TileEntityPWRController findController(Level level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            BlockPos neighbour = pos.relative(side);
            BlockEntity be = level.getBlockEntity(neighbour);
            if (be instanceof TileEntityPWRController controller) return controller;
            if (be instanceof TileEntityBlockPWR cell) {
                TileEntityPWRController core = cell.getCore();
                if (core != null) return core;
            }
        }
        return null;
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = world.getBlockState(pos);
        return state.getBlock() == this && state.getValue(IO_ENABLED);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityBlockPWR cell)) return null;

        List<String> text = new ArrayList<>();
        TileEntityPWRController controller = cell.getCore();
        if (controller == null) {
            text.add("No core detected.");
        } else {
            text.add("Core: " + controller.getBlockPos().toShortString());
            text.add("Assembled: " + controller.assembled);
        }
        text.add("IO: " + level.getBlockState(pos).getValue(IO_ENABLED));
        if (cell.originalBlockState != null) {
            text.add("Part: " + cell.originalBlockState.getBlock().getName().getString());
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> @Nullable BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
