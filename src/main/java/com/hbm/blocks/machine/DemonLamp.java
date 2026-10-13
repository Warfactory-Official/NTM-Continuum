package com.hbm.blocks.machine;

import com.hbm.util.ContaminationUtil;
import dev.golem.core.data.CompatBlockEntity;
import dev.golem.core.hook.Hook;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import dev.golem.core.registry.TickingBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import com.hbm.client.render.DemonLampRenderer;
import com.hbm.tileentity.ModDecoBlockEntities;

/**
 * 1.7 {@code DemonLamp} + {@code TileEntityDemonLamp}: a demon core in a lamp housing, mounted on the
 * clicked face. Every tick it irradiates every living thing within 25 blocks with 100,000 RAD/s,
 * divided by the summed blast resistance of the blocks in the way and by the distance squared, and
 * sets anything within two blocks on fire for 100 damage. Drawn (with its additive light fan) by
 * {@code DemonLampRenderer}.
 */
public class DemonLamp extends Block implements TickingBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public DemonLamp(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
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
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDemonLamp(pos, state);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    public static class TileEntityDemonLamp extends CompatBlockEntity {

        /** One living thing the lamp just irradiated, with its dose (after the RADIATION contamination). */
        @FunctionalInterface
        public interface Irradiated {
            void irradiated(ServerLevel level, BlockPos lamp, LivingEntity entity, float dose);
        }

        /** Plain HBM registers none (H-79); NTM: Space adds the dose as neutrons too (hbm-space). */
        public static final Hook<Irradiated> IRRADIATED = Hook.notifying();

        public TileEntityDemonLamp(BlockPos pos, BlockState state) {
            super(ModDecoBlockEntities.DEMON_LAMP.get(), pos, state);
        }

        public void serverTick() {
            if (getLevel() instanceof ServerLevel level) radiate(level, worldPosition);
        }

        private void radiate(ServerLevel world, BlockPos at) {
            float rads = 100000F;
            double range = 25D;
            double x = at.getX(), y = at.getY(), z = at.getZ();
            List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class,
                    new AABB(x + 0.5, y + 0.5, z + 0.5, x + 0.5, y + 0.5, z + 0.5).inflate(range, range, range));
            for (LivingEntity e : entities) {
                Vec3 vec = new Vec3(e.getX() - (x + 0.5), (e.getY() + e.getEyeHeight()) - (y + 0.5), e.getZ() - (z + 0.5));
                double len = vec.length();
                vec = vec.normalize();
                float res = 0;
                for (int i = 1; i < len; i++) {
                    int ix = (int) Math.floor(x + 0.5 + vec.x * i);
                    int iy = (int) Math.floor(y + 0.5 + vec.y * i);
                    int iz = (int) Math.floor(z + 0.5 + vec.z * i);
                    res += world.getBlockState(new BlockPos(ix, iy, iz)).getBlock().getExplosionResistance();
                }
                if (res < 1) res = 1;
                float eRads = rads;
                eRads /= res;
                eRads /= (float) (len * len);
                ContaminationUtil.contaminate(e, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.CREATIVE, eRads);
                for (Irradiated listener : IRRADIATED) listener.irradiated(world, at, e, eRads);
                if (len < 2) {
                    Compat.hurt(e, world, world.damageSources().inFire(), 100);
                }
            }
        }
    }
}
