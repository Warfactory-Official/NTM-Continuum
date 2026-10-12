package com.hbm.hazard.type;

import java.util.List;

import com.hbm.hazard.helper.HazardHelper;
import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HazardTypeRadiation implements IHazardType {

    /**
     * Radiation a stack carries on top of its hazard entries (H-77): per stack, not per item, added after the modifiers
     * and the stack count when a RADIATION entry is applied, and to the level a RADIATION tooltip line shows. The
     * listeners' answers are summed. Plain HBM registers none; NTM: Space adds hbm-space's neutron activation, which
     * hbm-space's {@code HazardTypeRadiation} read from the stack's {@code ntmNeutron} tag at exactly these two points.
     */
    public static final dev.golem.core.hook.Hook<java.util.function.ToDoubleFunction<ItemStack>> STACK_RADIATION =
            dev.golem.core.hook.Hook.notifying();

    /** The sum of {@link #STACK_RADIATION} for {@code stack}; 0 with no listener. */
    public static double stackRadiation(final ItemStack stack) {
        if (STACK_RADIATION.isEmpty()) return 0D;
        double extra = 0D;
        for (java.util.function.ToDoubleFunction<ItemStack> listener : STACK_RADIATION) extra += listener.applyAsDouble(stack);
        return extra;
    }

    @Override
    public void onUpdate(final LivingEntity target, double level, final ItemStack stack) {

        final boolean reacher = HazardHelper.isHoldingReacher(target);

        level *= stack.getCount();
        level += stackRadiation(stack);

        if (level > 0) {
            double rad = level / 20D;

            if (reacher) {
                // Reworked radiation function: sqrt(x+1/(x+2)^2)-1/(x+2)
                rad = Math.sqrt(rad + 1D / ((rad + 2D) * (rad + 2D))) - 1D / (rad + 2D);
            }

            ContaminationUtil.contaminate(target, HazardType.RADIATION, ContaminationType.CREATIVE, rad * hazardRate);
        }
    }

    @Override
    public void updateEntity(final ItemEntity item, final double level) {
    }

    @Override
    public void addHazardInformation(final Player player, final List<Component> list, double level, final ItemStack stack, final List<IHazardModifier> modifiers) {

        level = IHazardModifier.evalAllModifiers(stack, player, level, modifiers);
        level += stackRadiation(stack);
        if (level == 0) return;
        list.add(Component.literal("[").append(Component.translatable("trait.radioactive")).append(Component.literal("]")).withStyle(ChatFormatting.GREEN));
        list.add(Component.literal(" " + round3(getNewValue(level))).append(suffix(level)).append(Component.literal(" ")).append(Component.translatable("desc.rads")).withStyle(ChatFormatting.YELLOW));

        if (stack.getCount() > 1) {
            double stackRad = level * stack.getCount();
            list.add(Component.literal(" ").append(Component.translatable("desc.stack")).append(Component.literal(" " + round3(getNewValue(stackRad))))
                    .append(suffix(stackRad)).append(Component.literal(" ")).append(Component.translatable("desc.rads")).withStyle(ChatFormatting.YELLOW));
        }
    }

    private static String round3(double v) {
        return String.valueOf(Math.round(v * 1000D) / 1000D);
    }

    public static double getNewValue(double radiation) {
        if (radiation < 1000000) {
            return radiation;
        } else if (radiation < 1000000000) {
            return radiation * 0.000001D;
        } else {
            return radiation * 0.000000001D;
        }
    }

    private static Component suffix(double radiation) {
        if (radiation < 1000000) {
            return Component.empty();
        } else if (radiation < 1000000000) {
            return Component.translatable("desc.mil");
        } else {
            return Component.translatable("desc.bil");
        }
    }
}
