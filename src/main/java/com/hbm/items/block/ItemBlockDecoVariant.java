package com.hbm.items.block;

import com.hbm.items.LegacyMeta;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * One block item per subtype of a 1.7 metadata block whose subtype lives in the block entity or the block state: the
 * bobbleheads, plushies, snowglobes, vending machines, filing cabinets and glyphid hive blocks. 1.7's item damage is
 * the member's fixed {@link #variant()}, and each member has its own id as ntm-next names them
 * ({@code bobblehead_<type>}, {@code vending_machine_snacks}, {@code filing_cabinet_steel}, {@code glyphid_base_rad}).
 * Nothing about the subtype is stored on the stack.
 *
 * <p>{@code name} answers the display name per variant (1.7 {@code ItemBlockMeta} kept one name for all,
 * {@code BlockPlushie.getOverrideDisplayName} formatted the type's label into it); {@code tooltip} is the
 * block's {@code addInformation}, or null.
 */
public class ItemBlockDecoVariant extends CompatBlockItem implements LegacyMeta {

    private final int variant;
    private final IntFunction<Component> name;
    private final IntFunction<List<Component>> tooltip;

    public ItemBlockDecoVariant(Block block, Properties properties, int variant, IntFunction<Component> name,
                                IntFunction<List<Component>> tooltip) {
        super(block, properties);
        this.variant = variant;
        this.name = name;
        this.tooltip = tooltip;
    }

    /**
     * Every block's members, by variant, as registered. A holder class of its own: registration classes record into it
     * in a plain JVM at build time (Dejson), where this item class cannot be initialised.
     */
    public static final class Families {
        private static final List<Family> FAMILIES = new ArrayList<>();

        private record Family(Supplier<? extends Block> block, List<? extends RegistryObject<? extends ItemBlockDecoVariant>> members) {}

        private Families() {}

        /** Records {@code members} (index = 1.7 metadata) as the items of {@code block}, for {@link #stackOf}. */
        public static void record(Supplier<? extends Block> block,
                                  List<? extends RegistryObject<? extends ItemBlockDecoVariant>> members) {
            synchronized (FAMILIES) {
                FAMILIES.add(new Family(block, List.copyOf(members)));
            }
        }

        static ItemStack stackOf(Block block, int variant) {
            synchronized (FAMILIES) {
                for (Family family : FAMILIES) {
                    if (family.block().get() == block) {
                        var members = family.members();
                        return new ItemStack(members.get(Math.floorMod(variant, members.size())).get());
                    }
                }
            }
            return new ItemStack(block);
        }
    }

    /** A stack of {@code block}'s item for 1.7 metadata {@code variant} (wrapped into the family's range). */
    public static ItemStack stackOf(Block block, int variant) {
        return Families.stackOf(block, variant);
    }

    /** A stack of this block's member for {@code variant}. */
    public ItemStack of(int variant) {
        return stackOf(getBlock(), variant);
    }

    /** This member's 1.7 metadata. */
    public int variant() {
        return variant;
    }

    /** As {@link #variant()}; the stack is not consulted. */
    public int variant(ItemStack stack) {
        return variant;
    }

    @Override
    public int legacyMeta() {
        return variant;
    }

    @Override
    public Component getName(ItemStack stack) {
        return name.apply(variant);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> lines,
                                         TooltipFlag flag) {
        if (tooltip != null) lines.addAll(tooltip.apply(variant));
    }
}
