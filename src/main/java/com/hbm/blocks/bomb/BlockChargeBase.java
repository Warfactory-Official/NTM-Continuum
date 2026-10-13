package com.hbm.blocks.bomb;

import com.hbm.api.block.IFuckingExplode;
import com.hbm.api.block.IToolable;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.item.EntityTntNtm;
import com.hbm.interfaces.IBomb;
import com.hbm.sound.ModSounds;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import dev.golem.core.world.SubLevels;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.bomb.TileEntityCharge;

/**
 * 1.7 {@code BlockChargeBase}: a timed charge stuck to any face of a solid block. Right-click cycles
 * the timer (0, 5s, 10s, 15s, 30s, 1m, 3m, 5m), sneak-click arms it, and it can only be disarmed or
 * removed with the defuser. {@code FACING} is the face it was placed on (1.7 stored the side as the
 * metadata), so the supporting block is on the opposite side.
 *
 * <p>Load-bearing 1.7 oddities kept here: breaking the block by any means other than the defuser
 * (including losing its support) sets it off, because {@code breakBlock} explodes unless {@code safe}
 * is raised; and being caught in another explosion both removes it (which explodes it) and spawns a
 * zero-fuse primed entity that explodes it again.
 */
public abstract class BlockChargeBase extends CompatBlock implements TickingBlock, IBomb, IToolable, IFuckingExplode {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    /** Raised while the charge removes itself so the removal hook does not detonate it. */
    public static boolean safe = false;

    private static final VoxelShape SHAPE_DOWN = Block.box(0, 10, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_UP = Block.box(0, 0, 0, 16, 6, 16);
    private static final VoxelShape SHAPE_NORTH = Block.box(0, 0, 10, 16, 16, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0, 0, 0, 16, 16, 6);
    private static final VoxelShape SHAPE_WEST = Block.box(10, 0, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_EAST = Block.box(0, 0, 0, 6, 16, 16);

    protected BlockChargeBase(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCharge(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> SHAPE_DOWN;
            case UP -> SHAPE_UP;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
        };
    }

    /** 1.7 getCollisionBoundingBoxFromPool returns null: the charge is walked through. */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    private static boolean supported(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (!supported(context.getLevel(), context.getClickedPos(), face)) return null;
        return defaultBlockState().setValue(FACING, face);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return supported(level, pos, state.getValue(FACING));
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                     boolean movedByPiston) {
        if (level.isClientSide()) return;
        if (!supported(level, pos, state.getValue(FACING))) {
            // 1.7 setBlockToAir -> breakBlock -> explode (safe is false here). Kept.
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        // Carried by a sub-level system (Create Aeronautics), not broken: the charge lives on at its new position.
        if (!safe && !SubLevels.isMoving(level)) explode(level, pos, null);
    }

    @Override
    protected boolean onExplosion(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion,
                                  BiConsumer<ItemStack, BlockPos> onHit) {
        LivingEntity igniter = explosion.getIndirectSourceEntity();
        level.removeBlock(pos, false);
        EntityTntNtm primed = new EntityTntNtm(ModNtmEntities.TNT_NTM.get(), level,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, igniter, state);
        primed.setFuse(0);
        level.addFreshEntity(primed);
        return true;
    }

    @Override
    public void explodeEntity(Level level, double x, double y, double z, EntityTntNtm entity) {
        explode(level, BlockPos.containing(x, y, z), entity);
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit,
                           InteractionHand hand, ToolType tool) {
        if (tool != ToolType.DEFUSER) return false;
        if (level.isClientSide()) return true;
        if (!(level.getBlockEntity(pos) instanceof TileEntityCharge charge)) return false;

        if (charge.started) {
            charge.started = false;
            level.playSound(null, pos, ModSounds.FSTBMB_START.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            charge.setChanged();
        } else {
            safe = true;
            level.destroyBlock(pos, false);
            Block.popResource(level, pos, new ItemStack(this));
            safe = false;
        }
        return true;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof TileEntityCharge charge)) return InteractionResult.SUCCESS;

        if (!charge.started) {
            if (player.isShiftKeyDown()) {
                if (charge.timer > 0) {
                    charge.started = true;
                    level.playSound(null, pos, ModSounds.FSTBMB_START.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            } else {
                if (charge.timer == 0) charge.timer = 100;
                else if (charge.timer == 100) charge.timer = 200;
                else if (charge.timer == 200) charge.timer = 300;
                else if (charge.timer == 300) charge.timer = 600;
                else if (charge.timer == 600) charge.timer = 1200;
                else if (charge.timer == 1200) charge.timer = 3600;
                else if (charge.timer == 3600) charge.timer = 6000;
                else charge.timer = 0;

                level.playSound(null, pos, ModSounds.TECH_BOOP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            charge.setChanged();
        }
        // 1.7 returned false on the server, but a consumed interaction is what stops the block being used
        // "through" (IMPLEMENTATION.md section 22).
        return InteractionResult.CONSUME;
    }

    /** The shared 1.7 addInformation lines; subclasses append their own. */
    public List<Component> tooltip() {
        return List.of(
                Component.literal("Right-click to change timer.").withStyle(ChatFormatting.YELLOW),
                Component.literal("Sneak-click to arm.").withStyle(ChatFormatting.YELLOW),
                Component.literal("Can only be disarmed and removed with defuser.").withStyle(ChatFormatting.RED));
    }

    /** Removes the block without re-entering {@link #explode}; every subclass detonation starts with this. */
    protected static void removeSafely(Level level, BlockPos pos) {
        safe = true;
        level.removeBlock(pos, false);
        safe = false;
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
