package com.hbm.blocks.machine.pile;

import com.hbm.api.block.IToolable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.pile.TileEntityPileBaseMK2;
import com.hbm.tileentity.machine.pile.TileEntityPileCore;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A cell of an assembled Chicago Pile. Graphite bricks become these the moment the structure is
 * validated, and they carry the drilled state of the pile in their {@link #META}: solid, bored through,
 * a channel mouth, or the one cell that holds the simulation.
 *
 * <p>Breaking any cell collapses the whole structure back to loose bricks — the core goes with it, and
 * every remaining cell sees its core missing on its next tick and reverts itself.
 */
public class BlockPile extends dev.golem.core.world.CompatBlock implements EntityBlock, IToolable, ILookOverlay {

    public static final int META_DUMMY = 0;
    public static final int META_CORE = 1;
    public static final int META_CHANNEL = 2;
    public static final int META_FUEL_IN = 3;
    public static final int META_FUEL_OUT = 4;
    public static final int META_AIR_IN = 5;
    public static final int META_AIR_OUT = 6;
    public static final int META_CONTROL = 7;
    public static final int META_EDGE = 8;

    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 8);

    public BlockPile(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(META, META_DUMMY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(META);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) == META_CORE
                ? new TileEntityPileCore(pos, state)
                : new TileEntityPileBaseMK2(pos, state);
    }

    /** Pile cells are never an item; the bricks they were assembled from are what comes back. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    /**
     * Reverts the broken cell to graphite and takes the core with it, which collapses the rest of the pile
     * on its next tick.
     *
     * <p>The core is found through a <i>neighbouring</i> cell rather than this one: 26.1 runs the removal
     * hook after the block entity is already gone, so the cell that was broken can no longer say where its
     * core was. Any surviving neighbour still can.
     */
    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        if (level.isClientSide() || TileEntityPileCore.meltingDown) return;

        TileEntityPileCore core = findCore(level, pos);
        level.setBlockAndUpdate(pos, ModPileBlocks.pile_brick.get().defaultBlockState());
        if (core != null && !core.isRemoved()) core.destroy();
    }

    @Nullable
    private static TileEntityPileCore findCore(Level level, BlockPos pos) {
        TileEntityPileCore core = coreAt(level, pos);
        if (core != null) return core;

        for (Direction side : Direction.values()) {
            core = coreAt(level, pos.relative(side));
            if (core != null) return core;
        }
        return null;
    }

    @Nullable
    private static TileEntityPileCore coreAt(Level level, BlockPos pos) {
        BlockEntity tile = level.getBlockEntity(pos);
        if (tile instanceof TileEntityPileCore core) return core;
        if (tile instanceof TileEntityPileBaseMK2 pile) return pile.getCore();
        return null;
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit,
                           InteractionHand hand, ToolType tool) {
        if (tool != ToolType.HAND_DRILL) return false;

        BlockEntity tile = level.getBlockEntity(pos);
        if (tile instanceof TileEntityPileCore || level.getBlockState(pos).getValue(META) == META_CORE) {
            sendError(level, pos, "Cannot intersect core", player);
            return false;
        }

        if (tile instanceof TileEntityPileBaseMK2 pile) {
            if (level.isClientSide()) return true;
            TileEntityPileCore core = pile.getCore();
            if (core != null) {
                ForgeDirection dir = ForgeDirection.getOrientation(side).getOpposite();
                return core.drillChannel(pos.getX(), pos.getY(), pos.getZ(), dir, player);
            }
        }

        sendError(level, pos, "No core found", player);
        return false;
    }

    /**
     * Reports an assembly problem at {@code pos} to the player. Upstream marks the offending block with a
     * labelled floating box; this port has no marker particle, so the position rides along in the text.
     */
    public static void sendError(Level level, BlockPos pos, String message, Player player) {
        if (level.isClientSide()) return;
        player.sendSystemMessage(Component.literal(message + " ["
                + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]").withStyle(ChatFormatting.RED));
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(META)) return null;

        List<String> text = new ArrayList<>();
        switch (state.getValue(META)) {
            case META_FUEL_IN -> text.add("Fuel Loading Port");
            case META_FUEL_OUT -> text.add("Fuel Ejection Port");
            case META_AIR_IN -> text.add("Air Inlet");
            case META_AIR_OUT -> text.add("Air Outlet");
            case META_CONTROL -> text.add("Control Rod Channel");
            case META_CORE -> {
                if (level.getBlockEntity(pos) instanceof TileEntityPileCore core) {
                    text.add("Max Temp: " + Math.round(core.highestHeat) + " / "
                            + TileEntityPileCore.MAX_HEAT + "°C");
                }
            }
            default -> { }
        }

        if (text.isEmpty()) return null;
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        BlockEntityTicker<T> core = createTickerHelper(type, ModBlockEntities.PILE_CORE.get(),
                TileEntityPileCore::serverTick);
        return core != null ? core
                : createTickerHelper(type, ModBlockEntities.PILE_BLOCK.get(), TileEntityPileBaseMK2::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
