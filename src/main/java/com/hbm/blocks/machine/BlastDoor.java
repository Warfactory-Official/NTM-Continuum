package com.hbm.blocks.machine;

import com.hbm.interfaces.IBomb;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.machine.TileEntityBlastDoor;
import com.hbm.tileentity.machine.TileEntityLockableBase;
import com.hbm.interfaces.IRadResistantBlock;
import net.minecraft.world.level.BlockGetter;

/**
 * Port of 1.7.10 {@code BlastDoor}: the old one-wide, six-high sliding blast door. The core is the
 * bottom block; the leaf above it is {@link DummyBlockBlast}. The original kept only two metadata
 * values (2 for a north/south-facing player, 3 for east/west), which is the {@link #AXIS} the leaf
 * spans.
 */
public class BlastDoor extends CompatBlock implements TickingBlock, IBomb, IRadResistantBlock {

    /** {@code X} is the original's meta 2 (the renderer turns a further 90 degrees), {@code Z} is meta 3. */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public BlastDoor(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    /** A pillar: a quarter turn swaps the horizontal axes, a half turn or a mirror leaves it alone. */
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> switch (state.getValue(AXIS)) {
                case X -> state.setValue(AXIS, Direction.Axis.Z);
                case Z -> state.setValue(AXIS, Direction.Axis.X);
                default -> state;
            };
            default -> state;
        };
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityBlastDoor(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof TileEntityBlastDoor entity) {
                if (!entity.isLocked()) {
                    entity.tryToggle();
                    return BombReturnCode.TRIGGERED;
                }
                return BombReturnCode.ERROR_INCOMPATIBLE;
            }
        }
        return BombReturnCode.UNDEFINED;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        } else if (TileEntityLockableBase.LOCK_TOOLS.test(player.getMainHandItem())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!player.isShiftKeyDown()) {
            if (level.getBlockEntity(pos) instanceof TileEntityBlastDoor entity) {
                if (entity.isLocked()) {
                    if (entity.canAccess(player))
                        entity.tryToggle();
                } else {
                    entity.tryToggle();
                }
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer == null || level.isClientSide()) return;

        int i = Mth.floor(placer.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        Direction.Axis axis = (i == 1 || i == 3) ? Direction.Axis.Z : Direction.Axis.X;
        level.setBlock(pos, state.setValue(AXIS, axis), 2);

        if (!(level.getBlockEntity(pos) instanceof TileEntityBlastDoor te)) return;

        //frame
        if (!(te.placeDummy(pos.above(1), false) &&
                te.placeDummy(pos.above(2), false) &&
                te.placeDummy(pos.above(3), false) &&
                te.placeDummy(pos.above(4), false) &&
                te.placeDummy(pos.above(5), false) &&
                te.placeDummy(pos.above(6), false)))
            level.destroyBlock(pos, true);
    }

    /** 1.12.2: the blast door seals radiation only while closed. */
    @Override
    public boolean isRadResistant(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileEntityBlastDoor door && door.state == 0;
    }
}
