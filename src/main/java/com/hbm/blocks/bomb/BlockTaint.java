package com.hbm.blocks.bomb;

import com.hbm.potion.ModEffects;
import dev.golem.core.world.CompatBlock;
import dev.golem.core.world.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Taint residue, ported from the old random-spreading {@code BlockTaint}. */
public class BlockTaint extends CompatBlock {

    private static final VoxelShape COLLISION = Block.box(0, 0, 0, 16, 12, 16);
    public static final IntegerProperty TAINTAGE = IntegerProperty.create("taintage", 0, 15);

    public BlockTaint(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TAINTAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TAINTAGE);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return COLLISION;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(TAINTAGE);
        if (age >= 15) return;

        BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighbourPos = new BlockPos.MutableBlockPos();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 4 || random.nextFloat() > 0.25F) continue;

                    int x = pos.getX() + dx;
                    int y = pos.getY() + dy;
                    int z = pos.getZ() + dz;
                    targetPos.set(x, y, z);
                    BlockState targetState = level.getBlockState(targetPos);
                    if (targetState.isAir() || targetState.is(Blocks.BEDROCK)) continue;

                    int targetAge = age + 1;
                    boolean hasAir = false;
                    for (Direction direction : Direction.values()) {
                        neighbourPos.set(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ());
                        if (level.getBlockState(neighbourPos).isAir()) {
                            hasAir = true;
                            break;
                        }
                    }
                    if (!hasAir) targetAge = age + 3;
                    if (targetAge > 15) continue;
                    if (targetState.is(this) && targetState.getValue(TAINTAGE) >= targetAge) continue;

                    BlockState newState = defaultBlockState().setValue(TAINTAGE, targetAge);
                    level.setBlock(targetPos, newState, 3);
                    neighbourPos.set(x, y - 1, z);
                    if (random.nextFloat() < 0.25F && canFallThrough(level.getBlockState(neighbourPos))) {
                        FallingBlockEntity falling = FallingBlockEntity.fall(level, targetPos.immutable(), newState);
                        falling.dropItem = false;
                    }
                }
            }
        }
    }

    private static boolean canFallThrough(BlockState state) {
        return state.isAir() || state.is(Blocks.FIRE) || !state.getFluidState().isEmpty();
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity living && level.getRandom().nextInt(50) == 0) {
            Effects.addRegistered(living, ModEffects.TAINT.get(), 15 * 20,
                    15 - state.getValue(TAINTAGE));
        }
        // EntityCreeperTainted and EntityTaintCrab are not in the shared entity set yet; do not replace
        // those exact old conversions with unrelated mobs.
    }
}
