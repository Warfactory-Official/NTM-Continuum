package com.hbm.client.render.weapon;

//? if >=26.1 {

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A Sedna gun's item model.
 *
 * <p>This is an {@link ItemModel}, not a {@code minecraft:special} entry, and that distinction is the
 * whole point: {@link #update} is handed the {@link ItemDisplayContext}, while a special-model renderer
 * is not. A gun needs to know, because the first-person body is drawn in <em>camera space</em> (see
 * {@link WeaponRenderer#submitFirstPerson}) and has nothing to do with the pose vanilla would hand an
 * inventory icon.
 *
 * <p>So the context is resolved here and travels to the renderer inside the argument: first-person hands
 * carry a live, animated body; every other context gets the same geometry at rest, framed by the
 * weapon's own {@code setupStatic}.
 */
public class GunItemModel implements ItemModel {

    private final WeaponRenderer renderer;
    private final ModelRenderProperties properties;
    private final Body body = new Body();
    /** Memoised: the OBJ is only loadable once resources are up, and the bounds never change after. */
    private final java.util.function.Supplier<Vector3fc[]> extents;

    private GunItemModel(WeaponRenderer renderer, ModelRenderProperties properties) {
        this.renderer = renderer;
        this.properties = properties;
        this.extents = com.google.common.base.Suppliers.memoize(renderer::extents);
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
            ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        output.appendModelIdentityElement(this);
        ItemStackRenderState.LayerRenderState layer = output.newLayer();

        boolean firstPerson = displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        boolean otherArm = false;
        if (firstPerson) {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            // "Other arm" is whichever hand is not the player's main one, and its body is the main
            // hand's mirrored — which is why it needs to be known here and not guessed at draw time.
            boolean mainHand = minecraft.player != null
                    && (minecraft.player.getMainArm() == HumanoidArm.RIGHT)
                    == (displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
            otherArm = !mainHand;
        }

        // Without extents the layer reports an empty bounding box and the GUI icon never appears.
        layer.setExtents(extents);
        layer.setupSpecialModel(body, new Argument(item, renderer, firstPerson, otherArm, displayContext, owner));
        // A held gun animates every frame, so its render state must not be cached.
        if (firstPerson) output.setAnimated();
        properties.applyToLayer(layer, displayContext);
    }

    /** What the renderer needs, resolved once per frame from the stack and its display context. */
    public record Argument(ItemStack stack, WeaponRenderer renderer, boolean firstPerson, boolean otherArm,
            ItemDisplayContext context, @Nullable ItemOwner owner) {}

    /** The draw itself. Stateless — everything it needs arrives in the argument. */
    private static class Body implements SpecialModelRenderer<Argument> {

        @Override
        public void submit(@Nullable Argument argument, PoseStack pose, SubmitNodeCollector collector,
                int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            if (argument == null) return;
            // The renderers draw through Golem's sink so the same code serves both eras; here it wraps
            // 26.1's collector, and below 26.1 a recording sink flushed into a MultiBufferSource.
            dev.golem.core.render.SubmitSink sink = new dev.golem.core.render.VanillaSubmitSink(collector);
            if (argument.firstPerson()) {
                argument.renderer().submitFirstPerson(argument.stack(), pose, sink, lightCoords,
                        overlayCoords, argument.otherArm());
                return;
            }
            pose.pushPose();
            argument.renderer().setupStatic(pose, argument.context());
            argument.renderer().renderStatic(argument.stack(), pose, sink, lightCoords, overlayCoords,
                    argument.owner() == null ? null : argument.owner().asLivingEntity());
            pose.popPose();
        }

        @Override
        public void getExtents(Consumer<Vector3fc> output) {}

        @Override
        public @Nullable Argument extractArgument(ItemStack stack) {
            return null;
        }
    }

    /** {@code {"model": {"type": "hbm:gun", "base": ..., "weapon": ...}}} in the item definition. */
    public record Unbaked(Identifier base, String weapon) implements ItemModel.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.fieldOf("base").forGetter(Unbaked::base),
                com.mojang.serialization.Codec.STRING.fieldOf("weapon").forGetter(Unbaked::weapon)
        ).apply(i, Unbaked::new));

        @Override
        public MapCodec<? extends ItemModel.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(base);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            var baker = context.blockModelBaker();
            var model = baker.getModel(base);
            ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(
                    baker, model, model.getTopTextureSlots());
            return new GunItemModel(WeaponRenderers.byName(weapon), properties);
        }
    }
}
//?}
