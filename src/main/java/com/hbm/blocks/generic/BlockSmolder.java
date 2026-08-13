package com.hbm.blocks.generic;

import com.hbm.items.ModFuelItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
//? if >=26.1 {
import net.minecraft.world.entity.InsideBlockEffectApplier;
//?}
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/**
 * Smoldering netherrack — the 1.12.2 {@code BlockSmolder}. Drops {@code powder_fire}, sets anything
 * standing on it on fire for 3 seconds, and spits lava/flame particles from its top face when the
 * block above is air.
 *
 * <p>Deviation: 1.12 hooked {@code onEntityWalk} (touching the top face); 26.1's nearest cross-loader
 * seam is {@code entityInside}, so the burn triggers when an entity is in the block's space rather
 * than strictly walking on top. Standing on a full cube puts the entity's feet inside it, so the
 * practical behaviour matches.
 */
public class BlockSmolder extends Block {

    public BlockSmolder(Properties properties) {
        super(properties);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(ModFuelItems.powder_fire.get()));
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
        if (!level.getBlockState(pos.above()).isAir()) return;
        double x = pos.getX() + 0.25D + random.nextDouble() * 0.5D;
        double y = pos.getY() + 1.1D;
        double z = pos.getZ() + 0.25D + random.nextDouble() * 0.5D;
        level.addParticle(ParticleTypes.LAVA, x, y, z, 0.0D, 0.0D, 0.0D);
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0D, 0.0D, 0.0D);
    }
}
