package com.hbm.api.environment;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FluidTraitSimple;
import dev.golem.core.hook.Hook;
import dev.golem.core.hook.Verdict;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The environment HBM's machines, blocks, items and entities consult: whether a fire can breathe at a position, the
 * air pressure, gravity, sunlight, groundwater, whether a level is a surface world or in orbit, and the moments gas
 * goes into or out of the air. Plain HBM lives on Earth, so every query answers Earth unless an addon says otherwise;
 * NTM: Space answers for its planets, orbits and sealed rooms. Another space mod can answer for its own dimensions the
 * same way, and HBM's machines follow it.
 *
 * <p>The query points are exactly where hbm-space (NTM: Space for 1.7.10) changed HBM to consult its celestial bodies
 * and atmospheres, so a plain HBM install behaves exactly as 1.7: every default below is the 1.7 rule the
 * fork replaced, never the fork's own Earth values.
 */
public final class Environment {

    private Environment() {}

    // Breathing (hbm-space TileEntityMachineBase.breatheAir, ChunkAtmosphereManager.canBreathe)

    /** A listener's answer to "can a fire breathe here, consuming {@code amount} of oxygen?". */
    @FunctionalInterface
    public interface Breathing {
        /**
         * {@link Verdict#ALLOW} if a fire at {@code pos} gets air (and the listener has consumed {@code amount}
         * millibuckets of oxygen from wherever it came from: 0 only checks), {@link Verdict#DENY} if it gets none,
         * {@link Verdict#PASS} to leave it to the next listener.
         */
        Verdict breathe(Level level, BlockPos pos, int amount);
    }

    /**
     * Breathing answers, in order; the first that does not pass decides, and nothing deciding means Earth: a fire
     * breathes. hbm-space checked the planet's atmosphere first and sealed rooms second; an addon orders its phases
     * accordingly.
     */
    public static final Hook<Breathing> BREATHING = Hook.of(listeners -> (level, pos, amount) ->
            Verdict.first(listeners, listener -> listener.breathe(level, pos, amount)));

    /**
     * hbm-space's {@code TileEntityMachineBase.breatheAir}: whether a fire at {@code pos} can burn, consuming
     * {@code amount} of oxygen from a sealed room if that is where the air comes from (0 only checks).
     */
    public static boolean breatheAir(Level level, BlockPos pos, int amount) {
        return BREATHING.invoker().breathe(level, pos, amount).orElse(true);
    }

    /**
     * The breathing answer with a caller-supplied 1.7 fallback, for the places where 1.7 already had a rule that
     * hbm-space replaced with {@code canBreathe} (the FBI agent's space suit outside the overworld).
     */
    public static boolean breatheAir(Level level, BlockPos pos, int amount, boolean unanswered) {
        return BREATHING.invoker().breathe(level, pos, amount).orElse(unanswered);
    }

    /**
     * Whether an enabled oil-fired heater below its heat cap cools with an empty fuel tank. Plain HBM retains its
     * heat; hbm-space's oil burner cools. Any listener requesting cooling enables that rule.
     */
    public static final Hook<BooleanSupplier> EMPTY_OILBURNER_COOLING = Hook.of(listeners -> () -> {
        for (var listener : listeners) if (listener.getAsBoolean()) return true;
        return false;
    });

    public static boolean coolEmptyOilburner() {
        return EMPTY_OILBURNER_COOLING.invoker().getAsBoolean();
    }

    // Pressure (hbm-space CBT_Atmosphere.getPressure)

    /** A listener's air pressure at a position, in atmospheres, or {@link Double#NaN} to leave it to the next listener. */
    @FunctionalInterface
    public interface Pressure {
        double pressure(Level level, BlockPos pos);
    }

    /** Earth's air pressure, what plain HBM assumes everywhere. */
    public static final double EARTH_PRESSURE = 1.0D;

    /** Below this many atmospheres hbm-space treats a place as vacuum (condensers, towers, fans, the Torex). */
    public static final double VACUUM_PRESSURE = 0.01D;

    /** Pressure answers, in order; the first that is not NaN decides, and nothing deciding means Earth. */
    public static final Hook<Pressure> PRESSURE = Hook.of(listeners -> (level, pos) -> {
        for (Pressure listener : listeners) {
            double pressure = listener.pressure(level, pos);
            if (!Double.isNaN(pressure)) return pressure;
        }
        return Double.NaN;
    });

    /** The air pressure at {@code pos} in atmospheres (hbm-space's {@code CBT_Atmosphere.getPressure()}); Earth is 1. */
    public static double pressure(Level level, BlockPos pos) {
        double pressure = PRESSURE.invoker().pressure(level, pos);
        return Double.isNaN(pressure) ? EARTH_PRESSURE : pressure;
    }

    /**
     * hbm-space's recurring vacuum test {@code inOrbit(world) || atmosphere == null || atmosphere.getPressure() < 0.01}
     * (condenser, cooling towers, Torex). Never true on plain HBM.
     */
    public static boolean isVacuum(Level level, BlockPos pos) {
        return inOrbit(level) || pressure(level, pos) < VACUUM_PRESSURE;
    }

    /** An additional cause of condenser evaporation, even for a vacuum-optimised machine. */
    @FunctionalInterface
    public interface Evaporation {
        boolean evaporates(Level level, BlockPos pos, boolean vacuumProof);
    }

    /** Addons can contribute evaporation causes such as Space's exposed Tom firestorm. */
    public static final Hook<Evaporation> EVAPORATION = Hook.of(listeners -> (level, pos, vacuumProof) -> {
        for (var listener : listeners) if (listener.evaporates(level, pos, vacuumProof)) return true;
        return false;
    });

    public static boolean evaporates(Level level, BlockPos pos, boolean vacuumProof) {
        return EVAPORATION.invoker().evaporates(level, pos, vacuumProof) || !vacuumProof && isVacuum(level, pos);
    }

    // Surface and orbit (1.7 provider.isSurfaceWorld / dimensionId == 0; hbm-space CelestialBody.inOrbit)

    /** A listener's answer to "is this level a surface world?" ({@link Verdict#PASS} leaves it to the next one). */
    @FunctionalInterface
    public interface Surface {
        Verdict surface(Level level);
    }

    /**
     * Surface answers, in order; nothing deciding means 1.7's rule: only the overworld. hbm-space's planets are surface
     * worlds too (its {@code WorldProviderCelestial} extends {@code WorldProviderSurface}), so meteors fall there and
     * Tom's firestorm burns there; an addon answers {@link Verdict#ALLOW} for its planets.
     */
    public static final Hook<Surface> SURFACE = Hook.of(listeners -> level ->
            Verdict.first(listeners, listener -> listener.surface(level)));

    /** Whether {@code level} is a surface world in 1.7's sense (meteors, boss raids, Tom's impact on entities). */
    public static boolean isSurface(Level level) {
        return SURFACE.invoker().surface(level).orElse(level.dimension() == Level.OVERWORLD);
    }

    /** A listener's answer to "is this level in orbit?". */
    @FunctionalInterface
    public interface Orbit {
        Verdict orbit(Level level);
    }

    /** Orbit answers, in order; nothing deciding means not in orbit (plain HBM has no orbit). */
    public static final Hook<Orbit> ORBIT = Hook.of(listeners -> level ->
            Verdict.first(listeners, listener -> listener.orbit(level)));

    /** hbm-space's {@code CelestialBody.inOrbit(world)}: no tilt, no groundwater, no condensing, no infinite water. */
    public static boolean inOrbit(Level level) {
        return ORBIT.invoker().orbit(level).orElse(false);
    }

    /** A reason an off-world delivery cannot arrive, or null to accept the level. */
    @FunctionalInterface
    public interface DeliveryRefusal {
        @Nullable net.minecraft.network.chat.Component refuse(ServerLevel level);
    }

    /** The first delivery restriction wins; plain HBM accepts every level. */
    public static final Hook<DeliveryRefusal> DELIVERY_REFUSAL = Hook.of(listeners -> level -> {
        for (var listener : listeners) {
            var reason = listener.refuse(level);
            if (reason != null) return reason;
        }
        return null;
    });

    /** Queried before Bobmazon charges caps or launches a delivery drone. */
    public static @Nullable net.minecraft.network.chat.Component deliveryRefusal(ServerLevel level) {
        return DELIVERY_REFUSAL.invoker().refuse(level);
    }

    // Sunlight (hbm-space WorldProviderCelestial.getSunPower)

    /** A listener's sun-power multiplier at a position, or {@link Double#NaN} to leave it to the next listener. */
    @FunctionalInterface
    public interface Sunlight {
        double sunlight(Level level, BlockPos pos);
    }

    /** Sunlight answers, in order; the first that is not NaN decides, and nothing deciding means Earth's sun, 1. */
    public static final Hook<Sunlight> SUNLIGHT = Hook.of(listeners -> (level, pos) -> {
        for (Sunlight listener : listeners) {
            double sun = listener.sunlight(level, pos);
            if (!Double.isNaN(sun)) return sun;
        }
        return Double.NaN;
    });

    /** hbm-space's {@code getSunPower(provider, x, z)}: how strong the sun is here relative to Earth's. */
    public static double sunlight(Level level, BlockPos pos) {
        double sun = SUNLIGHT.invoker().sunlight(level, pos);
        return Double.isNaN(sun) ? 1.0D : sun;
    }

    // Groundwater (hbm-space CBT_Water: the body's water table)

    /**
     * A listener's water table at a position: the fluid the ground holds, {@link Fluids#NONE} for none (orbit, a dry
     * planet), or {@code null} to leave it to the next listener.
     */
    @FunctionalInterface
    public interface Groundwater {
        @Nullable FluidType groundwater(Level level, BlockPos pos);
    }

    /** Groundwater answers, in order; the first non-null decides. */
    public static final Hook<Groundwater> GROUNDWATER = Hook.of(listeners -> (level, pos) -> {
        for (Groundwater listener : listeners) {
            FluidType table = listener.groundwater(level, pos);
            if (table != null) return table;
        }
        return null;
    });

    /**
     * The water table at {@code pos}: what the ground pumps lift (hbm-space's {@code CBT_Water.fluid}); unanswered,
     * Earth's water. hbm-space's orbit check is folded in: in orbit there is no table.
     */
    public static FluidType groundwater(Level level, BlockPos pos) {
        if (inOrbit(level)) return Fluids.NONE;
        FluidType table = GROUNDWATER.invoker().groundwater(level, pos);
        return table == null ? Fluids.WATER : table;
    }

    /** The water table an addon has declared at {@code pos}, or {@code null} where nobody answered (plain HBM). */
    public static @Nullable FluidType declaredGroundwater(Level level, BlockPos pos) {
        if (inOrbit(level)) return Fluids.NONE;
        return GROUNDWATER.invoker().groundwater(level, pos);
    }

    // Gravity (hbm-space CelestialBody.getGravity)

    /**
     * hbm-space's {@code AstronomyUtil.STANDARD_GRAVITY}: Earth's gravity in its units (blocks per tick per second,
     * vanilla's 0.08 per tick times 20). Gravity here is a multiple of it.
     */
    public static final float EARTH_GRAVITY = 1.6F;

    /** A listener's gravity on an entity as a multiple of Earth's (0 is free fall), or NaN to pass. */
    @FunctionalInterface
    public interface Gravity {
        double gravity(Entity entity);
    }

    /** Gravity answers, in order; the first that is not NaN decides, and nothing deciding means Earth, 1. */
    public static final Hook<Gravity> GRAVITY = Hook.of(listeners -> entity -> {
        for (Gravity listener : listeners) {
            double gravity = listener.gravity(entity);
            if (!Double.isNaN(gravity)) return gravity;
        }
        return Double.NaN;
    });

    /**
     * Gravity on {@code entity} as a multiple of Earth's: hbm-space's {@code getGravity(player) / STANDARD_GRAVITY}.
     * Used by HBM's own flight items (jetpacks, DNT armour, Murk wings); vanilla motion itself is the addon's.
     */
    public static double gravity(Entity entity) {
        double gravity = GRAVITY.invoker().gravity(entity);
        return Double.isNaN(gravity) ? 1.0D : gravity;
    }

    // Gas into and out of the air (hbm-space FT_Gaseous.release / capture, CelestialBody.emitGas)

    /** A listener told that {@code mB} of a gas was vented into the air at {@code pos}. */
    @FunctionalInterface
    public interface GasRelease {
        void release(Level level, BlockPos pos, FluidType type, double mB);
    }

    /** Notified for every gas HBM vents into the open air (gas flare, drain); plain HBM has no listener. */
    public static final Hook<GasRelease> GAS_RELEASE = Hook.notifying();

    /**
     * hbm-space's {@code FT_Gaseous.release}: tells the listeners that {@code mB} of {@code type} went into the air,
     * if it is a gas ({@code FT_Gaseous} or {@code FT_Gaseous_ART}) and the level is a server level.
     */
    public static void releaseGas(Level level, BlockPos pos, FluidType type, double mB) {
        if (level.isClientSide() || GAS_RELEASE.isEmpty()) return;
        if (!type.hasTrait(FluidTraitSimple.FT_Gaseous.class) && !type.hasTrait(FluidTraitSimple.FT_Gaseous_ART.class)) return;
        for (GasRelease listener : GAS_RELEASE) listener.release(level, pos, type, mB);
    }

    /**
     * A listener's answer to "may an air intake at {@code pos} compress {@code amount} millibuckets of the local air?"
     * (and, when it allows, it has captured that much from the planet or the sealed room).
     */
    @FunctionalInterface
    public interface Compression {
        Verdict compress(Level level, BlockPos pos, int amount);
    }

    /** Compression answers, in order; nothing deciding means Earth: there is always air to compress. */
    public static final Hook<Compression> COMPRESSION = Hook.of(listeners -> (level, pos, amount) ->
            Verdict.first(listeners, listener -> listener.compress(level, pos, amount)));

    /** hbm-space's {@code TileEntityMachineIntake.canCompress} past its "tank already full" shortcut. */
    public static boolean compressAir(Level level, BlockPos pos, int amount) {
        return COMPRESSION.invoker().compress(level, pos, amount).orElse(true);
    }

    /** A listener's answer to "may seed slurry turn the dirt at {@code pos} into grass?" (and its side effects). */
    @FunctionalInterface
    public interface Growth {
        Verdict grow(Level level, BlockPos pos);
    }

    /**
     * Growth answers, in order; nothing deciding means Earth: the grass grows. hbm-space grew grass from seed slurry only
     * under an atmosphere with air, Duna air, CO2 or oxygen, then converted the CO2 into air; an addon does both here.
     */
    public static final Hook<Growth> GROWTH = Hook.of(listeners -> (level, pos) ->
            Verdict.first(listeners, listener -> listener.grow(level, pos)));

    /** Whether seed slurry may grow grass at {@code pos} (hbm-space {@code EntityChemical} seed-slurry branch). */
    public static boolean canGrow(Level level, BlockPos pos) {
        return GROWTH.invoker().grow(level, pos).orElse(true);
    }

    /** Addon fluid traits applied when an actual chemical projectile hits a block. */
    @FunctionalInterface
    public interface ChemicalImpact {
        void impact(ServerLevel level, BlockPos pos, FluidType type);
    }

    public static final Hook<ChemicalImpact> CHEMICAL_IMPACT = Hook.notifying();

    public static void chemicalImpact(ServerLevel level, BlockPos pos, FluidType type) {
        for (ChemicalImpact listener : CHEMICAL_IMPACT) listener.impact(level, pos, type);
    }

    /** Living-entity tick HEAD, on both sides and before vanilla movement and potion ticking. */
    public static final Hook<Consumer<LivingEntity>> LIVING_TICK_START = Hook.notifying();

    public static void livingTickStart(LivingEntity entity) {
        for (Consumer<LivingEntity> listener : LIVING_TICK_START) listener.accept(entity);
    }

    // Living entities (hbm-space EntityEffectHandler.handleOxy / handleCorrosion / celestial sun radiation)

    /**
     * Notified once per living tick, server side, after HBM's radiation, from 1.7's {@code EntityEffectHandler.onUpdate}
     * (where hbm-space ran {@code handleOxy} and {@code handleCorrosion}). An addon keeps its own state (oxygen,
     * corrosion) in a Golem {@code EntityData} attachment of its own, never in HBM's living props.
     */
    public static final Hook<Consumer<LivingEntity>> LIVING_TICK = Hook.notifying();

    /** Runs {@link #LIVING_TICK} for {@code entity}. */
    public static void livingTick(LivingEntity entity) {
        if (LIVING_TICK.isEmpty()) return;
        for (Consumer<LivingEntity> listener : LIVING_TICK) listener.accept(entity);
    }

    /** A listener's ambient radiation floor in RAD/s at a position, or NaN (or 0) to add none. */
    @FunctionalInterface
    public interface AmbientRadiation {
        double floor(Level level, BlockPos pos, LivingEntity entity);
    }

    /**
     * Ambient radiation floors: the highest answer raises an entity's background radiation to it, after 1.7's nether
     * {@code hellRad} floor (hbm-space added unshielded sunlight on its planets and in orbit here). Plain HBM: none.
     */
    public static final Hook<AmbientRadiation> AMBIENT_RADIATION = Hook.notifying();

    /** The highest ambient radiation floor at {@code pos}, 0 where nobody answers. */
    public static float ambientRadiationFloor(Level level, BlockPos pos, LivingEntity entity) {
        double floor = 0D;
        for (AmbientRadiation listener : AMBIENT_RADIATION) {
            double value = listener.floor(level, pos, entity);
            if (value > floor) floor = value;
        }
        return (float) floor;
    }

    // Events other mods track (hbm-space CBT_Impact, CBT_Lights)

    /** A listener told that Tom's impact happened in {@code level} at {@code pos}. */
    @FunctionalInterface
    public interface Impact {
        void impact(ServerLevel level, BlockPos pos);
    }

    /** Notified once when Tom hits ({@code EntityTomBlast}); hbm-space marked the body as impacted here. */
    public static final Hook<Impact> IMPACT = Hook.notifying();

    /** Runs {@link #IMPACT}. */
    public static void impact(ServerLevel level, BlockPos pos) {
        for (Impact listener : IMPACT) listener.impact(level, pos);
    }

    /** A listener told that an HBM light block came on at {@code pos}. */
    @FunctionalInterface
    public interface LightOn {
        void lightOn(ServerLevel level, BlockPos pos, BlockState state);
    }

    /** Notified when a spotlight is (re)lit; hbm-space registered the light with the body for its night-side view. */
    public static final Hook<LightOn> LIGHT_ON = Hook.notifying();

    /** Runs {@link #LIGHT_ON}. */
    public static void lightOn(ServerLevel level, BlockPos pos, BlockState state) {
        for (LightOn listener : LIGHT_ON) listener.lightOn(level, pos, state);
    }

    /** A listener told that a nuclear blast of {@code radius} blocks went off at {@code x} / {@code z} in {@code level}. */
    @FunctionalInterface
    public interface NuclearBlast {
        void blast(ServerLevel level, double x, double z, float radius);
    }

    /**
     * Notified for every nuclear blast, where hbm-space's {@code CelestialNukeShockHandler} was: an MK5 blast entity's
     * first server tick (1.7: joining the world with {@code ticksExisted <= 1}; its {@code length}, the blast radius) and
     * every mini nuke ({@code ExplosionNukeSmall} with {@code miniNuke}, its {@code blastRadius}). Plain HBM has no
     * listener; hbm-space showed the blast on the body seen from space.
     */
    public static final Hook<NuclearBlast> NUCLEAR_BLAST = Hook.notifying();

    /** Runs {@link #NUCLEAR_BLAST}. */
    public static void nuclearBlast(ServerLevel level, double x, double z, float radius) {
        for (NuclearBlast listener : NUCLEAR_BLAST) listener.blast(level, x, z, radius);
    }

    // Satellites (hbm-space SatelliteSavedData.getData(world, x, z), XSatelliteRegistry.getTargetDimensionId)

    /**
     * A listener's level whose satellites serve {@code level} at {@code pos} (an orbital station's satellites are the
     * body's below), or {@code null} to pass. {@code launched} is the satellite item being put into orbit, or
     * {@link ItemStack#EMPTY} for a lookup: hbm-space sent some satellites to other bodies than the launch site's.
     */
    @FunctionalInterface
    public interface Satellites {
        @Nullable ServerLevel satellites(ServerLevel level, @Nullable BlockPos pos, ItemStack launched);
    }

    /** Satellite level answers, in order; the first non-null decides, and nothing deciding means the level itself. */
    public static final Hook<Satellites> SATELLITES = Hook.of(listeners -> (level, pos, launched) -> {
        for (Satellites listener : listeners) {
            ServerLevel target = listener.satellites(level, pos, launched);
            if (target != null) return target;
        }
        return null;
    });

    /** The level whose satellite data serves {@code level} at {@code pos}; plain HBM: {@code level} itself. */
    public static ServerLevel satelliteLevel(ServerLevel level, @Nullable BlockPos pos, ItemStack launched) {
        ServerLevel target = SATELLITES.invoker().satellites(level, pos, launched);
        return target == null ? level : target;
    }
}
