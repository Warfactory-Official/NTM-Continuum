package com.hbm.inventory.fluid.trait;

import com.hbm.inventory.fluid.FluidType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The 1.7 {@code FluidTraitSimple} tags: value-less marker traits a {@link FluidType} either has or does not.
 * {@link com.hbm.inventory.fluid.Fluids} holds one shared instance of each (1.7's {@code LIQUID},
 * {@code GASEOUS}, {@code EVAP}, …) and assigns them to exactly 1.7's fluids. Every port predicate that used to
 * keep its own copy of one of these lists ({@code FluidCombustion.isGaseous}, {@code FluidContainers}'s lead and
 * no-container sets, {@code IFluidCopiable}'s no-ID set, the barrel and tank antimatter checks) now asks the
 * fluid for the trait instead.
 *
 * <p>Tooltip lines are 1.7's: most tags only show with Shift held ({@code addInfoHidden}); antimatter and the
 * lead-container requirement always show ({@code addInfo}); {@link FT_NoID} and {@link FT_NoContainer} add none.
 */
public final class FluidTraitSimple {

    private FluidTraitSimple() {}

    public static final class FT_Gaseous {
        public void addInfoHidden(List<Component> info) { info.add(tag("hbmfluid.trait.gaseous", ChatFormatting.BLUE)); }
    }

    /** Gaseous at room temperature, for cryogenic hydrogen for example. */
    public static final class FT_Gaseous_ART {
        public void addInfoHidden(List<Component> info) { info.add(tag("hbmfluid.trait.gaseousRoom", ChatFormatting.BLUE)); }
    }

    public static final class FT_Liquid {
        public void addInfoHidden(List<Component> info) { info.add(tag("hbmfluid.trait.liquid", ChatFormatting.BLUE)); }
    }

    /** Too viscous to be sprayed/turned into a mist. */
    public static final class FT_Viscous {
        public void addInfoHidden(List<Component> info) { info.add(tag("hbmfluid.trait.viscous", ChatFormatting.BLUE)); }
    }

    /** Deprecated in 1.7 but still on the six plasmas; its tag is a literal, untranslated {@code [Plasma]}. */
    public static final class FT_Plasma {
        public void addInfoHidden(List<Component> info) {
            info.add(Component.literal("[Plasma]").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    public static final class FT_Amat {
        public void addInfo(List<Component> info) { info.add(tag("hbmfluid.trait.antimatter", ChatFormatting.DARK_RED)); }
    }

    public static final class FT_LeadContainer {
        public void addInfo(List<Component> info) { info.add(tag("hbmfluid.trait.leadContainer", ChatFormatting.DARK_RED)); }
    }

    public static final class FT_Delicious {
        public void addInfoHidden(List<Component> info) { info.add(tag("hbmfluid.trait.delicious", ChatFormatting.DARK_GREEN)); }
    }

    public static final class FT_Unsiphonable {
        public void addInfoHidden(List<Component> info) { info.add(tag("hbmfluid.trait.unsiphonable", ChatFormatting.BLUE)); }
    }

    public static final class FT_NoID { }

    public static final class FT_NoContainer { }

    /**
     * {@code FT_Gaseous || FT_Gaseous_ART}: the pair 1.7's gas flare, fluid tank, mist and chemical thrower all
     * test together. (The drain alone tests {@code FT_Gaseous} by itself.)
     */
    public static boolean isGaseousAny(FluidType type) {
        return type != null && (type.hasTrait(FT_Gaseous.class) || type.hasTrait(FT_Gaseous_ART.class));
    }

    private static Component tag(String key, ChatFormatting style) {
        return Component.literal("[").append(Component.translatable(key)).append("]").withStyle(style);
    }
}
