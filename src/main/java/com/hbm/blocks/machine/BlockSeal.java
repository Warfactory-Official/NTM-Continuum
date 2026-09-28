package com.hbm.blocks.machine;

import com.hbm.blocks.ModRadarUtilityBlocks;
import com.hbm.interfaces.IBomb;
import com.hbm.tileentity.machine.TileEntityHatch;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code BlockSeal}, the "Silo Hatch Opener": the controller of a square ring of {@code seal_frame} blocks
 * (controllers count as frame) lying level with it, the ring's near edge running through the controller. A
 * click, a rising redstone edge or a detonator toggles the hatch: it fills every air block inside the ring with
 * {@code seal_hatch}, or clears the hatch blocks back to air. Rings from 3x3 to 13x13 are found, smallest first.
 *
 * <p>1.7 kept the orientation in metadata 0-3 and "was powered" as +4. The metadata named the direction the
 * ring extends away from the controller; the textured controller face looks the other way, at the placer:
 * meta 2 (+Z ring) showed the face on the north side. {@link #FACING} is that face, so the ring lies on
 * {@code FACING.getOpposite()}; {@link #POWERED} is the +4.
 */
public class BlockSeal extends CompatBlock implements IBomb {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public BlockSeal(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    /** 1.7 {@code onBlockPlacedBy}: the ring extends the way the placer looks; the face looks back at them. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer == null) return;
        Direction look = placer.getDirection();
        level.setBlock(pos, state.setValue(FACING, look.getOpposite()), 2);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        toggle(level, pos);
        return InteractionResult.SUCCESS;
    }

    /** Closes an open seal or opens a closed one; returns the ring size, 0 for no ring. */
    private static int toggle(Level level, BlockPos pos) {
        int i = getFrameSize(level, pos);
        if (i != 0) {
            if (isSealClosed(level, pos, i)) openSeal(level, pos, i);
            else closeSeal(level, pos, i);
        }
        return i;
    }

    /** The direction the ring lies in from the controller. */
    private static Direction ringSide(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.hasProperty(FACING) ? state.getValue(FACING).getOpposite() : Direction.SOUTH;
    }

    private static boolean isFrame(Level level, int x, int y, int z) {
        Block b = level.getBlockState(new BlockPos(x, y, z)).getBlock();
        return b == com.hbm.blocks.ModBlocks.seal_frame.get() || b == ModRadarUtilityBlocks.seal_controller.get();
    }

    public static int getFrameSize(Level level, BlockPos pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        Direction side = ringSide(level, pos);

        for (int size = 1; size < 7; size++) {

            boolean valid = true;
            int xOff = side.getStepX() * size;
            int zOff = side.getStepZ() * size;

            for (int X = x - size; X <= x + size; X++) {
                if (!isFrame(level, X + xOff, y, z + size + zOff)) valid = false;
            }
            for (int X = x - size; X <= x + size; X++) {
                if (!isFrame(level, X + xOff, y, z - size + zOff)) valid = false;
            }
            for (int Z = z - size; Z <= z + size; Z++) {
                if (!isFrame(level, x - size + xOff, y, Z + zOff)) valid = false;
            }
            for (int Z = z - size; Z <= z + size; Z++) {
                if (!isFrame(level, x + size + xOff, y, Z + zOff)) valid = false;
            }

            if (valid) return size;
        }

        return 0;
    }

    public static void closeSeal(Level level, BlockPos pos, int size) {
        Direction side = ringSide(level, pos);
        int xOff = side.getStepX() * size;
        int zOff = side.getStepZ() * size;

        for (int X = pos.getX() - size + 1; X <= pos.getX() + size - 1; X++) {
            for (int Z = pos.getZ() - size + 1; Z <= pos.getZ() + size - 1; Z++) {
                BlockPos at = new BlockPos(X + xOff, pos.getY(), Z + zOff);
                if (level.getBlockState(at).isAir() && !level.isClientSide()) {
                    level.setBlockAndUpdate(at, ModRadarUtilityBlocks.seal_hatch.get().defaultBlockState());
                    if (level.getBlockEntity(at) instanceof TileEntityHatch hatch) hatch.setControllerPos(pos);
                }
            }
        }
    }

    public static void openSeal(Level level, BlockPos pos, int size) {
        Direction side = ringSide(level, pos);
        int xOff = side.getStepX() * size;
        int zOff = side.getStepZ() * size;

        for (int X = pos.getX() - size + 1; X <= pos.getX() + size - 1; X++) {
            for (int Z = pos.getZ() - size + 1; Z <= pos.getZ() + size - 1; Z++) {
                BlockPos at = new BlockPos(X + xOff, pos.getY(), Z + zOff);
                if (level.getBlockState(at).getBlock() == ModRadarUtilityBlocks.seal_hatch.get() && !level.isClientSide()) {
                    level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    public static boolean isSealClosed(Level level, BlockPos pos, int size) {
        Direction side = ringSide(level, pos);
        int xOff = side.getStepX() * size;
        int zOff = side.getStepZ() * size;

        for (int X = pos.getX() - size + 1; X <= pos.getX() + size - 1; X++) {
            for (int Z = pos.getZ() - size + 1; Z <= pos.getZ() + size - 1; Z++) {
                if (level.getBlockState(new BlockPos(X + xOff, pos.getY(), Z + zOff)).getBlock()
                        == ModRadarUtilityBlocks.seal_hatch.get()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, net.minecraft.world.entity.@Nullable Entity detonator) {
        if (!level.isClientSide()) {
            return toggle(level, pos) != 0 ? BombReturnCode.TRIGGERED : BombReturnCode.ERROR_INCOMPATIBLE;
        }
        return BombReturnCode.UNDEFINED;
    }

    /** 1.7: a rising redstone edge toggles once and latches the +4 bit until the power goes away. */
    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        if (level.isClientSide()) return;
        if (level.hasNeighborSignal(pos)) {
            if (!state.getValue(POWERED)) {
                level.setBlock(pos, state.setValue(POWERED, true), 2);
                toggle(level, pos);
            }
        } else if (state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, false), 2);
        }
    }

    /** Structure rotation and mirroring turn the placed facing, as vanilla's directional blocks do. */
    @Override
    public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
