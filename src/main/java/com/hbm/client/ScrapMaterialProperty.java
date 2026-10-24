//? if >=26.1 {
package com.hbm.client;

import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm.items.machine.ItemScraps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 ItemAutogen keys the scraps sprite on the stack's material; the port has no meta to select on, so the model
 * dispatches on the material name the ScrapData component carries.
 *
 * <p>1.7 ItemScraps.getIconIndex:72-84 answers the two molten sheets for castable/additive materials;
 * getColorFromItemStack:57-68 modulates a noncastable liquid flag's solid palette with moltenColor.
 */
public record ScrapMaterialProperty() implements SelectItemModelProperty<String> {

    public static final String LIQUID = "liquid";
    public static final String ADDITIVE = "additive";

    public static final Codec<String> VALUE_CODEC = Codec.STRING;
    public static final SelectItemModelProperty.Type<ScrapMaterialProperty, String> TYPE =
            SelectItemModelProperty.Type.create(MapCodec.unit(new ScrapMaterialProperty()), VALUE_CODEC);

    @Override
    public @Nullable String get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner,
                                int seed, ItemDisplayContext displayContext) {
        MaterialStack contents = ItemScraps.getMats(stack);
        if (contents == null) return null;

        if (ItemScraps.isLiquid(stack)) {
            if (contents.material.smeltable == SmeltingBehavior.SMELTABLE) return LIQUID;
            if (contents.material.smeltable == SmeltingBehavior.ADDITIVE) return ADDITIVE;
            return com.hbm.items.ScrapsModels.key(contents.material) + "_liquid";
        }

        return com.hbm.items.ScrapsModels.key(contents.material);
    }

    @Override
    public SelectItemModelProperty.Type<ScrapMaterialProperty, String> type() {
        return TYPE;
    }

    @Override
    public Codec<String> valueCodec() {
        return VALUE_CODEC;
    }
}
//?}
