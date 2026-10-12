package com.hbm.blocks.generic;

import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.hazard.HazardSystem;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A radioactive / otherwise hazardous block, 1.7's {@code BlockHazard}. It does (any subset):
 * <ul>
 * <li><b>{@code radIn}</b> — 1.7 {@code updateTick}: {@code ChunkRadiationManager.incrementRad(rad)} every 20 ticks
 *     ({@code tickRate} 20, re-scheduled from {@code onBlockAdded}), uncapped, with {@code rad} the block item's
 *     RADIATION hazard × 0.1; {@code addRadiation(rad)} sets {@code radIn = rad·0.1}.</li>
 * <li><b>{@code rad3d}</b> — an area contamination pulse every second ({@code ContaminationUtil.radiate}, range 32): not
 *     1.7 (its blocks have none); kept for an addon's {@code radiation_emission} {@code area}.</li>
 * <li><b>contact</b> — applies the block's item hazards to any entity standing in/on it
 *     ({@code HazardSystem.applyHazards}).</li>
 * </ul>
 * Radiation values are assigned after registration by {@link com.hbm.blocks.ModHazardBlocks#initHazards()}
 * from the block‑item's configured RADIATION hazard (the old {@code getRawRadsFromBlock} two‑step), so
 * holding the block irradiates you too.
 */
public class BlockHazard extends dev.golem.core.world.CompatBlock {

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

    /**
     * The emission from the {@code hbm:radiation_emission} block data map when it names this block, read at use time so
     * a datapack reload changes it (1.7 copied the block item's RADIATION hazard into the block once, at start-up);
     * else what {@link #addRadiation}/{@link #addRad3d} set in code.
     */
    private com.hbm.api.data.HazardDataMaps.@org.jspecify.annotations.Nullable Emission emission() {
        return com.hbm.api.data.HazardDataMaps.RADIATION_EMISSION.get(this);
    }

    public float getRadIn() { var e = emission(); return e != null ? e.radiation() * 0.1F : radIn; }
    public float getRadMax() { var e = emission(); return e != null ? e.radiation() : radMax; }
    public float getRad3d() { var e = emission(); return e != null ? e.area() : rad3d; }

    private boolean emitsRadiation() {
        return getRadIn() > 0 || getRad3d() > 0;
    }

    /** 1.7 {@code tickRate}: 20 ticks while the block emits. */
    private int tickDelay(RandomSource random) {
        return 20;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // 1.7 onBlockAdded: the first scheduled tick; updateTick then re-schedules itself every 20 ticks
        if (emitsRadiation() && !level.isClientSide()) {
            level.scheduleTick(pos, this, tickDelay(level.getRandom()));
        }
    }

    /** 1.7 {@code updateTick}: add {@code radIn} to the chunk (no cap, as 1.7) and re-schedule in 20 ticks. */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        float rad3d = getRad3d(), radIn = getRadIn();
        if (rad3d > 0) ContaminationUtil.radiate(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 32, rad3d);
        if (radIn > 0) ChunkRadiationManager.proxy.incrementRad(level, pos, radIn);
        if (rad3d > 0 || radIn > 0) level.scheduleTick(pos, this, tickDelay(random));
    }

    /**
     * Starts the 20-tick cycle of a block that has none: 1.7 ran {@code onBlockAdded} for every placement, but a block
     * placed during chunk generation or saved by an older version gets no {@link #onPlace}. Emits nothing itself.
     */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (emitsRadiation() && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, tickDelay(random));
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState onState, Entity entity) {
        super.stepOn(level, pos, onState, entity);
        if (entity instanceof LivingEntity living) HazardSystem.applyHazards(this, living);
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
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
