package com.hbm.api.event;

import com.hbm.interfaces.IBomb;
import com.hbm.interfaces.IBomb.BombReturnCode;
import dev.golem.core.hook.Hook;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Moments in HBM that other mods (and, through {@code com.hbm.api.script.HbmScripts}, scripts) may observe. Every hook
 * here only <b>notifies</b>: a listener is told what happened after it happened and cannot change it. Each payload is a
 * small immutable record of plain values and game objects (level, entity, position, ids), the same record scripts see.
 *
 * <p>All moments are on the logical server; each {@code fire} method returns at once on a client level and when
 * nothing listens, so an unobserved HBM pays one volatile read per moment. A listener runs on the server thread, in
 * the middle of HBM's own work: it must not throw and should not change the world at the position it is told about
 * (the explosion, the machine) from inside the call.
 *
 * <pre>{@code
 * HbmEvents.EXPLOSION.register(e -> LOG.info("{} blast of {} at {} {} {}", e.kind(), e.size(), e.x(), e.y(), e.z()));
 * }</pre>
 *
 * <p>The nuclear blast moment is {@link com.hbm.api.environment.Environment#NUCLEAR_BLAST}, which predates this class.
 */
public final class HbmEvents {

    private HbmEvents() {}

    // Explosions

    /**
     * An HBM explosion went off (after its blocks and entities were processed).
     *
     * @param kind {@code "vnt"} for the composable {@code ExplosionVNT} (most HBM blasts, {@code ExplosionNT}'s erosion
     *             blast), {@code "large"} for {@code ExplosionLarge} (a vanilla blast plus HBM's smoke, rubble and
     *             shrapnel)
     * @param exploder the entity the blast is attributed to, if any
     */
    public record Explosion(Level level, double x, double y, double z, float size, @Nullable Entity exploder, String kind) {}

    public static final Hook<Consumer<Explosion>> EXPLOSION = Hook.notifying();

    public static void explosion(Level level, double x, double y, double z, float size, @Nullable Entity exploder, String kind) {
        if (EXPLOSION.isEmpty() || level.isClientSide()) return;
        Explosion event = new Explosion(level, x, y, z, size, exploder, kind);
        for (Consumer<Explosion> listener : EXPLOSION) listener.accept(event);
    }

    // Bombs

    /**
     * A detonator (the detonator, laser and multi detonators, the dead man's switch, or a detonation deferred until its
     * chunk loaded) triggered an {@link IBomb} block. Bombs set off by redstone are not reported here (1.7 had no
     * shared moment for them either).
     *
     * @param block    the bomb block's registry id, read before it went off
     * @param detonator the triggering entity, if online
     * @param result   the {@link BombReturnCode} name ({@code DETONATED}, {@code LAUNCHED}, {@code ERROR_MISSING_COMPONENT}, ...)
     * @param success  whether the code counts as a success
     */
    public record BombDetonated(Level level, BlockPos pos, String block, @Nullable Entity detonator, String result,
                                boolean success) {}

    public static final Hook<Consumer<BombDetonated>> BOMB_DETONATED = Hook.notifying();

    /** {@code bomb.explode(level, pos, detonator)}, then tells {@link #BOMB_DETONATED}. */
    public static BombReturnCode detonate(IBomb bomb, Level level, BlockPos pos, @Nullable Entity detonator) {
        if (BOMB_DETONATED.isEmpty() || level.isClientSide()) return bomb.explode(level, pos, detonator);
        String block = blockId(level, pos);
        BombReturnCode result = bomb.explode(level, pos, detonator);
        bombDetonated(level, pos, block, detonator, result);
        return result;
    }

    /** {@code bomb.explode(level, pos, detonator, detonatorId)}, then tells {@link #BOMB_DETONATED}. */
    public static BombReturnCode detonate(IBomb bomb, Level level, BlockPos pos, @Nullable Entity detonator,
                                          @Nullable UUID detonatorId) {
        if (BOMB_DETONATED.isEmpty() || level.isClientSide()) return bomb.explode(level, pos, detonator, detonatorId);
        String block = blockId(level, pos);
        BombReturnCode result = bomb.explode(level, pos, detonator, detonatorId);
        bombDetonated(level, pos, block, detonator, result);
        return result;
    }

    private static void bombDetonated(Level level, BlockPos pos, String block, @Nullable Entity detonator, BombReturnCode result) {
        BombDetonated event = new BombDetonated(level, pos.immutable(), block, detonator, result.name(), result.wasSuccessful());
        for (Consumer<BombDetonated> listener : BOMB_DETONATED) listener.accept(event);
    }

    // Missiles

    /**
     * A launch pad, the rusted launch pad or a custom-missile launcher (compact launcher, launch table) put a missile
     * into the world.
     *
     * @param launcher the launcher block entity's position
     * @param missile  the missile entity just added
     * @param missileType the missile's entity type id
     * @param launchedBy who gets credit (the detonator that fired the pad), if known
     */
    public record MissileLaunched(Level level, BlockPos launcher, Entity missile, String missileType, int targetX,
                                  int targetZ, @Nullable Entity launchedBy) {}

    public static final Hook<Consumer<MissileLaunched>> MISSILE_LAUNCHED = Hook.notifying();

    public static void missileLaunched(Level level, BlockPos launcher, Entity missile, int targetX, int targetZ,
                                       @Nullable Entity launchedBy) {
        if (MISSILE_LAUNCHED.isEmpty() || level.isClientSide()) return;
        MissileLaunched event = new MissileLaunched(level, launcher.immutable(), missile, entityId(missile), targetX,
                targetZ, launchedBy);
        for (Consumer<MissileLaunched> listener : MISSILE_LAUNCHED) listener.accept(event);
    }

    /**
     * A ballistic missile ({@code EntityMissileBaseNT}: every tier, the custom missile, stealth, shuttle) hit a block,
     * told just before its warhead goes off.
     *
     * @param hitPos the block it hit
     * @param thrower who launched it, if known
     */
    public record MissileImpact(Level level, Entity missile, String missileType, double x, double y, double z,
                                BlockPos hitPos, int targetX, int targetZ, @Nullable LivingEntity thrower) {}

    public static final Hook<Consumer<MissileImpact>> MISSILE_IMPACT = Hook.notifying();

    public static void missileImpact(Entity missile, BlockPos hitPos, int targetX, int targetZ, @Nullable LivingEntity thrower) {
        Level level = missile.level();
        if (MISSILE_IMPACT.isEmpty() || level.isClientSide()) return;
        MissileImpact event = new MissileImpact(level, missile, entityId(missile), missile.getX(), missile.getY(),
                missile.getZ(), hitPos.immutable(), targetX, targetZ, thrower);
        for (Consumer<MissileImpact> listener : MISSILE_IMPACT) listener.accept(event);
    }

    // Machines

    /**
     * A recipe-module machine (assembly machine and factory, chemical plant and factory, precision assembler, PUREX,
     * plasma forge, fusion torus) finished one operation of a recipe.
     *
     * @param machine the block entity type id ({@code hbm:machine_chemical_plant})
     * @param table   the recipe table id ({@code hbm:chemical_plant}), or the table class name for a table that is
     *                not a datapack table
     * @param recipe  the recipe's name within that table
     */
    public record MachineRecipeCompleted(Level level, BlockPos pos, String machine, String table, String recipe) {}

    public static final Hook<Consumer<MachineRecipeCompleted>> MACHINE_RECIPE_COMPLETED = Hook.notifying();

    /** Whether anything listens to {@link #MACHINE_RECIPE_COMPLETED}; lets a module skip resolving its table. */
    public static boolean machineRecipeObserved() {
        return !MACHINE_RECIPE_COMPLETED.isEmpty();
    }

    public static void machineRecipeCompleted(@Nullable BlockEntity owner, String table, String recipe) {
        if (owner == null || MACHINE_RECIPE_COMPLETED.isEmpty()) return;
        Level level = owner.getLevel();
        if (level == null || level.isClientSide()) return;
        var type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(owner.getType());
        MachineRecipeCompleted event = new MachineRecipeCompleted(level, owner.getBlockPos(),
                type == null ? "" : type.toString(), table, recipe);
        for (Consumer<MachineRecipeCompleted> listener : MACHINE_RECIPE_COMPLETED) listener.accept(event);
    }

    // Contamination

    /**
     * {@code ContaminationUtil.contaminate} gave a living entity a radiation dose (hazard {@code RADIATION} or
     * {@code NEUTRON}): every protection check passed. Told for every dose, which for an entity standing in radiation
     * is every tick, so keep listeners cheap.
     *
     * @param hazard        {@code RADIATION} or {@code NEUTRON}
     * @param contamination the {@code ContaminationType} name ({@code CREATIVE}, {@code HAZMAT}, ...)
     * @param amount        the dose asked for, in RAD
     * @param dose          the dose after the entity's radiation resistance ({@code amount} for {@code RAD_BYPASS})
     */
    public record EntityIrradiated(LivingEntity entity, String hazard, String contamination, double amount, double dose) {}

    public static final Hook<Consumer<EntityIrradiated>> ENTITY_IRRADIATED = Hook.notifying();

    /** Whether anything listens to {@link #ENTITY_IRRADIATED}; checked first on the contamination hot path. */
    public static boolean irradiationObserved() {
        return !ENTITY_IRRADIATED.isEmpty();
    }

    public static void entityIrradiated(LivingEntity entity, String hazard, String contamination, double amount, double dose) {
        if (ENTITY_IRRADIATED.isEmpty() || dose <= 0D || entity.level().isClientSide()) return;
        EntityIrradiated event = new EntityIrradiated(entity, hazard, contamination, amount, dose);
        for (Consumer<EntityIrradiated> listener : ENTITY_IRRADIATED) listener.accept(event);
    }


    private static String blockId(Level level, BlockPos pos) {
        var id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        return id == null ? "" : id.toString();
    }

    private static String entityId(Entity entity) {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id == null ? "" : id.toString();
    }
}
