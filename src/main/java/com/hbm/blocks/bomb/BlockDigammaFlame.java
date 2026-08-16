package com.hbm.blocks.bomb;

import com.hbm.util.ContaminationUtil;
//? if >=1.21.1 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import dev.golem.core.world.CompatFireBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Digamma flame — the 1.12.2 {@code DigammaFlame}: what a DRX-wavelength FEL beam leaves behind, and
 * the signature of digamma weaponry.
 *
 * <p>Deliberately <b>eternal</b>: {@code canDie} is false and the tick rate is 72,000 ticks (an hour),
 * so once it exists it stays. It never catches anything alight either — it does not spread, it just
 * sits there irradiating. Standing in it applies 0.05 digamma per tick, a contamination type that has
 * no cure.
 *
 * <p>Blast resistance 150 means explosives will not clear it; the only way to remove it is to break it.
 */
public class BlockDigammaFlame extends CompatFireBlock {

    //? if >=1.21.1 {
    public static final MapCodec<BlockDigammaFlame> CODEC = simpleCodec(BlockDigammaFlame::new);
    //?}

    public BlockDigammaFlame(Properties properties) {
        super(properties, 1.0F);
    }

    //? if >=1.21.1 {
    @Override
    protected MapCodec<? extends net.minecraft.world.level.block.BaseFireBlock> codec() {
        return CODEC;
    }
    //?}

    /** Nothing is flammable to a digamma flame — it does not propagate. */
    @Override
    protected boolean canBurn(BlockState state) {
        return false;
    }

    /** It survives anywhere, on anything, forever (the old {@code canDie() == false}). */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    /** The old {@code tickRate == 72000}: it reschedules an hour out and does nothing when it fires. */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.scheduleTick(pos, this, 72_000);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        // Deliberately NOT calling super: BaseFireBlock.onPlace tries to light a nether portal, which
        // has nothing to do with this block. Just start the (very long) tick cycle.
        if (!level.isClientSide() && !oldState.is(state.getBlock())) {
            level.scheduleTick(pos, this, 72_000);
        }
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            ContaminationUtil.contaminate(living, ContaminationUtil.HazardType.DIGAMMA,
                    ContaminationUtil.ContaminationType.DIGAMMA, 0.05F);
        }
    }
}
