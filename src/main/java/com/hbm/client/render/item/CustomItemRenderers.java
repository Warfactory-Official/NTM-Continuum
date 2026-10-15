package com.hbm.client.render.item;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The ported 1.7 item renderers, by the name an item definition's {@code hbm:custom_item} model gives.
 *
 * <p>Each is built on first use, on the render thread, because building one loads its OBJs.
 */
public final class CustomItemRenderers {

    /** Packed light 1.7 got from {@code setLightmapTextureCoords(240, 240)}. */
    static final int FULLBRIGHT = 0xF000F0;

    private static final Map<String, Supplier<CustomItemRenderer>> FACTORIES = new ConcurrentHashMap<>(Map.ofEntries(
            Map.entry("shim", ItemRenderShim::new),
            Map.entry("gavel", ItemRenderGavel::new),
            Map.entry("big_sword", ItemRenderBigSword::new),
            Map.entry("redstone_sword", ItemRenderRedstoneSword::new),
            Map.entry("chainsaw", ItemRenderChainsaw::new),
            Map.entry("crucible", ItemRenderCrucible::new),
            Map.entry("egon", ItemRenderGunEgon::new),
            Map.entry("jshotgun", ItemRenderJShotgun::new),
            Map.entry("boltgun", ItemRenderBoltgun::new), // G-C: 1.7 ItemRenderBoltgun (RECOIL bus)
            Map.entry("missile_custom", ItemRenderMissile::new),
            Map.entry("detonator_laser", ItemRenderDetonatorLaser::new),
            Map.entry("grenade", ItemRenderGrenade::new),
            Map.entry("deco_block", ItemRenderDecoBlock::new),
            Map.entry("bobblehead", ItemRenderBobble::new),
            Map.entry("plushie", ItemRenderPlushie::new),
            Map.entry("hot", ItemRendererHot::new),
            Map.entry("meteorite_sword", ItemRendererMeteorSword::new)));

    private static final Map<String, CustomItemRenderer> BUILT = new HashMap<>();

    private CustomItemRenderers() {}

    /**
     * Adds a renderer an item definition's {@code hbm:custom_item} model can name: how an addon brings its own ported
     * 1.7 {@code IItemRenderer}s. An addon names its renderers {@code "<namespace>:<name>"} (HBM's are bare names), and
     * registers from its client set-up, before the first resource load (definitions are checked against this list when
     * they are read).
     *
     * @throws IllegalStateException if {@code name} is taken
     */
    public static void register(String name, Supplier<CustomItemRenderer> factory) {
        if (FACTORIES.putIfAbsent(name, factory) != null) {
            throw new IllegalStateException("hbm:custom_item renderer '" + name + "' is already registered");
        }
    }

    /** Whether a definition may name {@code name}. Checked at bake/parse time, before anything is built. */
    public static boolean exists(String name) {
        return FACTORIES.containsKey(name);
    }

    /** The renderer registered under {@code name}; throws for an unknown name, which is a data error. */
    public static CustomItemRenderer byName(String name) {
        Supplier<CustomItemRenderer> factory = FACTORIES.get(name);
        if (factory == null) throw new IllegalArgumentException("Unknown hbm:custom_item renderer '" + name + "'");
        return new Lazy(name, factory);
    }

    /** Defers construction to the first draw, so a resource reload never loads OBJs off the render thread. */
    private record Lazy(String name, Supplier<CustomItemRenderer> factory) implements CustomItemRenderer {
        private CustomItemRenderer get() {
            return BUILT.computeIfAbsent(name, key -> factory.get());
        }

        @Override
        public boolean handles(ItemStack stack, Type type) {
            return get().handles(stack, type);
        }

        @Override
        public void render(ItemStack stack, Type type, PoseStack pose, SubmitSink sink, int light, int overlay,
                           @Nullable LivingEntity owner) {
            get().render(stack, type, pose, sink, light, overlay, owner);
        }

        @Override
        public void render(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                           SubmitSink sink, int light, int overlay, @Nullable LivingEntity owner) {
            get().render(stack, context, pose, sink, light, overlay, owner);
        }

        @Override
        public boolean animated(ItemStack stack, Type type) {
            return get().animated(stack, type);
        }
    }

    // Shared by the renderers

    static Identifier tex(String path) {
        return Ids.of(Constants.MOD_ID, path);
    }

    static WavefrontObj obj(String path) {
        return WavefrontObj.load(tex(path));
    }

    /** The item's registry path; the renderers told their items apart by identity, this is the same test. */
    static String id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    static void all(PoseStack pose, SubmitSink sink, WavefrontObj model, RenderType type, int light, int overlay) {
        sink.submitCustomGeometry(pose, type, (p, buffer) -> model.renderAll(p, buffer, light, overlay));
    }

    static void part(PoseStack pose, SubmitSink sink, WavefrontObj model, RenderType type, String part,
                     int light, int overlay) {
        sink.submitCustomGeometry(pose, type, (p, buffer) -> model.renderPart(part, p, buffer, light, overlay));
    }

    static RenderType culled(Identifier texture) {
        return CompatRenderTypes.entityCutoutCull(texture);
    }

    static RenderType open(Identifier texture) {
        return CompatRenderTypes.entityCutoutNoCull(texture);
    }

    /**
     * The render type for a renderer that never touched {@code GL_CULL_FACE}: culled as the GL default, except in
     * third person, where 1.7's {@code RendererLivingEntity.doRender} had disabled culling before it drew the held
     * item. That matters for a full-3D item: its hand pose ({@code weapon_full3d}) is a mirror, which turns culling
     * inside out, and 1.7 never culled it there.
     */
    static RenderType heldFaces(CustomItemRenderer.Type type, Identifier texture) {
        return type == CustomItemRenderer.Type.EQUIPPED ? open(texture) : culled(texture);
    }
}
