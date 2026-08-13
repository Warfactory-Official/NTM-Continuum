package com.hbm.blocks.generic;

import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.hazard.HazardSystem;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
//? if >=26.1 {
import net.minecraft.world.entity.InsideBlockEffectApplier;
//?}
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A radioactive / otherwise hazardous block, ported from 1.12.2's {@code BlockHazard}. It does three
 * independent things (any subset):
 * <ul>
 * <li><b>{@code radIn}/{@code radMax}</b> — pumps chunk radiation into the engine via scheduled ticks
 *     (mirrors the old {@code updateTick → ChunkRadiationManager.incrementRad}, tick delay {@code 60 +
 *     rand(500)}); {@code addRadiation(rad)} sets {@code radIn = rad·0.1, radMax = rad}.</li>
 * <li><b>{@code rad3d}</b> — an area contamination pulse every second ({@code ContaminationUtil.radiate},
 *     range 32) for things like corium.</li>
 * <li><b>contact</b> — applies the block's item hazards to any entity standing in/on it
 *     ({@code HazardSystem.applyHazards}).</li>
 * </ul>
 * Radiation values are assigned after registration by {@link com.hbm.blocks.ModHazardBlocks#initHazards()}
 * from the block‑item's configured RADIATION hazard (the old {@code getRawRadsFromBlock} two‑step), so
 * holding the block irradiates you too.
 */
public class BlockHazard extends Block {

    private float radIn = 0.0F;
    private float radMax = 0.0F;
    private float rad3d = 0.0F;
    private final DisplayEffect display;

    public BlockHazard(Properties props) {
        this(props, DisplayEffect.NONE);
    }

    public BlockHazard(Properties props, DisplayEffect display) {
        super(props);
        this.display = display;
    }

    public BlockHazard addRadiation(float radiation) {
        this.radIn = radiation * 0.1F;
        this.radMax = radiation;
        return this;
    }

    public BlockHazard addRad3d(float rad3d) {
        this.rad3d = rad3d;
        return this;
    }

    public float getRadIn() { return radIn; }
    public float getRadMax() { return radMax; }

    private boolean emitsRadiation() {
        return radIn > 0 || rad3d > 0;
    }

    /** The old {@code tickRate}: 20t for rad3d pulses, else the 3–28 s initial delay. */
    private int tickDelay(RandomSource random) {
        if (rad3d > 0) return 20;
        if (radIn > 0) return 60 + random.nextInt(500);
        return 20;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // Old onBlockAdded: ONE initial scheduled tick; the STEADY cadence for radIn-only blocks is vanilla
        // random ticks (setTickRandomly ≙ Properties.randomTicks()), NOT self-re-scheduling.
        if (emitsRadiation() && !level.isClientSide()) {
            level.scheduleTick(pos, this, tickDelay(level.getRandom()));
        }
    }

    /**
     * The old {@code updateTick}, reached from both the scheduled tick and vanilla random ticks (in 1.12
     * {@code randomTick} delegated to {@code updateTick}; 26.1 separates them — see {@link #randomTick}).
     * Only the rad3d pulse re-schedules (every 20t); a radIn-only block emits ONCE per tick received, so
     * its steady rate is the vanilla random-tick cadence (~1 per 68 s at randomTickSpeed 3) — the chunk
     * reading is a faithful sawtooth (spike +radIn, then decay/diffusion), capped at radMax.
     */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (rad3d > 0) {
            ContaminationUtil.radiate(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 32, rad3d);
            level.scheduleTick(pos, this, tickDelay(random));
        }
        if (radIn > 0) {
            ChunkRadiationManager.proxy.incrementRad(level, pos, radIn, radMax);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        tick(state, level, pos, random);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState onState, Entity entity) {
        super.stepOn(level, pos, onState, entity);
        if (entity instanceof LivingEntity living) HazardSystem.applyHazards(this, living);
    }

    @Override
    //? if >=26.1 {
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean isPrecise) {
    //?} else {
    /*protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
    *///?}
        //? if >=26.1 {
super.entityInside(state, level, pos, entity, effects, isPrecise);
        //?} else {
        /*super.entityInside(state, level, pos, entity);
        *///?}
        if (entity instanceof LivingEntity living) HazardSystem.applyHazards(this, living);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        switch (display) {
            case RADFOG -> spawnFaces(level, pos, random, ParticleTypes.MYCELIUM);
            case FLAMES -> {
                spawnFaces(level, pos, random, ParticleTypes.FLAME);
                spawnFaces(level, pos, random, ParticleTypes.SMOKE);
            }
            case LAVAPOP -> level.addParticle(ParticleTypes.LAVA,
                    pos.getX() + random.nextFloat(), pos.getY() + 1.1F, pos.getZ() + random.nextFloat(), 0, 0, 0);
            default -> { }
        }
    }

    /** Emits a particle on each air‑adjacent face (a slim port of the old {@code sPart}). */
    private void spawnFaces(Level level, BlockPos pos, RandomSource random, net.minecraft.core.particles.ParticleOptions particle) {
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            if (dir == net.minecraft.core.Direction.DOWN && display == DisplayEffect.FLAMES) continue;
            BlockPos np = pos.relative(dir);
            if (!level.getBlockState(np).isAir()) continue;
            double ix = pos.getX() + 0.5 + dir.getStepX() * 0.5 + (random.nextDouble() - 0.5) * (dir.getStepX() == 0 ? 1 : 0);
            double iy = pos.getY() + 0.5 + dir.getStepY() * 0.5 + (random.nextDouble() - 0.5) * (dir.getStepY() == 0 ? 1 : 0);
            double iz = pos.getZ() + 0.5 + dir.getStepZ() * 0.5 + (random.nextDouble() - 0.5) * (dir.getStepZ() == 0 ? 1 : 0);
            level.addParticle(particle, ix, iy, iz, 0, 0, 0);
        }
    }

    /** Client‑side ambient particle style (the old {@code ExtDisplayEffect}; SPARKS/SCHRAB need the HBM particle system → mapped to NONE/MYCELIUM). */
    public enum DisplayEffect { NONE, RADFOG, FLAMES, LAVAPOP }
}
