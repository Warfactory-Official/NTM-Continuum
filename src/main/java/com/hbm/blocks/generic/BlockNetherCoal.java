package com.hbm.blocks.generic;

import com.hbm.items.ModFuelItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
//? if >=26.1 {
import net.minecraft.world.entity.InsideBlockEffectApplier;
//?}
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/**
 * Burning nether coal — the 1.12.2 {@code BlockNetherCoal}, a {@code BlockOutgas(randomTick, rate=5,
 * onBreak)} that drops {@code coal_infernal}, ignites anything touching it, and licks flame/smoke out
 * of every non-downward face that borders air.
 *
 * <p>It inherits the outgas behaviour (a radon pocket on random tick and on break) from
 * {@link BlockOutgas} exactly as upstream does — the class really is an outgas block, which is why
 * mining it in the nether leaves gas behind.
 */
public class BlockNetherCoal extends BlockOutgas {

    public BlockNetherCoal(Properties properties) {
        super(properties);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(ModFuelItems.coal_infernal.get()));
    }

    @Override
    //? if >=26.1 {
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effects, boolean precise) {
    //?} else {
    /*protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
    *///?}
        if (!level.isClientSide()) entity.igniteForSeconds(3);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN) continue;                      // upstream skips the underside
            BlockPos neighbour = pos.relative(dir);
            if (!level.getBlockState(neighbour).isAir()) continue;

            // Off-axis coordinates scatter across the face; the on-axis one hugs it (the old formula).
            double x = pos.getX() + 0.5D + dir.getStepX() + random.nextDouble() - 0.5D;
            double y = pos.getY() + 0.5D + dir.getStepY() + random.nextDouble() - 0.5D;
            double z = pos.getZ() + 0.5D + dir.getStepZ() + random.nextDouble() - 0.5D;
            if (dir.getAxis() == Direction.Axis.X) x = pos.getX() + 0.5D + dir.getStepX() * 0.5D + random.nextDouble() * 0.125D * dir.getStepX();
            if (dir.getAxis() == Direction.Axis.Y) y = pos.getY() + 0.5D + dir.getStepY() * 0.5D + random.nextDouble() * 0.125D * dir.getStepY();
            if (dir.getAxis() == Direction.Axis.Z) z = pos.getZ() + 0.5D + dir.getStepZ() * 0.5D + random.nextDouble() * 0.125D * dir.getStepZ();

            level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0D, 0.0D, 0.0D);
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.0D, 0.0D);
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.1D, 0.0D);
        }
    }
}
