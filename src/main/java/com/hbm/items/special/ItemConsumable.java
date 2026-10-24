package com.hbm.items.special;

import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.ConsumableTreatment;
import com.hbm.handler.ConsumableTreatment.Context;
import com.hbm.handler.ConsumableTreatment.Trigger;
import com.hbm.handler.ConsumableTreatments;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.armor.JetpackFueledBase;
import com.hbm.sound.ModSounds;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Right-click and melee-injection consumables (1.7 syringes, med bag, CBT device, stopper, jetpack tank). What each
 * does is its {@code hbm:consumable_treatment}; the syringes also run it on the entity they hit.
 */
public final class ItemConsumable extends CompatItem implements ConsumableTreatments.Handled {

    private final String id;

    public ItemConsumable(Item.Properties properties, String id) {
        super(properties);
        this.id = id;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel serverLevel) {
            if (id.equals("jetpack_tank")) refuelJetpack(serverLevel, player, hand);
            ConsumableTreatment treatment = ConsumableTreatments.of(this);
            if (treatment != null && treatment.has(Trigger.USE)) apply(treatment, serverLevel, player, player, hand);
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected boolean compatHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        ConsumableTreatment treatment = ConsumableTreatments.of(this);
        if (treatment != null && treatment.has(Trigger.HIT) && attacker instanceof Player player
                && target.level() instanceof ServerLevel level) {
            apply(treatment, level, player, target, InteractionHand.MAIN_HAND);
        }
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return id.equals("syringe_awesome") || super.isFoil(stack);
    }

    private static void apply(ConsumableTreatment treatment, ServerLevel level, Player user, LivingEntity target,
                              InteractionHand hand) {
        if (!treatment.blocked(target)) treatment.run(Context.use(level, user, target, hand));
    }

    /** 1.7: fills a kerosene jetpack by 1000 mB, only when that changes its fuel; not data, it edits the jetpack. */
    private static void refuelJetpack(ServerLevel level, Player user, InteractionHand hand) {
        ItemStack chest = user.getItemBySlot(EquipmentSlot.CHEST);
        // 1.7: a jetpack module inside the chestplate is filled the same way
        ItemStack jetpack = chest.getItem() instanceof JetpackFueledBase || !ArmorModHandler.hasMods(chest)
                ? chest : ArmorModHandler.pryMod(chest, ArmorModHandler.plate_only);
        if (jetpack.getItem() instanceof JetpackFueledBase jetItem && jetItem.fuel == Fluids.KEROSENE) {
            int oldFuel = JetpackFueledBase.getFuel(jetpack);
            int newFuel = Math.min(oldFuel + 1000, jetItem.maxFuel);
            if (oldFuel != newFuel) {
                JetpackFueledBase.setFuel(jetpack, newFuel);
                if (jetpack != chest) ArmorModHandler.applyMod(chest, jetpack);
                level.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.JETPACK_TANK.get(),
                        SoundSource.PLAYERS, 1.0F, 1.0F);
                user.getItemInHand(hand).shrink(1);
            }
        }
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        switch (id) {
            case "syringe_antidote" -> tooltip.add(Component.literal("Removes all potion effects"));
            case "syringe_awesome" -> tooltip.add(Component.literal("§2Every good effect for 50 seconds"));
            case "syringe_metal_stimpak" -> tooltip.add(Component.literal("§aHeals 2.5 hearts"));
            case "syringe_metal_medx" -> tooltip.add(Component.literal("§dResistance III for 4 minutes"));
            case "syringe_metal_psycho" -> {
                tooltip.add(Component.literal("§dResistance I for 2 minutes"));
                tooltip.add(Component.literal("§6Strength I for 2 minutes"));
            }
            case "syringe_metal_super" -> {
                tooltip.add(Component.literal("§aHeals 25 hearts"));
                tooltip.add(Component.literal("§bSlowness I for 10 seconds"));
            }
            case "syringe_poison" -> tooltip.add(Component.literal("Deadly"));
            case "syringe_taint" -> {
                tooltip.add(Component.literal("§5Tainted I for 60 seconds"));
                tooltip.add(Component.literal("§eNausea I for 5 seconds"));
                tooltip.add(Component.literal("Cloud damage + taint = ghoulified effect"));
            }
            case "med_bag" -> {
                tooltip.add(Component.literal("§aFull heal, regardless of max health"));
                tooltip.add(Component.literal("Removes negative effects"));
            }
            case "syringe_mkunicorn" -> tooltip.add(Component.literal("§c?"));
            default -> {
            }
        }
    }
}
