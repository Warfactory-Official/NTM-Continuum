package com.hbm.items.machine;

import com.hbm.items.ModDataComponents;
import com.hbm.items.special.ItemNuclearWaste;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * RBMK recycling pellet. 1.7's ten damage values (0..4 depletion, 5..9 the same stage xenon-poisoned) are the typed
 * {@code hbm:rbmk_pellet} component here ({@link Stage}, ntm-next's shape); {@link #getVariant}/{@link #of(int)} still
 * speak the 1.7 number at run time for the recipe tables written against it.
 */
public class ItemRBMKPellet extends ItemNuclearWaste implements com.hbm.items.ModelState {

    /** ntm-next {@code ItemRBMKPellet.Stage}: depletion 0..4 and the xenon flag, saved as two named fields. */
    public record Stage(int depletion, boolean xenon) {
        public static final Stage FRESH = new Stage(0, false);
        public static final com.mojang.serialization.Codec<Stage> CODEC =
                com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
                        com.mojang.serialization.Codec.intRange(0, 4).optionalFieldOf("depletion", 0).forGetter(Stage::depletion),
                        com.mojang.serialization.Codec.BOOL.optionalFieldOf("xenon", false).forGetter(Stage::xenon)
                ).apply(i, Stage::new));

        /** The stage 1.7 metadata {@code meta} named ({@code abs(meta) % 10}). */
        public static Stage ofLegacy(int meta) {
            int m = rectify(meta);
            return new Stage(m % 5, m >= 5);
        }

        /** The 1.7 metadata of this stage. */
        public int legacyMeta() {
            return depletion + (xenon ? 5 : 0);
        }
    }
    public final String fullName;
    private boolean xenon = true;
    public ItemRBMKPellet(Properties properties, String fullName) { super(properties); this.fullName = fullName; }
    public static int rectify(int meta) { return Math.abs(meta) % 10; }
    public static boolean hasXenon(int meta) { return rectify(meta) >= 5; }
    public ItemRBMKPellet disableXenon() { xenon = false; return this; }
    public boolean isXenonEnabled() { return xenon; }
    public static Stage stage(ItemStack stack) { return ModDataComponents.RBMK_PELLET.getOr(stack, Stage.FRESH); }
    /** The stack's stage as 1.7's metadata (0..9); a run-time value. */
    public int getVariant(ItemStack stack) { return stage(stack).legacyMeta(); }
    /** A pellet at 1.7 metadata {@code meta}; the fresh stage carries no component, as 1.7's damage 0 carried none. */
    public ItemStack of(int meta) {
        ItemStack stack = new ItemStack(this);
        Stage stage = Stage.ofLegacy(meta);
        if (!stage.equals(Stage.FRESH)) ModDataComponents.RBMK_PELLET.set(stack, stage);
        return stack;
    }

    @Override
    public int modelState(ItemStack stack) { return getVariant(stack); }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.literal(fullName).withStyle(ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("trait.rbmk.pellet.recycling").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        int meta = rectify(getVariant(stack));
        switch (meta % 5) {
            case 0 -> tooltip.add(Component.translatable("trait.rbmk.pellet.brandNew").withStyle(ChatFormatting.GOLD));
            case 1 -> tooltip.add(Component.translatable("trait.rbmk.pellet.barelyDepleted").withStyle(ChatFormatting.YELLOW));
            case 2 -> tooltip.add(Component.translatable("trait.rbmk.pellet.moderatelyDepleted").withStyle(ChatFormatting.GREEN));
            case 3 -> tooltip.add(Component.translatable("trait.rbmk.pellet.highlyDepleted").withStyle(ChatFormatting.DARK_GREEN));
            case 4 -> tooltip.add(Component.translatable("trait.rbmk.pellet.fullyDepleted").withStyle(ChatFormatting.DARK_GRAY));
            default -> throw new IllegalStateException("Unexpected RBMK pellet depletion stage");
        }
        if (hasXenon(meta)) tooltip.add(Component.translatable("trait.rbmk.pellet.xenon").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
