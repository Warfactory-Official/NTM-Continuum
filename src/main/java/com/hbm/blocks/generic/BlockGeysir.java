package com.hbm.blocks.generic;

import dev.golem.core.data.Tags;

import com.hbm.entity.particle.EntityOrangeFX;
import com.hbm.entity.projectile.EntityShrapnel;
import com.hbm.particle.helper.HbmEffectNT;
import dev.golem.core.data.CompatBlockEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import dev.golem.core.registry.TickingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;

import java.util.List;
import com.hbm.tileentity.ModTerrainBlockEntities;

/**
 * 1.7 {@code BlockGeysir} + {@code TileEntityGeysir} ({@code geysir_chlorine}, {@code geysir_nether}):
 * a stone / netherrack block with a vent on top that alternates between resting and erupting while the
 * block above is air. Meta 0/1 (resting/erupting) is the {@code active} property. Drops nothing.
 *
 * <ul>
 * <li>chlorine: rests 20 t, erupts 400-499 t spitting three agent-orange clouds per tick;</li>
 * <li>nether: rests 300 or 450 t, erupts 80-139 t; only with a player within 32 blocks, throws a
 *     shrapnel one tick in three and a gas-fire puff every other tick; flame particle on the client.</li>
 * </ul>
 */
public class BlockGeysir extends CompatBlock implements TickingBlock {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public enum Kind { CHLORINE, NETHER }

    public final Kind kind;

    public BlockGeysir(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(getStateDefinition().any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (kind == Kind.NETHER) {
            level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5F, pos.getY() + 1.0625F, pos.getZ() + 0.5F, 0, 0, 0);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityGeysir(pos, state);
    }

    public static class TileEntityGeysir extends CompatBlockEntity {

        int timer;

        public TileEntityGeysir(BlockPos pos, BlockState state) {
            super(ModTerrainBlockEntities.GEYSIR.get(), pos, state);
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityGeysir te) {
            if (!(level instanceof ServerLevel world) || !world.getBlockState(pos.above()).isAir()) return;
            if (!(state.getBlock() instanceof BlockGeysir geysir)) return;

            te.timer--;
            boolean active = state.getValue(ACTIVE);

            if (te.timer <= 0) {
                te.timer = te.getDelay(world, geysir.kind, active);
                world.setBlock(pos, state.setValue(ACTIVE, !active), 2);
            }

            if (active) te.perform(world, geysir.kind);
        }

        private int getDelay(ServerLevel world, Kind kind, boolean active) {
            RandomSource rand = world.getRandom();
            return switch (kind) {
                case CHLORINE -> !active ? 20 : 400 + rand.nextInt(100);
                case NETHER -> !active ? (rand.nextBoolean() ? 300 : 450) : 80 + rand.nextInt(60);
            };
        }

        private void perform(ServerLevel world, Kind kind) {
            if (kind == Kind.CHLORINE) chlorine(world); else fire(world);
        }

        private void chlorine(ServerLevel world) {
            RandomSource rand = world.getRandom();
            for (int i = 0; i < 3; i++) {
                EntityOrangeFX fx = new EntityOrangeFX(world, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5,
                        worldPosition.getZ() + 0.5, 0.0, 0.0, 0.0);
                fx.setDeltaMovement(rand.nextGaussian() * 0.45, timer * 0.3, rand.nextGaussian() * 0.45);
                world.addFreshEntity(fx);
            }
        }

        private void fire(ServerLevel world) {
            int range = 32;
            double cx = worldPosition.getX() + 0.5, cy = worldPosition.getY() + 0.5, cz = worldPosition.getZ() + 0.5;
            if (world.getEntitiesOfClass(Player.class, new AABB(cx, cy, cz, cx, cy, cz).inflate(range, range, range)).isEmpty()) return;

            RandomSource rand = world.getRandom();
            if (rand.nextInt(3) == 0) {
                EntityShrapnel fx = new EntityShrapnel(world, cx, worldPosition.getY() + 1.5, cz);
                fx.setDeltaMovement(rand.nextGaussian() * 0.05, 0.5 + rand.nextDouble() * timer * 0.01, rand.nextGaussian() * 0.05);
                world.addFreshEntity(fx);
            }

            if (timer % 2 == 0) {
                CompoundTag data = new CompoundTag();
                data.putDouble("moX", rand.nextGaussian() * 0.05);
                data.putDouble("moY", 0.2);
                data.putDouble("moZ", rand.nextGaussian() * 0.05);
                HbmEffectNT.GasFlame.send(world, cx, worldPosition.getY() + 1.1, cz, data, 75);
            }
        }

        @Override
        protected void loadData(DataIn in) {
            timer = in.getIntOr("timer", 0);
        }

        @Override
        protected void saveData(DataOut out) {
            out.putInt("timer", timer);
        }
    }
}
