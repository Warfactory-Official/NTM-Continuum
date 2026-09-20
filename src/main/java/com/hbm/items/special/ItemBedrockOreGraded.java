package com.hbm.items.special;

import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/**
 * One flattened variant of the 1.7 {@code ItemBedrockOreNew} (registry {@code bedrock_ore}, metadata
 * {@code grade << 4 | type}): {@code bedrock_ore_<grade prefix>_<type suffix>}.
 *
 * <p>The 1.7 item carried 26 processing grades, each with a tint and a set of overlay traits drawn as
 * extra render passes. Only the untinted, trait-less {@link Grade#BASE} grade is registered by the ore
 * slopper family; the enum keeps the full table so the refining chain can extend it without renaming.
 */
public class ItemBedrockOreGraded extends CompatItem {

    public final Grade grade;
    public final BedrockOreType type;

    public ItemBedrockOreGraded(Properties properties, Grade grade, BedrockOreType type) {
        super(properties);
        this.grade = grade;
        this.type = type;
    }

    /** 1.7: {@code item.bedrock_ore.grade.<grade>.name} formatted with {@code item.bedrock_ore.type.<suffix>.name}. */
    @Override
    public Component getName(ItemStack stack) {
        Component typeName = Component.translatableWithFallback("item.bedrock_ore.type." + type.suffix + ".name", type.displayName);
        return Component.translatableWithFallback("item.bedrock_ore.grade." + grade.name().toLowerCase(Locale.ROOT) + ".name",
                "%s Bedrock Ore", typeName);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (ProcessingTrait trait : grade.traits) {
            tooltip.add(Component.translatable("item.bedrock_ore.trait." + trait.name().toLowerCase(Locale.ROOT)));
        }
    }

    public enum ProcessingTrait {
        ROASTED, ARC, WASHED, CENTRIFUGED, SULFURIC, SOLVENT, RAD
    }

    /** The exact 1.7 grade table (tint, texture prefix, traits). */
    public enum Grade {
        BASE(0xFFFFFF, "base"),
        BASE_ROASTED(0xCFCFCF, "base", ProcessingTrait.ROASTED),
        BASE_WASHED(0xDBE2CB, "base", ProcessingTrait.WASHED),
        PRIMARY(0xFFFFFF, "primary", ProcessingTrait.CENTRIFUGED),
        PRIMARY_ROASTED(0xCFCFCF, "primary", ProcessingTrait.ROASTED),
        PRIMARY_SULFURIC(0xFFFFD3, "primary", ProcessingTrait.SULFURIC),
        PRIMARY_NOSULFURIC(0xD3D4FF, "primary", ProcessingTrait.CENTRIFUGED, ProcessingTrait.SULFURIC),
        PRIMARY_SOLVENT(0xD3F0FF, "primary", ProcessingTrait.SOLVENT),
        PRIMARY_NOSOLVENT(0xFFDED3, "primary", ProcessingTrait.CENTRIFUGED, ProcessingTrait.SOLVENT),
        PRIMARY_RAD(0xECFFD3, "primary", ProcessingTrait.RAD),
        PRIMARY_NORAD(0xEBD3FF, "primary", ProcessingTrait.CENTRIFUGED, ProcessingTrait.RAD),
        PRIMARY_FIRST(0xFFD3D4, "primary", ProcessingTrait.CENTRIFUGED),
        PRIMARY_SECOND(0xD3FFEB, "primary", ProcessingTrait.CENTRIFUGED),
        CRUMBS(0xFFFFFF, "crumbs", ProcessingTrait.CENTRIFUGED),
        SULFURIC_BYPRODUCT(0xFFFFFF, "sulfuric", ProcessingTrait.CENTRIFUGED, ProcessingTrait.SULFURIC),
        SULFURIC_ROASTED(0xCFCFCF, "sulfuric", ProcessingTrait.ROASTED, ProcessingTrait.SULFURIC),
        SULFURIC_ARC(0xC3A2A2, "sulfuric", ProcessingTrait.ARC, ProcessingTrait.SULFURIC),
        SULFURIC_WASHED(0xDBE2CB, "sulfuric", ProcessingTrait.WASHED, ProcessingTrait.SULFURIC),
        SOLVENT_BYPRODUCT(0xFFFFFF, "solvent", ProcessingTrait.CENTRIFUGED, ProcessingTrait.SOLVENT),
        SOLVENT_ROASTED(0xCFCFCF, "solvent", ProcessingTrait.ROASTED, ProcessingTrait.SOLVENT),
        SOLVENT_ARC(0xC3A2A2, "solvent", ProcessingTrait.ARC, ProcessingTrait.SOLVENT),
        SOLVENT_WASHED(0xDBE2CB, "solvent", ProcessingTrait.WASHED, ProcessingTrait.SOLVENT),
        RAD_BYPRODUCT(0xFFFFFF, "rad", ProcessingTrait.CENTRIFUGED, ProcessingTrait.RAD),
        RAD_ROASTED(0xCFCFCF, "rad", ProcessingTrait.ROASTED, ProcessingTrait.RAD),
        RAD_ARC(0xC3A2A2, "rad", ProcessingTrait.ARC, ProcessingTrait.RAD),
        RAD_WASHED(0xDBE2CB, "rad", ProcessingTrait.WASHED, ProcessingTrait.RAD);

        public final int tint;
        public final String prefix;
        public final ProcessingTrait[] traits;

        Grade(int tint, String prefix, ProcessingTrait... traits) {
            this.tint = tint;
            this.prefix = prefix;
            this.traits = traits;
        }
    }
}
