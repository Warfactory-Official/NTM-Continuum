package com.hbm.blocks.network;

import com.hbm.blocks.ITooltipProvider;
import com.hbm.api.block.IToolable;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.api.energymk2.PowerGraphProvider;
import com.hbm.capability.MachineCaps;
import com.hbm.lib.ForgeDirection;
import com.hbm.uninos.graph.LevelNodeGraph;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.network.TileEntityCablePaintable;

/**
 * {@code red_cable_paintable}: a full-block cable that can be dressed as any plain full block by
 * right-clicking with it. The screwdriver strips the paint; the defuser hides the cable overlay (1.7's
 * metadata 1). While {@link #PAINTED}, its block model draws the paint block in its place.
 */
public class BlockCablePaintable extends CompatBlock implements TickingBlock, IToolable, IEnergyConnectorBlock, ITooltipProvider {

    /** The red-cable stripes over the paint; 1.7's metadata 0. */
    public static final BooleanProperty OVERLAY = BooleanProperty.create("overlay");
    public static final BooleanProperty PAINTED = BooleanProperty.create("painted");

    public BlockCablePaintable(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OVERLAY, true).setValue(PAINTED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OVERLAY, PAINTED);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCablePaintable(pos, state);
    }

    /** A full-cube cable, like 1.7's: every face takes a cable. */
    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return true;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this)) {
            PowerGraph.get(server).addNode(pos.asLong(), PowerGraphProvider.INSTANCE.createData(state),
                    MachineCaps.FACE_MASK_ALL);
        }
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                     boolean movedByPiston) {
        super.onNeighborChanged(state, level, pos, neighbourBlock, movedByPiston);
        if (level instanceof ServerLevel server) LevelNodeGraph.invalidateEndpointsAt(server, pos);
    }

    /** 1.7's {@code renderAsNormalBlock}: a plain full cube, which is what the renderer can dress a cable as. */
    public static boolean allowedPaint(Block paint, Block that) {
        if (paint == Blocks.GRASS_BLOCK) return false;
        BlockState state = paint.defaultBlockState();
        return state.canOcclude() && paint != that;
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (stack.getItem() instanceof BlockItem ib && allowedPaint(ib.getBlock(), this)
                && level.getBlockEntity(pos) instanceof TileEntityCablePaintable pipe && pipe.getPaint() == null) {
            if (!level.isClientSide()) pipe.setPaint(ib.getBlock());
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand,
                           ToolType tool) {
        if (tool == ToolType.SCREWDRIVER) {
            if (level.getBlockEntity(pos) instanceof TileEntityCablePaintable pipe && pipe.getPaint() != null) {
                if (!level.isClientSide()) pipe.setPaint(null);
                return true;
            }
        }
        if (tool == ToolType.DEFUSER) {
            BlockState state = level.getBlockState(pos);
            if (!level.isClientSide()) level.setBlock(pos, state.setValue(OVERLAY, !state.getValue(OVERLAY)), 3);
            return true;
        }
        return false;
    }
}
