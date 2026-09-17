package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.client.render.TorexCloudletRenderData;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityNukeTorex;
import dev.golem.core.render.Billboards;
import dev.golem.core.render.Instancing;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.joml.Quaternionfc;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The only class the rest of the mod talks to about Flywheel.
 *
 * <p>There are two engines behind it, and which one a node has is a build fact, not a runtime one:
 *
 * <ul>
 *   <li><b>The published engine</b>, {@code dev.engine_room}, on the 1.20.1 and 1.21.1 nodes. It is a
 *       separate mod that may not be installed, so no {@code dev.engine_room} type may be named here —
 *       every such reference lives in {@link FlywheelVisuals}, a file that does not exist on a node
 *       without the engine and which this class names only from inside a {@code //? if flywheel}
 *       branch. That way the JVM never tries to resolve those classes when the engine is absent.</li>
 *   <li><b>The vendored engine</b>, {@code dev.golem.core.flywheel}, on both 26.1 nodes. Engine Room
 *       has a 26.1 source branch but no published artifact used by this build, so Golem Core carries
 *       that port instead. It ships inside
 *       a hard dependency and its entry point is present on every node with inert bodies, so it can be
 *       named unguarded — the discipline above exists for absent classes, and this one is never
 *       absent.</li>
 * </ul>
 *
 * <p>Both are optional twice over and a caller has to survive both cases: the engine may not be there
 * at all, and even when it is the player can switch the backend off. So an instanced visual is never
 * the only renderer for anything — it is an acceleration for a machine that must already draw
 * correctly through its vanilla {@code CompatBER}. Gate on {@link #isAvailable} and fall through when
 * it is false.
 *
 * <p>Nothing outside this package may name a bridge class directly; that is the whole discipline.
 */
public final class FlywheelCompat {

    /** Flywheel's own mod id, on every loader it ships for. */
    public static final String MOD_ID = "flywheel";
    private static final Map<Level, Map<Long, Long>> FOLDED_CORE_CACHE = new WeakHashMap<>();

    private FlywheelCompat() {}

    /** Whether this node has either engine. Constant-folded per node. */
    public static boolean isSupportedOnThisVersion() {
        //? if flywheel {
        /*return true;
        *///?} else {
        return Instancing.isAvailable();
        //?}
    }

    /**
     * Whether an instanced visual will actually be drawn for {@code level}: an engine is present, this
     * node has it, and the player has not turned the backend off.
     */
    public static boolean isAvailable(@Nullable LevelAccessor level) {
        //? if flywheel {
        /*if (!dev.golem.core.platform.Platform.isClient()
                || !dev.golem.core.platform.Platform.isModLoaded(MOD_ID)) return false;
        return FlywheelVisuals.isAvailable(level);
        *///?} else {
        return Instancing.isVisualizing(level);
        //?}
    }

    /**
     * Starts the engine, from the mod constructor and nowhere later.
     *
     * <p>The published engine is a mod and subscribes itself, so this does nothing on those nodes. The
     * vendored one is a library: it has to be handed its listeners, and two of them are mod-bus
     * registry events — {@code RegisterRenderPipelinesEvent} for both backends' pipelines. A listener
     * added from inside another startup event may already have missed its own, so this is called at
     * construction time, which is where CE subscribes the same thirteen.
     *
     * <p>Safe to call unguarded: a node with no engine is a no-op.
     */
    public static void startEngine() {
        //? if flywheel {
        /*// The published engine subscribes its own listeners.
        *///?} else {
        if (!Instancing.isAvailable()) return;
        Instancing.init(FlywheelCompat::crumblingCore);
        //?}
    }

    /**
     * Binds every HBM instanced visual. Call once from renderer registration, guarded on
     * {@link #isSupportedOnThisVersion}, and always after {@link #startEngine}.
     */
    public static void registerVisuals() {
        //? if flywheel {
        /*if (!dev.golem.core.platform.Platform.isClient()
                || !dev.golem.core.platform.Platform.isModLoaded(MOD_ID)) return;
        FlywheelVisuals.register();
        *///?} else {
        if (!Instancing.isAvailable()) {
            throw new UnsupportedOperationException(
                    "Flywheel publishes no build for this Minecraft version and Golem Core vendors one "
                            + "only from 26.1, so there are no visuals to register. Guard the "
                            + "call site on FlywheelCompat.isSupportedOnThisVersion() and let the block "
                            + "entity draw through its vanilla CompatBER.");
        }
        Billboards.register(ModEntities.NUKE_TOREX.get(), List.of(
                        new Billboards.Layer("hbm:textures/particle/particle_base.png",
                                Billboards.Mode.DEPTH_WRITING_TRANSLUCENT),
                        new Billboards.Layer("hbm:textures/particle/flare.png",
                                Billboards.Mode.SOURCE_ALPHA_ADDITIVE)),
                TorexCloudletRenderData::writeInstanced);
        //?}
    }

    /** Whether Core atomically owns one Torex's cloudlet and flare billboards for this frame. */
    public static boolean isTorexBillboardsActive(EntityNukeTorex torex) {
        //? if flywheel {
        /*return false;
        *///?} else {
        return Billboards.owns(torex);
        //?}
    }

    /**
     * The block that owns the visual for a mined position, for the crumbling overlay.
     *
     * <p>A folded multiblock puts the block entity somewhere other than where the player is usually
     * swinging -- its cells carry none at all -- so without this the destroy-stage overlay silently
     * fails to draw on every multiblock in the game.
     *
     * <p>Deliberately not {@link MultiblockSurface#coreOfAny}: that falls through to a cell lookup for
     * any state at all, and the client half of that lookup is a 33-cubed scan around the position. This
     * runs per mined block per frame, so it has to reject a plain block on a state test alone.
     */
    public static @Nullable BlockPos crumblingCore(Level level, BlockPos mined, BlockState state) {
        if (MultiblockSurface.isFoldedCell(state)) return cachedFoldedCore(level, mined);
        return null;
    }

    private static @Nullable BlockPos cachedFoldedCore(Level level, BlockPos mined) {
        Map<Long, Long> cache = FOLDED_CORE_CACHE.computeIfAbsent(level, ignored -> new HashMap<>());
        Long packed = cache.get(mined.asLong());
        if (packed != null) {
            BlockPos core = BlockPos.of(packed);
            BlockState coreState = level.getBlockState(core);
            com.hbm.blocks.multiblock.BlockMultiblockCore block = MultiblockSurface.foldedCore(coreState);
            if (block != null && block.claimsCell(core, mined,
                    coreState.getValue(com.hbm.blocks.multiblock.BlockMultiblockCore.FACING))) return core;
            cache.remove(mined.asLong());
        }

        BlockPos core = MultiblockSurface.coreOfCell(level, mined);
        if (core != null) cache.put(mined.asLong(), core.asLong());
        return core;
    }
}
