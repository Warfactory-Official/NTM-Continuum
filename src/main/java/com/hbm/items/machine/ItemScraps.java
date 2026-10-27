package com.hbm.items.machine;

import dev.golem.core.data.ModelData;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModMachineItems;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Foundry scraps: an arbitrary amount of a single molten {@link NTMMaterial}, solidified into an item
 * (shovel-emptied crucibles/channels, broken foundry blocks, spilled pours). Melts back into exactly its
 * contents. The 1.12.2 {@code ItemScraps} stored material as item metadata + amount as NBT; the port uses
 * the {@link ModDataComponents#MATERIAL} (material name) + {@link ModDataComponents#MAT_AMOUNT} (quanta)
 * components. The model selects each material's recoloured solid sprite, or its molten sheet and colour.
 */
public class ItemScraps extends CompatItem {

    public ItemScraps(Properties properties) {
        super(properties);
    }

    /** The contained material+amount, or null if the components are missing/unknown. */
    public static Mats.@Nullable MaterialStack getMats(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemScraps)) return null;
        String name = ModDataComponents.material(stack);
        if (name == null) return null;
        NTMMaterial mat = Mats.byName(name);
        if (mat == null) return null;
        int amount = ModDataComponents.materialAmount(stack, MaterialShapes.INGOT.quantity);
        return new Mats.MaterialStack(mat, amount);
    }

    public static ItemStack create(Mats.MaterialStack mats) {
        return create(mats, false);
    }

    /**
     * {@code liquid} = the <b>molten</b> display form (the 1.12.2 {@code "liquid"} NBT flag, here
     * custom_model_data flag 0): the {@code scraps_liquid} sprite and the bare material name, which is what the
     * old crucible JEI screen shows, not an obtainable solid item.
     */
    public static ItemStack create(Mats.MaterialStack mats, boolean liquid) {
        if (mats.material == null) return ItemStack.EMPTY;
        ItemStack scrap = new ItemStack(ModMachineItems.scraps.get());
        ModDataComponents.material(scrap, mats.material.name);
        ModDataComponents.setMaterialAmount(scrap, mats.amount);
        ModelData.setColorAndFlag(scrap, liquid ? mats.material.moltenColor : mats.material.solidColorLight, liquid);
        return scrap;
    }

    /** Whether this is the molten display form (custom_model_data flag 0). */
    public static boolean isLiquid(ItemStack stack) {
        return ModelData.flag(stack);
    }

    /** Spawns the material as scrap item entities at the given position (old world-spill behaviour). */
    public static void spawnMats(Level level, double x, double y, double z, Mats.MaterialStack mats) {
        if (level.isClientSide() || mats.material == null || mats.amount <= 0) return;
        ItemStack stack = create(mats);
        if (stack.isEmpty()) return;
        ItemEntity entity = new ItemEntity(level, x, y, z, stack);
        level.addFreshEntity(entity);
    }

    @Override
    public Component getName(ItemStack stack) {
        Mats.MaterialStack contents = getMats(stack);
        if (contents == null) return Component.translatable("item.hbm.scraps");
        // Molten form: the bare material name, like the old getItemStackDisplayName's liquid branch.
        if (isLiquid(stack)) return contents.material.getLocalizedName();
        return Component.translatable("item.hbm.scraps.material", contents.material.getLocalizedName());
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        Mats.MaterialStack contents = getMats(stack);
        if (contents != null) {
            tooltip.add(Component.literal(Mats.formatAmount(contents.amount, false)).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(Mats.formatAmount(contents.amount, true)).withStyle(ChatFormatting.DARK_GRAY));
            if (isLiquid(stack) && contents.material.smeltable == NTMMaterial.SmeltingBehavior.ADDITIVE) {
                tooltip.add(Component.literal("Additive, not castable!").withStyle(ChatFormatting.DARK_RED));
            }
        }
    }
}
