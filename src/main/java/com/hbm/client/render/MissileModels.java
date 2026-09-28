package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.ModMissileItems;
import dev.golem.core.render.gem.Surface;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

/**
 * Which body each missile <em>item</em> is drawn with — the port of 1.12.2's
 * {@code ItemRenderMissileGeneric.renderers} map.
 *
 * <p>Used wherever a missile is drawn from its item rather than from a flying entity: the launch pad
 * standing one on its silo, and (later) the item's own held/inventory renderer.
 *
 * <p><b>This is deliberately not the same table the entity renderers use.</b> Upstream draws the N2
 * missile with a dedicated {@code missileneon.obj} body here, but reuses the Atlas body for the flying
 * entity (its entity renderer falls through to the Atlas model and only swaps the texture). Both are
 * kept as upstream has them rather than unified, because the two bodies are visibly different shapes
 * and "fixing" it would change what a player sees on the pad.
 *
 * <p>Scales are upstream's {@code generateStandard} (1.0) / {@code generateLarge} (1.5).
 */
public final class MissileModels {

    private MissileModels() {}

    /** One missile body: an OBJ, the surface it wears, and the scale it is drawn at. */
    public record MissileModel(WavefrontObj model, Surface surface, float scale) {}

    private static final Map<Item, MissileModel> MODELS = new HashMap<>();
    private static final Map<String, WavefrontObj> MESHES = new LinkedHashMap<>();
    private static boolean built = false;

    public static @Nullable MissileModel get(Item item) {
        if (!built) build();
        return MODELS.get(item);
    }

    /**
     * Every distinct body, each loaded once. Most missiles are one of a handful of shapes wearing a
     * different paint job, so a declaration that names the shapes and picks the paint from the loaded
     * missile has a part per shape rather than a part per missile.
     */
    public static Collection<WavefrontObj> meshes() {
        if (!built) build();
        return MESHES.values();
    }

    private static void build() {
        built = true;

        // Tier 0 — every micro warhead is one body wearing a different paint job.
        put(ModMissileItems.missile_micro.get(), "missile_micro.obj", "missile_micro.png", 1.0F);
        put(ModMissileItems.missile_taint.get(), "missile_micro.obj", "missile_micro_taint.png", 1.0F);
        put(ModMissileItems.missile_bhole.get(), "missile_micro.obj", "missile_micro_bhole.png", 1.0F);
        put(ModMissileItems.missile_schrabidium.get(), "missile_micro.obj", "missile_micro_schrab.png", 1.0F);
        put(ModMissileItems.missile_emp.get(), "missile_micro.obj", "missile_micro_emp.png", 1.0F);
        put(ModMissileItems.missile_test.get(), "missile_micro.obj", "missile_test.png", 1.0F);

        // Tier 1 — the V2 body.
        put(ModMissileItems.missile_generic.get(), "missile_v2.obj", "missile_v2.png", 1.0F);
        put(ModMissileItems.missile_incendiary.get(), "missile_v2.obj", "missile_v2_inc.png", 1.0F);
        put(ModMissileItems.missile_cluster.get(), "missile_v2.obj", "missile_v2_cl.png", 1.0F);
        put(ModMissileItems.missile_buster.get(), "missile_v2.obj", "missile_v2_bu.png", 1.0F);
        put(ModMissileItems.missile_decoy.get(), "missile_v2.obj", "missile_v2_decoy.png", 1.0F);
        put(ModMissileItems.missile_anti_ballistic.get(), "missile_abm.obj", "missile_abm.png", 1.0F);

        // Tier 2 — the strong body, drawn at 1.5 (upstream's generateLarge).
        put(ModMissileItems.missile_strong.get(), "missile_strong.obj", "missile_strong.png", 1.5F);
        put(ModMissileItems.missile_incendiary_strong.get(), "missile_strong.obj", "missile_strong_inc.png", 1.5F);
        put(ModMissileItems.missile_cluster_strong.get(), "missile_strong.obj", "missile_strong_cl.png", 1.5F);
        put(ModMissileItems.missile_buster_strong.get(), "missile_strong.obj", "missile_strong_bu.png", 1.5F);
        put(ModMissileItems.missile_emp_strong.get(), "missile_strong.obj", "missile_strong_emp.png", 1.5F);

        // Tier 3 — the huge body.
        put(ModMissileItems.missile_burst.get(), "missile_huge.obj", "missile_huge.png", 1.0F);
        put(ModMissileItems.missile_inferno.get(), "missile_huge.obj", "missile_huge_inc.png", 1.0F);
        put(ModMissileItems.missile_rain.get(), "missile_huge.obj", "missile_huge_cl.png", 1.0F);
        put(ModMissileItems.missile_drill.get(), "missile_huge.obj", "missile_huge_bu.png", 1.0F);

        // Tier 4 — the Atlas body, except the N2, which has its own.
        put(ModMissileItems.missile_nuclear.get(), "missile_atlas.obj", "missile_atlas_nuclear.png", 1.0F);
        put(ModMissileItems.missile_nuclear_cluster.get(), "missile_atlas.obj", "missile_atlas_thermo.png", 1.0F);
        put(ModMissileItems.missile_volcano.get(), "missile_atlas.obj", "missile_atlas_tectonic.png", 1.0F);
        put(ModMissileItems.missile_doomsday.get(), "missile_atlas.obj", "missile_atlas_doomsday.png", 1.0F);
        put(ModMissileItems.missile_doomsday_rusted.get(), "missile_atlas.obj", "missile_atlas_doomsday_weathered.png", 1.0F);
        put(ModMissileItems.missile_n2.get(), "missile_n2_body.obj", "missile_n2.png", 1.5F);

        put(ModMissileItems.missile_stealth.get(), "missile_stealth.obj", "missile_stealth.png", 1.0F);
        put(ModMissileItems.missile_shuttle.get(), "missile_shuttle.obj", "missile_shuttle.png", 1.0F);
    }

    private static void put(Item item, String model, String texture, float scale) {
        WavefrontObj mesh = MESHES.computeIfAbsent(model,
                path -> WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/missiles/" + path)));
        MODELS.put(item, new MissileModel(mesh,
                Surface.cutout(Constants.MOD_ID + ":textures/models/missiles/" + texture), scale));
    }
}
