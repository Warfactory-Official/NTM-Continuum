package com.hbm.client.render.armor;

import com.hbm.client.jetpack.JetpackClientVisuals;
import com.hbm.items.armor.ArmorFSB;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CompatHumanoidLayer;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.HumanoidPose;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Worn;
import dev.golem.core.world.ArmorSlot;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Draws HBM's modelled armour on anything humanoid: players, mannequins, armour stands, zombies.
 *
 * <p>One layer, attached by Golem Core to every humanoid renderer, dispatches each occupied slot to the
 * suit's {@link ArmorRenderer}. A suit with no declared model falls through to vanilla's equipment-texture
 * layer, which is what the hazmat suits want.
 */
public class ArmorRenderLayer extends CompatHumanoidLayer {

    @SuppressWarnings("rawtypes")
    public ArmorRenderLayer(RenderLayerParent parent) {
        super(parent);
        ModelMan.addSkinModel(humanoidModel());
    }

    private static final ArmorSlot[] SLOTS = ArmorSlot.values();

    @Override
    protected boolean drawsUnequipped() {
        return true;
    }

    @Override
    public void submit(Worn worn, PoseStack pose, SubmitSink sink, int light, HumanoidPose parts,
            CameraInfo camera) {
        boolean manly = ModelMan.isManly(parts.player());
        // 1.7 draws ModelMan in the armour pass, which also runs for invisible players
        if (parts.invisible && !manly) return;

        boolean crouching = parts.sneaking
                && !JetpackClientVisuals.freezesPlayerPose(parts.player());
        HumanoidPose armorPose = crouching == parts.sneaking ? parts : new HumanoidPose(parts.model(), false,
                parts.baby, parts.invisible, parts.player());
        pose.pushPose();
        // ModelArmorBase applies the same global offset after copying the live crouched bones.
        if (crouching && !parts.baby) pose.translate(0F, 0.2F, 0F);
        if (manly) ArmorRenderers.get("manly").submit(ArmorSlot.CHESTPLATE, pose, sink, light, armorPose);
        if (!parts.invisible) submitSuits(worn, pose, sink, light, armorPose);
        pose.popPose();
    }

    /**
     * A first-person view that draws its own arms: the sleeves of the modelled chestplate {@code chest} on its arm frames ({@link ArmorPose#arms}).
     *
     * @return whether anything was drawn (the chestplate is a modelled suit)
     */
    public static boolean submitFirstPersonArms(ItemStack chest, PoseStack pose, SubmitSink sink, int light, org.joml.Matrix4fc[] arms) {
        if (!(chest.getItem() instanceof ArmorFSB piece) || piece.slot != ArmorSlot.CHESTPLATE) return false;
        ArmorRenderer renderer = renderer(piece, chest);
        if (renderer == null) return false;
        renderer.submitArms(pose, sink, light, arms);
        return true;
    }

    /** The worn model of a suit piece, or null for a piece drawn as a flat armour layer. */
    static ArmorRenderer renderer(ArmorFSB piece, ItemStack stack) {
        String modelName = piece.armorModel();
        if (modelName == null) {
            String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            if (path.startsWith("bj_")) modelName = "bj";
            else if (path.startsWith("dns_") || path.startsWith("dnt_")) modelName = "dns";   // the DNT suit's items are dnt_*, its model "dns"
        }
        return ArmorRenderers.get(modelName);
    }

    private static void submitSuits(Worn worn, PoseStack pose, SubmitSink sink, int light, HumanoidPose armorPose) {
        for (ArmorSlot slot : SLOTS) {
            ItemStack stack = worn.get(slot);
            if (!(stack.getItem() instanceof ArmorFSB piece)) continue;

            ArmorRenderer renderer = renderer(piece, stack);
            if (renderer == null) continue;

            renderer.submit(slot, pose, sink, light, armorPose);
        }
    }
}
