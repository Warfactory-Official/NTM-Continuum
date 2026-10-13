package com.hbm.api.script;

import com.hbm.Constants;
import com.hbm.api.environment.Environment;
import com.hbm.api.event.HbmEvents;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import dev.golem.core.script.ScriptEvent;
import dev.golem.core.script.Scripts;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.level.ServerLevel;

/**
 * HBM's offer to scripts (KubeJS), through Golem Core's neutral {@link Scripts}: the {@code HBM} global
 * ({@link HbmScriptApi}), the observe-only {@code HBMEvents} and the startup registration event. HBM has no KubeJS
 * dependency; on a node or install without KubeJS all of this is inert and costs nothing (the forwarding listeners are
 * only registered once a scripting adapter has run startup scripts).
 *
 * <p>Every server event is first an {@link HbmEvents} hook (or {@link Environment#NUCLEAR_BLAST}), so Java addons see
 * the same moments; one listener per hook forwards it to its script event when a script listens.
 */
public final class HbmScripts {

    private HbmScripts() {}

    public static final Scripts.Group EVENTS = Scripts.group("HBMEvents");

    /** {@link Environment#NUCLEAR_BLAST} as a payload: a nuclear blast of {@code radius} blocks at {@code x}/{@code z}. */
    public record NuclearBlast(ServerLevel level, double x, double z, float radius) {}

    public static final ScriptEvent<NuclearBlast> NUCLEAR_BLAST = EVENTS.server("nuclearBlast", NuclearBlast.class);
    public static final ScriptEvent<HbmEvents.Explosion> EXPLOSION = EVENTS.server("explosion", HbmEvents.Explosion.class);
    public static final ScriptEvent<HbmEvents.BombDetonated> BOMB_DETONATED =
            EVENTS.server("bombDetonated", HbmEvents.BombDetonated.class);
    public static final ScriptEvent<HbmEvents.MissileLaunched> MISSILE_LAUNCHED =
            EVENTS.server("missileLaunched", HbmEvents.MissileLaunched.class);
    public static final ScriptEvent<HbmEvents.MissileImpact> MISSILE_IMPACT =
            EVENTS.server("missileImpact", HbmEvents.MissileImpact.class);
    public static final ScriptEvent<HbmEvents.MachineRecipeCompleted> MACHINE_RECIPE_COMPLETED =
            EVENTS.server("machineRecipeCompleted", HbmEvents.MachineRecipeCompleted.class);
    public static final ScriptEvent<HbmEvents.EntityIrradiated> ENTITY_IRRADIATED =
            EVENTS.server("entityIrradiated", HbmEvents.EntityIrradiated.class);
    /** Startup: {@link HbmScriptRegistry}, the data-only content a startup script may add. */
    public static final ScriptEvent<HbmScriptRegistry> REGISTRY = EVENTS.startup("registry", HbmScriptRegistry.class);

    private static final AtomicBoolean DECLARED = new AtomicBoolean();
    private static final List<HbmScriptRegistry.Material> PENDING = new ArrayList<>(); // guarded by HbmScripts.class
    private static boolean registered; // guarded by HbmScripts.class

    /** From {@code HbmEntry.declare}: the binding, the events and the work after startup scripts. Idempotent. */
    public static void declare() {
        if (!DECLARED.compareAndSet(false, true)) return;
        Scripts.binding("HBM", () -> HbmScriptApi.INSTANCE);
        Scripts.onStartupScriptsLoaded(HbmScripts::startupScriptsLoaded);
    }

    /** From {@code HbmEntry.registered}: every mod's registers are filled, so recorded materials may be added now. */
    public static void registered() {
        synchronized (HbmScripts.class) {
            registered = true;
            applyPending();
        }
    }

    private static void startupScriptsLoaded() {
        forward();
        HbmScriptRegistry registry = new HbmScriptRegistry();
        REGISTRY.post(registry);
        List<HbmScriptRegistry.Material> requested = registry.close();
        synchronized (HbmScripts.class) {
            PENDING.addAll(requested);
            // startup scripts may run before or after HBM's own start-up has finished (loader and mod order): apply
            // at whichever of the two comes last
            if (registered) applyPending();
        }
    }

    /** One forwarding listener per hook; each posts only when a script listens. */
    private static void forward() {
        Environment.NUCLEAR_BLAST.register((level, x, z, radius) -> {
            if (NUCLEAR_BLAST.active()) NUCLEAR_BLAST.post(new NuclearBlast(level, x, z, radius));
        });
        HbmEvents.EXPLOSION.register(event -> {
            if (EXPLOSION.active()) EXPLOSION.post(event);
        });
        HbmEvents.BOMB_DETONATED.register(event -> {
            if (BOMB_DETONATED.active()) BOMB_DETONATED.post(event);
        });
        HbmEvents.MISSILE_LAUNCHED.register(event -> {
            if (MISSILE_LAUNCHED.active()) MISSILE_LAUNCHED.post(event);
        });
        HbmEvents.MISSILE_IMPACT.register(event -> {
            if (MISSILE_IMPACT.active()) MISSILE_IMPACT.post(event);
        });
        HbmEvents.MACHINE_RECIPE_COMPLETED.register(event -> {
            if (MACHINE_RECIPE_COMPLETED.active()) MACHINE_RECIPE_COMPLETED.post(event);
        });
        HbmEvents.ENTITY_IRRADIATED.register(event -> {
            if (ENTITY_IRRADIATED.active()) ENTITY_IRRADIATED.post(event);
        });
    }

    // Materials from startup scripts

    /** Script materials' runtime numbers start here, far above HBM's 1.7 numbers and any addon's. */
    static final int SCRIPT_MATERIAL_IDS = 1 << 24;

    private static void applyPending() {
        if (PENDING.isEmpty()) return;
        int added = 0;
        for (HbmScriptRegistry.Material request : PENDING) {
            try {
                add(request);
                added++;
            } catch (RuntimeException e) {
                Constants.LOG.error("[HBM] Script material '{}' skipped: {}", request.name, e.getMessage());
            }
        }
        PENDING.clear();
        Constants.LOG.info("[HBM] Added {} material(s) from startup scripts", added);
    }

    private static void add(HbmScriptRegistry.Material request) {
        if (Mats.byName(request.name) != null) throw new IllegalArgumentException("the name is taken");
        NTMMaterial smeltsInto = null;
        if (request.smeltsInto != null) {
            smeltsInto = Mats.byName(request.smeltsInto);
            if (smeltsInto == null) throw new IllegalArgumentException("smeltsInto names no material: " + request.smeltsInto);
        }
        NTMMaterial material = Mats.register(runtimeId(request.name), request.name)
                .colors(request.solidColorLight, request.solidColorDark, request.moltenColor)
                .smelting(request.smelting);
        if (request.displayName != null) material.displayName(request.displayName);
        if (smeltsInto != null) material.conv(smeltsInto, request.convIn, request.convOut);
    }

    /**
     * The packet number of a script material: never saved (materials are saved by name), but it must be the same on the
     * client and the server. It is taken from the name alone (probing upwards past a collision), so the same startup
     * scripts give the same numbers on both sides whatever else is installed.
     */
    static int runtimeId(String name) {
        int id = SCRIPT_MATERIAL_IDS + Math.floorMod(name.hashCode(), 1 << 20);
        while (Mats.matById.containsKey(id)) id++;
        return id;
    }
}
