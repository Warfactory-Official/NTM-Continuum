package com.hbm.hazard.type;

import dev.golem.core.hook.Hook;
import java.util.List;

import com.hbm.config.GeneralConfig;
import com.hbm.hazard.helper.HazardHelper;
import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import java.util.function.ToDoubleFunction;
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
     * listeners' answers are summed. Plain HBM registers none. Per-item component contributions belong in
     * {@link com.hbm.hazard.transformer.HazardTransformerRadiationNBT#COMPONENT_RADIATION} instead.
     */
    public static final Hook<ToDoubleFunction<ItemStack>> STACK_RADIATION =
            Hook.notifying();

    /** The sum of {@link #STACK_RADIATION} for {@code stack}; 0 with no listener. */
    public static double stackRadiation(final ItemStack stack) {
        if (STACK_RADIATION.isEmpty()) return 0D;
        double extra = 0D;
        for (ToDoubleFunction<ItemStack> listener : STACK_RADIATION) extra += listener.applyAsDouble(stack);
        return extra;
    }

    @Override
    public void onUpdate(final LivingEntity target, double level, final ItemStack stack) {

        final boolean reacher = HazardHelper.hasReacher(target);

        level *= stack.getCount();
        level += stackRadiation(stack);

        if (level > 0) {
            double rad = level / 20D;

            if (GeneralConfig.enable528 && reacher) {
                // 528: x / distance^2
                rad = rad / 49D;
            } else if (reacher) {
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
        if (level < 1e-5) return;
        list.add(Component.literal("[").append(Component.translatable("trait.radioactive")).append(Component.literal("]")).withStyle(ChatFormatting.GREEN));
        // 1.7 builds the text from a float level, floored to three places, with no space before the unit
        float rad = (float) level;
        list.add(Component.translatable("desc.hbm.radiation.rate", Math.floor(rad * 1000) / 1000)
                          .withStyle(ChatFormatting.YELLOW));

        if (stack.getCount() > 1) {
            list.add(Component.translatable("desc.hbm.radiation.stack_rate",
                                      Component.translatable("desc.hbm.radiation.rate", Math.floor(rad * 1000 * stack.getCount()) / 1000))
                              .withStyle(ChatFormatting.YELLOW));
        }
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
}
