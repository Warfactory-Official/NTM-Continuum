package com.hbm.blocks.bomb;

import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityMist;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.interfaces.IBomb;
import com.hbm.inventory.fluid.Fluids;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.bomb.TileEntityBombMulti;

/**
 * 1.7 {@code BombMulti}: the Multi Purpose Bomb. Four TNT in the outer slots make it "loaded"; the two
 * middle slots (2 and 5) each pick an extra payload (gunpowder / TNT boost, cluster pellets, fire
 * powder, poison powder, gas pellets). Fires on redstone or a detonator; its inventory drops when the
 * block is broken (and is cleared first when it detonates). {@code FACING} 2..5 is the 1.7 metadata.
 */
public class BombMulti extends CompatBlock implements TickingBlock, IBomb {

    public static final IntegerProperty FACING = IntegerProperty.create("facing", 2, 5);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);

    public final float explosionBaseValue = 8.0F;

    public BombMulti(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, 2));
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
        return new TileEntityBombMulti(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        // 1.7 onBlockPlacedBy: i = floor(yaw * 4 / 360 + 0.5) & 3 -> 0:5, 1:3, 2:4, 3:2.
        int facing = switch (context.getHorizontalDirection()) {
            case SOUTH -> 5;
            case WEST -> 3;
            case NORTH -> 4;
            default -> 2; // EAST
        };
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            dev.golem.core.gui.Menus.openWithPos(serverPlayer, provider, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                     boolean movedByPiston) {
        if (level.isClientSide() || !level.hasNeighborSignal(pos)) return;
        if (level.getBlockEntity(pos) instanceof TileEntityBombMulti entity && entity.isLoaded()) {
            igniteTestBomb(level, pos, null);
        }
    }

    public BombReturnCode igniteTestBomb(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.ERROR_MISSING_COMPONENT;
        if (!(level.getBlockEntity(pos) instanceof TileEntityBombMulti entity) || !entity.isLoaded()) {
            return BombReturnCode.ERROR_MISSING_COMPONENT;
        }

        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        float explosionValue = this.explosionBaseValue;
        int clusterCount = 0;
        int fireRadius = 0;
        int poisonRadius = 0;
        int gasCloud = 0;

        for (int type : new int[] { entity.return2type(), entity.return5type() }) {
            switch (type) {
                case 1 -> explosionValue += 1.0F;
                case 2 -> explosionValue += 4.0F;
                case 3 -> clusterCount += 50;
                case 4 -> fireRadius += 10;
                case 5 -> poisonRadius += 15;
                case 6 -> gasCloud += 50;
                default -> { }
            }
        }

        entity.clearSlots();
        level.removeBlock(pos, false);
        ExplosionLarge.explode(level, x, y, z, explosionValue, true, true, true);

        if (clusterCount > 0) {
            ExplosionChaos.cluster(level, x + 0.5, y + 0.5, z + 0.5, clusterCount, 0, (float) Math.PI * 0.5F,
                    (float) Math.PI * 2F, (float) Math.PI * 0.125F, 0.375F);
        }
        if (fireRadius > 0 && level instanceof ServerLevel server) {
            ExplosionChaos.igniteAllBlocks(server, x, y, z, fireRadius);
        }
        if (poisonRadius > 0) {
            ExplosionNukeGeneric.wasteNoSchrab(level, pos, poisonRadius); // 1.7 BombMulti:170
        }
        if (gasCloud > 0) {
            EntityMist mist = new EntityMist(ModEntities.MIST.get(), level);
            mist.setType(Fluids.CHLORINE);
            mist.setPos(x + 0.5, y + 0.5, z + 0.5);
            mist.setArea(gasCloud * 15F / 50F, gasCloud * 7.5F / 50F);
            level.addFreshEntity(mist);
        }
        return BombReturnCode.DETONATED;
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        if (level.getBlockEntity(pos) instanceof TileEntityBombMulti entity && entity.isLoaded()) {
            return igniteTestBomb(level, pos, detonator);
        }
        return BombReturnCode.ERROR_MISSING_COMPONENT;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(Direction.from3DDataValue(state.getValue(FACING))).get3DDataValue());
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(Direction.from3DDataValue(state.getValue(FACING))).get3DDataValue());
    }
}
