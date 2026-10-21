package com.hbm.blocks.bomb;

import com.hbm.api.block.IFuckingExplode;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.item.EntityTntNtm;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.IBlockExplodedVNT;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.interfaces.IBomb;
import dev.golem.core.world.CompatBlock;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code BlockPlasticExplosive}: the solid blocks of C-4 and Semtex. Redstone sets off a
 * 20-strength standard VNT blast that drops nothing; caught in another explosion they become a
 * zero-fuse primed entity. Oriented like a 1.7 piston on placement: {@code FACING} is the piston
 * orientation (the player's look direction, or up/down when standing right over/under the block) and
 * the {@code _front} texture sits on the opposite face, toward the player.
 */
public class BlockPlasticExplosive extends CompatBlock implements IBomb, IFuckingExplode, IBlockExplodedVNT {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public BlockPlasticExplosive(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        LivingEntity placer = context.getPlayer();
        Direction facing = placer == null ? Direction.NORTH : determineOrientation(context.getClickedPos(), placer);
        return defaultBlockState().setValue(FACING, facing);
    }

    /** 1.7 {@code BlockPistonBase.determineOrientation}, verbatim in its position and yaw rules. */
    public static Direction determineOrientation(BlockPos pos, LivingEntity player) {
        if (Mth.abs((float) player.getX() - (float) pos.getX()) < 2.0F
                && Mth.abs((float) player.getZ() - (float) pos.getZ()) < 2.0F) {
            double d0 = player.getY() + 1.82D; // 1.7: posY + 1.82 - yOffset, and a player's yOffset is 0
            if (d0 - pos.getY() > 2.0D) return Direction.DOWN;
            if (pos.getY() - d0 > 0.0D) return Direction.UP;
        }
        int l = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        return switch (l) {
            case 0 -> Direction.SOUTH;
            case 1 -> Direction.WEST;
            case 2 -> Direction.NORTH;
            default -> Direction.EAST;
        };
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                     boolean movedByPiston) {
        if (!level.isClientSide() && level.hasNeighborSignal(pos)) {
            explode(level, pos, null);
        }
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.DETONATED;
        // 1.7 does not remove the block itself; the 20-strength blast eats it (and drops nothing).
        new ExplosionVNT(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20).makeStandard()
                .setBlockProcessor(new BlockProcessorStandard().setNoDrop()).explode();
        return BombReturnCode.DETONATED;
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
    public boolean onVNTExploded(ServerLevel level, BlockPos pos, BlockState state, ExplosionVNT explosion) {
        level.removeBlock(pos, false);
        EntityTntNtm primed = new EntityTntNtm(ModNtmEntities.TNT_NTM.get(), level,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, IBlockExplodedVNT.igniter(explosion), state);
        primed.setFuse(0);
        level.addFreshEntity(primed);
        return true;
    }

    @Override
    public void explodeEntity(Level level, double x, double y, double z, EntityTntNtm entity) {
        explode(level, BlockPos.containing(x, y, z), entity);
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
