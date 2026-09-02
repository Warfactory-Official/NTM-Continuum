package com.hbm.items.special;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.NTMMaterial;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One auto-generated metal-shape item — a single {@link MaterialShapes shape} of a single
 * {@link NTMMaterial material} (e.g. {@code plate_cast_iron} = an iron cast plate). The 1.12.2
 * {@code ItemAutogen} packed all materials into one item via metadata + a runtime-recoloured sprite; 1.21
 * has no metadata, so the faithful port is <b>one registered item per material×shape</b> with a
 * <b>pre-baked</b> texture (the exact per-channel colour remap, computed by
 * {@code port-tools/gen_autogen_textures.py}) and a plain {@code item/generated} model — no
 * {@code custom_model_data}/select indirection, and tag-native.
 *
 * <p>Display names use the exact legacy shape template (e.g. "Light Steel Barrel" and "Ivory Grip").
 * Recipes match these via {@link com.hbm.inventory.RecipesCommon.MaterialStack} (which resolves the exact
 * item for a material+shape).
 */
public class ItemAutogen extends Item {

    public final MaterialShapes shape;
    public final NTMMaterial material;

    public ItemAutogen(Properties properties, MaterialShapes shape, NTMMaterial material) {
        super(properties);
        this.shape = shape;
        this.material = material;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(shape.displayName(material.getDisplayName()));
    }
}
