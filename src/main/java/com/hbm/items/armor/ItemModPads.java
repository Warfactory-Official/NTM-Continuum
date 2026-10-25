package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.DamageEvents;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.7 {@code ItemModPads}: rubber (-50%), slime (-75%) and static (-25%) fall-damage pads for boots.
 * Static pads also trickle-charge a worn electric suit while the wearer walks.
 */
public class ItemModPads extends ItemArmorMod {

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST,
            EquipmentSlot.HEAD};

    private static final Map<Player, Float> LAST_MOVE = new WeakHashMap<>();

    private final float damageMod;
    private final boolean charging;

    public ItemModPads(float damageMod, boolean charging, Item.Properties properties) {
        super(ArmorModHandler.boots_only, false, false, false, true, properties);
        this.damageMod = damageMod;
        this.charging = charging;
    }

    public float damageMod() {
        return damageMod;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        if (damageMod != 1F)
            tooltip.add(coloured(ChatFormatting.RED, "-" + Math.round((1F - damageMod) * 100) + "% fall damage"));
        if (charging)
            tooltip.add(coloured(ChatFormatting.DARK_PURPLE, "Passively charges electric armor when walking"));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        String pct = "-" + Math.round((1F - damageMod) * 100) + "% fall dmg";
        tooltip.add(modLine(ChatFormatting.DARK_PURPLE, stack, charging ? " (" + pct + " / passive charge)" : " (" + pct + ")"));
    }

    @Override
    public void modDamage(DamageEvents.DamageEvent event, ItemStack armor) {
        if (event.source().is(DamageTypes.FALL)) event.setAmount(event.amount() * damageMod);
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (entity.level().isClientSide() || !charging || !(entity instanceof Player player)) return;
        // 1.7 distanceWalkedModified != prevDistanceWalkedModified: the wearer moved this tick. 26.1 dropped
        // the previous-tick copy, so the last seen moveDist is remembered here instead.
        Float last = LAST_MOVE.put(player, player.moveDist);
        if (last == null || last == player.moveDist) return;
        if (!ArmorFSB.hasFSBArmorIgnoreCharge(player)) return;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof ArmorFSBPowered powered) {
                long charge = powered.drain / 2;
                if (charge == 0) charge = powered.consumption / 40;
                long power = Math.min(powered.getMaxCharge(stack), powered.getCharge(stack) + charge);
                powered.setCharge(stack, power);
            }
        }
    }
}
