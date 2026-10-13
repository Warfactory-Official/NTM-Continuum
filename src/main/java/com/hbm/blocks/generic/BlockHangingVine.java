package com.hbm.blocks.generic;

import dev.golem.core.world.BlockTickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code BlockHangingVine} ({@code vine_phosphor}): glowing vines that hang from a solid face above
 * (or from more vine), halve the motion of anything inside them and cancel its fall distance. The old
 * renderer picked its icon from the block below. {@code part} carries the same choice: {@code ground}
 * when the block below has a solid top, {@code middle} when it is more vine, {@code hang} otherwise.
 * Drops itself (the old {@code onSheared} placeholder also returned the block).
 */
public class BlockHangingVine extends BlockTerrainPlant {

    public enum Part implements StringRepresentable {
        GROUND("ground"), MIDDLE("middle"), HANG("hang");

        private final String name;
        Part(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    public BlockHangingVine(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(PART, Part.HANG));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    protected boolean canStay(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState up = level.getBlockState(above);
        return up.getBlock() == this || up.isFaceSturdy(level, above, Direction.DOWN);
    }

    /** 1.7 {@code checkAndDropBlock} only sets air. */
    @Override
    protected boolean dropsWhenUnsupported(BlockState state) {
        return false;
    }

    public Part partFor(LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState b = level.getBlockState(below);
        if (b.isFaceSturdy(level, below, Direction.UP)) return Part.GROUND;
        return b.getBlock() == this ? Part.MIDDLE : Part.HANG;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!canStay(defaultBlockState(), context.getLevel(), context.getClickedPos())) return null;
        return defaultBlockState().setValue(PART, partFor(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction direction, BlockPos neighbourPos, BlockState neighbourState) {
        BlockState result = super.onUpdateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState);
        if (result != state) return result;
        return state.setValue(PART, partFor(level, pos));
    }

    /** 1.7 {@code onEntityCollidedWithBlock}: motion halved on every axis, fall distance zeroed. */
    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x * 0.5, motion.y * 0.5, motion.z * 0.5);
        entity.fallDistance = 0;
    }
}
