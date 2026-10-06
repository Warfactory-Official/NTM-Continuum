package com.hbm.client.render.weapon;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import com.hbm.items.ModGunItems;
import dev.golem.core.Ids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The weapon renderers, by name.
 *
 * <p>The item definition names one ({@code "weapon": "atlas"}) rather than describing the geometry,
 * because a Sedna weapon's first-person body is code — a hand-built transform tree reading animation
 * buses — not something a model JSON can express. What the JSON still owns is the {@code base} model
 * whose {@code display{}} block frames the non-first-person contexts.
 */
public final class WeaponRenderers {

    private static final Map<String, WeaponRenderer> BY_NAME = new HashMap<>();
    private static final Map<Supplier<Item>, WeaponRenderer> DECLARED_BY_ITEM = new LinkedHashMap<>();
    private static Map<Item, WeaponRenderer> byItem;

    private WeaponRenderers() {}

    public static WeaponRenderer byName(String name) {
        WeaponRenderer renderer = BY_NAME.get(name);
        if (renderer == null) throw new IllegalArgumentException("Unknown weapon renderer: " + name);
        return renderer;
    }

    public static float viewFov(ItemStack stack, float fov, float partialTick) {
        WeaponRenderer renderer = byItem().get(stack.getItem());
        return renderer == null ? fov : renderer.viewFov(stack, fov, partialTick);
    }

    /** The renderer drawing this stack, or null when it is not a Sedna weapon renderer's. */
    public static @org.jspecify.annotations.Nullable WeaponRenderer forStack(ItemStack stack) {
        return stack.isEmpty() ? null : byItem().get(stack.getItem());
    }

    /**
     * Whether the stack is drawn by a weapon renderer - 1.7's {@code ItemRenderWeaponBase}, an
     * {@code IItemRenderer} - rather than by a sprite or block model. A caller that reproduces 1.7's
     * {@code EntityItem} route needs to know, because Forge sent such an item down its own arm.
     */
    public static boolean handles(ItemStack stack) {
        return !stack.isEmpty() && byItem().containsKey(stack.getItem());
    }

    private static Map<Item, WeaponRenderer> byItem() {
        if (byItem == null) {
            Map<Item, WeaponRenderer> resolved = new HashMap<>();
            DECLARED_BY_ITEM.forEach((item, renderer) -> {
                Item value = item.get();
                if (value != null && value != Items.AIR) resolved.put(value, renderer);
            });
            byItem = resolved;
        }
        return byItem;
    }

    /**
     * Declares the renderers without resolving item suppliers, so this can run at client-init time on
     * either loader without touching an unflushed registry.
     */
    public static void init() {
        register("debug", ModGunItems.gun_debug::get, new DebugWeaponRenderer());
        register("atlas", ModGunItems.gun_light_revolver::get, new AtlasWeaponRenderer());
        register("am180", ModGunItems.gun_am180::get,
                new Sedna22lrWeaponRenderer(Sedna22lrWeaponRenderer.Variant.AM180));
        register("star_f", ModGunItems.gun_star_f::get,
                new Sedna22lrWeaponRenderer(Sedna22lrWeaponRenderer.Variant.STAR_F));
        register("star_f_akimbo", ModGunItems.gun_star_f_akimbo::get,
                new Sedna22lrWeaponRenderer(Sedna22lrWeaponRenderer.Variant.STAR_F_AKIMBO));
        register("greasegun", ModGunItems.gun_greasegun::get,
                new Sedna9mmWeaponRenderer(Sedna9mmWeaponRenderer.Variant.GREASEGUN));
        register("lag", ModGunItems.gun_lag::get,
                new Sedna9mmWeaponRenderer(Sedna9mmWeaponRenderer.Variant.LAG));
        register("uzi", ModGunItems.gun_uzi::get,
                new Sedna9mmWeaponRenderer(Sedna9mmWeaponRenderer.Variant.UZI));
        register("uzi_akimbo", ModGunItems.gun_uzi_akimbo::get,
                new Sedna9mmWeaponRenderer(Sedna9mmWeaponRenderer.Variant.UZI_AKIMBO));
        register("henry", ModGunItems.gun_henry::get,
                new Sedna44WeaponRenderer(Sedna44WeaponRenderer.Variant.HENRY));
        register("henry_lincoln", ModGunItems.gun_henry_lincoln::get,
                new Sedna44WeaponRenderer(Sedna44WeaponRenderer.Variant.HENRY_LINCOLN));
        register("heavy_revolver", ModGunItems.gun_heavy_revolver::get,
                new Sedna44WeaponRenderer(Sedna44WeaponRenderer.Variant.HEAVY_REVOLVER));
        register("heavy_revolver_lilmac", ModGunItems.gun_heavy_revolver_lilmac::get,
                new Sedna44WeaponRenderer(Sedna44WeaponRenderer.Variant.LILMAC));
        register("heavy_revolver_protege", ModGunItems.gun_heavy_revolver_protege::get,
                new Sedna44WeaponRenderer(Sedna44WeaponRenderer.Variant.PROTEGE));
        register("hangman", ModGunItems.gun_hangman::get,
                new Sedna44WeaponRenderer(Sedna44WeaponRenderer.Variant.HANGMAN));
        register("atlas_variant", ModGunItems.gun_light_revolver_atlas::get,
                new AtlasWeaponRenderer("bio_revolver_atlas"));
        register("dani", ModGunItems.gun_light_revolver_dani::get, new DaniWeaponRenderer());
        register("pepperbox", ModGunItems.gun_pepperbox::get, new PepperboxWeaponRenderer());
        register("g3", ModGunItems.gun_g3::get, new G3WeaponRenderer());
        register("b92", ModGunItems.gun_b92::get, new B92WeaponRenderer());

        // The catalogue helpers own their renderer families. Keep item resolution lazy because client
        // initialization precedes deferred registry flush on NeoForge and Fabric.
        BallisticWeaponRenderers.register(WeaponRenderers::registerCatalogueEntry);
        SpecialWeaponRenderers.register(WeaponRenderers::registerCatalogueEntry);
    }

    private static void register(String name, Supplier<Item> item, WeaponRenderer renderer) {
        BY_NAME.put(name, renderer);
        DECLARED_BY_ITEM.put(item, renderer);
        byItem = null;
    }

    private static void registerCatalogueEntry(String name, WeaponRenderer renderer) {
        register(name, () -> BuiltInRegistries.ITEM.getValue(Ids.of("hbm", "gun_" + name)), renderer);
    }
}
