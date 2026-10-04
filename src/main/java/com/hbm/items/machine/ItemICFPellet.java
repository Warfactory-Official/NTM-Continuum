package com.hbm.items.machine;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModIcfItems;
import com.hbm.util.BobMathUtil;
import dev.golem.core.data.ModelData;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * The filled ICF fuel pellet (1.7 {@code ItemICFPellet}): two fuels, an optional muon catalyst, and the laser
 * heat it has soaked up. The reactor calls {@link #react} every tick the laser beats {@link #getFusingDifficulty}
 * and swaps in a depleted pellet once {@link #getDepletion} reaches {@link #getMaxDepletion}.
 *
 * <p>1.7 drew two passes: the {@code icf_pellet_bg} layer tinted with the average of both fuels' colours and the
 * untinted {@code icf_pellet} shell over it. The colour is stored with {@link ModelData#setColor} when the pellet
 * is set up; the item definition's first tint reads it, and a pellet without one falls back to the
 * deuterium/tritium average, which is what 1.7 showed for a pellet without NBT.
 */
public class ItemICFPellet extends CompatItem {

    public enum EnumICFFuel {

        HYDROGEN(0x4040FF, 1.00D, 0.85D, 1.00D),
        DEUTERIUM(0x2828CB, 1.25D, 1.00D, 1.00D),
        TRITIUM(0x000092, 1.50D, 1.00D, 1.05D),
        HELIUM3(0xFFF09F, 1.75D, 1.00D, 1.25D),
        HELIUM4(0xFF9B60, 2.00D, 1.00D, 1.50D),
        LITHIUM(0xE9E9E9, 1.25D, 0.85D, 2.00D),
        BERYLLIUM(0xA79D80, 2.00D, 1.00D, 2.50D),
        BORON(0x697F89, 3.00D, 0.50D, 3.50D),
        CARBON(0x454545, 2.00D, 1.00D, 5.00D),
        OXYGEN(0xB4E2FF, 1.25D, 1.50D, 7.50D),
        SODIUM(0xDFE4E7, 3.00D, 0.75D, 8.75D),
        //aluminium, silicon, phosphorus
        CHLORINE(0xDAE598, 2.50D, 1.00D, 9.25D),
        CALCIUM(0xD2C7A9, 3.00D, 1.00D, 9.75D),
        //titanium
        ;

        public final int color;
        public final double reactionMult;
        public final double depletionSpeed;
        public final double fusingDifficulty;

        EnumICFFuel(int color, double react, double depl, double laser) {
            this.color = color;
            this.reactionMult = react;
            this.depletionSpeed = depl;
            this.fusingDifficulty = laser;
        }
    }

    public static final HashMap<FluidType, EnumICFFuel> fluidMap = new HashMap<>();
    public static final HashMap<NTMMaterial, EnumICFFuel> materialMap = new HashMap<>();

    public static void init() {
        if (!fluidMap.isEmpty() && !materialMap.isEmpty()) return;
        fluidMap.put(Fluids.HYDROGEN, EnumICFFuel.HYDROGEN);
        fluidMap.put(Fluids.DEUTERIUM, EnumICFFuel.DEUTERIUM);
        fluidMap.put(Fluids.TRITIUM, EnumICFFuel.TRITIUM);
        fluidMap.put(Fluids.HELIUM3, EnumICFFuel.HELIUM3);
        fluidMap.put(Fluids.HELIUM4, EnumICFFuel.HELIUM4);
        materialMap.put(Mats.MAT_LITHIUM, EnumICFFuel.LITHIUM);
        materialMap.put(Mats.MAT_BERYLLIUM, EnumICFFuel.BERYLLIUM);
        materialMap.put(Mats.MAT_BORON, EnumICFFuel.BORON);
        materialMap.put(Mats.MAT_GRAPHITE, EnumICFFuel.CARBON);
        fluidMap.put(Fluids.OXYGEN, EnumICFFuel.OXYGEN);
        materialMap.put(Mats.MAT_SODIUM, EnumICFFuel.SODIUM);
        fluidMap.put(Fluids.CHLORINE, EnumICFFuel.CHLORINE);
        materialMap.put(Mats.MAT_CALCIUM, EnumICFFuel.CALCIUM);
    }

    public ItemICFPellet(Properties properties) {
        super(properties);
    }

    public static long getMaxDepletion(ItemStack stack) {
        long base = 50_000_000_000L;
        base /= getType(stack, true).depletionSpeed;
        base /= getType(stack, false).depletionSpeed;
        return base;
    }

    public static long getFusingDifficulty(ItemStack stack) {
        long base = 10_000_000L;
        base *= getType(stack, true).fusingDifficulty * getType(stack, false).fusingDifficulty;
        if (ModIcfItems.PELLET_MUON.getOr(stack, false)) base /= 4;
        return base;
    }

    public static long getDepletion(ItemStack stack) {
        return ModIcfItems.PELLET_DEPLETION.getOr(stack, 0L);
    }

    /** Adds {@code heat} to the depletion and returns the heat the reaction gives back. */
    public static long react(ItemStack stack, long heat) {
        ModIcfItems.PELLET_DEPLETION.set(stack, getDepletion(stack) + heat);
        return (long) (heat * getType(stack, true).reactionMult * getType(stack, false).reactionMult);
    }

    public static ItemStack setup(EnumICFFuel type1, EnumICFFuel type2, boolean muon) {
        return setup(new ItemStack(ModIcfItems.icf_pellet.get()), type1, type2, muon);
    }

    public static ItemStack setup(ItemStack stack, EnumICFFuel type1, EnumICFFuel type2, boolean muon) {
        ModIcfItems.PELLET_TYPE1.set(stack, type1.name().toLowerCase(java.util.Locale.ROOT));
        ModIcfItems.PELLET_TYPE2.set(stack, type2.name().toLowerCase(java.util.Locale.ROOT));
        ModIcfItems.PELLET_MUON.set(stack, muon);
        ModelData.setColor(stack, mixedColor(type1, type2));
        return stack;
    }

    /** 1.7 with no NBT: deuterium and tritium. The fuel is saved by name; an unknown name reads as the default. */
    public static EnumICFFuel getType(ItemStack stack, boolean first) {
        var component = first ? ModIcfItems.PELLET_TYPE1 : ModIcfItems.PELLET_TYPE2;
        if (!component.has(stack)) return first ? EnumICFFuel.DEUTERIUM : EnumICFFuel.TRITIUM;
        EnumICFFuel fallback = first ? EnumICFFuel.DEUTERIUM : EnumICFFuel.TRITIUM;
        return com.hbm.util.SavedIds.byName(EnumICFFuel.class, component.getOr(stack, ""), fallback);
    }

    /** 1.7 {@code getColorFromItemStack} pass 0: the per-channel average of both fuels. */
    public static int mixedColor(EnumICFFuel type1, EnumICFFuel type2) {
        int r = (((type1.color & 0xff0000) >> 16) + ((type2.color & 0xff0000) >> 16)) / 2;
        int g = (((type1.color & 0x00ff00) >> 8) + ((type2.color & 0x00ff00) >> 8)) / 2;
        int b = ((type1.color & 0x0000ff) + (type2.color & 0x0000ff)) / 2;
        return r << 16 | g << 8 | b;
    }

    public static double getDurabilityForDisplay(ItemStack stack) {
        return (double) getDepletion(stack) / (double) getMaxDepletion(stack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getDurabilityForDisplay(stack) > 0D;
    }

    /** 1.7's durability bar: {@code 13 - round(fraction * 13)} pixels, hue sliding from green to red. */
    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.max(0, 13 - (int) Math.round(getDurabilityForDisplay(stack) * 13.0D));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = (float) Math.max(0.0D, 1.0D - getDurabilityForDisplay(stack));
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        boolean muon = ModIcfItems.PELLET_MUON.getOr(stack, false);
        list.add(Component.literal("Depletion: " + String.format(Locale.US, "%.1f", getDurabilityForDisplay(stack) * 100D) + "%")
                .withStyle(ChatFormatting.GREEN));
        list.add(Component.literal("Fuel: ")
                .append(Component.translatable("icffuel." + getType(stack, true).name().toLowerCase(Locale.US)))
                .append(" / ")
                .append(Component.translatable("icffuel." + getType(stack, false).name().toLowerCase(Locale.US)))
                .withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("Heat required: " + BobMathUtil.getShortNumber(getFusingDifficulty(stack)) + "TU")
                .withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("Reactivity multiplier: x"
                + (int) (getType(stack, true).reactionMult * getType(stack, false).reactionMult * 100) / 100D)
                .withStyle(ChatFormatting.YELLOW));
        if (muon) list.add(Component.literal("Muon catalyzed!").withStyle(ChatFormatting.DARK_AQUA));
    }
}
