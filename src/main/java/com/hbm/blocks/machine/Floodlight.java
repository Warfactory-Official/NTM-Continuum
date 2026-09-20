package com.hbm.blocks.machine;

import com.hbm.api.block.IToolable;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import dev.golem.core.registry.TickingBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import com.hbm.client.render.FloodlightRenderer;
import com.hbm.tileentity.machine.TileEntityFloodlight;

/**
 * 1.7 {@code Floodlight}: the powered floodlight. Old metadata {@code side} (0-5) is {@link #FACING} — the
 * face it was placed on, which is also where power comes in from behind — and the {@code +6} the old
 * code added for an up/down fixture placed while looking north or south is {@link #FLIP}. The pitch of
 * the lamp head lives in the block entity ({@code rotation}) and is set from the placer's pitch, or
 * re-set with a screwdriver. Drawn by {@code FloodlightRenderer}.
 */
public class Floodlight extends CompatBlock implements IToolable, TickingBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty FLIP = BooleanProperty.create("flip");

    public Floodlight(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(FLIP, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FLIP);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFloodlight(pos, state);
    }

    /** The old metadata: 0-5 the facing, +6 when flipped. */
    public static int meta(BlockState state) {
        return state.getValue(FACING).ordinal() + (state.getValue(FLIP) ? 6 : 0);
    }

    //only method that respects sides, called first for orientation
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    //only method with player param, called second for variable rotation
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer != null) setAngle(level, pos, placer, true);
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        setAngle(level, pos, player, false);
        return true;
    }

    public void setAngle(Level level, BlockPos pos, LivingEntity player, boolean updateMeta) {
        int i = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        float rotation = player.getXRot();
        if (level.getBlockEntity(pos) instanceof TileEntityFloodlight floodlight) {
            BlockState state = level.getBlockState(pos);
            int meta = state.getValue(FACING).ordinal();
            if (meta == 0 || meta == 1) {
                if (i == 0 || i == 2) if (updateMeta) level.setBlock(pos, state.setValue(FLIP, true), 3);
                if (meta == 1) if (i == 0 || i == 1) rotation = 180F - rotation;
                if (meta == 0) if (i == 0 || i == 3) rotation = 180F - rotation;
            }
            floodlight.rotation = -Math.round(rotation / 5F) * 5F;
            if (floodlight.isOn) floodlight.destroyLights();
            floodlight.setChanged();
            floodlight.dataChanged();
        }
    }

    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        // The beams check their source every 5 ticks and remove themselves; nothing to do here (1.7 had no breakBlock).
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
