package com.hbm.blocks.generic;

import com.hbm.entity.particle.EntityChlorineFX;
import com.hbm.entity.particle.EntityCloudFX;
import com.hbm.entity.particle.EntityPinkCloudFX;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import dev.golem.core.registry.TickingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;
import com.hbm.tileentity.ModTerrainBlockEntities;

/**
 * 1.7 {@code BlockVent} + {@code TileEntityVent} ({@code vent_chlorine}, {@code vent_cloud},
 * {@code vent_pink_cloud}): while powered, every tick spawns one gas-cloud entity at a gaussian offset
 * (spread 1.5 / 1.75 / 2) if that spot is not a full cube, moving at half the offset. Drops nothing.
 */
public class BlockVent extends CompatBlock implements TickingBlock {

    public enum Kind {
        CHLORINE(1.5), CLOUD(1.75), PINK_CLOUD(2.0);

        public final double spread;
        Kind(double spread) { this.spread = spread; }
    }

    public final Kind kind;

    public BlockVent(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityVent(pos, state);
    }

    public static class TileEntityVent extends BlockEntity {

        public TileEntityVent(BlockPos pos, BlockState state) {
            super(ModTerrainBlockEntities.VENT.get(), pos, state);
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityVent vent) {
            if (!(level instanceof ServerLevel world) || !world.hasNeighborSignal(pos)) return;
            if (!(state.getBlock() instanceof BlockVent block)) return;

            RandomSource rand = world.getRandom();
            double x = rand.nextGaussian() * block.kind.spread;
            double y = rand.nextGaussian() * block.kind.spread;
            double z = rand.nextGaussian() * block.kind.spread;
            BlockPos at = pos.offset((int) x, (int) y, (int) z);
            if (Compat.isSolidRender(world.getBlockState(at), world, at)) return;

            Entity fx = switch (block.kind) {
                case CHLORINE -> new EntityChlorineFX(world, at.getX(), at.getY(), at.getZ(), x / 2, y / 2, z / 2);
                case CLOUD -> new EntityCloudFX(world, at.getX(), at.getY(), at.getZ(), x / 2, y / 2, z / 2);
                case PINK_CLOUD -> new EntityPinkCloudFX(world, at.getX(), at.getY(), at.getZ(), x / 2, y / 2, z / 2);
            };
            world.addFreshEntity(fx);
        }
    }
}
