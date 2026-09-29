package com.hbm.blocks.network;

import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.items.ModConveyorItems;
import com.hbm.items.tool.ItemConveyorWand.ConveyorType;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code blocks/network/BlockConveyorBase}. The 1.7 metadata names the side items ARRIVE from; {@link #FACING}
 * here is the direction items TRAVEL, its opposite, so the blockstate turns the model the way vanilla turns a
 * furnace and the wand/placement code reads the way the player looks.
 */
public abstract class BlockConveyorBase extends CompatBlock implements IConveyorBelt {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SLAB = Shapes.box(0D, 0D, 0D, 1D, 0.25D, 1D);

    protected BlockConveyorBase(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SLAB;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SLAB;
    }

    /**
     * The wand subtype this belt comes from: 1.7 {@code getItemDropped} is {@code conveyor_wand} for every belt,
     * with {@code damageDropped} 1/2/3 on express/double/triple and 0 (regular) elsewhere, chute and lift included.
     */
    protected ConveyorType wandType() {
        return ConveyorType.REGULAR;
    }

    /**
     * Pick-block gives the wand the belt drops. Literal 1.7 would pick the hidden block's own ItemBlock
     * (these blocks have no creative tab); the wand is the only way to place a belt, so pick matches the drop.
     */
    @Override
    protected ItemStack onPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
        return ModConveyorItems.wand(wandType(), 1);
    }

    /** 1.7 {@code onBlockPlacedBy}: the belt carries items the way the placer faces. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    /** 1.7 {@code getInputDirection}: the side items arrive from, which is what the metadata spelled. */
    public Direction getInputDirection(Level level, BlockPos pos) {
        return level.getBlockState(pos).getValue(FACING).getOpposite();
    }

    /** 1.7 {@code getOutputDirection}: the side items leave by. */
    public Direction getOutputDirection(Level level, BlockPos pos) {
        return level.getBlockState(pos).getValue(FACING);
    }

    /** 1.7 {@code getTravelDirection}: the direction travel runs AGAINST; {@link #getTravelLocation} subtracts it. */
    public Direction getTravelDirection(Level level, BlockPos pos, Vec3 itemPos) {
        return level.getBlockState(pos).getValue(FACING).getOpposite();
    }

    @Override
    public boolean canItemStay(Level level, BlockPos pos, Vec3 itemPos) {
        return true;
    }

    @Override
    public Vec3 getTravelLocation(Level level, BlockPos pos, Vec3 itemPos, double speed) {
        Direction dir = getTravelDirection(level, pos, itemPos);
        Snap snap = snap(level, pos, itemPos);

        // snapping point + speed
        Vec3 dest = snap.snap().subtract(dir.getStepX() * speed, dir.getStepY() * speed, dir.getStepZ() * speed);
        // delta to get to that point
        Vec3 motion = dest.subtract(snap.item());
        double len = motion.length();
        // the effective destination towards "dest" after taking speed into consideration
        return snap.item().add(motion.x / len * speed, motion.y / len * speed, motion.z / len * speed);
    }

    @Override
    public Vec3 getClosestSnappingPosition(Level level, BlockPos pos, Vec3 itemPos) {
        return snap(level, pos, itemPos).snap();
    }

    /**
     * 1.7 {@code getClosestSnappingPosition} clamps the caller's own Vec3 in place and {@code getTravelLocation}
     * then reads the clamped value back; Vec3 is immutable here, so the pair is returned together.
     */
    protected Snap snap(Level level, BlockPos pos, Vec3 itemPos) {
        Direction dir = getTravelDirection(level, pos, itemPos);
        Vec3 clamped = clamp(pos, itemPos);

        double posX = pos.getX() + 0.5;
        double posZ = pos.getZ() + 0.5;

        if (dir.getStepX() != 0) posX = clamped.x;
        if (dir.getStepZ() != 0) posZ = clamped.z;

        return new Snap(new Vec3(posX, pos.getY() + 0.25, posZ), clamped);
    }

    protected static Vec3 clamp(BlockPos pos, Vec3 itemPos) {
        return new Vec3(Mth.clamp(itemPos.x, pos.getX(), pos.getX() + 1D), itemPos.y,
                Mth.clamp(itemPos.z, pos.getZ(), pos.getZ() + 1D));
    }

    /** 1.7 {@code onEntityCollidedWithBlock}: a dropped item older than ten ticks is picked up onto the belt. */
    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide()) return;
        if (!(entity instanceof ItemEntity item) || entity.tickCount <= 10 || entity.isRemoved()) return;

        EntityMovingItem moving = new EntityMovingItem(level);
        moving.setItemStack(item.getItem().copy());
        Vec3 snap = getClosestSnappingPosition(level, pos, entity.position());
        moving.snapTo(snap.x, snap.y, snap.z, 0F, 0F);
        level.addFreshEntity(moving);

        entity.discard();
    }

    /** 1.7 {@code onScrew}: a screwdriver turns the belt clockwise; sneaking walks the bend/type ring. */
    protected boolean screw(Level level, Player player, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!player.isShiftKeyDown()) {
            level.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()), UPDATE_ALL);
        } else {
            onSneakScrew(level, pos, state);
        }
        return true;
    }

    protected abstract void onSneakScrew(Level level, BlockPos pos, BlockState state);

    protected record Snap(Vec3 snap, Vec3 item) {}
}
