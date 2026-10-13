package com.hbm.blocks.bomb;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Digamma matter: the 1.12.2 {@code DigammaMatter}. The rarest of the FEL's DRX outputs: it only
 * appears instead of {@link BlockDigammaFlame} when {@code MainRegistry.polaroidID == 11}, a 1-in-18
 * per-world easter egg.
 *
 * <p>It behaves like a spreading blight. On placement it schedules itself {@code 10 + rand(40)} ticks
 * out; when that fires it <b>deletes itself</b> and seeds every neighbour within a Manhattan distance
 * of 1 or 2 (i.e. faces and edges, not corners) at {@code meta + 1}. At {@code meta >= 7} it
 * vanishes, so the growth burns itself out after seven generations rather than consuming the world.
 *
 * <p>Unbreakable with 18,000,000 blast resistance: nothing removes it, you wait it out. It is
 * non-solid and has no collision: the old {@code getBoundingBox} even randomised its visual shape
 * every query, which is not reproducible under 26.1's cached shapes and is dropped.
 */
public class BlockDigammaMatter extends Block {

    /** Upstream carries the generation count in the block's metadata; here it is a property of its own. */
    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 15);

    public BlockDigammaMatter(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(META, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(META);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, 10 + level.getRandom().nextInt(40));
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int meta = state.getValue(META);
        // The cell always dies; at generation 7 it dies WITHOUT seeding, which is what bounds the growth.
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        if (meta >= 7) return;

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    int dist = Math.abs(i) + Math.abs(j) + Math.abs(k);
                    if (dist == 0 || dist >= 3) continue; // faces + edges, never the 8 corners
                    BlockPos target = pos.offset(i, j, k);
                    if (level.getBlockState(target).is(this)) continue;
                    level.setBlock(target, state.setValue(META, meta + 1), 3);
                }
            }
        }
    }
}
