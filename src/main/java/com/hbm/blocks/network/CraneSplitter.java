package com.hbm.blocks.network;

import com.hbm.blocks.ITooltipProvider;
import com.hbm.api.block.IToolable;
import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.api.conveyor.IConveyorItem;
import com.hbm.api.conveyor.IConveyorPackage;
import com.hbm.api.conveyor.IEnterableBlock;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.interfaces.ILookOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import com.hbm.tileentity.network.TileEntityCraneSplitter;

/**
 * 1.7 {@code blocks/network/CraneSplitter}: two belt lanes side by side that deal one input stack out between
 * them. A {@code BlockDummyable} of dimensions {0,0,0,0,0,1}: the core is the LEFT lane and its one cell the
 * right. {@link #FACING} keeps the 1.7 metadata meaning, the side items arrive from.
 */
public class CraneSplitter extends BlockMultiblockMachine implements IConveyorBelt, IEnterableBlock, IToolable, ILookOverlay, ITooltipProvider {

    private static final int[] DIMENSIONS = {0, 0, 0, 0, 0, 1};

    public CraneSplitter(Properties properties) {
        super(properties);
    }

    /** 1.7 {@code getRotation(DOWN)} on the facing: the side the right-hand lane sits on. */
    public static Direction rightLane(Direction facing) {
        return facing.getCounterClockWise();
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCraneSplitter(pos, state);
    }

    private static @Nullable BlockPos coreOf(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(CELL)) return null;
        return state.getValue(CELL) ? MultiblockSurface.coreOfCell(level, pos) : pos;
    }

    private static @Nullable Direction facingAt(Level level, BlockPos pos) {
        BlockPos core = coreOf(level, pos);
        if (core == null) return null;
        BlockState state = level.getBlockState(core);
        return state.hasProperty(FACING) ? state.getValue(FACING) : null;
    }

    /** 1.7 {@code getTravelDirection}: the direction travel runs AGAINST; both lanes answer the core's facing. */
    public Direction getTravelDirection(Level level, BlockPos pos, @Nullable Vec3 itemPos) {
        Direction facing = facingAt(level, pos);
        return facing == null ? Direction.NORTH : facing;
    }

    @Override
    public boolean canItemEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorItem entity) {
        return getTravelDirection(level, pos, null) == dir;
    }

    @Override
    public boolean canPackageEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorPackage entity) {
        return false;
    }

    @Override
    public void onPackageEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorPackage entity) {}

    @Override
    public void onItemEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorItem entity) {
        BlockPos core = coreOf(level, pos);
        if (core == null) return;
        if (!(level.getBlockEntity(core) instanceof TileEntityCraneSplitter splitter)) return;

        Direction right = rightLane(level.getBlockState(core).getValue(FACING));
        List<ItemStack> splits = splitter.splitStack(entity.getItemStack());

        spawnMovingItem(level, core, splits.get(0));
        spawnMovingItem(level, core.relative(right), splits.get(1));
    }

    private void spawnMovingItem(Level level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) return;

        Vec3 snap = getClosestSnappingPosition(level, pos, Vec3.atCenterOf(pos));
        EntityMovingItem moving = new EntityMovingItem(level);
        moving.snapTo(snap.x, snap.y, snap.z, 0F, 0F);
        moving.setItemStack(stack);
        level.addFreshEntity(moving);
    }

    @Override
    public boolean canItemStay(Level level, BlockPos pos, Vec3 itemPos) {
        return true;
    }

    @Override
    public Vec3 getTravelLocation(Level level, BlockPos pos, Vec3 itemPos, double speed) {
        Direction dir = getTravelDirection(level, pos, itemPos);
        Vec3 clamped = clamp(pos, itemPos);
        Vec3 snap = snap(pos, clamped, dir);

        Vec3 dest = snap.subtract(dir.getStepX() * speed, dir.getStepY() * speed, dir.getStepZ() * speed);
        Vec3 motion = dest.subtract(clamped);
        double len = motion.length();
        return clamped.add(motion.x / len * speed, motion.y / len * speed, motion.z / len * speed);
    }

    @Override
    public Vec3 getClosestSnappingPosition(Level level, BlockPos pos, Vec3 itemPos) {
        return snap(pos, clamp(pos, itemPos), getTravelDirection(level, pos, itemPos));
    }

    private static Vec3 snap(BlockPos pos, Vec3 clamped, Direction dir) {
        double posX = dir.getStepX() != 0 ? clamped.x : pos.getX() + 0.5;
        double posZ = dir.getStepZ() != 0 ? clamped.z : pos.getZ() + 0.5;
        return new Vec3(posX, pos.getY() + 0.25, posZ);
    }

    private static Vec3 clamp(BlockPos pos, Vec3 itemPos) {
        return new Vec3(Mth.clamp(itemPos.x, pos.getX(), pos.getX() + 1D), itemPos.y,
                Mth.clamp(itemPos.z, pos.getZ(), pos.getZ() + 1D));
    }

    /** 1.7 {@code onScrew}: the lane clicked takes the step, and sneaking takes it back. */
    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (level.isClientSide()) return true;
        if (tool != ToolType.SCREWDRIVER) return false;

        BlockPos core = coreOf(level, pos);
        if (core == null) return false;
        if (!(level.getBlockEntity(core) instanceof TileEntityCraneSplitter splitter)) return false;

        // The core of the dummy is always the left hand block
        boolean isLeft = core.equals(pos);
        int adjust = player.isShiftKeyDown() ? -1 : 1;

        if (isLeft) {
            splitter.leftRatio = (byte) Mth.clamp(splitter.leftRatio + adjust, 1, 16);
        } else {
            splitter.rightRatio = (byte) Mth.clamp(splitter.rightRatio + adjust, 1, 16);
        }
        splitter.setChanged();

        return true;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        BlockPos core = coreOf(level, pos);
        if (core == null) return null;
        if (!(level.getBlockEntity(core) instanceof TileEntityCraneSplitter splitter)) return null;

        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000,
                List.of("Splitter ratio: " + splitter.leftRatio + ":" + splitter.rightRatio));
    }
}
