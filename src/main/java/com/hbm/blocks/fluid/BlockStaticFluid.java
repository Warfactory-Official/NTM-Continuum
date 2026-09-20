package com.hbm.blocks.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Base for the 1.7 {@code blocks/fluid} blocks ({@code BlockFluidClassic}/{@code BlockFluidFinite}
 * subclasses) ported as <b>static</b> blocks, the way {@link com.hbm.blocks.generic.BlockMud} already
 * is: the port has no flowing-fluid facade (HBM fluids live in their own network and never became
 * {@code FlowingFluid}s; Golem Core exposes no {@code LiquidBlock}), so a placed fluid block sits where
 * it is placed like a finite fluid at rest. Everything a player experiences on contact — the cobweb
 * drag, the damage, the contamination, the neighbour reactions, the solidifying — is ported per block.
 * Spreading is the one thing dropped; revisit when the port gains flowing fluids.
 *
 * <p>No collision (you sink in), an 8/9-height selection box (1.7 fluid bounds), a cube model with the
 * still texture on top/bottom and the flowing texture on the sides (1.7 {@code getIcon}).
 */
public abstract class BlockStaticFluid extends dev.golem.core.world.CompatBlock {

    private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 0.875, 1);

    protected BlockStaticFluid(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** The old {@code setInWeb()}: 26.1 spells it as a movement multiplier. */
    protected static void setInWeb(Entity entity, BlockState state) {
        entity.makeStuckInBlock(state, new Vec3(0.25D, 0.05D, 0.25D));
    }

    /** Whether a neighbouring block is any liquid but this one (vanilla water/lava, or another static fluid). */
    protected boolean isForeignLiquid(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(this)) return false;
        return !state.getFluidState().isEmpty() || state.getBlock() instanceof BlockStaticFluid;
    }
}
